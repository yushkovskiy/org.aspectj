/* *******************************************************************
 * Copyright (c) 2002,2010 Contributors
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     PARC                 initial implementation
 *     Alexandre Vasseur    support for @AJ perClause
 * ******************************************************************/

package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.ajdt.internal.compiler.ast.*;
import org.aspectj.ajdt.internal.core.builder.EclipseSourceContext;
import org.aspectj.bridge.IMessage;
import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.*;
import org.aspectj.weaver.bcel.AtAjAttributes.LazyResolvedPointcutDefinition;
import org.aspectj.weaver.patterns.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Supports viewing eclipse TypeDeclarations/SourceTypeBindings as a ResolvedType
 *
 * @author Jim Hugunin
 * @author Andy Clement
 */
public class EclipseSourceType extends AbstractReferenceTypeDelegate {
  private static final char[] pointcutSig = "Lorg/aspectj/lang/annotation/Pointcut;".toCharArray();
  private static final char[] aspectSig = "Lorg/aspectj/lang/annotation/Aspect;".toCharArray();
  protected ResolvedPointcutDefinition[] declaredPointcuts = null;
  protected ResolvedMember[] declaredMethods = null;
  protected ResolvedMember[] declaredFields = null;

  public List<Declare> declares = new ArrayList<Declare>();
  public List<EclipseTypeMunger> typeMungers = new ArrayList<EclipseTypeMunger>();

  private final EclipseFactory factory;

  private final SourceTypeBinding binding;
  private final TypeDeclaration declaration;
  private final CompilationUnitDeclaration unit;
  private boolean annotationsFullyResolved = false;
  private boolean annotationTypesAreResolved = false;
  private ResolvedType[] annotationTypes = null;

  private boolean discoveredAnnotationTargetKinds = false;
  private AnnotationTargetKind[] annotationTargetKinds;
  private AnnotationAJ[] annotations = null;

  protected EclipseFactory eclipseWorld() {
    return factory;
  }

  public EclipseSourceType(ReferenceType resolvedTypeX, EclipseFactory factory, SourceTypeBinding binding,
                           TypeDeclaration declaration, CompilationUnitDeclaration unit) {
    super(resolvedTypeX, true);
    this.factory = factory;
    this.binding = binding;
    this.declaration = declaration;
    this.unit = unit;

    setSourceContext(new EclipseSourceContext(declaration.compilationResult));
    resolvedTypeX.setStartPos(declaration.sourceStart);
    resolvedTypeX.setEndPos(declaration.sourceEnd);
  }

  @Override
  public boolean isAspect() {
    final boolean isCodeStyle = declaration instanceof AspectDeclaration;
    return isCodeStyle ? isCodeStyle : isAnnotationStyleAspect();
  }

  @Override
  public boolean isAnonymous() {
    if (declaration.binding != null) {
      return declaration.binding.isAnonymousType();
    }
    return ((declaration.modifiers & (ASTNode.IsAnonymousType | ASTNode.IsLocalType)) != 0);
  }

  @Override
  public boolean isNested() {
    if (declaration.binding != null) {
      return (declaration.binding.isMemberType());
    }
    return ((declaration.modifiers & ASTNode.IsMemberType) != 0);
  }

  @Override
  public ResolvedType getOuterClass() {
    if (declaration.binding != null) {
      final ReferenceBinding enclosingType = declaration.binding.enclosingType();
      return enclosingType == null ? null : eclipseWorld().fromEclipse(enclosingType);
    }
    // TODO are we going to make a mistake here if the binding is null?
    // Do we ever get asked when the binding is null
    if (declaration.enclosingType == null) {
      return null;
    }
    return eclipseWorld().fromEclipse(declaration.enclosingType.binding);
  }

