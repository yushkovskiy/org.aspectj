/* *******************************************************************
 * Copyright (c) 2006 Contributors
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andy Clement                 initial implementation
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.IntConstant;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.*;
import org.aspectj.weaver.bcel.BcelObjectType;

/**
 * In the pipeline world, we can be weaving before all types have come through from compilation. In some cases this means the weaver
 * will want to ask questions of eclipse types and this subtype of ResolvedMemberImpl is here to answer some of those questions - it
 * is backed by the real eclipse MethodBinding object and can translate from Eclipse -> Weaver information.
 */
public class EclipseResolvedMember extends ResolvedMemberImpl {

  private static final String[] NO_ARGS = new String[]{};

  private final Binding realBinding;
  private String[] argumentNames;
  private final World w;
  private ResolvedType[] cachedAnnotationTypes;
  private ResolvedType[][] cachedParameterAnnotationTypes;
  private EclipseFactory eclipseFactory;

  public EclipseResolvedMember(MethodBinding binding, MemberKind memberKind, ResolvedType realDeclaringType, int modifiers,
                               UnresolvedType rettype, String name, UnresolvedType[] paramtypes, UnresolvedType[] extypes,
                               EclipseFactory eclipseFactory) {
    super(memberKind, realDeclaringType, modifiers, rettype, name, paramtypes, extypes);
    this.realBinding = binding;
    this.eclipseFactory = eclipseFactory;
    this.w = realDeclaringType.getWorld();
  }

