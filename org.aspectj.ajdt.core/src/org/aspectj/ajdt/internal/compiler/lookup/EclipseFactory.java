/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 *     Mik Kersten	2004-07-26 extended to allow overloading of 
 * 					hierarchy builder
 * ******************************************************************/

package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.ajdt.internal.compiler.ast.AspectDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.AstUtil;
import org.aspectj.ajdt.internal.core.builder.AjBuildManager;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.ISourceLocation;
import org.aspectj.org.eclipse.jdt.core.Flags;
import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.*;
import org.aspectj.weaver.UnresolvedType.TypeKind;
import org.aspectj.weaver.patterns.DeclareAnnotation;
import org.aspectj.weaver.patterns.DeclareParents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Jim Hugunin
 */
public final class EclipseFactory {
  public static boolean DEBUG = false;
  public static int debug_mungerCount = -1;

  /**
   * Some type variables refer to themselves recursively, this enables us to avoid recursion problems.
   */
  @NotNull
  private static final Map<TypeVariableBinding, UnresolvedType> typeVariableBindingsInProgress = new HashMap<>();

  @Nullable
  private final AjBuildManager buildManager;
  @NotNull
  private final LookupEnvironment lookupEnvironment;
  private final boolean xSerializableAspects;
  @NotNull
  private final World world;
  @NotNull
  public final PushinCollector pushinCollector;
  @Nullable
  public List<ConcreteTypeMunger> finishedTypeMungers = null;

  // We can get clashes if we don't treat raw types differently - we end up looking
  // up a raw and getting the generic type (pr115788)
  @NotNull
  private final Map/* UnresolvedType, TypeBinding */typexToBinding = new HashMap();
  @NotNull
  private final Map/* UnresolvedType, TypeBinding */rawTypeXToBinding = new HashMap();

  /**
   * Conversion from a methodbinding (eclipse) to a resolvedmember (aspectj) is now done in the scope of some type variables.
   * Before converting the parts of a methodbinding (params, return type) we store the type variables in this structure, then
   * should any component of the method binding refer to them, we grab them from the map.
   */
  @NotNull
  private final Map typeVariablesForThisMember = new HashMap();

  /**
   * This is a map from typevariablebindings (eclipsey things) to the names the user originally specified in their ITD. For
   * example if the target is 'interface I<N extends Number> {}' and the ITD was 'public void I<X>.m(List<X> lxs) {}' then this
   * map would contain a pointer from the eclipse type 'N extends Number' to the letter 'X'.
   */
  @Nullable
  private Map typeVariablesForAliasRecovery;

  // When converting a parameterized type from our world to the eclipse world, these get set so that
  // resolution of the type parameters may known in what context it is occurring (pr114744)
  @Nullable
  private ReferenceBinding baseTypeForParameterizedType;
  private int indexOfTypeParameterBeingConverted;
  @Nullable
  private ReferenceBinding currentType = null;

  // only accessed through private methods in this class. Ensures all type variables we encounter
  // map back to the same type binding - this is important later when Eclipse code is processing
  // a methodbinding trying to come up with possible bindings for the type variables.
  // key is currently the name of the type variable...is that ok?
  @NotNull
  private final Map typeVariableToTypeBinding = new HashMap();

  // XXX currently unused
  // private Map/*TypeBinding, ResolvedType*/ bindingToResolvedTypeX = new HashMap();

  @NotNull
  public static EclipseFactory fromLookupEnvironment(@NotNull LookupEnvironment env) {
    final AjLookupEnvironment aenv = (AjLookupEnvironment) env;
    return aenv.factory;
  }

  @NotNull
  public static EclipseFactory fromScopeLookupEnvironment(@NotNull Scope scope) {
    return fromLookupEnvironment(AstUtil.getCompilationUnitScope(scope).environment);
  }

  @NotNull
  public static String getName(@NotNull TypeBinding binding) {
    if (binding instanceof TypeVariableBinding) {
      // The first bound may be null - so default to object?
      final TypeVariableBinding tvb = (TypeVariableBinding) binding;
      if (tvb.firstBound != null) {
        return getName(tvb.firstBound);
      } else {
        return getName(tvb.superclass);
      }
    }

    if (binding instanceof ReferenceBinding) {
      return new String(CharOperation.concatWith(((ReferenceBinding) binding).compoundName, '.'));
    }

    final String packageName = new String(binding.qualifiedPackageName());
    String className = new String(binding.qualifiedSourceName()).replace('.', '$');
    if (packageName.length() > 0) {
      className = packageName + "." + className;
    }
    // XXX doesn't handle arrays correctly (or primitives?)
    return new String(className);
  }

  @NotNull
  public static ASTNode astForLocation(@NotNull IHasPosition location) {
    return new EmptyStatement(location.getStart(), location.getEnd());
  }

  public EclipseFactory(@NotNull LookupEnvironment lookupEnvironment, @NotNull AjBuildManager buildManager) {
    this.lookupEnvironment = lookupEnvironment;
    this.buildManager = buildManager;
    this.world = buildManager.getWorld();
    this.pushinCollector = PushinCollector.createInstance(this.world);
    this.xSerializableAspects = buildManager.buildConfig.isXserializableAspects();
  }

  public EclipseFactory(@NotNull LookupEnvironment lookupEnvironment, @NotNull World world, boolean xSer) {
    this.lookupEnvironment = lookupEnvironment;
    this.world = world;
    this.xSerializableAspects = xSer;
    this.pushinCollector = PushinCollector.createInstance(this.world);
    this.buildManager = null;
  }

  @NotNull
  public LookupEnvironment getLookupEnvironment() {
    return this.lookupEnvironment;
  }

  @NotNull
  public World getWorld() {
    return world;
  }

  public void showMessage(@NotNull Kind kind, @NotNull String message, @Nullable ISourceLocation loc1, @Nullable ISourceLocation loc2) {
    getWorld().showMessage(kind, message, loc1, loc2);
  }

  @NotNull
  public ResolvedType fromEclipse(@Nullable ReferenceBinding binding) {
    if (binding == null) {
      return ResolvedType.MISSING;
    }
    // ??? this seems terribly inefficient
    // System.err.println("resolving: " + binding.getClass() + ", name = " + getName(binding));
    final ResolvedType ret = getWorld().resolve(fromBinding(binding));
    // System.err.println("      got: " + ret);
    return ret;
  }