  @Override
  public boolean isAnnotationStyleAspect() {
    if (declaration.annotations == null) {
      return false;
    }
    final ResolvedType[] annotations = getAnnotationTypes();
    for (int i = 0; i < annotations.length; i++) {
      if ("org.aspectj.lang.annotation.Aspect".equals(annotations[i].getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns "" if there is a problem
   */
  private static String getPointcutStringFromAnnotationStylePointcut(AbstractMethodDeclaration amd) {
    final Annotation[] ans = amd.annotations;
    if (ans == null) {
      return "";
    }
    for (int i = 0; i < ans.length; i++) {
      if (ans[i].resolvedType == null) {
        continue; // XXX happens if we do this very early from
      }
      // buildInterTypeandPerClause
      // may prevent us from resolving references made in @Pointcuts to
      // an @Pointcut in a code-style aspect
      final char[] sig = ans[i].resolvedType.signature();
      if (CharOperation.equals(pointcutSig, sig)) {
        if (ans[i].memberValuePairs().length == 0) {
          return ""; // empty pointcut expression
        }
        final Expression expr = ans[i].memberValuePairs()[0].value;
        if (expr instanceof StringLiteral) {
          final StringLiteral sLit = ((StringLiteral) expr);
          return new String(sLit.source());
        } else if (expr instanceof NameReference && (((NameReference) expr).binding instanceof FieldBinding)) {
          final Binding b = ((NameReference) expr).binding;
          final Constant c = ((FieldBinding) b).constant;
          return c.stringValue();
        } else {
          throw new BCException("Do not know how to recover pointcut definition from " + expr + " (type "
              + expr.getClass().getName() + ")");
        }
      }
    }
    return "";
  }

  private static boolean isAnnotationStylePointcut(Annotation[] annotations) {
    if (annotations == null) {
      return false;
    }
    for (int i = 0; i < annotations.length; i++) {
      if (annotations[i].resolvedType == null) {
        continue; // XXX happens if we do this very early from
      }
      // buildInterTypeandPerClause
      // may prevent us from resolving references made in @Pointcuts to
      // an @Pointcut in a code-style aspect
      final char[] sig = annotations[i].resolvedType.signature();
      if (CharOperation.equals(pointcutSig, sig)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public WeaverStateInfo getWeaverState() {
    return null;
  }

  @Override
  public ResolvedType getSuperclass() {
    if (binding.isInterface()) {
      return getResolvedTypeX().getWorld().getCoreType(UnresolvedType.OBJECT);
    }
    // XXX what about java.lang.Object
    return eclipseWorld().fromEclipse(binding.superclass());
  }

  @Override
  public ResolvedType[] getDeclaredInterfaces() {
    return eclipseWorld().fromEclipse(binding.superInterfaces());
  }

  protected void fillDeclaredMembers() {
    final List<ResolvedMember> declaredPointcuts = new ArrayList<ResolvedMember>();
    final List<ResolvedMember> declaredMethods = new ArrayList<ResolvedMember>();
    final List<ResolvedMember> declaredFields = new ArrayList<ResolvedMember>();

    final MethodBinding[] ms = binding.methods(); // the important side-effect of this call is to make
    // sure bindings are completed
    final AbstractMethodDeclaration[] methods = declaration.methods;
    if (methods != null) {
      for (int i = 0, len = methods.length; i < len; i++) {
        final AbstractMethodDeclaration amd = methods[i];
        if (amd == null || amd.ignoreFurtherInvestigation) {
          continue;
        }
        if (amd instanceof PointcutDeclaration) {
          final PointcutDeclaration d = (PointcutDeclaration) amd;
          final ResolvedPointcutDefinition df = d.makeResolvedPointcutDefinition(factory);
          if (df != null) {
            declaredPointcuts.add(df);
          }
        } else if (amd instanceof InterTypeDeclaration) {
          // these are handled in a separate pass
          continue;
        } else if (amd instanceof DeclareDeclaration && !(amd instanceof DeclareAnnotationDeclaration)) { // surfaces
          // the
          // annotated
          // ajc$ method
          // these are handled in a separate pass
          continue;
        } else if (amd instanceof AdviceDeclaration) {
          // these are ignored during compilation and only used during
          // weaving
          continue;
        } else if ((amd.annotations != null) && isAnnotationStylePointcut(amd.annotations)) {
          // consider pointcuts defined via annotations
          final ResolvedPointcutDefinition df = makeResolvedPointcutDefinition(amd);
          if (df != null) {
            declaredPointcuts.add(df);
          }
        } else {
          if (amd.binding == null || !amd.binding.isValidBinding()) {
            continue;
          }
          final ResolvedMember member = factory.makeResolvedMember(amd.binding);
          if (unit != null) {
            boolean positionKnown = true;
            if (amd.binding.sourceMethod() == null) {
              if (amd.binding.declaringClass instanceof SourceTypeBinding) {
                final SourceTypeBinding stb = ((SourceTypeBinding) amd.binding.declaringClass);
                if (stb.scope == null || stb.scope.referenceContext == null) {
                  positionKnown = false;
                }
              }
            }
            if (positionKnown) { // pr229829
              member.setSourceContext(new EclipseSourceContext(unit.compilationResult, amd.binding.sourceStart()));
              member.setPosition(amd.binding.sourceStart(), amd.binding.sourceEnd());
            } else {
              member.setSourceContext(new EclipseSourceContext(unit.compilationResult, 0));
              member.setPosition(0, 0);
            }
          }
          declaredMethods.add(member);
        }
      }
    }

    if (isEnum()) {
      // The bindings for the eclipse binding will include values/valueof
      for (int m = 0, len = ms.length; m < len; m++) {
        final MethodBinding mb = ms[m];
        if ((mb instanceof SyntheticMethodBinding) && mb.isStatic()) { // cannot use .isSynthetic() because it isn't truly synthetic
          if (CharOperation.equals(mb.selector, valuesCharArray) && mb.parameters.length == 0 && mb.returnType.isArrayType() && ((ArrayBinding) mb.returnType).leafComponentType() == binding) {
            // static <EnumType>[] values()
            final ResolvedMember valuesMember = factory.makeResolvedMember(mb);
            valuesMember.setSourceContext(new EclipseSourceContext(unit.compilationResult, 0));
            valuesMember.setPosition(0, 0);
            declaredMethods.add(valuesMember);
          } else if (CharOperation.equals(mb.selector, valueOfCharArray) && mb.parameters.length == 1 && CharOperation.equals(mb.parameters[0].signature(), jlString) && mb.returnType == binding) {
            // static <EnumType> valueOf(String)
            final ResolvedMember valueOfMember = factory.makeResolvedMember(mb);
            valueOfMember.setSourceContext(new EclipseSourceContext(unit.compilationResult, 0));
            valueOfMember.setPosition(0, 0);
            declaredMethods.add(valueOfMember);
          }
        }
      }
    }

    final FieldBinding[] fields = binding.fields();
    for (int i = 0, len = fields.length; i < len; i++) {
      final FieldBinding f = fields[i];
      declaredFields.add(factory.makeResolvedMember(f));
    }

    this.declaredPointcuts = declaredPointcuts.toArray(new ResolvedPointcutDefinition[declaredPointcuts.size()]);
    this.declaredMethods = declaredMethods.toArray(new ResolvedMember[declaredMethods.size()]);
    this.declaredFields = declaredFields.toArray(new ResolvedMember[declaredFields.size()]);
  }

  private final static char[] valuesCharArray = "values".toCharArray();
  private final static char[] valueOfCharArray = "valueOf".toCharArray();
  private final static char[] jlString = "Ljava/lang/String;".toCharArray();


  private ResolvedPointcutDefinition makeResolvedPointcutDefinition(AbstractMethodDeclaration md) {
    if (md.binding == null) {
      return null; // there is another error that has caused this...
      // pr138143
    }

    final EclipseSourceContext eSourceContext = new EclipseSourceContext(md.compilationResult);
    Pointcut pc = null;
    if (!md.isAbstract()) {
      final String expression = getPointcutStringFromAnnotationStylePointcut(md);
      try {
        pc = new PatternParser(expression, eSourceContext).parsePointcut();
      } catch (ParserException pe) { // error will be reported by other
        // means...
        pc = Pointcut.makeMatchesNothing(Pointcut.SYMBOLIC);
      }
    }

    final FormalBinding[] bindings = buildFormalAdviceBindingsFrom(md);

    final ResolvedPointcutDefinition rpd = new LazyResolvedPointcutDefinition(factory.fromBinding(md.binding.declaringClass),
        md.modifiers, new String(md.selector), factory.fromBindings(md.binding.parameters),
        factory.fromBinding(md.binding.returnType), pc, new EclipseScope(bindings, md.scope));

    rpd.setPosition(md.sourceStart, md.sourceEnd);
    rpd.setSourceContext(eSourceContext);
    return rpd;
  }

  private static final char[] joinPoint = "Lorg/aspectj/lang/JoinPoint;".toCharArray();
  private static final char[] joinPointStaticPart = "Lorg/aspectj/lang/JoinPoint$StaticPart;".toCharArray();
  private static final char[] joinPointEnclosingStaticPart = "Lorg/aspectj/lang/JoinPoint$EnclosingStaticPart;".toCharArray();
  private static final char[] proceedingJoinPoint = "Lorg/aspectj/lang/ProceedingJoinPoint;".toCharArray();

  private static FormalBinding[] buildFormalAdviceBindingsFrom(AbstractMethodDeclaration mDecl) {
    if (mDecl.arguments == null) {
      return new FormalBinding[0];
    }
    if (mDecl.binding == null) {
      return new FormalBinding[0];
    }
    final EclipseFactory factory = EclipseFactory.fromScopeLookupEnvironment(mDecl.scope);
    final String extraArgName = "";// maybeGetExtraArgName();
    final FormalBinding[] ret = new FormalBinding[mDecl.arguments.length];
    for (int i = 0; i < mDecl.arguments.length; i++) {
      final Argument arg = mDecl.arguments[i];
      final String name = new String(arg.name);
      final TypeBinding argTypeBinding = mDecl.binding.parameters[i];
      final UnresolvedType type = factory.fromBinding(argTypeBinding);
      if (CharOperation.equals(joinPoint, argTypeBinding.signature())
          || CharOperation.equals(joinPointStaticPart, argTypeBinding.signature())
          || CharOperation.equals(joinPointEnclosingStaticPart, argTypeBinding.signature())
          || CharOperation.equals(proceedingJoinPoint, argTypeBinding.signature()) || name.equals(extraArgName)) {
        ret[i] = new FormalBinding.ImplicitFormalBinding(type, name, i);
      } else {
        ret[i] = new FormalBinding(type, name, i, arg.sourceStart, arg.sourceEnd);
      }
    }
    return ret;
  }

  /**
   * This method may not return all fields, for example it may not include the ajc$initFailureCause or ajc$perSingletonInstance
   * fields - see bug 129613
   */
  @Override
  public ResolvedMember[] getDeclaredFields() {
    if (declaredFields == null) {
      fillDeclaredMembers();
    }
    return declaredFields;
  }

  /**
   * This method may not return all methods, for example it may not include clinit, aspectOf, hasAspect or ajc$postClinit methods
   * - see bug 129613
   */
  @Override
  public ResolvedMember[] getDeclaredMethods() {
    if (declaredMethods == null) {
      fillDeclaredMembers();
    }
    return declaredMethods;
  }

  @Override
  public ResolvedMember[] getDeclaredPointcuts() {
    if (declaredPointcuts == null) {
      fillDeclaredMembers();
    }
    return declaredPointcuts;
  }

  @Override
  public int getModifiers() {
    // only return the real Java modifiers, not the extra eclipse ones
    return binding.modifiers & ExtraCompilerModifiers.AccJustFlag;
  }

  public String toString() {
    return "EclipseSourceType(" + new String(binding.sourceName()) + ")";
  }

  // XXX make sure this is applied to classes and interfaces
  public void checkPointcutDeclarations() {
    final ResolvedMember[] pointcuts = getDeclaredPointcuts();
    boolean sawError = false;
    for (int i = 0, len = pointcuts.length; i < len; i++) {
      if (pointcuts[i] == null) {
        // Something else is broken in this file and will be reported separately
        continue;
      }
      if (pointcuts[i].isAbstract()) {
        if (!this.isAspect()) {
          eclipseWorld().showMessage(IMessage.ERROR, "abstract pointcut only allowed in aspect" + pointcuts[i].getName(),
              pointcuts[i].getSourceLocation(), null);
          sawError = true;
        } else if (!binding.isAbstract()) {
          eclipseWorld().showMessage(IMessage.ERROR, "abstract pointcut in concrete aspect" + pointcuts[i],
              pointcuts[i].getSourceLocation(), null);
          sawError = true;
        }
      }

      for (int j = i + 1; j < len; j++) {
        if (pointcuts[j] == null) {
          // Something else is broken in this file and will be reported separately
          continue;
        }
        if (pointcuts[i].getName().equals(pointcuts[j].getName())) {
          eclipseWorld().showMessage(IMessage.ERROR, "duplicate pointcut name: " + pointcuts[j].getName(),
              pointcuts[i].getSourceLocation(), pointcuts[j].getSourceLocation());
          sawError = true;
        }
      }
    }

    // now check all inherited pointcuts to be sure that they're handled
    // reasonably
    if (sawError || !isAspect()) {
      return;
    }

    // find all pointcuts that override ones from super and check override
    // is legal
    // i.e. same signatures and greater or equal visibility
    // find all inherited abstract pointcuts and make sure they're
    // concretized if I'm concrete
    // find all inherited pointcuts and make sure they don't conflict
    getResolvedTypeX().getExposedPointcuts(); // ??? this is an odd
    // construction

  }

  // ???
  // public CrosscuttingMembers collectCrosscuttingMembers() {
  // return crosscuttingMembers;
  // }

  // public ISourceLocation getSourceLocation() {
  // TypeDeclaration dec = binding.scope.referenceContext;
  // return new EclipseSourceLocation(dec.compilationResult, dec.sourceStart,
  // dec.sourceEnd);
  // }

  @Override
  public boolean isInterface() {
    return binding.isInterface();
  }

  // XXXAJ5: Should be constants in the eclipse compiler somewhere, once it
  // supports 1.5
  public final static short ACC_ANNOTATION = 0x2000;
  public final static short ACC_ENUM = 0x4000;

  @Override
  public boolean isEnum() {
    return (binding.getAccessFlags() & ACC_ENUM) != 0;
  }

  @Override
  public boolean isAnnotation() {
    return (binding.getAccessFlags() & ACC_ANNOTATION) != 0;
  }

  @Override
  public boolean isAnnotationWithRuntimeRetention() {
    if (!isAnnotation()) {
      return false;
    } else {
      return (binding.getAnnotationTagBits() & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
    }
  }

  @Override
  public String getRetentionPolicy() {
    if (isAnnotation()) {
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention) {
        return "RUNTIME";
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationSourceRetention) {
        return "SOURCE";
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationClassRetention) {
        return "CLASS";
      }
    }
    return null;
  }

  @Override
  public boolean canAnnotationTargetType() {
    if (isAnnotation()) {
      return ((binding.getAnnotationTagBits() & TagBits.AnnotationForType) != 0);
    }
    return false;
  }

  @Override
  public AnnotationTargetKind[] getAnnotationTargetKinds() {
    if (discoveredAnnotationTargetKinds) {
      return annotationTargetKinds;
    }
    discoveredAnnotationTargetKinds = true;
    annotationTargetKinds = null; // null means we have no idea or the
    // @Target annotation hasn't been used
    // if (isAnnotation()) {
    // Annotation[] annotationsOnThisType = declaration.annotations;
    // if (annotationsOnThisType != null) {
    // for (int i = 0; i < annotationsOnThisType.length; i++) {
    // Annotation a = annotationsOnThisType[i];
    // if (a.resolvedType != null) {
    // String packageName = new
    // String(a.resolvedType.qualifiedPackageName()).concat(".");
    // String sourceName = new String(a.resolvedType.qualifiedSourceName());
    // if ((packageName +
    // sourceName).equals(UnresolvedType.AT_TARGET.getName())) {
    // MemberValuePair[] pairs = a.memberValuePairs();
    // for (int j = 0; j < pairs.length; j++) {
    // MemberValuePair pair = pairs[j];
    // targetKind = pair.value.toString();
    // return targetKind;
    // }
    // }
    // }
    // }
    // }
    // }
    // return targetKind;
    if (isAnnotation()) {
      final List<AnnotationTargetKind> targetKinds = new ArrayList<AnnotationTargetKind>();

      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForAnnotationType) != 0) {
        targetKinds.add(AnnotationTargetKind.ANNOTATION_TYPE);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForConstructor) != 0) {
        targetKinds.add(AnnotationTargetKind.CONSTRUCTOR);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForField) != 0) {
        targetKinds.add(AnnotationTargetKind.FIELD);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForLocalVariable) != 0) {
        targetKinds.add(AnnotationTargetKind.LOCAL_VARIABLE);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForMethod) != 0) {
        targetKinds.add(AnnotationTargetKind.METHOD);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForPackage) != 0) {
        targetKinds.add(AnnotationTargetKind.PACKAGE);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForParameter) != 0) {
        targetKinds.add(AnnotationTargetKind.PARAMETER);
      }
      if ((binding.getAnnotationTagBits() & TagBits.AnnotationForType) != 0) {
        targetKinds.add(AnnotationTargetKind.TYPE);
      }

      if (!targetKinds.isEmpty()) {
        annotationTargetKinds = new AnnotationTargetKind[targetKinds.size()];
        return targetKinds.toArray(annotationTargetKinds);
      }
    }
    return annotationTargetKinds;
  }

  /**
   * Ensure the annotation types have been resolved, where resolved means the eclipse type bindings have been converted to their
   * ResolvedType representations. This does not deeply resolve the annotations, it only does the type names.
   */
  private void ensureAnnotationTypesResolved() {
    // may need to re-resolve if new annotations have been added
    final int declarationAnnoCount = (declaration.annotations == null ? 0 : declaration.annotations.length);
    if (!annotationTypesAreResolved || declarationAnnoCount != annotationTypes.length) {
      final Annotation[] as = declaration.annotations;
      if (as == null) {
        annotationTypes = ResolvedType.NONE;
      } else {
        annotationTypes = new ResolvedType[as.length];
        for (int a = 0; a < as.length; a++) {
          final TypeBinding tb = as[a].type.resolveType(declaration.staticInitializerScope);
          if (tb == null) {
            annotationTypes[a] = ResolvedType.MISSING;
          } else {
            annotationTypes[a] = factory.fromTypeBindingToRTX(tb);
          }
        }
      }
      annotationTypesAreResolved = true;
    }
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    ensureAnnotationTypesResolved();
    for (int a = 0, max = annotationTypes.length; a < max; a++) {
      if (ofType.equals(annotationTypes[a])) {
        return true;
      }
    }
    return false;
  }

  /**
   * WARNING: This method does not have a complete implementation.
   * <p/>
   * The aim is that it converts Eclipse annotation objects to the AspectJ form of annotations (the type AnnotationAJ). The
   * AnnotationX objects returned are wrappers over either a Bcel annotation type or the AspectJ AnnotationAJ type. The minimal
   * implementation provided here is for processing the RetentionPolicy and Target annotation types - these are the only ones
   * which the weaver will attempt to process from an EclipseSourceType.
   * <p/>
   * More notes: The pipeline has required us to implement this. With the pipeline we can be weaving a type and asking questions
   * of annotations before they have been turned into Bcel objects - ie. when they are still in EclipseSourceType form. Without
   * the pipeline we would have converted everything to Bcel objects before proceeding with weaving. Because the pipeline won't
   * start weaving until all aspects have been compiled and the fact that no AspectJ constructs match on the values within
   * annotations, this code only needs to deal with converting system annotations that the weaver needs to process
   * (RetentionPolicy, Target).
   */
  @Override
  public AnnotationAJ[] getAnnotations() {
    final int declarationAnnoCount = (declaration.annotations == null ? 0 : declaration.annotations.length);
    if (annotations != null && annotations.length == declarationAnnoCount) {
      return annotations; // only do this once
    }
    if (!annotationsFullyResolved || annotations.length != declarationAnnoCount) {
      TypeDeclaration.resolveAnnotations(declaration.staticInitializerScope, declaration.annotations, binding);
      annotationsFullyResolved = true;
    }
    final Annotation[] as = declaration.annotations;
    if (as == null || as.length == 0) {
      annotations = AnnotationAJ.EMPTY_ARRAY;
    } else {
      annotations = new AnnotationAJ[as.length];
      for (int i = 0; i < as.length; i++) {
        annotations[i] = convertEclipseAnnotation(as[i], factory.getWorld());
      }
    }
    return annotations;
  }

  /**
   * Convert one eclipse annotation into an AnnotationX object containing an AnnotationAJ object.
   * <p/>
   * This code and the helper methods used by it will go *BANG* if they encounter anything not currently supported - this is safer
   * than limping along with a malformed annotation. When the *BANG* is encountered the bug reporter should indicate the kind of
   * annotation they were working with and this code can be enhanced to support it.
   */
  public AnnotationAJ convertEclipseAnnotation(Annotation eclipseAnnotation, World w) {
    // TODO if it is sourcevisible, we shouldn't let it through!!!!!!!!!
    // testcase!
    final ResolvedType annotationType = factory.fromTypeBindingToRTX(eclipseAnnotation.type.resolvedType);
    // long bs = (eclipseAnnotation.bits & TagBits.AnnotationRetentionMASK);
    final boolean isRuntimeVisible = (eclipseAnnotation.bits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
    final StandardAnnotation annotationAJ = new StandardAnnotation(annotationType, isRuntimeVisible);
    generateAnnotation(eclipseAnnotation, annotationAJ, w);
    return annotationAJ;
  }

  static class MissingImplementationException extends RuntimeException {
    MissingImplementationException(String reason) {
      super(reason);
    }
  }

  /**
   * Use the information in the supplied eclipse based annotation to fill in the standard annotation.
   *
   * @param annotation   eclipse based annotation representation
   * @param annotationAJ AspectJ based annotation representation
   */
  private void generateAnnotation(Annotation annotation, StandardAnnotation annotationAJ, World w) {
    if (annotation instanceof NormalAnnotation) {
      final NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
      final MemberValuePair[] memberValuePairs = normalAnnotation.memberValuePairs;
      if (memberValuePairs != null) {
        final int memberValuePairsLength = memberValuePairs.length;
        for (int i = 0; i < memberValuePairsLength; i++) {
          final MemberValuePair memberValuePair = memberValuePairs[i];
          final MethodBinding methodBinding = memberValuePair.binding;
          if (methodBinding == null) {
            // is this just a marker annotation?
            if (memberValuePair.value instanceof MarkerAnnotation) {
              final MarkerAnnotation eMarkerAnnotation = (MarkerAnnotation) memberValuePair.value;
              final AnnotationBinding eMarkerAnnotationBinding = eMarkerAnnotation.getCompilerAnnotation();
              final ReferenceBinding eAnnotationType = eMarkerAnnotationBinding.getAnnotationType();
              final ResolvedType ajAnnotationType = factory.fromTypeBindingToRTX(eAnnotationType);
              final boolean isRuntimeVisible = (eMarkerAnnotation.bits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
              final StandardAnnotation ajAnnotation = new StandardAnnotation(ajAnnotationType, isRuntimeVisible);
              final AnnotationValue av = new AnnotationAnnotationValue(ajAnnotation);
              final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), av);
              annotationAJ.addNameValuePair(anvp);
              // } else if (memberValuePair.value instanceof NormalAnnotation) {
              // NormalAnnotation eNormalAnnotation = (NormalAnnotation) memberValuePair.value;
              // AnnotationBinding eMarkerAnnotationBinding = eNormalAnnotation.getCompilerAnnotation();
              // ReferenceBinding eAnnotationType = eMarkerAnnotationBinding.getAnnotationType();
              // ResolvedType ajAnnotationType = factory.fromTypeBindingToRTX(eAnnotationType);
              // boolean isRuntimeVisible = (eNormalAnnotation.bits & TagBits.AnnotationRetentionMASK) ==
              // TagBits.AnnotationRuntimeRetention;
              // StandardAnnotation ajAnnotation = new StandardAnnotation(ajAnnotationType, isRuntimeVisible);
              // MemberValuePair[] pairs = eNormalAnnotation.memberValuePairs;
              // if (pairs != null) {
              // for (int p = 0; p < pairs.length; p++) {
              // MemberValuePair pair = pairs[p];
              // throw new IllegalStateException("nyi");
              //
              // }
              // }
              // AnnotationValue av = new AnnotationAnnotationValue(ajAnnotation);
              // AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), av);
              // annotationAJ.addNameValuePair(anvp);
            } else if (memberValuePair.value instanceof Literal) {
              final AnnotationValue av = generateElementValue(memberValuePair.value,
                  ((Literal) memberValuePair.value).resolvedType);
              final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), av);
              annotationAJ.addNameValuePair(anvp);
            } else if (memberValuePair.value instanceof ArrayInitializer) {
              final ArrayInitializer arrayInitializer = (ArrayInitializer) memberValuePair.value;
              final Expression[] expressions = arrayInitializer.expressions;
              final AnnotationValue[] arrayValues = new AnnotationValue[expressions.length];
              for (int e = 0; e < expressions.length; e++) {
                arrayValues[e] = generateElementValue(expressions[e],
                    ((ArrayBinding) arrayInitializer.resolvedType).leafComponentType);
              }
              final AnnotationValue array = new ArrayAnnotationValue(arrayValues);
              final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), array);
              annotationAJ.addNameValuePair(anvp);
            } else {
              throw new MissingImplementationException(
                  "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation ["
                      + annotation + "]");
            }
          } else {
            final AnnotationValue av = generateElementValue(memberValuePair.value, methodBinding.returnType);
            final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), av);
            annotationAJ.addNameValuePair(anvp);
          }
        }
      }
    } else if (annotation instanceof SingleMemberAnnotation) {
      // this is a single member annotation (one member value)
      final SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
      final MemberValuePair mvp = singleMemberAnnotation.memberValuePairs()[0];
      if (mvp.value instanceof ArrayInitializer) {
        final ArrayInitializer arrayInitializer = (ArrayInitializer) mvp.value;
        final Expression[] expressions = arrayInitializer.expressions;
        final AnnotationValue[] arrayValues = new AnnotationValue[expressions.length];
        for (int e = 0; e < expressions.length; e++) {
          arrayValues[e] = generateElementValue(expressions[e],
              ((ArrayBinding) arrayInitializer.resolvedType).leafComponentType);
        }
        final AnnotationValue array = new ArrayAnnotationValue(arrayValues);
        final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(mvp.name), array);
        annotationAJ.addNameValuePair(anvp);
      } else {
        final MethodBinding methodBinding = mvp.binding;
        if (methodBinding == null) {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation + "]");
        } else {
          final AnnotationValue av = generateElementValue(singleMemberAnnotation.memberValue, methodBinding.returnType);
          annotationAJ.addNameValuePair(new AnnotationNameValuePair(new String(
              singleMemberAnnotation.memberValuePairs()[0].name), av));
        }
      }
    } else if (annotation instanceof MarkerAnnotation) {
      return;
    } else {
      // this is something else...
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation + "]");
    }
  }

  private AnnotationValue generateElementValue(Expression defaultValue, TypeBinding memberValuePairReturnType) {
    final Constant constant = defaultValue.constant;
    final TypeBinding defaultValueBinding = defaultValue.resolvedType;
    if (defaultValueBinding == null) {
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
              + "]");
    } else {
      if (memberValuePairReturnType.isArrayType() && !defaultValueBinding.isArrayType()) {
        if (constant != null && constant != Constant.NotAConstant) {
          // Testcase for this clause is MultiProjectIncrementalTests.testAnnotations_pr262154()
          final AnnotationValue av = EclipseAnnotationConvertor.generateElementValueForConstantExpression(defaultValue,
              defaultValueBinding);
          return new ArrayAnnotationValue(new AnnotationValue[]{av});
        } else {
          final AnnotationValue av = generateElementValueForNonConstantExpression(defaultValue, defaultValueBinding);
          return new ArrayAnnotationValue(new AnnotationValue[]{av});
        }
      } else {
        if (constant != null && constant != Constant.NotAConstant) {
          final AnnotationValue av = EclipseAnnotationConvertor.generateElementValueForConstantExpression(defaultValue,
              defaultValueBinding);
          if (av == null) {
            throw new MissingImplementationException(
                "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                    + defaultValue + "]");
          }
          return av;
          // generateElementValue(attributeOffset, defaultValue,
          // constant, memberValuePairReturnType.leafComponentType());
        } else {
          final AnnotationValue av = generateElementValueForNonConstantExpression(defaultValue, defaultValueBinding);
          return av;
        }
      }
    }
  }

  private AnnotationValue generateElementValueForNonConstantExpression(Expression defaultValue, TypeBinding defaultValueBinding) {
    if (defaultValueBinding != null) {
      if (defaultValueBinding.isEnum()) {
        FieldBinding fieldBinding = null;
        if (defaultValue instanceof QualifiedNameReference) {
          final QualifiedNameReference nameReference = (QualifiedNameReference) defaultValue;
          fieldBinding = (FieldBinding) nameReference.binding;
        } else if (defaultValue instanceof SingleNameReference) {
          final SingleNameReference nameReference = (SingleNameReference) defaultValue;
          fieldBinding = (FieldBinding) nameReference.binding;
        } else {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
        }
        if (fieldBinding != null) {
          final String sig = new String(fieldBinding.type.signature());
          final AnnotationValue enumValue = new EnumAnnotationValue(sig, new String(fieldBinding.name));
          return enumValue;
        }
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
                + "]");
      } else if (defaultValue instanceof ClassLiteralAccess) {
        final ClassLiteralAccess cla = (ClassLiteralAccess) defaultValue;
        final TypeBinding claTargetType = cla.targetType;
//				ResolvedType classLiteralType = factory.fromTypeBindingToRTX(defaultValueBinding);
        final String classLiteralSig = new String(claTargetType.signature());
        final AnnotationValue classValue = new ClassAnnotationValue(classLiteralSig);
        return classValue;
      } else if (defaultValueBinding.isAnnotationType()) {
        if (defaultValue instanceof MarkerAnnotation) {
          final ResolvedType ajAnnotationType = factory.fromTypeBindingToRTX(defaultValueBinding);
          final StandardAnnotation ajAnnotation = new StandardAnnotation(ajAnnotationType,
              ajAnnotationType.isAnnotationWithRuntimeRetention());
          final AnnotationValue av = new AnnotationAnnotationValue(ajAnnotation);
          return av;
        } else if (defaultValue instanceof NormalAnnotation) {
          final NormalAnnotation normalAnnotation = (NormalAnnotation) defaultValue;
          final ResolvedType ajAnnotationType = factory.fromTypeBindingToRTX(defaultValueBinding);
          final StandardAnnotation ajAnnotation = new StandardAnnotation(ajAnnotationType,
              ajAnnotationType.isAnnotationWithRuntimeRetention());
          final MemberValuePair[] pairs = normalAnnotation.memberValuePairs;
          if (pairs != null) {
            for (int p = 0; p < pairs.length; p++) {
              final MemberValuePair pair = pairs[p];
              final Expression valueEx = pair.value;
              AnnotationValue pairValue = null;
              if (valueEx instanceof Literal) {
                pairValue = generateElementValue(valueEx, ((Literal) valueEx).resolvedType);
              } else {
                pairValue = generateElementValue(pair.value, pair.binding.returnType);
              }
              ajAnnotation.addNameValuePair(new AnnotationNameValuePair(new String(pair.name), pairValue));
            }
          }
          final AnnotationValue av = new AnnotationAnnotationValue(ajAnnotation);
          return av;
        } else {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
        }
      } else if (defaultValueBinding.isArrayType()) {
        // array type
        if (defaultValue instanceof ArrayInitializer) {
          final ArrayInitializer arrayInitializer = (ArrayInitializer) defaultValue;
          final int arrayLength = arrayInitializer.expressions != null ? arrayInitializer.expressions.length : 0;
          final AnnotationValue[] values = new AnnotationValue[arrayLength];
          for (int i = 0; i < arrayLength; i++) {
            values[i] = generateElementValue(arrayInitializer.expressions[i], defaultValueBinding.leafComponentType());// ,
            // attributeOffset
            // )
            // ;
          }
          final ArrayAnnotationValue aav = new ArrayAnnotationValue(values);
          return aav;
        } else {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
        }
        // } else if (defaultValue instanceof MagicLiteral) {
        // if (defaultValue instanceof FalseLiteral) {
        // new AnnotationValue
        // } else if (defaultValue instanceof TrueLiteral) {
        //
        // } else {
        // throw new MissingImplementationException(
        // "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
        // +defaultValue+"]");
        // }
      } else {
        // class type
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
                + "]");
        // if (contentsOffset + 3 >= this.contents.length) {
        // resizeContents(3);
        // }
        // contents[contentsOffset++] = (byte) 'c';
        // if (defaultValue instanceof ClassLiteralAccess) {
        // ClassLiteralAccess classLiteralAccess = (ClassLiteralAccess)
        // defaultValue;
        // final int classInfoIndex =
        // constantPool.literalIndex(classLiteralAccess
        // .targetType.signature());
        // contents[contentsOffset++] = (byte) (classInfoIndex >> 8);
        // contents[contentsOffset++] = (byte) classInfoIndex;
        // } else {
        // contentsOffset = attributeOffset;
        // }
      }
    } else {
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
              + "]");
      // contentsOffset = attributeOffset;
    }
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    ensureAnnotationTypesResolved();
    return annotationTypes;
  }

  @Override
  public PerClause getPerClause() {
    // should probably be: ((AspectDeclaration)declaration).perClause;
    // but we don't need this level of detail, and working with real per
    // clauses
    // at this stage of compilation is not worth the trouble
    if (!isAnnotationStyleAspect()) {
      if (declaration instanceof AspectDeclaration) {
        final PerClause pc = ((AspectDeclaration) declaration).perClause;
        if (pc != null) {
          return pc;
        }
      }
      return new PerSingleton();
    } else {
      // for @Aspect, we do need the real kind though we don't need the
      // real perClause
      // at least try to get the right perclause
      PerClause pc = null;
      if (declaration instanceof AspectDeclaration) {
        pc = ((AspectDeclaration) declaration).perClause;
      }
      if (pc == null) {
        final PerClause.Kind kind = getPerClauseForTypeDeclaration(declaration);
        // returning a perFromSuper is enough to get the correct kind..
        // (that's really a hack - AV)
        return new PerFromSuper(kind);
      }
      return pc;
    }
  }

  PerClause.Kind getPerClauseForTypeDeclaration(TypeDeclaration typeDeclaration) {
    final Annotation[] annotations = typeDeclaration.annotations;
    for (int i = 0; i < annotations.length; i++) {
      final Annotation annotation = annotations[i];
      if (annotation != null && annotation.resolvedType != null
          && CharOperation.equals(aspectSig, annotation.resolvedType.signature())) {
        // found @Aspect(...)
        if (annotation.memberValuePairs() == null || annotation.memberValuePairs().length == 0) {
          // it is an @Aspect or @Aspect()
          // needs to use PerFromSuper if declaration extends a super
          // aspect
          final PerClause.Kind kind = lookupPerClauseKind(typeDeclaration.binding.superclass);
          // if no super aspect, we have a @Aspect() means singleton
          if (kind == null) {
            return PerClause.SINGLETON;
          } else {
            return kind;
          }
        } else if (annotation instanceof SingleMemberAnnotation) {
          // it is an @Aspect(...something...)
          final SingleMemberAnnotation theAnnotation = (SingleMemberAnnotation) annotation;
          final String clause = new String(((StringLiteral) theAnnotation.memberValue).source());// TODO
          // cast
          // safe
          // ?
          return determinePerClause(typeDeclaration, clause);
        } else if (annotation instanceof NormalAnnotation) { // this
          // kind
          // if it
          // was
          // added
          // by
          // the
          // visitor
          // !
          // it is an @Aspect(...something...)
          final NormalAnnotation theAnnotation = (NormalAnnotation) annotation;
          if (theAnnotation.memberValuePairs == null || theAnnotation.memberValuePairs.length < 1) {
            return PerClause.SINGLETON;
          }
          final String clause = new String(((StringLiteral) theAnnotation.memberValuePairs[0].value).source());// TODO
          // cast
          // safe
          // ?
          return determinePerClause(typeDeclaration, clause);
        } else {
          eclipseWorld().showMessage(
              IMessage.ABORT,
              "@Aspect annotation is expected to be SingleMemberAnnotation with 'String value()' as unique element",
              new EclipseSourceLocation(typeDeclaration.compilationResult, typeDeclaration.sourceStart,
                  typeDeclaration.sourceEnd), null);
          return PerClause.SINGLETON;// fallback strategy just to
          // avoid NPE
        }
      }
    }
    return null;// no @Aspect annotation at all (not as aspect)
  }

  private PerClause.Kind determinePerClause(TypeDeclaration typeDeclaration, String clause) {
    if (clause.startsWith("perthis(")) {
      return PerClause.PEROBJECT;
    } else if (clause.startsWith("pertarget(")) {
      return PerClause.PEROBJECT;
    } else if (clause.startsWith("percflow(")) {
      return PerClause.PERCFLOW;
    } else if (clause.startsWith("percflowbelow(")) {
      return PerClause.PERCFLOW;
    } else if (clause.startsWith("pertypewithin(")) {
      return PerClause.PERTYPEWITHIN;
    } else if (clause.startsWith("issingleton(")) {
      return PerClause.SINGLETON;
    } else {
      eclipseWorld().showMessage(
          IMessage.ABORT,
          "cannot determine perClause '" + clause + "'",
          new EclipseSourceLocation(typeDeclaration.compilationResult, typeDeclaration.sourceStart,
              typeDeclaration.sourceEnd), null);
      return PerClause.SINGLETON;// fallback strategy just to avoid NPE
    }
  }

  // adapted from AspectDeclaration
  private PerClause.Kind lookupPerClauseKind(ReferenceBinding binding) {
    final PerClause.Kind kind;
    if (binding instanceof BinaryTypeBinding) {
      final ResolvedType superTypeX = factory.fromEclipse(binding);
      final PerClause perClause = superTypeX.getPerClause();
      // clause is null for non aspect classes since coming from BCEL
      // attributes
      if (perClause != null) {
        kind = superTypeX.getPerClause().getKind();
      } else {
        kind = null;
      }
    } else if (binding instanceof SourceTypeBinding) {
      final SourceTypeBinding sourceSc = (SourceTypeBinding) binding;
      if (sourceSc.scope.referenceContext instanceof AspectDeclaration) {
        // code style
        kind = ((AspectDeclaration) sourceSc.scope.referenceContext).perClause.getKind();
      } else { // if (sourceSc.scope.referenceContext instanceof
        // TypeDeclaration) {
        // if @Aspect: perFromSuper, else if @Aspect(..) get from anno
        // value, else null
        kind = getPerClauseForTypeDeclaration((sourceSc.scope.referenceContext));
      }
    } else {
      // XXX need to handle this too
      kind = null;
    }
    return kind;
  }

  @Override
  public Collection getDeclares() {
    return declares;
  }

  @Override
  public Collection getPrivilegedAccesses() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public Collection getTypeMungers() {
    return typeMungers;
  }

  @Override
  public boolean doesNotExposeShadowMungers() {
    return true;
  }

  @Override
  public String getDeclaredGenericSignature() {
    return CharOperation.charToString(binding.genericSignature());
  }

  @Override
  public boolean isGeneric() {
    return binding.isGenericType();
  }

  @Override
  public TypeVariable[] getTypeVariables() {
    if (declaration.typeParameters == null) {
      return new TypeVariable[0];
    }
    final TypeVariable[] typeVariables = new TypeVariable[declaration.typeParameters.length];
    for (int i = 0; i < typeVariables.length; i++) {
      typeVariables[i] = typeParameter2TypeVariable(declaration.typeParameters[i]);
    }
    return typeVariables;
  }

  private TypeVariable typeParameter2TypeVariable(TypeParameter typeParameter) {
    final String name = new String(typeParameter.name);
    final ReferenceBinding superclassBinding = typeParameter.binding.superclass;
    final UnresolvedType superclass = UnresolvedType.forSignature(new String(superclassBinding.signature()));
    UnresolvedType[] superinterfaces = null;
    final ReferenceBinding[] superInterfaceBindings = typeParameter.binding.superInterfaces;
    if (superInterfaceBindings != null) {
      superinterfaces = new UnresolvedType[superInterfaceBindings.length];
      for (int i = 0; i < superInterfaceBindings.length; i++) {
        superinterfaces[i] = UnresolvedType.forSignature(new String(superInterfaceBindings[i].signature()));
      }
    }
    // XXX what about lower binding?
    final TypeVariable tv = new TypeVariable(name, superclass, superinterfaces);
    tv.setDeclaringElement(factory.fromBinding(typeParameter.binding.declaringElement));
    tv.setRank(typeParameter.binding.rank);
    return tv;
  }

}
