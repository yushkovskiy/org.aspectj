/* *******************************************************************
 * Copyright (c) 2008 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *
 * ******************************************************************/
package org.aspectj.weaver;

import org.aspectj.weaver.patterns.Declare;
import org.aspectj.weaver.patterns.PerClause;

import java.util.Collection;

/**
 * A delegate that can sit in the ReferenceType instance created for an aspect generated from aop.xml. Only answers the minimal set
 * of information required as the type is processed.
 *
 * @author Andy Clement
 */
public class GeneratedReferenceTypeDelegate extends AbstractReferenceTypeDelegate {

  private ResolvedType superclass;

  public GeneratedReferenceTypeDelegate(ReferenceType backing) {
    super(backing, false);
  }

  @Override
  public boolean isAspect() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isAnnotationStyleAspect() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isInterface() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isEnum() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isAnnotation() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isAnnotationWithRuntimeRetention() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isAnonymous() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isNested() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public ResolvedType getOuterClass() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public String getRetentionPolicy() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean canAnnotationTargetType() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public AnnotationTargetKind[] getAnnotationTargetKinds() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean isGeneric() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public String getDeclaredGenericSignature() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public ResolvedMember[] getDeclaredFields() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public ResolvedType[] getDeclaredInterfaces() {
    return ResolvedType.NONE;
  }

  @Override
  public ResolvedMember[] getDeclaredMethods() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public ResolvedMember[] getDeclaredPointcuts() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public PerClause getPerClause() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public Collection<Declare> getDeclares() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public Collection<ConcreteTypeMunger> getTypeMungers() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public Collection<ResolvedMember> getPrivilegedAccesses() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public int getModifiers() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  public void setSuperclass(ResolvedType superclass) {
    this.superclass = superclass;
  }

  @Override
  public ResolvedType getSuperclass() {
    return this.superclass;
  }

  @Override
  public WeaverStateInfo getWeaverState() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

  @Override
  public TypeVariable[] getTypeVariables() {
    throw new UnsupportedOperationException("Not supported for GeneratedReferenceTypeDelegate");
  }

}