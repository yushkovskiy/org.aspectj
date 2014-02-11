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

public class AndTypePattern extends AbstractBooleanTypePattern {

  public static final String AND_OPERATOR = "&&";

  AndTypePattern(AST ast, TypePattern left, TypePattern right) {
    super(ast, left, right, AND_OPERATOR);
  }

  @Override
  List<?> internalStructuralPropertiesForType(int apiLevel) {
    return null;
  }

  @Override
  ASTNode clone0(AST target) {
    final AndTypePattern cloned = new AndTypePattern(target,
        (TypePattern) getLeft().clone(target), (TypePattern) getRight()
        .clone(target));
    cloned.setSourceRange(getStartPosition(), getLength());
    return cloned;
  }

  @Override
  void accept0(ASTVisitor visitor) {
    if (visitor instanceof AjASTVisitor) {
      final AjASTVisitor ajVisitor = (AjASTVisitor) visitor;
      final boolean visit = ajVisitor.visit(this);
      if (visit) {
        acceptChild(visitor, getLeft());
        acceptChild(visitor, getRight());
      }
      ajVisitor.endVisit(this);
    }
  }

  @Override
  boolean subtreeMatch0(ASTMatcher matcher, Object other) {
    if (matcher instanceof AjASTMatcher) {
      final AjASTMatcher ajmatcher = (AjASTMatcher) matcher;
      return ajmatcher.match(this, other);
    }
    return false;
  }

}
