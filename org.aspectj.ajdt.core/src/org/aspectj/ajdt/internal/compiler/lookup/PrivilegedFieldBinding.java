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
 * ******************************************************************/

package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.ajdt.internal.compiler.ast.AspectDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.AjcMemberMaker;

public class PrivilegedFieldBinding extends FieldBinding {
  public SimpleSyntheticAccessMethodBinding reader;
  public SimpleSyntheticAccessMethodBinding writer;

  public FieldBinding baseField;

  public PrivilegedFieldBinding(AspectDeclaration inAspect, FieldBinding baseField) {
    super(baseField, baseField.declaringClass);

    this.reader = new SimpleSyntheticAccessMethodBinding(inAspect.factory.makeMethodBinding(AjcMemberMaker
        .privilegedAccessMethodForFieldGet(inAspect.typeX, inAspect.factory.makeResolvedMember(baseField), true)));
    this.writer = new SimpleSyntheticAccessMethodBinding(inAspect.factory.makeMethodBinding(AjcMemberMaker
        .privilegedAccessMethodForFieldSet(inAspect.typeX, inAspect.factory.makeResolvedMember(baseField), true)));

    this.constant = Constant.NotAConstant;
    this.baseField = baseField;
  }

  @Override
  public boolean canBeSeenBy(TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
    return true;
  }

  @Override
  public SyntheticMethodBinding getAccessMethod(boolean isReadAccess) {
    if (baseField.alwaysNeedsAccessMethod(isReadAccess)) {
      return baseField.getAccessMethod(isReadAccess);
    }
    if (isReadAccess) {
      return reader;
    } else {
      return writer;
    }
  }

  @Override
  public boolean alwaysNeedsAccessMethod(boolean isReadAccess) {
    return true;
  }

  @Override
  public FieldBinding getFieldBindingForLookup() {
    return baseField;
  }

  public String toString() {
    return "PrivilegedWrapper(" + baseField + ")";
  }
  // public ReferenceBinding getTargetType() {
  // return introducedField.declaringClass;
  // }

}
