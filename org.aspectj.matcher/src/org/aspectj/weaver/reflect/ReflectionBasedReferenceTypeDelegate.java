/* *******************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *   Adrian Colyer			Initial implementation
 * ******************************************************************/
package org.aspectj.weaver.reflect;

import org.aspectj.weaver.AjAttribute.WeaverVersionInfo;
import org.aspectj.weaver.*;
import org.aspectj.weaver.patterns.PerClause;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;

/**
 * @author colyer A delegate for a resolved type that uses runtime type information (java.lang.reflect) to answer questions. This
 *         class uses only Java 1.4 features to answer questions. In a Java 1.5 environment use the
 *         Java5ReflectionBasedReferenceTypeDelegate subtype.
 */
public class ReflectionBasedReferenceTypeDelegate implements ReferenceTypeDelegate {

  private static final ClassLoader bootClassLoader = new URLClassLoader(new URL[0]);// ReflectionBasedReferenceTypeDelegate.class.
  // getClassLoader();

  protected Class myClass = null;
  protected WeakClassLoaderReference classLoaderReference = null;
  protected World world;
  private ReferenceType resolvedType;
  private ResolvedMember[] fields = null;
  private ResolvedMember[] methods = null;
  private ResolvedType[] interfaces = null;

  public ReflectionBasedReferenceTypeDelegate(Class forClass, ClassLoader aClassLoader, World inWorld, ReferenceType resolvedType) {
    initialize(resolvedType, forClass, aClassLoader, inWorld);
  }

  /**
   * for reflective construction only
   */
  public ReflectionBasedReferenceTypeDelegate() {
  }

  public void initialize(ReferenceType aType, Class aClass, ClassLoader aClassLoader, World aWorld) {
    this.myClass = aClass;
    this.resolvedType = aType;
    this.world = aWorld;
    this.classLoaderReference = new WeakClassLoaderReference((aClassLoader != null) ? aClassLoader : bootClassLoader);
  }

  protected Class getBaseClass() {
    return this.myClass;
  }

  protected World getWorld() {
    return this.world;
  }

  public static ReferenceType buildGenericType() {
    throw new UnsupportedOperationException("Shouldn't be asking for generic type at 1.4 source level or lower");
  }

