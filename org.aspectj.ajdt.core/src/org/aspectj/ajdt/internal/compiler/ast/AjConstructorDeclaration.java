/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation 
 *******************************************************************************/
package org.aspectj.ajdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.internal.compiler.ClassFile;
import org.aspectj.org.eclipse.jdt.internal.compiler.CompilationResult;
import org.aspectj.org.eclipse.jdt.internal.compiler.IAttribute;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.weaver.AjAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for all ConstructorDeclaration objects created by the parser.
 * Enables us to generate extra attributes in the method_info attribute
 * to support aspectj.
 */
public final class AjConstructorDeclaration extends ConstructorDeclaration {

  public AjConstructorDeclaration(@NotNull CompilationResult compilationResult) {
    super(compilationResult);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration#generateInfoAttributes(org.eclipse.jdt.internal.compiler.ClassFile)
   */
  @Override
  protected int generateInfoAttributes(@NotNull ClassFile classFile) {
    // add extra attributes into list then call 2-arg version of generateInfoAttributes...
    final List<IAttribute> extras = new ArrayList<>();
    addDeclarationStartLineAttribute(extras, classFile);
    return classFile.generateMethodInfoAttributes(binding, extras);
  }

  protected void addDeclarationStartLineAttribute(@NotNull List<IAttribute> extraAttributeList, @NotNull ClassFile classFile) {
    if ((classFile.codeStream.generateAttributes & ClassFileConstants.ATTR_LINES) == 0) return;

    final int[] separators = compilationResult().lineSeparatorPositions;
    int declarationStartLine = 1;
    for (int i = 0; i < separators.length; i++) {
      if (sourceStart < separators[i]) break;
      declarationStartLine++;
    }

    extraAttributeList.add(
        new EclipseAttributeAdapter(new AjAttribute.MethodDeclarationLineNumberAttribute(declarationStartLine, this.sourceStart())));
  }
}
