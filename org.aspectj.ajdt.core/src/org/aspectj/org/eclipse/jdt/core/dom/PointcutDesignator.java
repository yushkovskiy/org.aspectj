/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.core.dom;

/**
 * abstract PointcutDesignator DOM AST node.
 * has:
 * nothing at the moment
 *
 * @author ajh02
 */
public abstract class PointcutDesignator extends ASTNode {
  PointcutDesignator(AST ast) {
    super(ast);
  }

  @Override
  final int getNodeType0() {
    return FIELD_DECLARATION; // ajh02: hmmmmmmm.. should make a POINTCUT_DESIGNATOR thing
  }

  @Override
  int memSize() {
    return 0; // ajh02: stub method
  }
}