  @NotNull
  public ResolvedType fromTypeBindingToRTX(@Nullable TypeBinding tb) {
    if (tb == null) {
      return ResolvedType.MISSING;
    }
    final ResolvedType ret = getWorld().resolve(fromBinding(tb));
    return ret;
  }

  @NotNull
  public ResolvedType[] fromEclipse(@Nullable ReferenceBinding[] bindings) {
    if (bindings == null) {
      return ResolvedType.NONE;
    }
    final int len = bindings.length;
    final ResolvedType[] ret = new ResolvedType[len];
    for (int i = 0; i < len; i++) {
      ret[i] = fromEclipse(bindings[i]);
    }
    return ret;
  }

  /**
   * Some generics notes:
   * <p/>
   * Andy 6-May-05 We were having trouble with parameterized types in a couple of places - due to TypeVariableBindings. When we
   * see a TypeVariableBinding now we default to either the firstBound if it is specified or java.lang.Object. Not sure when/if
   * this gets us unstuck? It does mean we forget that it is a type variable when going back the other way from the UnresolvedType
   * and that would seem a bad thing - but I've yet to see the reason we need to remember the type variable. Adrian 10-July-05
   * When we forget it's a type variable we come unstuck when getting the declared members of a parameterized type - since we
   * don't know it's a type variable we can't replace it with the type parameter.
   */
  // ??? going back and forth between strings and bindings is a waste of cycles
  @NotNull
  public UnresolvedType fromBinding(@Nullable TypeBinding binding) {
    if (binding instanceof HelperInterfaceBinding) {
      return ((HelperInterfaceBinding) binding).getTypeX();
    }
    if (binding == null || binding.qualifiedSourceName() == null) {
      return ResolvedType.MISSING;
    }
    // first piece of generics support!
    if (binding instanceof TypeVariableBinding) {
      final TypeVariableBinding tb = (TypeVariableBinding) binding;
      final UnresolvedTypeVariableReferenceType utvrt = (UnresolvedTypeVariableReferenceType) fromTypeVariableBinding(tb);
      return utvrt;
    }

    // handle arrays since the component type may need special treatment too...
    if (binding instanceof ArrayBinding) {
      final ArrayBinding aBinding = (ArrayBinding) binding;
      final UnresolvedType componentType = fromBinding(aBinding.leafComponentType);
      return UnresolvedType.makeArray(componentType, aBinding.dimensions);
    }

    if (binding instanceof WildcardBinding) {
      final WildcardBinding eWB = (WildcardBinding) binding;
      // Repair the bound
      // e.g. If the bound for the wildcard is a typevariable, e.g. '? extends E' then
      // the type variable in the unresolvedtype will be correct only in name. In that
      // case let's set it correctly based on the one in the eclipse WildcardBinding
      UnresolvedType theBound = null;
      if (eWB.bound instanceof TypeVariableBinding) {
        theBound = fromTypeVariableBinding((TypeVariableBinding) eWB.bound);
      } else {
        theBound = fromBinding(eWB.bound);
      }
      // if (eWB.boundKind == WildCard.SUPER) {
      //
      // }
      final WildcardedUnresolvedType theType = (WildcardedUnresolvedType) TypeFactory.createTypeFromSignature(CharOperation
          .charToString(eWB.genericTypeSignature()));
      // if (theType.isGenericWildcard() && theType.isSuper()) theType.setLowerBound(theBound);
      // if (theType.isGenericWildcard() && theType.isExtends()) theType.setUpperBound(theBound);
      return theType;
    }

    if (binding instanceof ParameterizedTypeBinding) {
      if (binding instanceof RawTypeBinding) {
        // special case where no parameters are specified!
        return UnresolvedType.forRawTypeName(getName(binding));
      }
      final ParameterizedTypeBinding ptb = (ParameterizedTypeBinding) binding;

      UnresolvedType[] arguments = null;

      if (ptb.arguments != null) { // null can mean this is an inner type of a Parameterized Type with no bounds of its own
        // (pr100227)
        arguments = new UnresolvedType[ptb.arguments.length];
        for (int i = 0; i < arguments.length; i++) {
          arguments[i] = fromBinding(ptb.arguments[i]);
        }
      }

      String baseTypeSignature = null;

      final ResolvedType baseType = getWorld().resolve(UnresolvedType.forName(getName(binding)), true);
      if (!baseType.isMissing()) {
        // can legitimately be missing if a bound refers to a type we haven't added to the world yet...
        // pr168044 - sometimes (whilst resolving types) we are working with 'half finished' types and so (for example) the
        // underlying generic type for a raw type hasnt been set yet
        // if (!baseType.isGenericType() && arguments!=null) baseType = baseType.getGenericType();
// pr384398 - secondary testcase in 1.7.1 tests - this needs work as this code
// currently discards the parameterization on the outer type, which is important info
//				ReferenceBinding enclosingTypeBinding = ptb.enclosingType();
//				if (enclosingTypeBinding!=null) {
//					UnresolvedType ttt = fromBinding(enclosingTypeBinding);
//					baseTypeSignature = ttt.getSignature();
//					baseTypeSignature= baseTypeSignature.substring(0,baseTypeSignature.length()-1);
//					baseTypeSignature = baseTypeSignature + "."+new String(ptb.sourceName)+";";
//				} else {			
        baseTypeSignature = baseType.getErasureSignature();
//				}
      } else {
        baseTypeSignature = UnresolvedType.forName(getName(binding)).getSignature();
      }

      // Create an unresolved parameterized type. We can't create a resolved one as the
      // act of resolution here may cause recursion problems since the parameters may
      // be type variables that we haven't fixed up yet.
      if (arguments == null) {
        arguments = new UnresolvedType[0];
        // for pr384398
        if (!hasAnyArguments(ptb)) {
          return UnresolvedType.forRawTypeName(getName(binding));
        }
      }
      // StringBuffer parameterizedSig = new StringBuffer();
      // parameterizedSig.append(ResolvedType.PARAMETERIZED_TYPE_IDENTIFIER);
      //
      // // String parameterizedSig = new
      // StringBuffer().append(ResolvedType.PARAMETERIZED_TYPE_IDENTIFIER).append(CharOperation
      // .charToString(binding.genericTypeSignature()).substring(1)).toString();
      // return TypeFactory.createUnresolvedParameterizedType(parameterizedSig,baseTypeSignature,arguments);
      return TypeFactory.createUnresolvedParameterizedType(baseTypeSignature, arguments);
    }

    // Convert the source type binding for a generic type into a generic UnresolvedType
    // notice we can easily determine the type variables from the eclipse object
    // and we can recover the generic signature from it too - so we pass those
    // to the forGenericType() method.
    if (binding.isGenericType() && !binding.isParameterizedType() && !binding.isRawType()) {
      final TypeVariableBinding[] tvbs = binding.typeVariables();
      final TypeVariable[] tVars = new TypeVariable[tvbs.length];
      for (int i = 0; i < tvbs.length; i++) {
        final TypeVariableBinding eclipseV = tvbs[i];
        tVars[i] = ((TypeVariableReference) fromTypeVariableBinding(eclipseV)).getTypeVariable();
      }
      // TODO asc generics - temporary guard....
      if (!(binding instanceof SourceTypeBinding)) {
        throw new RuntimeException("Cant get the generic sig for " + binding.debugName());
      }
      return UnresolvedType.forGenericType(getName(binding), tVars,
          CharOperation.charToString(((SourceTypeBinding) binding).genericSignature()));
    }

    // LocalTypeBinding have a name $Local$, we can get the real name by using the signature....
    if (binding instanceof LocalTypeBinding) {
      final LocalTypeBinding ltb = (LocalTypeBinding) binding;
      if (ltb.constantPoolName() != null && ltb.constantPoolName().length > 0) {
        return UnresolvedType.forSignature(new String(binding.signature()));
      } else {
        // we're reporting a problem and don't have a resolved name for an
        // anonymous local type yet, report the issue on the enclosing type
        return UnresolvedType.forSignature(new String(ltb.enclosingType.signature()));
      }
    }

    // was: UnresolvedType.forName(getName(binding));
    final UnresolvedType ut = UnresolvedType.forSignature(new String(binding.signature()));
    return ut;
  }

