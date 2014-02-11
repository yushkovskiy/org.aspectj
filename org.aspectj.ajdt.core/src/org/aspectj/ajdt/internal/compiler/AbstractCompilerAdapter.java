/* *******************************************************************
 * Copyright (c) 2006 Contributors
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andy Clement                 initial implementation
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler;

import org.aspectj.org.eclipse.jdt.internal.compiler.CompilationResult;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import java.util.List;

/**
 * @author AndyClement
 */
public abstract class AbstractCompilerAdapter implements ICompilerAdapter {

  public abstract List /*InterimResult*/ getResultsPendingWeave();

  public abstract void acceptResult(CompilationResult result);

  @Override
  public abstract void afterAnalysing(CompilationUnitDeclaration unit);

  @Override
  public abstract void afterCompiling(CompilationUnitDeclaration[] units);

  @Override
  public abstract void afterDietParsing(CompilationUnitDeclaration[] units);

  @Override
  public abstract void afterGenerating(CompilationUnitDeclaration unit);

  @Override
  public abstract void afterProcessing(CompilationUnitDeclaration unit, int unitIndex);

  @Override
  public abstract void afterResolving(CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeAnalysing(CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeCompiling(ICompilationUnit[] sourceUnits);

  @Override
  public abstract void beforeGenerating(CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeProcessing(CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeResolving(CompilationUnitDeclaration unit);

}
