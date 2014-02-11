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

public class WildTypePattern extends IdentifierTypePattern {

  /**
   * Use this constructor if Type is known
   *
   * @param ast
   * @param type
   */
  WildTypePattern(AST ast, Type type) {
    super(ast, type);
  }

  /**
   * Use this constructor if Type cannot be determined
   *
   * @param ast
   * @param typeExpression
   */
  WildTypePattern(AST ast, String typeExpression) {
    super(ast, null);
    setTypePatternExpression(typeExpression);
  }

  @Override
  List<?> internalStructuralPropertiesForType(int apiLevel) {
    return null;
  }

  @Override
  ASTNode clone0(AST target) {
    final Type type = getType();

    final ASTNode cloned = type != null ? new WildTypePattern(target,
        (Type) type.clone(target)) : new WildTypePattern(target,
        getTypePatternExpression());
    cloned.setSourceRange(getStartPosition(), getLength());

    return cloned;
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
