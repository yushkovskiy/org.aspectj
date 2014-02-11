/* *******************************************************************
 * Copyright (c) 2002 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 *     RonBodkin/AndyClement optimizations for memory consumption/speed
 * ******************************************************************/

package org.aspectj.weaver.bcel;

import org.aspectj.apache.bcel.classfile.*;
import org.aspectj.apache.bcel.classfile.annotation.AnnotationGen;
import org.aspectj.apache.bcel.classfile.annotation.EnumElementValue;
import org.aspectj.apache.bcel.classfile.annotation.NameValuePair;
import org.aspectj.asm.AsmManager;
import org.aspectj.bridge.IMessageHandler;
import org.aspectj.bridge.MessageUtil;
import org.aspectj.util.GenericSignature;
import org.aspectj.util.GenericSignature.FormalTypeParameter;
import org.aspectj.weaver.*;
import org.aspectj.weaver.bcel.BcelGenericSignatureToTypeXConverter.GenericSignatureFormatException;
import org.aspectj.weaver.patterns.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.*;

public final class BcelObjectType extends AbstractReferenceTypeDelegate {

  // discovery bits
  private static final int DISCOVERED_ANNOTATION_RETENTION_POLICY = 0x0001;
  private static final int UNPACKED_GENERIC_SIGNATURE = 0x0002;
  private static final int UNPACKED_AJATTRIBUTES = 0x0004; // see note(1)
  // below
  private static final int DISCOVERED_ANNOTATION_TARGET_KINDS = 0x0008;
  private static final int DISCOVERED_DECLARED_SIGNATURE = 0x0010;
  private static final int DISCOVERED_WHETHER_ANNOTATION_STYLE = 0x0020;

  private static final int ANNOTATION_UNPACK_IN_PROGRESS = 0x0100;
  @NotNull
  private static final String[] NO_INTERFACE_SIGS = new String[]{};
  @Nullable
  public JavaClass javaClass;
  private boolean artificial; // Was the BcelObject built from an artificial set of bytes? Or from the real ondisk stuff?
  @Nullable
  private LazyClassGen lazyClassGen = null; // set lazily if it's an aspect

  private int modifiers;
  private String className;

  private String superclassSignature;
  private String superclassName;
  private String[] interfaceSignatures;

  @Nullable
  private ResolvedMember[] fields = null;
  @Nullable
  private ResolvedMember[] methods = null;
  @Nullable
  private ResolvedType[] annotationTypes = null;
  @Nullable
  private AnnotationAJ[] annotations = null;
  @Nullable
  private TypeVariable[] typeVars = null;
  private String retentionPolicy;
  private AnnotationTargetKind[] annotationTargetKinds;

  // Aspect related stuff (pointcuts *could* be in a java class)
  @NotNull
  private AjAttribute.WeaverVersionInfo wvInfo = AjAttribute.WeaverVersionInfo.UNKNOWN;
  @Nullable
  private ResolvedPointcutDefinition[] pointcuts = null;
  @Nullable
  private ResolvedMember[] privilegedAccess = null;
  @Nullable
  private WeaverStateInfo weaverState = null;
  @Nullable
  private PerClause perClause = null;
  @NotNull
  private List<ConcreteTypeMunger> typeMungers = Collections.emptyList();
  @NotNull
  private List<Declare> declares = Collections.emptyList();
  @Nullable
  private GenericSignature.FormalTypeParameter[] formalsForResolution = null;
  @Nullable
  private String declaredSignature = null;

  private boolean hasBeenWoven = false;
  private boolean isGenericType = false;
  private boolean isInterface;
  private boolean isEnum;
  private boolean isAnnotation;
  private boolean isAnonymous;
  private boolean isNested;
  private boolean isObject = false; // set upon construction
  private boolean isAnnotationStyleAspect = false;// set upon construction
  private boolean isCodeStyleAspect = false; // not redundant with field
  // above!

  @NotNull
  private WeakReference<ResolvedType> superTypeReference = new WeakReference<ResolvedType>(null);
  @NotNull
  private WeakReference<ResolvedType[]> superInterfaceReferences = new WeakReference<ResolvedType[]>(null);

  private int bitflag = 0x0000;

	/*
   * Notes: note(1): in some cases (perclause inheritance) we encounter unpacked state when calling getPerClause
	 * 
	 * note(2): A BcelObjectType is 'damaged' if it has been modified from what was original constructed from the bytecode. This
	 * currently happens if the parents are modified or an annotation is added - ideally BcelObjectType should be immutable but
	 * that's a bigger piece of work. XXX
	 */

