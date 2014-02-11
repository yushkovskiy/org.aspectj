/********************************************************************
 * Copyright (c) 2006 Contributors. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *
 * Contributors: IBM Corporation - initial API and implementation 
 * 				 Helen Hawkins   - iniital version
 *******************************************************************/
package org.aspectj.org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * SignaturePattern DOM AST node. has: nothing
 * <p/>
 * For the moment this does nothing except enable us to set a SignaturePattern for the declare annotation declarations
 */
public class SignaturePattern extends PatternNode {

  private String detail;

  SignaturePattern(AST ast, String d) {
    super(ast);
    this.detail = d;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String d) {
    this.detail = d;
  }

  public static List propertyDescriptors(int apiLevel) {
    final List propertyList = new ArrayList(0);
    createPropertyList(SignaturePattern.class, propertyList);
    return reapPropertyList(propertyList);
  }

  @Override
  final List internalStructuralPropertiesForType(int apiLevel) {
    return propertyDescriptors(apiLevel);
  }

  @Override
  final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
    // allow default implementation to flag the error
    return super.internalGetSetChildProperty(property, get, child);
  }

  @Override
  ASTNode clone0(AST target) {
    final SignaturePattern result = new SignaturePattern(target, getDetail());
    result.setSourceRange(this.getStartPosition(), this.getLength());
    return result;
  }

  @Override
  final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
    // dispatch to correct overloaded match method
    return ((AjASTMatcher) matcher).match(this, other);
  }

  @Override
  void accept0(ASTVisitor visitor) {
    if (visitor instanceof AjASTVisitor) {
      // boolean visitChildren =
      ((AjASTVisitor) visitor).visit(this);
      ((AjASTVisitor) visitor).endVisit(this);
    }
  }

  @Override
  int treeSize() {
    return memSize();
  }

}