  @NotNull
  public UnresolvedType[] fromBindings(@Nullable TypeBinding[] bindings) {
    if (bindings == null) {
      return UnresolvedType.NONE;
    }
    final int len = bindings.length;
    final UnresolvedType[] ret = new UnresolvedType[len];
    for (int i = 0; i < len; i++) {
      ret[i] = fromBinding(bindings[i]);
    }
    return ret;
  }

  public List<DeclareParents> getDeclareParents() {
    return getWorld().getDeclareParents();
  }

  public List<DeclareAnnotation> getDeclareAnnotationOnTypes() {
    return getWorld().getDeclareAnnotationOnTypes();
  }

  public List<DeclareAnnotation> getDeclareAnnotationOnFields() {
    return getWorld().getDeclareAnnotationOnFields();
  }

  public List<DeclareAnnotation> getDeclareAnnotationOnMethods() {
    return getWorld().getDeclareAnnotationOnMethods();
  }

  public boolean areTypeMungersFinished() {
    return finishedTypeMungers != null;
  }

  public void finishTypeMungers() {
    // make sure that type mungers are
    final List<ConcreteTypeMunger> ret = new ArrayList<ConcreteTypeMunger>();
    final List<ConcreteTypeMunger> baseTypeMungers = getWorld().getCrosscuttingMembersSet().getTypeMungers();

    // XXX by Andy: why do we mix up the mungers here? it means later we know about two sets
    // and the late ones are a subset of the complete set? (see pr114436)
    // XXX by Andy removed this line finally, see pr141956
    // baseTypeMungers.addAll(getWorld().getCrosscuttingMembersSet().getLateTypeMungers());
    debug_mungerCount = baseTypeMungers.size();

    for (ConcreteTypeMunger munger : baseTypeMungers) {
      final EclipseTypeMunger etm = makeEclipseTypeMunger(munger);
      if (etm != null) {
        if (munger.getMunger().getKind() == ResolvedTypeMunger.InnerClass) {
          ret.add(0, etm);
        } else {
          ret.add(etm);
        }
      }
    }
    for (ConcreteTypeMunger ctm : ret) {
      final ResolvedMember rm = ctm.getSignature();
      if (rm != null) {
        rm.resolve(this.getWorld());
      }
    }
    finishedTypeMungers = ret;
  }

  @Nullable
  public EclipseTypeMunger makeEclipseTypeMunger(@NotNull ConcreteTypeMunger concrete) {
    // System.err.println("make munger: " + concrete);
    // !!! can't do this if we want incremental to work right
    // if (concrete instanceof EclipseTypeMunger) return (EclipseTypeMunger)concrete;
    // System.err.println("   was not eclipse");

    if (concrete.getMunger() != null && EclipseTypeMunger.supportsKind(concrete.getMunger().getKind())) {
      AbstractMethodDeclaration method = null;
      if (concrete instanceof EclipseTypeMunger) {
        method = ((EclipseTypeMunger) concrete).getSourceMethod();
      }
      final EclipseTypeMunger ret = new EclipseTypeMunger(this, concrete.getMunger(), concrete.getAspectType(), method);
      if (ret.getSourceLocation() == null) {
        ret.setSourceLocation(concrete.getSourceLocation());
      }
      return ret;
    } else {
      return null;
    }
  }

  @Nullable
  public List<ConcreteTypeMunger> getTypeMungers() {
    // ??? assert finishedTypeMungers != null
    return finishedTypeMungers;
  }

  @NotNull
  public ResolvedMemberImpl makeResolvedMember(@NotNull MethodBinding binding) {
    return makeResolvedMember(binding, binding.declaringClass);
  }

  @NotNull
  public ResolvedMemberImpl makeResolvedMember(@NotNull MethodBinding binding, @NotNull Shadow.Kind shadowKind) {
    MemberKind memberKind = binding.isConstructor() ? Member.CONSTRUCTOR : Member.METHOD;
    if (shadowKind == Shadow.AdviceExecution) {
      memberKind = Member.ADVICE;
    }
    return makeResolvedMember(binding, binding.declaringClass, memberKind);
  }

  /**
   * Construct a resolvedmember from a methodbinding. The supplied map tells us about any typevariablebindings that replaced
   * typevariables whilst the compiler was resolving types - this only happens if it is a generic itd that shares type variables
   * with its target type.
   */
  @Nullable
  public ResolvedMemberImpl makeResolvedMemberForITD(@NotNull MethodBinding binding, @NotNull TypeBinding declaringType,
                                                     @Nullable Map /*
                                                       * TypeVariableBinding >
																											 * original alias name
																											 */recoveryAliases) {
    ResolvedMemberImpl result = null;
    try {
      typeVariablesForAliasRecovery = recoveryAliases;
      result = makeResolvedMember(binding, declaringType);
    } finally {
      typeVariablesForAliasRecovery = null;
    }
    return result;
  }