  BcelObjectType(@NotNull ReferenceType resolvedTypeX, @NotNull JavaClass javaClass, boolean artificial, boolean exposedToWeaver) {
    super(resolvedTypeX, exposedToWeaver);
    this.javaClass = javaClass;
    this.artificial = artificial;
    initializeFromJavaclass();

    // ATAJ: set the delegate right now for @AJ pointcut, else it is done
    // too late to lookup
    // @AJ pc refs annotation in class hierarchy
    resolvedTypeX.setDelegate(this);

    ISourceContext sourceContext = resolvedTypeX.getSourceContext();
    if (sourceContext == SourceContextImpl.UNKNOWN_SOURCE_CONTEXT) {
      sourceContext = new SourceContextImpl(this);
      setSourceContext(sourceContext);
    }

    // this should only ever be java.lang.Object which is
    // the only class in Java-1.4 with no superclasses
    isObject = (javaClass.getSuperclassNameIndex() == 0);
    ensureAspectJAttributesUnpacked();
    // if (sourceContext instanceof SourceContextImpl) {
    // ((SourceContextImpl)sourceContext).setSourceFileName(javaClass.
    // getSourceFileName());
    // }
    setSourcefilename(javaClass.getSourceFileName());
  }

  // repeat initialization
  public void setJavaClass(@NotNull JavaClass newclass, boolean artificial) {
    this.javaClass = newclass;
    this.artificial = artificial;
    resetState();
    initializeFromJavaclass();
  }

  @Override
  public boolean isCacheable() {
    return true;
  }

  // --- getters

  // Java related
  @Override
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean isEnum() {
    return isEnum;
  }

  @Override
  public boolean isAnnotation() {
    return isAnnotation;
  }

  @Override
  public boolean isAnonymous() {
    return isAnonymous;
  }

  @Override
  public boolean isNested() {
    return isNested;
  }

  @Override
  public int getModifiers() {
    return modifiers;
  }

  /**
   * Must take into account generic signature
   */
  @Override
  @Nullable
  public ResolvedType getSuperclass() {
    if (isObject) {
      return null;
    }
    ResolvedType supertype = superTypeReference.get();
    if (supertype == null) {
      ensureGenericSignatureUnpacked();
      if (superclassSignature == null) {
        if (superclassName == null) {
          superclassName = javaClass.getSuperclassName();
        }
        superclassSignature = getResolvedTypeX().getWorld().resolve(UnresolvedType.forName(superclassName)).getSignature();
      }
      final World world = getResolvedTypeX().getWorld();
      supertype = world.resolve(UnresolvedType.forSignature(superclassSignature));
      superTypeReference = new WeakReference<ResolvedType>(supertype);
    }
    return supertype;
  }

  public World getWorld() {
    return getResolvedTypeX().getWorld();
  }

  /**
   * Retrieves the declared interfaces - this allows for the generic signature on a type. If specified then the generic signature
   * is used to work out the types - this gets around the results of erasure when the class was originally compiled.
   */
  @Override
  @NotNull
  public ResolvedType[] getDeclaredInterfaces() {
    final ResolvedType[] cachedInterfaceTypes = superInterfaceReferences.get();
    if (cachedInterfaceTypes == null) {
      ensureGenericSignatureUnpacked();
      ResolvedType[] interfaceTypes = null;
      if (interfaceSignatures == null) {
        final String[] names = javaClass.getInterfaceNames();
        if (names.length == 0) {
          interfaceSignatures = NO_INTERFACE_SIGS;
          interfaceTypes = ResolvedType.NONE;
        } else {
          interfaceSignatures = new String[names.length];
          interfaceTypes = new ResolvedType[names.length];
          for (int i = 0, len = names.length; i < len; i++) {
            interfaceTypes[i] = getResolvedTypeX().getWorld().resolve(UnresolvedType.forName(names[i]));
            interfaceSignatures[i] = interfaceTypes[i].getSignature();
          }
        }
      } else {
        interfaceTypes = new ResolvedType[interfaceSignatures.length];
        for (int i = 0, len = interfaceSignatures.length; i < len; i++) {
          interfaceTypes[i] = getResolvedTypeX().getWorld().resolve(UnresolvedType.forSignature(interfaceSignatures[i]));
        }
      }
      superInterfaceReferences = new WeakReference<ResolvedType[]>(interfaceTypes);
      return interfaceTypes;
    } else {
      return cachedInterfaceTypes;
    }
  }

  @Override
  @NotNull
  public ResolvedMember[] getDeclaredMethods() {
    ensureGenericSignatureUnpacked();
    if (methods == null) {
      final Method[] ms = javaClass.getMethods();
      final ResolvedMember[] newMethods = new ResolvedMember[ms.length];
      for (int i = ms.length - 1; i >= 0; i--) {
        newMethods[i] = new BcelMethod(this, ms[i]);
      }
      methods = newMethods;
    }
    return methods;
  }