  public EclipseResolvedMember(FieldBinding binding, MemberKind field, ResolvedType realDeclaringType, int modifiers,
                               ResolvedType type, String string, UnresolvedType[] none) {
    super(field, realDeclaringType, modifiers, type, string, none);
    this.realBinding = binding;
    this.w = realDeclaringType.getWorld();
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    final ResolvedType[] annotationTypes = getAnnotationTypes();
    if (annotationTypes == null) {
      return false;
    }
    for (int i = 0; i < annotationTypes.length; i++) {
      final ResolvedType type = annotationTypes[i];
      if (type.equals(ofType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    if (isTypeDeclarationAvailable()) {
      // long abits =
      realBinding.getAnnotationTagBits(); // ensure resolved
      final Annotation[] annos = getEclipseAnnotations();
      if (annos == null) {
        return null;
      }
      final AnnotationAJ[] annoAJs = new AnnotationAJ[annos.length];
      for (int i = 0; i < annos.length; i++) {
        annoAJs[i] = EclipseAnnotationConvertor.convertEclipseAnnotation(annos[i], w, eclipseFactory);
      }
      return annoAJs;
    } else {
      final UnresolvedType declaringType = this.getDeclaringType();
      if (declaringType instanceof ReferenceType) {
        final ReferenceType referenceDeclaringType = (ReferenceType) declaringType;
        if (referenceDeclaringType.getDelegate() instanceof BcelObjectType) {
          // worth a look!
          final ResolvedMember field = ((ResolvedType) declaringType).lookupField(this);
          if (field != null) {
            return field.getAnnotations();
          }
        }
      }
      return null;
    }
  }

  @Override
  public AnnotationAJ getAnnotationOfType(UnresolvedType ofType) {
    if (isTypeDeclarationAvailable()) {
      // long abits =
      realBinding.getAnnotationTagBits(); // ensure resolved
      final Annotation[] annos = getEclipseAnnotations();
      if (annos == null) {
        return null;
      }
      for (int i = 0; i < annos.length; i++) {
        final Annotation anno = annos[i];
        final UnresolvedType ut = UnresolvedType.forSignature(new String(anno.resolvedType.signature()));
        if (w.resolve(ut).equals(ofType)) {
          // Found the one
          return EclipseAnnotationConvertor.convertEclipseAnnotation(anno, w, eclipseFactory);
        }
      }
    } else {
      final UnresolvedType declaringType = this.getDeclaringType();
      if (declaringType instanceof ReferenceType) {
        final ReferenceType referenceDeclaringType = (ReferenceType) declaringType;
        if (referenceDeclaringType.getDelegate() instanceof BcelObjectType) {
          // worth a look!
          final ResolvedMember field = ((ResolvedType) declaringType).lookupField(this);
          if (field != null) {
            return field.getAnnotationOfType(ofType);
          }
        }
      }
    }
    return null;
  }

  @Override
  public String getAnnotationDefaultValue() {
    // Andy, if you are debugging in here and your problem is some kind of incremental build failure where a match on
    // a member annotation value is failing then you should look at bug 307120. Under that bug you discovered that
    // for privileged field accesses from ITDs, an EclipseResolvedMember may be created (inside the privileged munger - see
    // PrivilegedHandler) and then later when the annotations are looked up on it, that fails because we can't find the
    // declaration for the member. This is because on the incremental build the member will likely represent something
    // inside a binary type (BinaryTypeBinding) - and when that happens we should not look on the type declaration but
    // instead on the delegate for the declaringClass because it will likely be a BcelObjectType with the right stuff
    // in it - see the other checks on BcelObjectType in this class.
    if (realBinding instanceof MethodBinding) {
      final AbstractMethodDeclaration methodDecl = getTypeDeclaration().declarationOf((MethodBinding) realBinding);
      if (methodDecl instanceof AnnotationMethodDeclaration) {
        final AnnotationMethodDeclaration annoMethodDecl = (AnnotationMethodDeclaration) methodDecl;
        final Expression e = annoMethodDecl.defaultValue;
        if (e.resolvedType == null) {
          e.resolve(methodDecl.scope);
        }
        // TODO does not cope with many cases...
        if (e instanceof QualifiedNameReference) {

          final QualifiedNameReference qnr = (QualifiedNameReference) e;
          if (qnr.binding instanceof FieldBinding) {
            final FieldBinding fb = (FieldBinding) qnr.binding;
            final StringBuilder sb = new StringBuilder();
            sb.append(fb.declaringClass.signature());
            sb.append(fb.name);
            return sb.toString();
          }
        } else if (e instanceof TrueLiteral) {
          return "true";
        } else if (e instanceof FalseLiteral) {
          return "false";
        } else if (e instanceof StringLiteral) {
          return new String(((StringLiteral) e).source());
        } else if (e instanceof IntLiteral) {
          return Integer.toString(((IntConstant) e.constant).intValue());
        } else {
          throw new BCException("EclipseResolvedMember.getAnnotationDefaultValue() not implemented for value of type '"
              + e.getClass() + "' - raise an AspectJ bug !");
        }
      }
    }
    return null;
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    if (cachedAnnotationTypes == null) {
      // long abits =
      realBinding.getAnnotationTagBits(); // ensure resolved
      if (isTypeDeclarationAvailable()) {
        final Annotation[] annos = getEclipseAnnotations();
        if (annos == null) {
          cachedAnnotationTypes = ResolvedType.EMPTY_RESOLVED_TYPE_ARRAY;
        } else {
          cachedAnnotationTypes = new ResolvedType[annos.length];
          for (int i = 0; i < annos.length; i++) {
            final Annotation type = annos[i];
            final TypeBinding typebinding = type.resolvedType;
            // If there was a problem resolving the annotation (the import couldn't be found) then that can manifest
            // here as typebinding == null. Normally errors are reported prior to weaving (so weaving is avoided and
            // the null is not encountered) but the use of hasfield/hasmethod can cause early attempts to look at
            // annotations and if we NPE here then the real error will not get reported.
            if (typebinding == null) {
              // Give up now - expect proper error to be reported
              cachedAnnotationTypes = ResolvedType.EMPTY_RESOLVED_TYPE_ARRAY;
              return cachedAnnotationTypes;
            }
            cachedAnnotationTypes[i] = w.resolve(UnresolvedType.forSignature(new String(typebinding.signature())));
          }
        }
      } else {
        // annotations may be accessible through the declaringClass if there is a bcel object behind it...
        cachedAnnotationTypes = ResolvedType.EMPTY_RESOLVED_TYPE_ARRAY;
        final UnresolvedType declaringType = this.getDeclaringType();
        if (declaringType instanceof ReferenceType) {
          final ReferenceType referenceDeclaringType = (ReferenceType) declaringType;
          if (referenceDeclaringType.getDelegate() instanceof BcelObjectType) {
            // worth a look!
            if (this.getKind() == Member.METHOD) {
              final ResolvedMember method = ((ResolvedType) declaringType).lookupMethod(this);
              if (method != null) {
                cachedAnnotationTypes = method.getAnnotationTypes();
              }
            } else {
              final ResolvedMember field = ((ResolvedType) declaringType).lookupField(this);
              if (field != null) {
                cachedAnnotationTypes = field.getAnnotationTypes();
              }
            }
          }
        }
      }
    }
    return cachedAnnotationTypes;
  }

  private static final ResolvedType[][] NO_PARAM_ANNOS = new ResolvedType[0][];

  @Override
  public ResolvedType[][] getParameterAnnotationTypes() {
    if (cachedParameterAnnotationTypes == null) {
      realBinding.getAnnotationTagBits();
      final Annotation[][] pannos = getEclipseParameterAnnotations();
      if (pannos == null) {
        cachedParameterAnnotationTypes = NO_PARAM_ANNOS;
      } else {
        cachedParameterAnnotationTypes = new ResolvedType[pannos.length][];
        for (int i = 0; i < pannos.length; i++) {
          cachedParameterAnnotationTypes[i] = new ResolvedType[pannos[i].length];
          for (int j = 0; j < pannos[i].length; j++) {
            final TypeBinding typebinding = pannos[i][j].resolvedType;
            // If there was a problem resolving the annotation (the import couldn't be found) then that can manifest
            // here as typebinding == null. Normally errors are reported prior to weaving (so weaving is avoided and
            // the null is not encountered) but the use of hasfield/hasmethod can cause early attempts to look at
            // annotations and if we NPE here then the real error will not get reported.
            if (typebinding == null) {
              // Give up now - expect proper error to be reported
              cachedParameterAnnotationTypes = NO_PARAM_ANNOS;
              return cachedParameterAnnotationTypes;
            }
            cachedParameterAnnotationTypes[i][j] = w.resolve(UnresolvedType.forSignature(new String(typebinding
                .signature())));
          }
        }

      }
    }
    return cachedParameterAnnotationTypes;
  }

  @Override
  public String[] getParameterNames() {
    if (argumentNames != null) {
      return argumentNames;
    }
    if (realBinding instanceof FieldBinding) {
      argumentNames = NO_ARGS;
    } else {
      final TypeDeclaration typeDecl = getTypeDeclaration();
      final AbstractMethodDeclaration methodDecl = (typeDecl == null ? null : typeDecl.declarationOf((MethodBinding) realBinding));
      final Argument[] args = (methodDecl == null ? null : methodDecl.arguments); // dont
      // like
      // this
      // -
      // why
      // isnt
      // the
      // method
      // found
      // sometimes? is it because other errors are
      // being reported?
      if (args == null) {
        argumentNames = NO_ARGS;
      } else {
        argumentNames = new String[args.length];
        for (int i = 0; i < argumentNames.length; i++) {
          argumentNames[i] = new String(methodDecl.arguments[i].name);
        }
      }
    }
    return argumentNames;
  }

  /**
   * Discover the (eclipse form) annotations on this resolved member. This is done by going to the type declaration, looking up
   * the member (field/method) then grabbing the annotations.
   *
   * @return an array of (eclipse form) annotations on this member
   */
  private Annotation[] getEclipseAnnotations() {
    final TypeDeclaration tDecl = getTypeDeclaration();
    // two possible reasons for it being null:
    // 1. code is broken
    // 2. this resolvedmember is an EclipseResolvedMember created up front to represent a privileged'd accessed member
    if (tDecl != null) {
      if (realBinding instanceof MethodBinding) {
        final MethodBinding methodBinding = (MethodBinding) realBinding;
        final AbstractMethodDeclaration methodDecl = tDecl.declarationOf(methodBinding);
        if (methodDecl == null) {
          // pr284862
          // bindings may have been trashed by InterTypeMemberFinder.addInterTypeMethod() - and so we need to take
          // a better look. Really this EclipseResolvedMember is broken...

          // Grab the set of bindings with matching selector
          final MethodBinding[] mb = ((MethodBinding) realBinding).declaringClass.getMethods(methodBinding.selector);
          if (mb != null) {
            for (int m = 0, max = mb.length; m < max; m++) {
              final MethodBinding candidate = mb[m];
              if (candidate instanceof InterTypeMethodBinding) {
                if (InterTypeMemberFinder.matches(mb[m], methodBinding)) {
                  final InterTypeMethodBinding intertypeMethodBinding = (InterTypeMethodBinding) candidate;
                  final Annotation[] annos = intertypeMethodBinding.sourceMethod.annotations;
                  return annos;
                }
              }
            }
          }
          return null; // give up! kind of assuming here that the code has other problems (and they will be reported)
        }
        return methodDecl.annotations;
      } else if (realBinding instanceof FieldBinding) {
        final FieldDeclaration fieldDecl = tDecl.declarationOf((FieldBinding) realBinding);
        return fieldDecl.annotations;
      }
    }
    return null;
  }

  private Annotation[][] getEclipseParameterAnnotations() {
    final TypeDeclaration tDecl = getTypeDeclaration();
    // two possible reasons for it being null:
    // 1. code is broken
    // 2. this resolvedmember is an EclipseResolvedMember created up front to represent a privileged'd accessed member
    if (tDecl != null) {
      if (realBinding instanceof MethodBinding) {
        final MethodBinding methodBinding = (MethodBinding) realBinding;
        final AbstractMethodDeclaration methodDecl = tDecl.declarationOf(methodBinding);
        boolean foundsome = false;
        if (methodDecl != null) {
          final Argument[] args = methodDecl.arguments;
          if (args != null) {
            final int pcount = args.length;
            final Annotation[][] pannos = new Annotation[pcount][];
            for (int i = 0; i < pcount; i++) {
              pannos[i] = args[i].annotations;
              if (pannos[i] == null) {
                pannos[i] = NO_ANNOTATIONS;
              } else {
                for (int j = 0; j < pannos[i].length; j++) {
                  pannos[i][j].resolveType(methodDecl.scope);
                }
              }
              foundsome = foundsome || pannos[i].length != 0;
            }
            if (foundsome) {
              return pannos;
            }
          }
        }
      }
    }
    return null;
  }

  private final static Annotation[] NO_ANNOTATIONS = new Annotation[0];

  private boolean isTypeDeclarationAvailable() {
    return getTypeDeclaration() != null;
  }

  /**
   * @return the type declaration that contained this member, or NULL if it is not available (eg. this isn't currently related to
   * a SOURCE-FORM artifact, it is instead related to a BINARY-FORM artifact)
   */
  private TypeDeclaration getTypeDeclaration() {
    if (realBinding instanceof MethodBinding) {
      final MethodBinding mb = (MethodBinding) realBinding;
      if (mb != null) {
        final SourceTypeBinding stb = (SourceTypeBinding) mb.declaringClass;
        if (stb != null) {
          final ClassScope cScope = stb.scope;
          if (cScope != null) {
            return cScope.referenceContext;
          }
        }
      }
    } else if (realBinding instanceof FieldBinding) {
      final FieldBinding fb = (FieldBinding) realBinding;
      if (fb != null) {
        final SourceTypeBinding stb = (SourceTypeBinding) fb.declaringClass;
        if (stb != null) {
          final ClassScope cScope = stb.scope;
          if (cScope != null) {
            return cScope.referenceContext;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return true if this is the default constructor. The default constructor is the one generated if there isn't one in the
   * source. Eclipse helpfully uses a bit to indicate the default constructor.
   *
   * @return true if this is the default constructor.
   */
  @Override
  public boolean isDefaultConstructor() {
    if (!(realBinding instanceof MethodBinding)) {
      return false;
    }
    final MethodBinding mb = (MethodBinding) realBinding;
    return mb.isConstructor() && ((mb.modifiers & ExtraCompilerModifiers.AccIsDefaultConstructor) != 0);
  }

}
