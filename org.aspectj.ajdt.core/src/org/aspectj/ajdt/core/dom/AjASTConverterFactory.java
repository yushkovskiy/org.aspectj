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
package org.aspectj.ajdt.core.dom;

import org.aspectj.org.eclipse.jdt.core.dom.ASTConverter;
import org.aspectj.org.eclipse.jdt.core.dom.ASTConverter.IASTConverterFactory;
import org.aspectj.org.eclipse.jdt.core.dom.AjASTConverter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Factory, dynamically loaded by the TypeDeclaration class in the shadows/dom tree.
 * This is a factory for type declaration that returns the Aj subclass of typedeclaration.
 *
 * @author AndyClement
 */
public final class AjASTConverterFactory implements IASTConverterFactory {
  @Override
  @NotNull
  public ASTConverter getASTConverter(Map options, boolean resolveBindings, IProgressMonitor monitor) {
    return new AjASTConverter(options, resolveBindings, monitor);
  }
}
