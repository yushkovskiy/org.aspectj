/* *******************************************************************
 * Copyright (c) 2005 IBM Corporation Ltd
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Adrian Colyer  initial implementation 
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.jetbrains.annotations.NotNull;

/**
 * Adds runtime visible annotations to code-style aspect declarations so that the MAP can provide aspect information at runtime.
 * <p/>
 * Done: - AspectDeclaration - AdviceDeclaration - PointcutDeclaration
 * <p/>
 * To Do: - DeclareDeclaration - Inter-Type Declaration
 */
public final class AddAtAspectJAnnotationsVisitor extends ASTVisitor {

  private final boolean makeReflectable;
  // private CompilationUnitDeclaration unit;

  public AddAtAspectJAnnotationsVisitor(@NotNull CompilationUnitDeclaration unit, boolean makeReflectable) {
    // this.unit = unit;
    this.makeReflectable = makeReflectable;
  }

  @Override
  public boolean visit(@NotNull TypeDeclaration localTypeDeclaration, BlockScope scope) {
    if (localTypeDeclaration instanceof AspectDeclaration) {
      ((AspectDeclaration) localTypeDeclaration).addAtAspectJAnnotations();
    }
    return true;
  }

  @Override
  public boolean visit(@NotNull TypeDeclaration memberTypeDeclaration, ClassScope scope) {
    if (memberTypeDeclaration instanceof AspectDeclaration) {
      ((AspectDeclaration) memberTypeDeclaration).addAtAspectJAnnotations();
    }
    return true;
  }

  @Override
  public boolean visit(@NotNull TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
    if (typeDeclaration instanceof AspectDeclaration) {
      ((AspectDeclaration) typeDeclaration).addAtAspectJAnnotations();
    }
    return true;
  }

  @Override
  public boolean visit(@NotNull MethodDeclaration methodDeclaration, ClassScope scope) {
    if (methodDeclaration instanceof AdviceDeclaration) {
      ((AdviceDeclaration) methodDeclaration).addAtAspectJAnnotations();
    } else if (methodDeclaration instanceof PointcutDeclaration) {
      ((PointcutDeclaration) methodDeclaration).addAtAspectJAnnotations();
    } else if (methodDeclaration instanceof DeclareDeclaration) {
      ((DeclareDeclaration) methodDeclaration).addAtAspectJAnnotations();
    } else if (methodDeclaration instanceof InterTypeDeclaration) {
      if (makeReflectable) {
        ((InterTypeDeclaration) methodDeclaration).addAtAspectJAnnotations();
      }
    }
    return false;
  }

}