  @NotNull
  public ResolvedMemberImpl makeResolvedMember(@NotNull MethodBinding binding, @NotNull TypeBinding declaringType) {
    return makeResolvedMember(binding, declaringType, binding.isConstructor() ? Member.CONSTRUCTOR : Member.METHOD);
  }

  @NotNull
  public ResolvedMemberImpl makeResolvedMember(@NotNull MethodBinding binding, @NotNull TypeBinding declaringType, @NotNull MemberKind memberKind) {
    // System.err.println("member for: " + binding + ", " + new String(binding.declaringClass.sourceName));

    // Convert the type variables and store them
    UnresolvedType[] ajTypeRefs = null;
    typeVariablesForThisMember.clear();
    // This is the set of type variables available whilst building the resolved member...
    if (binding.typeVariables != null) {
      ajTypeRefs = new UnresolvedType[binding.typeVariables.length];
      for (int i = 0; i < binding.typeVariables.length; i++) {
        ajTypeRefs[i] = fromBinding(binding.typeVariables[i]);
        typeVariablesForThisMember.put(new String(binding.typeVariables[i].sourceName),/*
                                                 * new
																								 * Integer(binding.typeVariables[
																								 * i].rank),
																								 */ajTypeRefs[i]);
      }
    }

    // AMC these next two lines shouldn't be needed once we sort out generic types properly in the world map
    ResolvedType realDeclaringType = world.resolve(fromBinding(declaringType));
    if (realDeclaringType.isRawType()) {
      realDeclaringType = realDeclaringType.getGenericType();
    }
    final ResolvedMemberImpl ret = new EclipseResolvedMember(binding, memberKind, realDeclaringType, binding.modifiers,
        fromBinding(binding.returnType), new String(binding.selector), fromBindings(binding.parameters),
        fromBindings(binding.thrownExceptions), this);
    if (binding.isVarargs()) {
      ret.setVarargsMethod();
    }
    if (ajTypeRefs != null) {
      final TypeVariable[] tVars = new TypeVariable[ajTypeRefs.length];
      for (int i = 0; i < ajTypeRefs.length; i++) {
        tVars[i] = ((TypeVariableReference) ajTypeRefs[i]).getTypeVariable();
      }
      ret.setTypeVariables(tVars);
    }
    typeVariablesForThisMember.clear();
    ret.resolve(world);
    return ret;
  }

  @NotNull
  public ResolvedMember makeResolvedMember(@NotNull FieldBinding binding) {
    return makeResolvedMember(binding, binding.declaringClass);
  }

  @NotNull
  public ResolvedMember makeResolvedMember(@NotNull FieldBinding binding, @NotNull TypeBinding receiverType) {
    // AMC these next two lines shouldn't be needed once we sort out generic types properly in the world map
    ResolvedType realDeclaringType = world.resolve(fromBinding(receiverType));
    if (realDeclaringType.isRawType()) {
      realDeclaringType = realDeclaringType.getGenericType();
    }
    final ResolvedMemberImpl ret = new EclipseResolvedMember(binding, Member.FIELD, realDeclaringType, binding.modifiers,
        world.resolve(fromBinding(binding.type)), new String(binding.name), UnresolvedType.NONE);
    return ret;
  }

  @Nullable
  public TypeBinding makeTypeBinding(@NotNull UnresolvedType typeX) {
    TypeBinding ret = null;

    // looking up type variables can get us into trouble
    if (!typeX.isTypeVariableReference() && !isParameterizedWithTypeVariables(typeX)) {
      if (typeX.isRawType()) {
        ret = (TypeBinding) rawTypeXToBinding.get(typeX);
      } else {
        ret = (TypeBinding) typexToBinding.get(typeX);
      }
    }

    if (ret == null) {
      ret = makeTypeBinding1(typeX);
      if (ret != null) {// && !(ret instanceof ProblemReferenceBinding)) {
        if (!(typeX instanceof BoundedReferenceType) && !(typeX instanceof UnresolvedTypeVariableReferenceType)) {
          if (typeX.isRawType()) {
            rawTypeXToBinding.put(typeX, ret);
          } else {
            typexToBinding.put(typeX, ret);
          }
        }
      }
    }
    return ret;
  }

  @NotNull
  public TypeBinding[] makeTypeBindings(@NotNull UnresolvedType[] types) {
    final int len = types.length;
    final TypeBinding[] ret = new TypeBinding[len];

    for (int i = 0; i < len; i++) {
      ret[i] = makeTypeBinding(types[i]);
    }
    return ret;
  }

  // field related
  @NotNull
  public FieldBinding makeFieldBinding(@NotNull NewFieldTypeMunger nftm) {
    return internalMakeFieldBinding(nftm.getSignature(), nftm.getTypeVariableAliases());
  }