  @Override
  @NotNull
  public ResolvedMember[] getDeclaredFields() {
    ensureGenericSignatureUnpacked();
    if (fields == null) {
      final Field[] fs = javaClass.getFields();
      final ResolvedMember[] newfields = new ResolvedMember[fs.length];
      for (int i = 0, len = fs.length; i < len; i++) {
        newfields[i] = new BcelField(this, fs[i]);
      }
      fields = newfields;
    }
    return fields;
  }

  @Override
  @NotNull
  public TypeVariable[] getTypeVariables() {
    if (!isGeneric()) {
      return TypeVariable.NONE;
    }

    if (typeVars == null) {
      final GenericSignature.ClassSignature classSig = getGenericClassTypeSignature();
      typeVars = new TypeVariable[classSig.formalTypeParameters.length];
      for (int i = 0; i < typeVars.length; i++) {
        final GenericSignature.FormalTypeParameter ftp = classSig.formalTypeParameters[i];
        try {
          typeVars[i] = BcelGenericSignatureToTypeXConverter.formalTypeParameter2TypeVariable(ftp,
              classSig.formalTypeParameters, getResolvedTypeX().getWorld());
        } catch (GenericSignatureFormatException e) {
          // this is a development bug, so fail fast with good info
          throw new IllegalStateException("While getting the type variables for type " + this.toString()
              + " with generic signature " + classSig + " the following error condition was detected: "
              + e.getMessage());
        }
      }
    }
    return typeVars;
  }

  @Override
  public Collection<ConcreteTypeMunger> getTypeMungers() {
    return typeMungers;
  }

  @Override
  public Collection<Declare> getDeclares() {
    return declares;
  }

