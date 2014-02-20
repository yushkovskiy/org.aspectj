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
package org.aspectj.weaver;

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.weaver.AjAttribute.EffectiveSignatureAttribute;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author colyer Instances of this class are created by ResolvedMember.getSignatures() when collating all of the signatures for a
 *         member. We need to create entries in the set for the "gaps" in the hierarchy. For example:
 *         <p/>
 *         class A { void foo(); }
 *         <p/>
 *         class B extends A {}
 *         <p/>
 *         Join Point : call(* B.foo())
 *         <p/>
 *         has signatures:
 *         <p/>
 *         B.foo() AND A.foo() B.foo() will be created as a ResolvedMemberWithSubstituteDeclaringType
 *         <p/>
 *         Oh for a JDK 1.4 dynamic proxy.... we have to run on 1.3 :(
 */
public class JoinPointSignature implements ResolvedMember {

  public static final JoinPointSignature[] EMPTY_ARRAY = new JoinPointSignature[0];

  private final ResolvedMember realMember;
  private final ResolvedType substituteDeclaringType;

  public JoinPointSignature(ResolvedMember backing, ResolvedType aType) {
    this.realMember = backing;
    this.substituteDeclaringType = aType;
  }

  @Override
  public UnresolvedType getDeclaringType() {
    return substituteDeclaringType;
  }

  @Override
  public int getModifiers(World world) {
    return realMember.getModifiers(world);
  }

  @Override
  public int getModifiers() {
    return realMember.getModifiers();
  }

  @Override
  public UnresolvedType[] getExceptions(World world) {
    return realMember.getExceptions(world);
  }

  @Override
  public UnresolvedType[] getExceptions() {
    return realMember.getExceptions();
  }

  @Override
  public ShadowMunger getAssociatedShadowMunger() {
    return realMember.getAssociatedShadowMunger();
  }

  @Override
  public boolean isAjSynthetic() {
    return realMember.isAjSynthetic();
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    return realMember.hasAnnotation(ofType);
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    return realMember.getAnnotationTypes();
  }

  @Override
  public AnnotationAJ getAnnotationOfType(UnresolvedType ofType) {
    return realMember.getAnnotationOfType(ofType);
  }

  @Override
  public void setAnnotationTypes(ResolvedType[] annotationtypes) {
    realMember.setAnnotationTypes(annotationtypes);
  }

  @Override
  public void setAnnotations(AnnotationAJ[] annotations) {
    realMember.setAnnotations(annotations);
  }

  @Override
  public void addAnnotation(AnnotationAJ annotation) {
    realMember.addAnnotation(annotation);
  }

  @Override
  public boolean isBridgeMethod() {
    return realMember.isBridgeMethod();
  }

  @Override
  public boolean isVarargsMethod() {
    return realMember.isVarargsMethod();
  }

  @Override
  public boolean isSynthetic() {
    return realMember.isSynthetic();
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    realMember.write(s);
  }

  @Override
  public ISourceContext getSourceContext(World world) {
    return realMember.getSourceContext(world);
  }

  @Override
  public String[] getParameterNames() {
    return realMember.getParameterNames();
  }

  @Override
  public void setParameterNames(String[] names) {
    realMember.setParameterNames(names);
  }

  @Override
  public String[] getParameterNames(World world) {
    return realMember.getParameterNames(world);
  }

  @Override
  public EffectiveSignatureAttribute getEffectiveSignature() {
    return realMember.getEffectiveSignature();
  }

  @Override
  public ISourceLocation getSourceLocation() {
    return realMember.getSourceLocation();
  }

  @Override
  public int getEnd() {
    return realMember.getEnd();
  }

  @Override
  public ISourceContext getSourceContext() {
    return realMember.getSourceContext();
  }

  @Override
  public int getStart() {
    return realMember.getStart();
  }

  @Override
  public void setPosition(int sourceStart, int sourceEnd) {
    realMember.setPosition(sourceStart, sourceEnd);
  }

  @Override
  public void setSourceContext(ISourceContext sourceContext) {
    realMember.setSourceContext(sourceContext);
  }

  @Override
  public boolean isAbstract() {
    return realMember.isAbstract();
  }

  @Override
  public boolean isPublic() {
    return realMember.isPublic();
  }

  @Override
  public boolean isDefault() {
    return realMember.isDefault();
  }

  @Override
  public boolean isVisible(ResolvedType fromType) {
    return realMember.isVisible(fromType);
  }

  @Override
  public void setCheckedExceptions(UnresolvedType[] checkedExceptions) {
    realMember.setCheckedExceptions(checkedExceptions);
  }

  @Override
  public void setAnnotatedElsewhere(boolean b) {
    realMember.setAnnotatedElsewhere(b);
  }

  @Override
  public boolean isAnnotatedElsewhere() {
    return realMember.isAnnotatedElsewhere();
  }

  @Override
  public UnresolvedType getGenericReturnType() {
    return realMember.getGenericReturnType();
  }

  @Override
  public UnresolvedType[] getGenericParameterTypes() {
    return realMember.getGenericParameterTypes();
  }

  @Override
  public ResolvedMemberImpl parameterizedWith(UnresolvedType[] typeParameters, ResolvedType newDeclaringType,
                                              boolean isParameterized) {
    return realMember.parameterizedWith(typeParameters, newDeclaringType, isParameterized);
  }

  @Override
  public ResolvedMemberImpl parameterizedWith(UnresolvedType[] typeParameters, ResolvedType newDeclaringType,
                                              boolean isParameterized, List<String> aliases) {
    return realMember.parameterizedWith(typeParameters, newDeclaringType, isParameterized, aliases);
  }

  @Override
  public void setTypeVariables(TypeVariable[] types) {
    realMember.setTypeVariables(types);
  }

  @Override
  public TypeVariable[] getTypeVariables() {
    return realMember.getTypeVariables();
  }

  @Override
  public TypeVariable getTypeVariableNamed(String name) {
    return realMember.getTypeVariableNamed(name);
  }

  @Override
  public boolean matches(ResolvedMember aCandidateMatch, boolean ignoreGenerics) {
    return realMember.matches(aCandidateMatch, ignoreGenerics);
  }

  @Override
  public ResolvedMember resolve(World world) {
    return realMember.resolve(world);
  }

  @Override
  public int compareTo(@NotNull Member other) {
    return realMember.compareTo(other);
  }

  @Override
  public MemberKind getKind() {
    return realMember.getKind();
  }

  @Override
  public UnresolvedType getReturnType() {
    return realMember.getReturnType();
  }

  @Override
  public UnresolvedType getType() {
    return realMember.getType();
  }

  @Override
  public String getName() {
    return realMember.getName();
  }

  @Override
  public UnresolvedType[] getParameterTypes() {
    return realMember.getParameterTypes();
  }

  @Override
  public AnnotationAJ[][] getParameterAnnotations() {
    return realMember.getParameterAnnotations();
  }

  @Override
  public ResolvedType[][] getParameterAnnotationTypes() {
    return realMember.getParameterAnnotationTypes();
  }

  @Override
  public String getSignature() {
    return realMember.getSignature();
  }

  @Override
  public int getArity() {
    return realMember.getArity();
  }

  @Override
  public String getParameterSignature() {
    return realMember.getParameterSignature();
  }

  @Override
  public boolean isCompatibleWith(Member am) {
    return realMember.isCompatibleWith(am);
  }

  @Override
  public boolean canBeParameterized() {
    return realMember.canBeParameterized();
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    return realMember.getAnnotations();
  }

  @Override
  public Collection<ResolvedType> getDeclaringTypes(World world) {
    throw new UnsupportedOperationException("Adrian doesn't think you should be calling this...");
  }

  @Override
  public JoinPointSignatureIterator getJoinPointSignatures(World world) {
    return realMember.getJoinPointSignatures(world);
  }

  @Override
  public String toString() {
    final StringBuffer buf = new StringBuffer();
    buf.append(getReturnType().getName());
    buf.append(' ');
    buf.append(getDeclaringType().getName());
    buf.append('.');
    buf.append(getName());
    if (getKind() != FIELD) {
      buf.append("(");
      final UnresolvedType[] parameterTypes = getParameterTypes();
      if (parameterTypes.length != 0) {
        buf.append(parameterTypes[0]);
        for (int i = 1, len = parameterTypes.length; i < len; i++) {
          buf.append(", ");
          buf.append(parameterTypes[i].getName());
        }
      }
      buf.append(")");
    }
    return buf.toString();
  }

  @Override
  public String toGenericString() {
    return realMember.toGenericString();
  }

  @Override
  public String toDebugString() {
    return realMember.toDebugString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof JoinPointSignature)) {
      return false;
    }
    final JoinPointSignature other = (JoinPointSignature) obj;
    if (!realMember.equals(other.realMember)) {
      return false;
    }
    if (!substituteDeclaringType.equals(other.substituteDeclaringType)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return 17 + (37 * realMember.hashCode()) + (37 * substituteDeclaringType.hashCode());
  }

  @Override
  public boolean hasBackingGenericMember() {
    return realMember.hasBackingGenericMember();
  }

  @Override
  public ResolvedMember getBackingGenericMember() {
    return realMember.getBackingGenericMember();
  }

  @Override
  public void evictWeavingState() {
    realMember.evictWeavingState();
  }

  @Override
  public ResolvedMember parameterizedWith(Map m, World w) {
    return realMember.parameterizedWith(m, w);
  }

  @Override
  public String getAnnotationDefaultValue() {
    return realMember.getAnnotationDefaultValue();
  }

  @Override
  public String getParameterSignatureErased() {
    return realMember.getParameterSignatureErased();
  }

  @Override
  public String getSignatureErased() {
    return realMember.getSignatureErased();
  }

  @Override
  public boolean isDefaultConstructor() {
    return realMember.isDefaultConstructor();
  }

  @Override
  public boolean equalsApartFromDeclaringType(Object other) {
    return realMember.equalsApartFromDeclaringType(other);
  }
}