  /**
   * Convert a resolvedmember into an eclipse field binding
   */
  @NotNull
  public FieldBinding makeFieldBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases) {
    return internalMakeFieldBinding(member, aliases);
  }

  /**
   * Convert a resolvedmember into an eclipse field binding
   */
  @NotNull
  public FieldBinding makeFieldBinding(@NotNull ResolvedMember member) {
    return internalMakeFieldBinding(member, null);
  }

  // OPTIMIZE tidy this up, must be able to optimize for the synthetic case, if we passed in the binding for the declaring type,
  // that would make things easier

  /**
   * Build a new Eclipse SyntheticFieldBinding for an AspectJ ResolvedMember.
   */
  @NotNull
  public SyntheticFieldBinding createSyntheticFieldBinding(@NotNull SourceTypeBinding owningType, @NotNull ResolvedMember member) {
    final SyntheticFieldBinding sfb = new SyntheticFieldBinding(member.getName().toCharArray(),
        makeTypeBinding(member.getReturnType()), member.getModifiers() | Flags.AccSynthetic, owningType,
        Constant.NotAConstant, -1); // index
    // filled in
    // later
    owningType.addSyntheticField(sfb);
    return sfb;
  }

  /**
   * Take a normal AJ member and convert it into an eclipse fieldBinding. Taking into account any aliases that it may include due
   * to being a generic itd. Any aliases are put into the typeVariableToBinding map so that they will be substituted as
   * appropriate in the returned fieldbinding.
   */
  @NotNull
  public FieldBinding internalMakeFieldBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases) {
    typeVariableToTypeBinding.clear();

    final ReferenceBinding declaringType = (ReferenceBinding) makeTypeBinding(member.getDeclaringType());

    // If there are aliases, place them in the map
    if (aliases != null && aliases.size() > 0 && declaringType.typeVariables() != null
        && declaringType.typeVariables().length != 0) {
      int i = 0;
      for (final Iterator<String> iter = aliases.iterator(); iter.hasNext(); ) {
        final String element = iter.next();
        typeVariableToTypeBinding.put(element, declaringType.typeVariables()[i++]);
      }
    }

    currentType = declaringType;

    FieldBinding fb = null;
    if (member.getName().startsWith(NameMangler.PREFIX)) {
      fb = new SyntheticFieldBinding(member.getName().toCharArray(), makeTypeBinding(member.getReturnType()),
          member.getModifiers() | Flags.AccSynthetic, currentType, Constant.NotAConstant, -1); // index filled in later
    } else {
      fb = new FieldBinding(member.getName().toCharArray(), makeTypeBinding(member.getReturnType()), member.getModifiers(),
          currentType, Constant.NotAConstant);
    }
    typeVariableToTypeBinding.clear();
    currentType = null;

    if (member.getName().startsWith(NameMangler.PREFIX)) {
      fb.modifiers |= Flags.AccSynthetic;
    }
    return fb;
  }

  // method binding related
  @NotNull
  public MethodBinding makeMethodBinding(@NotNull NewMethodTypeMunger nmtm) {
    return internalMakeMethodBinding(nmtm.getSignature(), nmtm.getTypeVariableAliases());
  }

  /**
   * Convert a resolvedmember into an eclipse method binding.
   */
  @NotNull
  public MethodBinding makeMethodBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases) {
    return internalMakeMethodBinding(member, aliases);
  }

  /**
   * Creates a method binding for a resolvedmember taking into account type variable aliases - this variant can take an
   * aliasTargetType and should be used when the alias target type cannot be retrieved from the resolvedmember.
   */
  @NotNull
  public MethodBinding makeMethodBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases, @NotNull UnresolvedType aliasTargetType) {
    return internalMakeMethodBinding(member, aliases, aliasTargetType);
  }

  /**
   * Convert a resolvedmember into an eclipse method binding.
   */
  @NotNull
  public MethodBinding makeMethodBinding(@NotNull ResolvedMember member) {
    return internalMakeMethodBinding(member, null); // there are no aliases
  }

  @NotNull
  public MethodBinding internalMakeMethodBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases) {
    return internalMakeMethodBinding(member, aliases, member.getDeclaringType());
  }

  /**
   * Take a normal AJ member and convert it into an eclipse methodBinding. Taking into account any aliases that it may include due
   * to being a generic ITD. Any aliases are put into the typeVariableToBinding map so that they will be substituted as
   * appropriate in the returned methodbinding
   */
  @NotNull
  public MethodBinding internalMakeMethodBinding(@NotNull ResolvedMember member, @Nullable List<String> aliases, @NotNull UnresolvedType aliasTargetType) {
    typeVariableToTypeBinding.clear();
    TypeVariableBinding[] tvbs = null;

    if (member.getTypeVariables() != null) {
      if (member.getTypeVariables().length == 0) {
        tvbs = Binding.NO_TYPE_VARIABLES;
      } else {
        tvbs = makeTypeVariableBindingsFromAJTypeVariables(member.getTypeVariables());
        // QQQ do we need to bother fixing up the declaring element here?
      }
    }

    final ReferenceBinding declaringType = (ReferenceBinding) makeTypeBinding(member.getDeclaringType());

    // If there are aliases, place them in the map
    if (aliases != null && aliases.size() != 0 && declaringType.typeVariables() != null
        && declaringType.typeVariables().length != 0) {
      int i = 0;
      ReferenceBinding aliasTarget = (ReferenceBinding) makeTypeBinding(aliasTargetType);
      if (aliasTarget.isRawType()) {
        aliasTarget = ((RawTypeBinding) aliasTarget).genericType();
      }
      for (final Iterator<String> iter = aliases.iterator(); iter.hasNext(); ) {
        final String element = iter.next();
        typeVariableToTypeBinding.put(element, aliasTarget.typeVariables()[i++]);
      }
    }

    currentType = declaringType;
    final MethodBinding mb = new MethodBinding(member.getModifiers(), member.getName().toCharArray(),
        makeTypeBinding(member.getReturnType()), makeTypeBindings(member.getParameterTypes()),
        makeReferenceBindings(member.getExceptions()), declaringType);

    if (tvbs != null) {
      mb.typeVariables = tvbs;
    }
    typeVariableToTypeBinding.clear();
    currentType = null;

    if (NameMangler.isSyntheticMethod(member.getName(), true)) {
      mb.modifiers |= Flags.AccSynthetic;
    }

    return mb;
  }

  @NotNull
  public MethodBinding makeMethodBindingForCall(@NotNull Member member) {
    return new MethodBinding(member.getModifiers() & ~Modifier.INTERFACE, member.getName().toCharArray(),
        makeTypeBinding(member.getReturnType()), makeTypeBindings(member.getParameterTypes()), new ReferenceBinding[0],
        (ReferenceBinding) makeTypeBinding(member.getDeclaringType()));
  }

  public void finishedCompilationUnit(@NotNull CompilationUnitDeclaration unit) {
    if ((buildManager != null) && buildManager.doGenerateModel()) {
      AjBuildManager.getAsmHierarchyBuilder().buildStructureForCompilationUnit(unit, buildManager.getStructureModel(),
          buildManager.buildConfig);
    }
  }

  public void addTypeBinding(@Nullable TypeBinding binding) {
    typexToBinding.put(fromBinding(binding), binding);
  }

  public void addTypeBindingAndStoreInWorld(@NotNull TypeBinding binding) {
    final UnresolvedType ut = fromBinding(binding);
    typexToBinding.put(ut, binding);
    world.lookupOrCreateName(ut);
  }

  @Nullable
  public Shadow makeShadow(@NotNull ASTNode location, @NotNull ReferenceContext context) {
    return EclipseShadow.makeShadow(this, location, context);
  }

  @Nullable
  public Shadow makeShadow(@NotNull ReferenceContext context) {
    return EclipseShadow.makeShadow(this, (ASTNode) context, context);
  }

  public void addSourceTypeBinding(@NotNull SourceTypeBinding binding, @NotNull CompilationUnitDeclaration unit) {
    final TypeDeclaration decl = binding.scope.referenceContext;

    // Deal with the raw/basic type to give us an entry in the world type map
    UnresolvedType unresolvedRawType = null;
    if (binding.isGenericType()) {
      unresolvedRawType = UnresolvedType.forRawTypeName(getName(binding));
    } else if (binding.isLocalType()) {
      final LocalTypeBinding ltb = (LocalTypeBinding) binding;
      if (ltb.constantPoolName() != null && ltb.constantPoolName().length > 0) {
        unresolvedRawType = UnresolvedType.forSignature(new String(binding.signature()));
      } else {
        unresolvedRawType = UnresolvedType.forName(getName(binding));
      }
    } else {
      unresolvedRawType = UnresolvedType.forName(getName(binding));
    }

    final ReferenceType resolvedRawType = getWorld().lookupOrCreateName(unresolvedRawType);

    // A type can change from simple > generic > simple across a set of compiles. We need
    // to ensure the entry in the typemap is promoted and demoted correctly. The call
    // to setGenericType() below promotes a simple to a raw. This call demotes it back
    // to simple
    // pr125405
    if (!binding.isRawType() && !binding.isGenericType() && resolvedRawType.getTypekind() == TypeKind.RAW) {
      resolvedRawType.demoteToSimpleType();
    }

    final EclipseSourceType t = new EclipseSourceType(resolvedRawType, this, binding, decl, unit);

    // For generics, go a bit further - build a typex for the generic type
    // give it the same delegate and link it to the raw type
    if (binding.isGenericType()) {
      final UnresolvedType unresolvedGenericType = fromBinding(binding); // fully aware of any generics info
      final ResolvedType resolvedGenericType = world.resolve(unresolvedGenericType, true);
      ReferenceType complexName = null;
      if (!resolvedGenericType.isMissing()) {
        complexName = (ReferenceType) resolvedGenericType;
        complexName = (ReferenceType) complexName.getGenericType();
        if (complexName == null) {
          complexName = new ReferenceType(unresolvedGenericType, world);
        }
      } else {
        complexName = new ReferenceType(unresolvedGenericType, world);
      }
      resolvedRawType.setGenericType(complexName);
      complexName.setDelegate(t);
    }

    resolvedRawType.setDelegate(t);
    if (decl instanceof AspectDeclaration) {
      ((AspectDeclaration) decl).typeX = resolvedRawType;
      ((AspectDeclaration) decl).concreteName = t;
    }

    final ReferenceBinding[] memberTypes = binding.memberTypes;
    for (int i = 0, length = memberTypes.length; i < length; i++) {
      addSourceTypeBinding((SourceTypeBinding) memberTypes[i], unit);
    }
  }

  // XXX this doesn't feel like it belongs here, but it breaks a hard dependency on
  // exposing AjBuildManager (needed by AspectDeclaration).
  public boolean isXSerializableAspects() {
    return xSerializableAspects;
  }

  @NotNull
  public ResolvedMember fromBinding(@NotNull MethodBinding binding) {
    return new ResolvedMemberImpl(Member.METHOD, fromBinding(binding.declaringClass), binding.modifiers,
        fromBinding(binding.returnType), CharOperation.charToString(binding.selector), fromBindings(binding.parameters));
  }

  @NotNull
  public TypeVariableDeclaringElement fromBinding(@NotNull Binding declaringElement) {
    if (declaringElement instanceof TypeBinding) {
      return fromBinding(((TypeBinding) declaringElement));
    } else {
      return fromBinding((MethodBinding) declaringElement);
    }
  }

  public void cleanup() {
    this.typexToBinding.clear();
    this.rawTypeXToBinding.clear();
    this.finishedTypeMungers = null;
  }

  public void minicleanup() {
    this.typexToBinding.clear();
    this.rawTypeXToBinding.clear();
  }

  public int getItdVersion() {
    return world.getItdVersion();
  }

  /**
   * Search up a parameterized type binding for any arguments at any level.
   */
  private static boolean hasAnyArguments(@NotNull ParameterizedTypeBinding ptb) {
    if (ptb.arguments != null && ptb.arguments.length > 0) {
      return true;
    }
    final ReferenceBinding enclosingType = ptb.enclosingType();
    if (enclosingType instanceof ParameterizedTypeBinding) {
      return hasAnyArguments((ParameterizedTypeBinding) enclosingType);
    }
    return false;
  }

  /**
   * Convert from the eclipse form of type variable (TypeVariableBinding) to the AspectJ form (TypeVariable).
   */
  @NotNull
  private UnresolvedType fromTypeVariableBinding(@NotNull TypeVariableBinding aTypeVariableBinding) {
    // first, check for recursive call to this method for the same tvBinding
    if (typeVariableBindingsInProgress.containsKey(aTypeVariableBinding)) {
      return (UnresolvedType) typeVariableBindingsInProgress.get(aTypeVariableBinding);
    }

    // Check if its a type variable binding that we need to recover to an alias...
    if (typeVariablesForAliasRecovery != null) {
      final String aliasname = (String) typeVariablesForAliasRecovery.get(aTypeVariableBinding);
      if (aliasname != null) {
        final UnresolvedTypeVariableReferenceType ret = new UnresolvedTypeVariableReferenceType();
        ret.setTypeVariable(new TypeVariable(aliasname));
        return ret;
      }
    }

    if (typeVariablesForThisMember.containsKey(new String(aTypeVariableBinding.sourceName))) {
      return (UnresolvedType) typeVariablesForThisMember.get(new String(aTypeVariableBinding.sourceName));
    }

    // Create the UnresolvedTypeVariableReferenceType for the type variable
    final String name = CharOperation.charToString(aTypeVariableBinding.sourceName());

    final UnresolvedTypeVariableReferenceType ret = new UnresolvedTypeVariableReferenceType();
    typeVariableBindingsInProgress.put(aTypeVariableBinding, ret);

    final TypeVariable tv = new TypeVariable(name);
    ret.setTypeVariable(tv);
    // Dont set any bounds here, you'll get in a recursive mess
    // TODO -- what about lower bounds??
    final UnresolvedType superclassType = fromBinding(aTypeVariableBinding.superclass());
    UnresolvedType[] superinterfaces = null;
    if (aTypeVariableBinding == null || aTypeVariableBinding.superInterfaces == null) {
      // sign of another bug that will be reported elsewhere
      superinterfaces = UnresolvedType.NONE;
    } else {
      superinterfaces = new UnresolvedType[aTypeVariableBinding.superInterfaces.length];
      for (int i = 0; i < superinterfaces.length; i++) {
        superinterfaces[i] = fromBinding(aTypeVariableBinding.superInterfaces[i]);
      }
    }
    tv.setSuperclass(superclassType);
    tv.setAdditionalInterfaceBounds(superinterfaces);
    tv.setRank(aTypeVariableBinding.rank);
    if (aTypeVariableBinding.declaringElement instanceof MethodBinding) {
      tv.setDeclaringElementKind(TypeVariable.METHOD);
      // tv.setDeclaringElement(fromBinding((MethodBinding)aTypeVariableBinding.declaringElement);
    } else {
      tv.setDeclaringElementKind(TypeVariable.TYPE);
      // // tv.setDeclaringElement(fromBinding(aTypeVariableBinding.declaringElement));
    }
    if (aTypeVariableBinding.declaringElement instanceof MethodBinding) {
      typeVariablesForThisMember.put(new String(aTypeVariableBinding.sourceName), ret);
    }
    typeVariableBindingsInProgress.remove(aTypeVariableBinding);
    return ret;
  }

  // return true if this is type variables are in the type arguments
  private static boolean isParameterizedWithTypeVariables(@NotNull UnresolvedType typeX) {
    if (!typeX.isParameterizedType()) {
      return false;
    }
    final UnresolvedType[] typeArguments = typeX.getTypeParameters();
    if (typeArguments != null) {
      for (int i = 0; i < typeArguments.length; i++) {
        if (typeArguments[i].isTypeVariableReference()) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private TypeBinding makeTypeBinding1(@NotNull UnresolvedType typeX) {
    if (typeX.isPrimitiveType()) {
      if (typeX.equals(UnresolvedType.BOOLEAN)) {
        return TypeBinding.BOOLEAN;
      }
      if (typeX.equals(UnresolvedType.BYTE)) {
        return TypeBinding.BYTE;
      }
      if (typeX.equals(UnresolvedType.CHAR)) {
        return TypeBinding.CHAR;
      }
      if (typeX.equals(UnresolvedType.DOUBLE)) {
        return TypeBinding.DOUBLE;
      }
      if (typeX.equals(UnresolvedType.FLOAT)) {
        return TypeBinding.FLOAT;
      }
      if (typeX.equals(UnresolvedType.INT)) {
        return TypeBinding.INT;
      }
      if (typeX.equals(UnresolvedType.LONG)) {
        return TypeBinding.LONG;
      }
      if (typeX.equals(UnresolvedType.SHORT)) {
        return TypeBinding.SHORT;
      }
      if (typeX.equals(UnresolvedType.VOID)) {
        return TypeBinding.VOID;
      }
      throw new RuntimeException("weird primitive type " + typeX);
    } else if (typeX.isArray()) {
      int dim = 0;
      while (typeX.isArray()) {
        dim++;
        typeX = typeX.getComponentType();
      }
      return lookupEnvironment.createArrayType(makeTypeBinding(typeX), dim);
    } else if (typeX.isParameterizedType()) {
      // Converting back to a binding from a UnresolvedType
      final UnresolvedType[] typeParameters = typeX.getTypeParameters();
      final ReferenceBinding baseTypeBinding = lookupBinding(typeX.getBaseName());
      final TypeBinding[] argumentBindings = new TypeBinding[typeParameters.length];
      baseTypeForParameterizedType = baseTypeBinding;
      for (int i = 0; i < argumentBindings.length; i++) {
        indexOfTypeParameterBeingConverted = i;
        argumentBindings[i] = makeTypeBinding(typeParameters[i]);
      }
      indexOfTypeParameterBeingConverted = 0;
      baseTypeForParameterizedType = null;
      final ParameterizedTypeBinding ptb = lookupEnvironment.createParameterizedType(baseTypeBinding, argumentBindings,
          baseTypeBinding.enclosingType());
      return ptb;
    } else if (typeX.isTypeVariableReference()) {
      // return makeTypeVariableBinding((TypeVariableReference)typeX);
      return makeTypeVariableBindingFromAJTypeVariable(((TypeVariableReference) typeX).getTypeVariable());
    } else if (typeX.isRawType()) {
      final ReferenceBinding baseTypeBinding = lookupBinding(typeX.getBaseName());
      final RawTypeBinding rtb = lookupEnvironment.createRawType(baseTypeBinding, baseTypeBinding.enclosingType());
      return rtb;
    } else if (typeX.isGenericWildcard()) {
      if (typeX instanceof WildcardedUnresolvedType) {
        final WildcardedUnresolvedType wut = (WildcardedUnresolvedType) typeX;
        int boundkind = Wildcard.UNBOUND;
        TypeBinding bound = null;
        if (wut.isExtends()) {
          boundkind = Wildcard.EXTENDS;
          bound = makeTypeBinding(wut.getUpperBound());
        } else if (wut.isSuper()) {
          boundkind = Wildcard.SUPER;
          bound = makeTypeBinding(wut.getLowerBound());
        }
        final TypeBinding[] otherBounds = null;
        // TODO 2 ought to support extra bounds for WildcardUnresolvedType
        // if (wut.getAdditionalBounds()!=null && wut.getAdditionalBounds().length!=0) otherBounds =
        // makeTypeBindings(wut.getAdditionalBounds());
        final WildcardBinding wb = lookupEnvironment.createWildcard(baseTypeForParameterizedType,
            indexOfTypeParameterBeingConverted, bound, otherBounds, boundkind);
        return wb;
      } else if (typeX instanceof BoundedReferenceType) {
        // translate from boundedreferencetype to WildcardBinding
        final BoundedReferenceType brt = (BoundedReferenceType) typeX;
        // Work out 'kind' for the WildcardBinding
        int boundkind = Wildcard.UNBOUND;
        TypeBinding bound = null;
        if (brt.isExtends()) {
          boundkind = Wildcard.EXTENDS;
          bound = makeTypeBinding(brt.getUpperBound());
        } else if (brt.isSuper()) {
          boundkind = Wildcard.SUPER;
          bound = makeTypeBinding(brt.getLowerBound());
        }
        TypeBinding[] otherBounds = null;
        if (brt.getAdditionalBounds() != null && brt.getAdditionalBounds().length != 0) {
          otherBounds = makeTypeBindings(brt.getAdditionalBounds());
        }
        final WildcardBinding wb = lookupEnvironment.createWildcard(baseTypeForParameterizedType,
            indexOfTypeParameterBeingConverted, bound, otherBounds, boundkind);
        return wb;
      } else {
        throw new BCException("This type " + typeX + " (class " + typeX.getClass().getName()
            + ") should not be claiming to be a wildcard!");
      }
    } else {
      return lookupBinding(typeX.getName());
    }
  }

  @Nullable
  private ReferenceBinding lookupBinding(@NotNull String sname) {
    final char[][] name = CharOperation.splitOn('.', sname.toCharArray());
    final ReferenceBinding rb = lookupEnvironment.getType(name);
    if (rb == null && !sname.equals(UnresolvedType.MISSING_NAME)) {
      return new ProblemReferenceBinding(name, null, ProblemReasons.NotFound);
    }
    return rb;
  }

  // just like the code above except it returns an array of ReferenceBindings
  @NotNull
  private ReferenceBinding[] makeReferenceBindings(@NotNull UnresolvedType[] types) {
    final int len = types.length;
    final ReferenceBinding[] ret = new ReferenceBinding[len];

    for (int i = 0; i < len; i++) {
      ret[i] = (ReferenceBinding) makeTypeBinding(types[i]);
    }
    return ret;
  }

  /**
   * Convert a bunch of type variables in one go, from AspectJ form to Eclipse form.
   */
  // private TypeVariableBinding[] makeTypeVariableBindings(UnresolvedType[] typeVariables) {
  // int len = typeVariables.length;
  // TypeVariableBinding[] ret = new TypeVariableBinding[len];
  // for (int i = 0; i < len; i++) {
  // ret[i] = makeTypeVariableBinding((TypeVariableReference)typeVariables[i]);
  // }
  // return ret;
  // }
  @NotNull
  private TypeVariableBinding[] makeTypeVariableBindingsFromAJTypeVariables(@NotNull TypeVariable[] typeVariables) {
    final int len = typeVariables.length;
    final TypeVariableBinding[] ret = new TypeVariableBinding[len];
    for (int i = 0; i < len; i++) {
      ret[i] = makeTypeVariableBindingFromAJTypeVariable(typeVariables[i]);
    }
    return ret;
  }

  // /**
  // * Converts from an TypeVariableReference to a TypeVariableBinding. A TypeVariableReference
  // * in AspectJ world holds a TypeVariable and it is this type variable that is converted
  // * to the TypeVariableBinding.
  // */
  // private TypeVariableBinding makeTypeVariableBinding(TypeVariableReference tvReference) {
  // TypeVariable tv = tvReference.getTypeVariable();
  // TypeVariableBinding tvBinding = (TypeVariableBinding)typeVariableToTypeBinding.get(tv.getName());
  // if (currentType!=null) {
  // TypeVariableBinding tvb = currentType.getTypeVariable(tv.getName().toCharArray());
  // if (tvb!=null) return tvb;
  // }
  // if (tvBinding==null) {
  // Binding declaringElement = null;
  // // this will cause an infinite loop or NPE... not required yet luckily.
  // // if (tVar.getDeclaringElement() instanceof Member) {
  // // declaringElement = makeMethodBinding((ResolvedMember)tVar.getDeclaringElement());
  // // } else {
  // // declaringElement = makeTypeBinding((UnresolvedType)tVar.getDeclaringElement());
  // // }
  //
  // tvBinding = new TypeVariableBinding(tv.getName().toCharArray(),null,tv.getRank());
  //
  // typeVariableToTypeBinding.put(tv.getName(),tvBinding);
  // tvBinding.superclass=(ReferenceBinding)makeTypeBinding(tv.getUpperBound());
  // tvBinding.firstBound=makeTypeBinding(tv.getFirstBound());
  // if (tv.getAdditionalInterfaceBounds()==null) {
  // tvBinding.superInterfaces=TypeVariableBinding.NO_SUPERINTERFACES;
  // } else {
  // TypeBinding tbs[] = makeTypeBindings(tv.getAdditionalInterfaceBounds());
  // ReferenceBinding[] rbs= new ReferenceBinding[tbs.length];
  // for (int i = 0; i < tbs.length; i++) {
  // rbs[i] = (ReferenceBinding)tbs[i];
  // }
  // tvBinding.superInterfaces=rbs;
  // }
  // }
  // return tvBinding;
  // }

  @NotNull
  private TypeVariableBinding makeTypeVariableBindingFromAJTypeVariable(@NotNull TypeVariable tv) {
    TypeVariableBinding tvBinding = (TypeVariableBinding) typeVariableToTypeBinding.get(tv.getName());
    if (currentType != null) {
      final TypeVariableBinding tvb = currentType.getTypeVariable(tv.getName().toCharArray());
      if (tvb != null) {
        return tvb;
      }
    }
    if (tvBinding == null) {
      final Binding declaringElement = null;
      // this will cause an infinite loop or NPE... not required yet luckily.
      // if (tVar.getDeclaringElement() instanceof Member) {
      // declaringElement = makeMethodBinding((ResolvedMember)tVar.getDeclaringElement());
      // } else {
      // declaringElement = makeTypeBinding((UnresolvedType)tVar.getDeclaringElement());
      // }
      tvBinding = new TypeVariableBinding(tv.getName().toCharArray(), declaringElement, tv.getRank(), this.lookupEnvironment);
      typeVariableToTypeBinding.put(tv.getName(), tvBinding);

      if (tv.getSuperclass() != null
          && (!tv.getSuperclass().getSignature().equals("Ljava/lang/Object;") || tv.getSuperInterfaces() != null)) {
        tvBinding.superclass = (ReferenceBinding) makeTypeBinding(tv.getSuperclass());
      }
      tvBinding.firstBound = makeTypeBinding(tv.getFirstBound());
      if (tv.getSuperInterfaces() == null) {
        tvBinding.superInterfaces = TypeVariableBinding.NO_SUPERINTERFACES;
      } else {
        final TypeBinding[] tbs = makeTypeBindings(tv.getSuperInterfaces());
        final ReferenceBinding[] rbs = new ReferenceBinding[tbs.length];
        for (int i = 0; i < tbs.length; i++) {
          rbs[i] = (ReferenceBinding) tbs[i];
        }
        tvBinding.superInterfaces = rbs;
      }
    }
    return tvBinding;
  }

}
