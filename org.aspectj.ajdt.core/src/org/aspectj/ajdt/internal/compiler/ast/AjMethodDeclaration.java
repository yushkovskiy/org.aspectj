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
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.weaver.AjAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for all MethodDeclaration objects created by the parser.
 * Enables us to generate extra attributes in the method_info attribute
 * to support aspectj.
 */
public class AjMethodDeclaration extends MethodDeclaration {
  @Nullable
  private List<IAttribute> attributes = null;

  public AjMethodDeclaration(@Nullable CompilationResult compilationResult) {
    super(compilationResult);
  }

  @Override
  protected int generateInfoAttributes(@NotNull ClassFile classFile) {
    return generateInfoAttributes(classFile, false);
  }

  // general purpose hook to add an AjAttribute to this method
  // used by @AspectJ visitor to add pointcut attribute to @Advice
  protected void addAttribute(@NotNull EclipseAttributeAdapter eaa) {
    if (attributes == null)
      attributes = new ArrayList<>();
    attributes.add(eaa);
  }

  /**
   * Overridden to add extra AJ stuff, also adds synthetic if boolean is true.
   */
  protected int generateInfoAttributes(@NotNull ClassFile classFile, boolean addAjSynthetic) {
    // add extra attributes into list then call 2-arg version of generateInfoAttributes...
    final List<IAttribute> extras = (attributes == null ? new ArrayList<IAttribute>() : attributes);
    addDeclarationStartLineAttribute(extras, classFile);
    if (addAjSynthetic) {
      extras.add(new EclipseAttributeAdapter(new AjAttribute.AjSynthetic()));
    }
    return classFile.generateMethodInfoAttributes(binding, extras);
  }

  protected void addDeclarationStartLineAttribute(@NotNull List<IAttribute> extraAttributeList, @NotNull ClassFile classFile) {
    if ((classFile.codeStream.generateAttributes & ClassFileConstants.ATTR_LINES) == 0)
      return;

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
