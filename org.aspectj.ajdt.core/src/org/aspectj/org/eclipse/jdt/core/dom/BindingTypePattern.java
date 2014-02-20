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

public class BindingTypePattern extends IdentifierTypePattern {

  private final FormalBinding formalBinding;

  public BindingTypePattern(AST ast, FormalBinding formalBinding) {
    super(ast, null);
    this.formalBinding = formalBinding;
    setTypePatternExpression(generateTypePatternExpression(this.formalBinding));
  }

  @Override
  List<?> internalStructuralPropertiesForType(int apiLevel) {
    return null;
  }

  public FormalBinding getFormalBinding() {
    return formalBinding;
  }

  @Override
  ASTNode clone0(AST target) {
    final ASTNode node = new BindingTypePattern(target,
        (FormalBinding) getFormalBinding().clone(target));
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
      final boolean visited = ajVisitor.visit(this);

      if (visited) {
        ajVisitor.visit(getFormalBinding());
      }
      ajVisitor.endVisit(this);
    }
  }

  protected String generateTypePatternExpression(FormalBinding formalBinding) {
    String expression = super.generateTypePatternExpression(formalBinding
        .getType());
    expression += expression + " " + formalBinding.getBinding();
    return expression;
  }

}
