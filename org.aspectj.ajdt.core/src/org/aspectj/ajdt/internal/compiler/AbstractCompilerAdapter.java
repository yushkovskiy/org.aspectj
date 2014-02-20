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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author AndyClement
 */
public abstract class AbstractCompilerAdapter implements ICompilerAdapter {

  @NotNull
  public abstract List<InterimCompilationResult> /*InterimResult*/ getResultsPendingWeave();

  public abstract void acceptResult(@NotNull CompilationResult result);

  @Override
  public abstract void afterAnalysing(CompilationUnitDeclaration unit);

  @Override
  public abstract void afterCompiling(@NotNull CompilationUnitDeclaration[] units);

  @Override
  public abstract void afterDietParsing(@NotNull CompilationUnitDeclaration[] units);

  @Override
  public abstract void afterGenerating(@NotNull CompilationUnitDeclaration unit);

  @Override
  public abstract void afterProcessing(@NotNull CompilationUnitDeclaration unit, int unitIndex);

  @Override
  public abstract void afterResolving(CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeAnalysing(@NotNull CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeCompiling(@NotNull ICompilationUnit[] sourceUnits);

  @Override
  public abstract void beforeGenerating(@NotNull CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeProcessing(@NotNull CompilationUnitDeclaration unit);

  @Override
  public abstract void beforeResolving(@NotNull CompilationUnitDeclaration unit);

}
