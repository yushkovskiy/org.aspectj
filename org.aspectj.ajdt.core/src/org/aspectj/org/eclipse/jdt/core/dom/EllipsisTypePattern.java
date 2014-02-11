/********************************************************************
 * Copyright (c) 2010 Contributors. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *
 * Contributors: Nieraj Singh - initial implementation
 *******************************************************************/
package org.aspectj.org.eclipse.jdt.core.dom;

import java.util.List;

public class EllipsisTypePattern extends TypePattern {

  public static final String ELLIPSIS_DETAIL = "..";

  EllipsisTypePattern(AST ast) {
    super(ast, ELLIPSIS_DETAIL);
  }

  @Override
  List<?> internalStructuralPropertiesForType(int apiLevel) {
    return null;
  }

  @Override
  ASTNode clone0(AST target) {
    final ASTNode node = new EllipsisTypePattern(target);
    node.setSourceRange(getStartPosition(), getLength());
    return node;
  }

  @Override
  boolean subtreeMatch0(ASTMatcher matcher, Object other) {
    if (matcher instanceof AjASTMatcher) {
      final AjASTMatcher ajmatcher = (AjASTMatcher) matcher;
      return ajmatcher.match(this, other);
    }
    return false;
  }

  @Override
  void accept0(ASTVisitor visitor) {
    if (visitor instanceof AjASTVisitor) {
      final AjASTVisitor ajVisitor = (AjASTVisitor) visitor;
      ajVisitor.visit(this);
      ajVisitor.endVisit(this);
    }
  }

}
