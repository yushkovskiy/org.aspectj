/* *******************************************************************
 * Copyright (c) 2008 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement     initial implementation 
 * ******************************************************************/
package org.aspectj.weaver;

import java.lang.reflect.Modifier;

/**
 * Represents a resolved array type
 *
 * @author Andy Clement
 */
public class ArrayReferenceType extends ReferenceType {

  private final ResolvedType componentType;

  public ArrayReferenceType(String sig, String erasureSig, World world, ResolvedType componentType) {
    super(sig, erasureSig, world);
    this.componentType = componentType;
  }

  // These methods are from the original implementation when Array was a ResolvedType and not a ReferenceType

  @Override
  public final ResolvedMember[] getDeclaredFields() {
    return ResolvedMember.NONE;
  }

  @Override
  public final ResolvedMember[] getDeclaredMethods() {
    // ??? should this return clone? Probably not...
    // If it ever does, here is the code:
    // ResolvedMember cloneMethod =
    // new ResolvedMember(Member.METHOD,this,Modifier.PUBLIC,UnresolvedType.OBJECT,"clone",new UnresolvedType[]{});
    // return new ResolvedMember[]{cloneMethod};
    return ResolvedMember.NONE;
  }

  @Override
  public final ResolvedType[] getDeclaredInterfaces() {
    return new ResolvedType[]{world.getCoreType(CLONEABLE), world.getCoreType(SERIALIZABLE)};
  }

  @Override
  public AnnotationAJ getAnnotationOfType(UnresolvedType ofType) {
    return null;
  }

  @Override
  public AnnotationAJ[] getAnnotations() {
    return AnnotationAJ.EMPTY_ARRAY;
  }

  @Override
  public ResolvedType[] getAnnotationTypes() {
    return ResolvedType.NONE;
  }

  @Override
  public final ResolvedMember[] getDeclaredPointcuts() {
    return ResolvedMember.NONE;
  }

  @Override
  public boolean hasAnnotation(UnresolvedType ofType) {
    return false;
  }

  @Override
  public final ResolvedType getSuperclass() {
    return world.getCoreType(OBJECT);
  }

  @Override
  public final boolean isAssignableFrom(ResolvedType o) {
    if (!o.isArray())
      return false;
    if (o.getComponentType().isPrimitiveType()) {
      return o.equals(this);
    } else {
      return getComponentType().resolve(world).isAssignableFrom(o.getComponentType().resolve(world));
    }
  }

  @Override
  public boolean isAssignableFrom(ResolvedType o, boolean allowMissing) {
    return isAssignableFrom(o);
  }

  @Override
  public final boolean isCoerceableFrom(ResolvedType o) {
    if (o.equals(UnresolvedType.OBJECT) || o.equals(UnresolvedType.SERIALIZABLE) || o.equals(UnresolvedType.CLONEABLE)) {
      return true;
    }
    if (!o.isArray())
      return false;
    if (o.getComponentType().isPrimitiveType()) {
      return o.equals(this);
    } else {
      return getComponentType().resolve(world).isCoerceableFrom(o.getComponentType().resolve(world));
    }
  }

  @Override
  public final int getModifiers() {
    final int mask = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
    return (componentType.getModifiers() & mask) | Modifier.FINAL;
  }

  @Override
  public UnresolvedType getComponentType() {
    return componentType;
  }

  @Override
  public ResolvedType getResolvedComponentType() {
    return componentType;
  }

  @Override
  public ISourceContext getSourceContext() {
    return getResolvedComponentType().getSourceContext();
  }

  // Methods overridden from ReferenceType follow

  @Override
  public TypeVariable[] getTypeVariables() {
    if (this.typeVariables == null && componentType.getTypeVariables() != null) {
      this.typeVariables = componentType.getTypeVariables();
      for (int i = 0; i < this.typeVariables.length; i++) {
        this.typeVariables[i].resolve(world);
      }
    }
    return this.typeVariables;
  }

  @Override
  public boolean isAnnotation() {
    return false;
  }

  @Override
  public boolean isAnonymous() {
    return false;
  }

  @Override
  public boolean isAnnotationStyleAspect() {
    return false;
  }

  @Override
  public boolean isAspect() {
    return false;
  }

  @Override
  public boolean isPrimitiveType() {
    return typeKind == TypeKind.PRIMITIVE;
  }

  @Override
  public boolean isSimpleType() {
    return typeKind == TypeKind.SIMPLE;
  }

  @Override
  public boolean isRawType() {
    return typeKind == TypeKind.RAW;
  }

  @Override
  public boolean isGenericType() {
    return typeKind == TypeKind.GENERIC;
  }

  @Override
  public boolean isParameterizedType() {
    return typeKind == TypeKind.PARAMETERIZED;
  }

  @Override
  public boolean isTypeVariableReference() {
    return typeKind == TypeKind.TYPE_VARIABLE;
  }

  @Override
  public boolean isGenericWildcard() {
    return typeKind == TypeKind.WILDCARD;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isNested() {
    return false;
  }

  @Override
  public boolean isClass() {
    return false;
  }

  @Override
  public boolean isExposedToWeaver() {
    return false;
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
  public boolean isAnnotationWithRuntimeRetention() {
    return false;
  }

  @Override
  public boolean isPrimitiveArray() {
    if (componentType.isPrimitiveType()) {
      return true;
    } else if (componentType.isArray()) {
      return componentType.isPrimitiveArray();
    } else {
      return false;
    }
  }
}