  @Override
  public boolean isAspect() {
    // we could do better than this in Java 5 by looking at the annotations
    // on the type...
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#isAnnotationStyleAspect()
   */
  @Override
  public boolean isAnnotationStyleAspect() {
    // we could do better than this in Java 5 by looking at the annotations
    // on the type...
    return false;
  }

  @Override
  public boolean isInterface() {
    return this.myClass.isInterface();
  }

  @Override
  public boolean isEnum() {
    // cant be an enum in Java 1.4 or prior
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#isAnnotationWithRuntimeRetention ()
   */
  @Override
  public boolean isAnnotationWithRuntimeRetention() {
    // cant be an annotation in Java 1.4 or prior
    return false;
  }

  @Override
  public boolean isAnnotation() {
    // cant be an annotation in Java 1.4 or prior
    return false;
  }

  @Override
  public String getRetentionPolicy() {
    // cant be an annotation in Java 1.4 or prior
    return null;
  }

  @Override
  public boolean canAnnotationTargetType() {
    return false;
  }

  @Override
  public AnnotationTargetKind[] getAnnotationTargetKinds() {
    return null;
  }

  @Override
  public boolean isClass() {
    return !this.myClass.isInterface() && !this.myClass.isPrimitive() && !this.myClass.isArray();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#isGeneric()
   */
  @Override
  public boolean isGeneric() {
    // cant be generic in 1.4
    return false;
  }

  @Override
  public boolean isAnonymous() {
    // this isn't in < Java 1.5 but I think we are moving beyond the need to support those levels
    return this.myClass.isAnonymousClass();
  }

  @Override
  public boolean isNested() {
    // this isn't in < Java 1.5 but I think we are moving beyond the need to support those levels
    return this.myClass.isMemberClass();
  }

  @Override
  public ResolvedType getOuterClass() {
    // this isn't in < Java 1.5 but I think we are moving beyond the need to support those levels
    return ReflectionBasedReferenceTypeDelegateFactory.resolveTypeInWorld(
        myClass.getEnclosingClass(), world);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#isExposedToWeaver()
   */
  @Override
  public boolean isExposedToWeaver() {
    // reflection based types are never exposed to the weaver
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#hasAnnotation(org.aspectj.weaver .UnresolvedType)
   */
  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    // in Java 1.4 we cant have an annotation
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getAnnotations()
   */
  @Override
  public AnnotationAJ[] getAnnotations() {
    // no annotations in Java 1.4
    return AnnotationAJ.EMPTY_ARRAY;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getAnnotationTypes()
   */
  @Override
  public ResolvedType[] getAnnotationTypes() {
    // no annotations in Java 1.4
    return new ResolvedType[0];
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclaredFields()
   */
  @Override
  public ResolvedMember[] getDeclaredFields() {
    if (fields == null) {
      final Field[] reflectFields = this.myClass.getDeclaredFields();
      final ResolvedMember[] rFields = new ResolvedMember[reflectFields.length];
      for (int i = 0; i < reflectFields.length; i++) {
        rFields[i] = ReflectionBasedReferenceTypeDelegateFactory.createResolvedMember(reflectFields[i], world);
      }
      this.fields = rFields;
    }
    return fields;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclaredInterfaces()
   */
  @Override
  public ResolvedType[] getDeclaredInterfaces() {
    if (interfaces == null) {
      final Class[] reflectInterfaces = this.myClass.getInterfaces();
      final ResolvedType[] rInterfaces = new ResolvedType[reflectInterfaces.length];
      for (int i = 0; i < reflectInterfaces.length; i++) {
        rInterfaces[i] = ReflectionBasedReferenceTypeDelegateFactory.resolveTypeInWorld(reflectInterfaces[i], world);
      }
      this.interfaces = rInterfaces;
    }
    return interfaces;
  }

  @Override
  public boolean isCacheable() {
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclaredMethods()
   */
  @Override
  public ResolvedMember[] getDeclaredMethods() {
    if (methods == null) {
      final Method[] reflectMethods = this.myClass.getDeclaredMethods();
      final Constructor[] reflectCons = this.myClass.getDeclaredConstructors();
      final ResolvedMember[] rMethods = new ResolvedMember[reflectMethods.length + reflectCons.length];
      for (int i = 0; i < reflectMethods.length; i++) {
        rMethods[i] = ReflectionBasedReferenceTypeDelegateFactory.createResolvedMember(reflectMethods[i], world);
      }
      for (int i = 0; i < reflectCons.length; i++) {
        rMethods[i + reflectMethods.length] = ReflectionBasedReferenceTypeDelegateFactory.createResolvedMember(
            reflectCons[i], world);
      }
      this.methods = rMethods;
    }
    return methods;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclaredPointcuts()
   */
  @Override
  public ResolvedMember[] getDeclaredPointcuts() {
    return new ResolvedMember[0];
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getTypeVariables()
   */
  @Override
  public TypeVariable[] getTypeVariables() {
    // no type variables in Java 1.4
    return new TypeVariable[0];
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getPerClause()
   */
  @Override
  public PerClause getPerClause() {
    // no per clause...
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclares()
   */
  @Override
  public Collection getDeclares() {
    // no declares
    return Collections.EMPTY_SET;
  }

  /*
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getTypeMungers()
   */
  @Override
  public Collection getTypeMungers() {
    // no type mungers
    return Collections.EMPTY_SET;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getPrivilegedAccesses()
   */
  @Override
  public Collection getPrivilegedAccesses() {
    // no aspect members..., not used for weaving
    return Collections.EMPTY_SET;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getModifiers()
   */
  @Override
  public int getModifiers() {
    return this.myClass.getModifiers();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getSuperclass()
   */
  @Override
  public ResolvedType getSuperclass() {
    if (this.myClass.getSuperclass() == null) {
      if (myClass == Object.class) {
        return null;
      }
      return world.resolve(UnresolvedType.OBJECT);
    }
    return ReflectionBasedReferenceTypeDelegateFactory.resolveTypeInWorld(this.myClass.getSuperclass(), world);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getWeaverState()
   */
  @Override
  public WeaverStateInfo getWeaverState() {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getResolvedTypeX()
   */
  @Override
  public ReferenceType getResolvedTypeX() {
    return this.resolvedType;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#doesNotExposeShadowMungers()
   */
  @Override
  public boolean doesNotExposeShadowMungers() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.ReferenceTypeDelegate#getDeclaredGenericSignature()
   */
  @Override
  public String getDeclaredGenericSignature() {
    // no generic sig in 1.4
    return null;
  }

  public static ReflectionBasedResolvedMemberImpl createResolvedMemberFor(Member aMember) {
    return null;
  }

  @Override
  public String getSourcefilename() {
    // crappy guess..
    return resolvedType.getName() + ".class";
  }

  @Override
  public ISourceContext getSourceContext() {
    return SourceContextImpl.UNKNOWN_SOURCE_CONTEXT;
  }

  @Override
  public boolean copySourceContext() {
    return true;
  }

  @Override
  public int getCompilerVersion() {
    return WeaverVersionInfo.getCurrentWeaverMajorVersion();
  }

  @Override
  public void ensureConsistent() {

  }

  @Override
  public boolean isWeavable() {
    return false;
  }

  @Override
  public boolean hasBeenWoven() {
    return false;
  }
}
