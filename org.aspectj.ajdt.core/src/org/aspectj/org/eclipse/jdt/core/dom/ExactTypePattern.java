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

public class ExactTypePattern extends IdentifierTypePattern {

  ExactTypePattern(AST ast, Type type) {
    super(ast, type);
  }


  @Override
  List<?> internalStructuralPropertiesForType(int apiLevel) {
    return null;
  }

  @Override
  ASTNode clone0(AST target) {
    Type clonedType = getType();
    if (clonedType != null) {
      clonedType = (Type) clonedType.clone(target);
    }
    final ASTNode node = new ExactTypePattern(target, clonedType);
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

}