  @Override
  @NotNull
  public Collection<ResolvedMember> getPrivilegedAccesses() {
    if (privilegedAccess == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(privilegedAccess);
  }

  @Override
  public ResolvedMember[] getDeclaredPointcuts() {
    return pointcuts;
  }

  @Override
  public boolean isAspect() {
    return perClause != null;
  }

  /**
   * Check if the type is an @AJ aspect (no matter if used from an LTW point of view). Such aspects are annotated with @Aspect
   *
   * @return true for @AJ aspect
   */
  @Override
  public boolean isAnnotationStyleAspect() {
    if ((bitflag & DISCOVERED_WHETHER_ANNOTATION_STYLE) == 0) {
      bitflag |= DISCOVERED_WHETHER_ANNOTATION_STYLE;
      isAnnotationStyleAspect = !isCodeStyleAspect && hasAnnotation(AjcMemberMaker.ASPECT_ANNOTATION);
    }
    return isAnnotationStyleAspect;
  }

  @Override
  public PerClause getPerClause() {
    ensureAspectJAttributesUnpacked();
    return perClause;
  }

  public JavaClass getJavaClass() {
    return javaClass;
  }

  public boolean isArtificial() {
    return artificial;
  }

  public void resetState() {
    if (javaClass == null) {
      // we might store the classname and allow reloading?
      // At this point we are relying on the world to not evict if it
      // might want to reweave multiple times
      throw new BCException("can't weave evicted type");
    }

    bitflag = 0x0000;

    this.annotationTypes = null;
    this.annotations = null;
    this.interfaceSignatures = null;
    this.superclassSignature = null;
    this.superclassName = null;
    this.fields = null;
    this.methods = null;
    this.pointcuts = null;
    this.perClause = null;
    this.weaverState = null;
    this.lazyClassGen = null;
    hasBeenWoven = false;

    isObject = (javaClass.getSuperclassNameIndex() == 0);
    isAnnotationStyleAspect = false;
    ensureAspectJAttributesUnpacked();
  }

  public void finishedWith() {
    // memory usage experiments....
    // this.interfaces = null;
    // this.superClass = null;
    // this.fields = null;
    // this.methods = null;
    // this.pointcuts = null;
    // this.perClause = null;
    // this.weaverState = null;
    // this.lazyClassGen = null;
    // this next line frees up memory, but need to understand incremental
    // implications
    // before leaving it in.
    // getResolvedTypeX().setSourceContext(null);
  }

  @Override
  public WeaverStateInfo getWeaverState() {
    return weaverState;
  }

  public void printWackyStuff(@NotNull PrintStream out) {
    if (typeMungers.size() > 0) {
      out.println("  TypeMungers: " + typeMungers);
    }
    if (declares.size() > 0) {
      out.println("     declares: " + declares);
    }
  }

  /**
   * Return the lazyClassGen associated with this type. For aspect types, this value will be cached, since it is used to inline
   * advice. For non-aspect types, this lazyClassGen is always newly constructed.
   */
  @NotNull
  public LazyClassGen getLazyClassGen() {
    LazyClassGen ret = lazyClassGen;
    if (ret == null) {
      // System.err.println("creating lazy class gen for: " + this);
      ret = new LazyClassGen(this);
      // ret.print(System.err);
      // System.err.println("made LCG from : " +
      // this.getJavaClass().getSuperclassName );
      if (isAspect()) {
        lazyClassGen = ret;
      }
    }
    return ret;
  }

  public boolean isSynthetic() {
    return getResolvedTypeX().isSynthetic();
  }

  @NotNull
  public AjAttribute.WeaverVersionInfo getWeaverVersionAttribute() {
    return wvInfo;
  }

  // -- annotation related

  @Override
  public ResolvedType[] getAnnotationTypes() {
    ensureAnnotationsUnpacked();
    return annotationTypes;
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    ensureAnnotationsUnpacked();
    return annotations;
  }

  @Override
  public boolean hasAnnotation(@NotNull UnresolvedType ofType) {
    // Due to re-entrancy we may be in the middle of unpacking the annotations already... in which case use this slow
    // alternative until the stack unwinds itself
    if (isUnpackingAnnotations()) {
      final AnnotationGen[] annos = javaClass.getAnnotations();
      if (annos == null || annos.length == 0) {
        return false;
      } else {
        final String lookingForSignature = ofType.getSignature();
        for (int a = 0; a < annos.length; a++) {
          final AnnotationGen annotation = annos[a];
          if (lookingForSignature.equals(annotation.getTypeSignature())) {
            return true;
          }
        }
      }
      return false;
    }
    ensureAnnotationsUnpacked();
    for (int i = 0, max = annotationTypes.length; i < max; i++) {
      final UnresolvedType ax = annotationTypes[i];
      if (ax == null) {
        throw new RuntimeException("Annotation entry " + i + " on type " + this.getResolvedTypeX().getName() + " is null!");
      }
      if (ax.equals(ofType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAnnotationWithRuntimeRetention() {
    return (getRetentionPolicy() == null ? false : getRetentionPolicy().equals("RUNTIME"));
  }

  @Override
  @Nullable
  public String getRetentionPolicy() {
    if ((bitflag & DISCOVERED_ANNOTATION_RETENTION_POLICY) == 0) {
      bitflag |= DISCOVERED_ANNOTATION_RETENTION_POLICY;
      retentionPolicy = null; // null means we have no idea
      if (isAnnotation()) {
        ensureAnnotationsUnpacked();
        for (int i = annotations.length - 1; i >= 0; i--) {
          final AnnotationAJ ax = annotations[i];
          if (ax.getTypeName().equals(UnresolvedType.AT_RETENTION.getName())) {
            final List<NameValuePair> values = ((BcelAnnotation) ax).getBcelAnnotation().getValues();
            for (final Iterator<NameValuePair> it = values.iterator(); it.hasNext(); ) {
              final NameValuePair element = it.next();
              final EnumElementValue v = (EnumElementValue) element.getValue();
              retentionPolicy = v.getEnumValueString();
              return retentionPolicy;
            }
          }
        }
      }
    }
    return retentionPolicy;
  }

  @Override
  public boolean canAnnotationTargetType() {
    final AnnotationTargetKind[] targetKinds = getAnnotationTargetKinds();
    if (targetKinds == null) {
      return true;
    }
    for (int i = 0; i < targetKinds.length; i++) {
      if (targetKinds[i].equals(AnnotationTargetKind.TYPE)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnnotationTargetKind[] getAnnotationTargetKinds() {
    if ((bitflag & DISCOVERED_ANNOTATION_TARGET_KINDS) != 0) {
      return annotationTargetKinds;
    }
    bitflag |= DISCOVERED_ANNOTATION_TARGET_KINDS;
    annotationTargetKinds = null; // null means we have no idea or the
    // @Target annotation hasn't been used
    final List<AnnotationTargetKind> targetKinds = new ArrayList<AnnotationTargetKind>();
    if (isAnnotation()) {
      final AnnotationAJ[] annotationsOnThisType = getAnnotations();
      for (int i = 0; i < annotationsOnThisType.length; i++) {
        final AnnotationAJ a = annotationsOnThisType[i];
        if (a.getTypeName().equals(UnresolvedType.AT_TARGET.getName())) {
          final Set<String> targets = a.getTargets();
          if (targets != null) {
            for (String targetKind : targets) {
              if (targetKind.equals("ANNOTATION_TYPE")) {
                targetKinds.add(AnnotationTargetKind.ANNOTATION_TYPE);
              } else if (targetKind.equals("CONSTRUCTOR")) {
                targetKinds.add(AnnotationTargetKind.CONSTRUCTOR);
              } else if (targetKind.equals("FIELD")) {
                targetKinds.add(AnnotationTargetKind.FIELD);
              } else if (targetKind.equals("LOCAL_VARIABLE")) {
                targetKinds.add(AnnotationTargetKind.LOCAL_VARIABLE);
              } else if (targetKind.equals("METHOD")) {
                targetKinds.add(AnnotationTargetKind.METHOD);
              } else if (targetKind.equals("PACKAGE")) {
                targetKinds.add(AnnotationTargetKind.PACKAGE);
              } else if (targetKind.equals("PARAMETER")) {
                targetKinds.add(AnnotationTargetKind.PARAMETER);
              } else if (targetKind.equals("TYPE")) {
                targetKinds.add(AnnotationTargetKind.TYPE);
              }
            }
          }
        }
      }
      if (!targetKinds.isEmpty()) {
        annotationTargetKinds = new AnnotationTargetKind[targetKinds.size()];
        return targetKinds.toArray(annotationTargetKinds);
      }
    }
    return annotationTargetKinds;
  }

  // ---

  @Override
  public String getDeclaredGenericSignature() {
    ensureGenericInfoProcessed();
    return declaredSignature;
  }

  @Nullable
  public GenericSignature.FormalTypeParameter[] getAllFormals() {
    ensureGenericSignatureUnpacked();
    if (formalsForResolution == null) {
      return new GenericSignature.FormalTypeParameter[0];
    } else {
      return formalsForResolution;
    }
  }

  @Override
  public ResolvedType getOuterClass() {
    if (!isNested()) {
      throw new IllegalStateException("Can't get the outer class of non-nested type: " + className);
    }

    // try finding outer class name from InnerClasses attribute assigned to this class
    for (Attribute attr : javaClass.getAttributes()) {
      if (attr instanceof InnerClasses) {
        // search for InnerClass entry that has current class as inner and some other class as outer
        final InnerClass[] innerClss = ((InnerClasses) attr).getInnerClasses();
        final ConstantPool cpool = javaClass.getConstantPool();
        for (InnerClass innerCls : innerClss) {
          // skip entries that miss any necessary component, 0 index means "undefined", from JVM Spec 2nd ed. par. 4.7.5
          if (innerCls.getInnerClassIndex() == 0 || innerCls.getOuterClassIndex() == 0) {
            continue;
          }

          // resolve inner class name, check if it matches current class name
          final ConstantClass innerClsInfo = (ConstantClass) cpool.getConstant(innerCls.getInnerClassIndex());

          // class names in constant pool use '/' instead of '.', from JVM Spec 2nd ed. par. 4.2
          final String innerClsName = cpool.getConstantUtf8(innerClsInfo.getNameIndex()).getValue().replace('/', '.');

          if (innerClsName.compareTo(className) == 0) {
            // resolve outer class name
            final ConstantClass outerClsInfo = (ConstantClass) cpool.getConstant(innerCls.getOuterClassIndex());

            // class names in constant pool use '/' instead of '.', from JVM Spec 2nd ed. par. 4.2
            final String outerClsName = cpool.getConstantUtf8(outerClsInfo.getNameIndex()).getValue().replace('/', '.');

            final UnresolvedType outer = UnresolvedType.forName(outerClsName);
            return outer.resolve(getResolvedTypeX().getWorld());
          }
        }
      }
    }

    for (Attribute attr : javaClass.getAttributes()) { // bug339300
      final ConstantPool cpool = javaClass.getConstantPool();
      if (attr instanceof EnclosingMethod) {
        final EnclosingMethod enclosingMethodAttribute = (EnclosingMethod) attr;
        if (enclosingMethodAttribute.getEnclosingClassIndex() != 0) {
          final ConstantClass outerClassInfo = enclosingMethodAttribute.getEnclosingClass();
          final String outerClassName = cpool.getConstantUtf8(outerClassInfo.getNameIndex()).getValue().replace('/', '.');
          final UnresolvedType outer = UnresolvedType.forName(outerClassName);
          return outer.resolve(getResolvedTypeX().getWorld());
        }
      }
    }

    // try finding outer class name by assuming standard class name mangling convention of javac for this class
    final int lastDollar = className.lastIndexOf('$');
    final String superClassName = className.substring(0, lastDollar);
    final UnresolvedType outer = UnresolvedType.forName(superClassName);
    return outer.resolve(getResolvedTypeX().getWorld());
  }

  @Override
  public boolean isGeneric() {
    ensureGenericInfoProcessed();
    return isGenericType;
  }

  @Override
  public String toString() {
    return (javaClass == null ? "BcelObjectType" : "BcelObjectTypeFor:" + className);
  }

  // --- state management

  public void evictWeavingState() {
    // Can't chuck all this away
    if (getResolvedTypeX().getWorld().couldIncrementalCompileFollow()) {
      return;
    }

    if (javaClass != null) {
      // Force retrieval of any lazy information
      ensureAnnotationsUnpacked();
      ensureGenericInfoProcessed();

      getDeclaredInterfaces();
      getDeclaredFields();
      getDeclaredMethods();
      // The lazyClassGen is preserved for aspects - it exists to enable
      // around advice
      // inlining since the method will need 'injecting' into the affected
      // class. If
      // XnoInline is on, we can chuck away the lazyClassGen since it
      // won't be required
      // later.
      if (getResolvedTypeX().getWorld().isXnoInline()) {
        lazyClassGen = null;
      }

      // discard expensive bytecode array containing reweavable info
      if (weaverState != null) {
        weaverState.setReweavable(false);
        weaverState.setUnwovenClassFileData(null);
      }
      for (int i = methods.length - 1; i >= 0; i--) {
        methods[i].evictWeavingState();
      }
      for (int i = fields.length - 1; i >= 0; i--) {
        fields[i].evictWeavingState();
      }
      javaClass = null;
      this.artificial = true;
      // setSourceContext(SourceContextImpl.UNKNOWN_SOURCE_CONTEXT); //
      // bit naughty
      // interfaces=null; // force reinit - may get us the right
      // instances!
      // superClass=null;
    }
  }

  public void weavingCompleted() {
    hasBeenWoven = true;
    if (getResolvedTypeX().getWorld().isRunMinimalMemory()) {
      evictWeavingState();
    }
    if (getSourceContext() != null && !getResolvedTypeX().isAspect()) {
      getSourceContext().tidy();
    }
  }

  @Override
  public boolean hasBeenWoven() {
    return hasBeenWoven;
  }

  @Override
  public boolean copySourceContext() {
    return false;
  }

  public void setExposedToWeaver(boolean b) {
    exposedToWeaver = b;
  }

  @Override
  public int getCompilerVersion() {
    return wvInfo.getMajorVersion();
  }

  @Override
  public void ensureConsistent() {
    superTypeReference.clear();
    superInterfaceReferences.clear();
  }

  @Override
  public boolean isWeavable() {
    return true;
  }

  void setWeaverState(@NotNull WeaverStateInfo weaverState) {
    this.weaverState = weaverState;
  }

  private void initializeFromJavaclass() {
    isInterface = javaClass.isInterface();
    isEnum = javaClass.isEnum();
    isAnnotation = javaClass.isAnnotation();
    isAnonymous = javaClass.isAnonymous();
    isNested = javaClass.isNested();
    modifiers = javaClass.getModifiers();
    superclassName = javaClass.getSuperclassName();
    className = javaClass.getClassName();
    cachedGenericClassTypeSignature = null;
  }

  /**
   * Process any org.aspectj.weaver attributes stored against the class.
   */
  private void ensureAspectJAttributesUnpacked() {
    if ((bitflag & UNPACKED_AJATTRIBUTES) != 0) {
      return;
    }
    bitflag |= UNPACKED_AJATTRIBUTES;
    final IMessageHandler msgHandler = getResolvedTypeX().getWorld().getMessageHandler();
    // Pass in empty list that can store things for readAj5 to process
    List<AjAttribute> l = null;
    try {
      l = Utility.readAjAttributes(className, javaClass.getAttributes(), getResolvedTypeX().getSourceContext(),
          getResolvedTypeX().getWorld(), AjAttribute.WeaverVersionInfo.UNKNOWN,
          new BcelConstantPoolReader(javaClass.getConstantPool()));
    } catch (RuntimeException re) {
      throw new RuntimeException("Problem processing attributes in " + javaClass.getFileName(), re);
    }
    final List<ResolvedPointcutDefinition> pointcuts = new ArrayList<ResolvedPointcutDefinition>();
    typeMungers = new ArrayList<ConcreteTypeMunger>();
    declares = new ArrayList<Declare>();
    processAttributes(l, pointcuts, false);
    final ReferenceType type = getResolvedTypeX();
    final AsmManager asmManager = ((BcelWorld) type.getWorld()).getModelAsAsmManager();
    l = AtAjAttributes.readAj5ClassAttributes(asmManager, javaClass, type, type.getSourceContext(), msgHandler,
        isCodeStyleAspect);
    final AjAttribute.Aspect deferredAspectAttribute = processAttributes(l, pointcuts, true);

    if (pointcuts.size() == 0) {
      this.pointcuts = ResolvedPointcutDefinition.NO_POINTCUTS;
    } else {
      this.pointcuts = pointcuts.toArray(new ResolvedPointcutDefinition[pointcuts.size()]);
    }

    resolveAnnotationDeclares(l);

    if (deferredAspectAttribute != null) {
      // we can finally process the aspect and its associated perclause...
      perClause = deferredAspectAttribute.reifyFromAtAspectJ(this.getResolvedTypeX());
    }
    if (isAspect() && !Modifier.isAbstract(getModifiers()) && isGeneric()) {
      msgHandler.handleMessage(MessageUtil.error("The generic aspect '" + getResolvedTypeX().getName()
          + "' must be declared abstract", getResolvedTypeX().getSourceLocation()));
    }

  }

  @Nullable
  private AjAttribute.Aspect processAttributes(@NotNull List<AjAttribute> attributeList,
                                               @NotNull List<ResolvedPointcutDefinition> pointcuts,
                                               boolean fromAnnotations) {
    AjAttribute.Aspect deferredAspectAttribute = null;
    for (AjAttribute a : attributeList) {
      if (a instanceof AjAttribute.Aspect) {
        if (fromAnnotations) {
          deferredAspectAttribute = (AjAttribute.Aspect) a;
        } else {
          perClause = ((AjAttribute.Aspect) a).reify(this.getResolvedTypeX());
          isCodeStyleAspect = true;
        }
      } else if (a instanceof AjAttribute.PointcutDeclarationAttribute) {
        pointcuts.add(((AjAttribute.PointcutDeclarationAttribute) a).reify());
      } else if (a instanceof AjAttribute.WeaverState) {
        weaverState = ((AjAttribute.WeaverState) a).reify();
      } else if (a instanceof AjAttribute.TypeMunger) {
        typeMungers.add(((AjAttribute.TypeMunger) a).reify(getResolvedTypeX().getWorld(), getResolvedTypeX()));
      } else if (a instanceof AjAttribute.DeclareAttribute) {
        declares.add(((AjAttribute.DeclareAttribute) a).getDeclare());
      } else if (a instanceof AjAttribute.PrivilegedAttribute) {
        final AjAttribute.PrivilegedAttribute privAttribute = (AjAttribute.PrivilegedAttribute) a;
        privilegedAccess = privAttribute.getAccessedMembers();
      } else if (a instanceof AjAttribute.SourceContextAttribute) {
        if (getResolvedTypeX().getSourceContext() instanceof SourceContextImpl) {
          final AjAttribute.SourceContextAttribute sca = (AjAttribute.SourceContextAttribute) a;
          ((SourceContextImpl) getResolvedTypeX().getSourceContext()).configureFromAttribute(sca.getSourceFileName(),
              sca.getLineBreaks());

          setSourcefilename(sca.getSourceFileName());
        }
      } else if (a instanceof AjAttribute.WeaverVersionInfo) {
        // Set the weaver version used to build this type
        wvInfo = (AjAttribute.WeaverVersionInfo) a;
      } else {
        throw new BCException("bad attribute " + a);
      }
    }
    return deferredAspectAttribute;
  }

  /**
   * Extra processing step needed because declares that come from annotations are not pre-resolved. We can't do the resolution
   * until *after* the pointcuts have been resolved.
   */
  private void resolveAnnotationDeclares(@NotNull List<AjAttribute> attributeList) {
    final FormalBinding[] bindings = new org.aspectj.weaver.patterns.FormalBinding[0];
    final IScope bindingScope = new BindingScope(getResolvedTypeX(), getResolvedTypeX().getSourceContext(), bindings);
    for (final Iterator<AjAttribute> iter = attributeList.iterator(); iter.hasNext(); ) {
      final AjAttribute a = iter.next();
      if (a instanceof AjAttribute.DeclareAttribute) {
        final Declare decl = (((AjAttribute.DeclareAttribute) a).getDeclare());
        if (decl instanceof DeclareErrorOrWarning) {
          decl.resolve(bindingScope);
        } else if (decl instanceof DeclarePrecedence) {
          ((DeclarePrecedence) decl).setScopeForResolution(bindingScope);
        }
      }
    }
  }

  // --- unpacking methods

  private boolean isUnpackingAnnotations() {
    return (bitflag & ANNOTATION_UNPACK_IN_PROGRESS) != 0;
  }

  private void ensureAnnotationsUnpacked() {
    if (isUnpackingAnnotations()) {
      throw new BCException("Re-entered weaver instance whilst unpacking annotations on " + this.className);
    }
    if (annotationTypes == null) {
      try {
        bitflag |= ANNOTATION_UNPACK_IN_PROGRESS;
        final AnnotationGen[] annos = javaClass.getAnnotations();
        if (annos == null || annos.length == 0) {
          annotationTypes = ResolvedType.NONE;
          annotations = AnnotationAJ.EMPTY_ARRAY;
        } else {
          final World w = getResolvedTypeX().getWorld();
          annotationTypes = new ResolvedType[annos.length];
          annotations = new AnnotationAJ[annos.length];
          for (int i = 0; i < annos.length; i++) {
            final AnnotationGen annotation = annos[i];
            final String typeSignature = annotation.getTypeSignature();
            final ResolvedType rType = w.resolve(UnresolvedType.forSignature(typeSignature));
            if (rType == null) {
              throw new RuntimeException("Whilst unpacking annotations on '" + getResolvedTypeX().getName()
                  + "', failed to resolve type '" + typeSignature + "'");
            }
            annotationTypes[i] = rType;
            annotations[i] = new BcelAnnotation(annotation, rType);
          }
        }
      } finally {
        bitflag &= ~ANNOTATION_UNPACK_IN_PROGRESS;
      }
    }
  }

  private void ensureGenericSignatureUnpacked() {
    if ((bitflag & UNPACKED_GENERIC_SIGNATURE) != 0) {
      return;
    }
    bitflag |= UNPACKED_GENERIC_SIGNATURE;
    if (!getResolvedTypeX().getWorld().isInJava5Mode()) {
      return;
    }
    final GenericSignature.ClassSignature cSig = getGenericClassTypeSignature();
    if (cSig != null) {
      formalsForResolution = cSig.formalTypeParameters;
      if (isNested()) {
        // we have to find any type variables from the outer type before
        // proceeding with resolution.
        final GenericSignature.FormalTypeParameter[] extraFormals = getFormalTypeParametersFromOuterClass();
        if (extraFormals.length > 0) {
          final List<FormalTypeParameter> allFormals = new ArrayList<FormalTypeParameter>();
          for (int i = 0; i < formalsForResolution.length; i++) {
            allFormals.add(formalsForResolution[i]);
          }
          for (int i = 0; i < extraFormals.length; i++) {
            allFormals.add(extraFormals[i]);
          }
          formalsForResolution = new GenericSignature.FormalTypeParameter[allFormals.size()];
          allFormals.toArray(formalsForResolution);
        }
      }
      final GenericSignature.ClassTypeSignature superSig = cSig.superclassSignature;
      try {
        // this.superClass =
        // BcelGenericSignatureToTypeXConverter.classTypeSignature2TypeX(
        // superSig, formalsForResolution,
        // getResolvedTypeX().getWorld());

        final ResolvedType rt = BcelGenericSignatureToTypeXConverter.classTypeSignature2TypeX(superSig, formalsForResolution,
            getResolvedTypeX().getWorld());
        this.superclassSignature = rt.getSignature();
        this.superclassName = rt.getName();
      } catch (GenericSignatureFormatException e) {
        // development bug, fail fast with good info
        throw new IllegalStateException("While determining the generic superclass of " + this.className
            + " with generic signature " + getDeclaredGenericSignature() + " the following error was detected: "
            + e.getMessage());
      }
      // this.interfaces = new
      // ResolvedType[cSig.superInterfaceSignatures.length];
      if (cSig.superInterfaceSignatures.length == 0) {
        this.interfaceSignatures = NO_INTERFACE_SIGS;
      } else {
        this.interfaceSignatures = new String[cSig.superInterfaceSignatures.length];
        for (int i = 0; i < cSig.superInterfaceSignatures.length; i++) {
          try {
            // this.interfaces[i] =
            // BcelGenericSignatureToTypeXConverter.
            // classTypeSignature2TypeX(
            // cSig.superInterfaceSignatures[i],
            // formalsForResolution,
            // getResolvedTypeX().getWorld());
            this.interfaceSignatures[i] = BcelGenericSignatureToTypeXConverter.classTypeSignature2TypeX(
                cSig.superInterfaceSignatures[i], formalsForResolution, getResolvedTypeX().getWorld())
                .getSignature();
          } catch (GenericSignatureFormatException e) {
            // development bug, fail fast with good info
            throw new IllegalStateException("While determing the generic superinterfaces of " + this.className
                + " with generic signature " + getDeclaredGenericSignature()
                + " the following error was detected: " + e.getMessage());
          }
        }
      }
    }
    if (isGeneric()) {
      // update resolved typex to point at generic type not raw type.
      final ReferenceType genericType = (ReferenceType) this.resolvedTypeX.getGenericType();
      // genericType.setSourceContext(this.resolvedTypeX.getSourceContext());
      // Can be null if unpacking whilst building the bcel delegate (in call hierarchy from BcelWorld.addSourceObjectType()
      // line 453) - see 317139
      if (genericType != null) {
        genericType.setStartPos(this.resolvedTypeX.getStartPos());
        this.resolvedTypeX = genericType;
      }
    }
  }

  private void ensureGenericInfoProcessed() {
    if ((bitflag & DISCOVERED_DECLARED_SIGNATURE) != 0) {
      return;
    }
    bitflag |= DISCOVERED_DECLARED_SIGNATURE;
    final Signature sigAttr = AttributeUtils.getSignatureAttribute(javaClass.getAttributes());
    declaredSignature = (sigAttr == null ? null : sigAttr.getSignature());
    if (declaredSignature != null) {
      isGenericType = (declaredSignature.charAt(0) == '<');
    }
  }
}
