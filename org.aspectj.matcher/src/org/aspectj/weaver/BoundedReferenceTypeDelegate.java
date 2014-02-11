/**
 *
 */
package org.aspectj.weaver;

import org.aspectj.weaver.patterns.Declare;
import org.aspectj.weaver.patterns.PerClause;

import java.util.Collection;
import java.util.Collections;

class BoundedReferenceTypeDelegate extends AbstractReferenceTypeDelegate {

  public BoundedReferenceTypeDelegate(ReferenceType backing) {
    super(backing, false);
  }

  @Override
  public boolean isAspect() {
    return resolvedTypeX.isAspect();
  }

  @Override
  public boolean isAnnotationStyleAspect() {
    return resolvedTypeX.isAnnotationStyleAspect();
  }

  @Override
  public boolean isInterface() {
    return resolvedTypeX.isInterface();
  }

  @Override
  public boolean isEnum() {
    return resolvedTypeX.isEnum();
  }

  @Override
  public boolean isAnnotation() {
    return resolvedTypeX.isAnnotation();
  }

  @Override
  public boolean isAnnotationWithRuntimeRetention() {
    return resolvedTypeX.isAnnotationWithRuntimeRetention();
  }

  @Override
  public boolean isAnonymous() {
    return resolvedTypeX.isAnonymous();
  }

  @Override
  public boolean isNested() {
    return resolvedTypeX.isNested();
  }

  @Override
  public ResolvedType getOuterClass() {
    return resolvedTypeX.getOuterClass();
  }

  @Override
  public String getRetentionPolicy() {
    return resolvedTypeX.getRetentionPolicy();
  }

  @Override
  public boolean canAnnotationTargetType() {
    return resolvedTypeX.canAnnotationTargetType();
  }

  @Override
  public AnnotationTargetKind[] getAnnotationTargetKinds() {
    return resolvedTypeX.getAnnotationTargetKinds();
  }

  @Override
  public boolean isGeneric() {
    return resolvedTypeX.isGenericType();
  }

  @Override
  public String getDeclaredGenericSignature() {
    return resolvedTypeX.getDeclaredGenericSignature();
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    return resolvedTypeX.hasAnnotation(ofType);
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    return resolvedTypeX.getAnnotations();
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    return resolvedTypeX.getAnnotationTypes();
  }

  @Override
  public ResolvedMember[] getDeclaredFields() {
    return resolvedTypeX.getDeclaredFields();
  }

  @Override
  public ResolvedType[] getDeclaredInterfaces() {
    return resolvedTypeX.getDeclaredInterfaces();
  }

  @Override
  public ResolvedMember[] getDeclaredMethods() {
    return resolvedTypeX.getDeclaredMethods();
  }

  @Override
  public ResolvedMember[] getDeclaredPointcuts() {
    return resolvedTypeX.getDeclaredPointcuts();
  }

  @Override
  public PerClause getPerClause() {
    return resolvedTypeX.getPerClause();
  }

  @Override
  public Collection<Declare> getDeclares() {
    return resolvedTypeX.getDeclares();
  }

  @Override
  public Collection<ConcreteTypeMunger> getTypeMungers() {
    return resolvedTypeX.getTypeMungers();
  }

  @Override
  public Collection<ResolvedMember> getPrivilegedAccesses() {
    return Collections.emptyList();
  }

  @Override
  public int getModifiers() {
    return resolvedTypeX.getModifiers();
  }

  @Override
  public ResolvedType getSuperclass() {
    return resolvedTypeX.getSuperclass();
  }

  @Override
  public WeaverStateInfo getWeaverState() {
    return null;
  }

  @Override
  public TypeVariable[] getTypeVariables() {
    return resolvedTypeX.getTypeVariables();
  }

}