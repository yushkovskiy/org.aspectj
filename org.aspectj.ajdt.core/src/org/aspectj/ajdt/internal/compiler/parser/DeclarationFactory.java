/*******************************************************************************
 * Copyright (c) 2002,2003 Palo Alto Research Center, Incorporated (PARC).
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     PARC initial implementation 
 *     IBM Corporation 
 *******************************************************************************/
package org.aspectj.ajdt.internal.compiler.parser;

import org.aspectj.ajdt.internal.compiler.ast.*;
import org.aspectj.ajdt.internal.core.builder.EclipseSourceContext;
import org.aspectj.org.eclipse.jdt.internal.compiler.CompilationResult;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.parser.Parser;
import org.aspectj.org.eclipse.jdt.internal.compiler.parser.Parser.IDeclarationFactory;
import org.aspectj.weaver.AdviceKind;
import org.aspectj.weaver.patterns.Declare;
import org.aspectj.weaver.patterns.DeclareAnnotation;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrian Colyer
 * @author Andy Clement
 */
public final class DeclarationFactory implements IDeclarationFactory {

  @Override
  @NotNull
  public MethodDeclaration createMethodDeclaration(@NotNull CompilationResult result) {
    return new AjMethodDeclaration(result);
  }

  @Override
  @NotNull
  public ConstructorDeclaration createConstructorDeclaration(@NotNull CompilationResult result) {
    return new AjConstructorDeclaration(result);
  }

  @Override
  @NotNull
  public MessageSend createProceed(MessageSend m) {
    return new Proceed(m);
  }

  @Override
  @NotNull
  public TypeDeclaration createAspect(@NotNull CompilationResult result) {
    return new AspectDeclaration(result);
  }

  @Override
  public void setPrivileged(@NotNull TypeDeclaration aspectDecl, boolean isPrivileged) {
    ((AspectDeclaration) aspectDecl).isPrivileged = isPrivileged;
  }

  @Override
  public void setPerClauseFrom(@NotNull TypeDeclaration aspectDecl, @NotNull ASTNode pseudoTokens, Parser parser) {
    final AspectDeclaration aspect = (AspectDeclaration) aspectDecl;
    final PseudoTokens tok = (PseudoTokens) pseudoTokens;
    aspect.perClause = tok.parsePerClause(parser);
    // For the ast support: currently the below line is not finished! The start is set incorrectly
    ((AspectDeclaration) aspectDecl).perClause.setLocation(null, 1, parser.getCurrentTokenStart() + 1);
  }

  @Override
  public void setDominatesPatternFrom(@NotNull TypeDeclaration aspectDecl, @NotNull ASTNode pseudoTokens, Parser parser) {
    final AspectDeclaration aspect = (AspectDeclaration) aspectDecl;
    final PseudoTokens tok = (PseudoTokens) pseudoTokens;
    aspect.dominatesPattern = tok.maybeParseDominatesPattern(parser);
  }

  @Override
  @NotNull
  public ASTNode createPseudoTokensFrom(@NotNull ASTNode[] tokens, @NotNull CompilationResult result) {
    final PseudoToken[] psts = new PseudoToken[tokens.length];
    for (int i = 0; i < psts.length; i++) {
      psts[i] = (PseudoToken) tokens[i];
    }
    return new PseudoTokens(psts, new EclipseSourceContext(result));
  }

  @Override
  @NotNull
  public MethodDeclaration createPointcutDeclaration(@NotNull CompilationResult result) {
    return new PointcutDeclaration(result);
  }

  @Override
  @NotNull
  public MethodDeclaration createAroundAdviceDeclaration(@NotNull CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.Around);
  }

  @Override
  @NotNull
  public MethodDeclaration createAfterAdviceDeclaration(@NotNull CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.After);
  }

  @Override
  @NotNull
  public MethodDeclaration createBeforeAdviceDeclaration(@NotNull CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.Before);
  }

  @Override
  @NotNull
  public ASTNode createPointcutDesignator(Parser parser, ASTNode pseudoTokens) {
    return new PointcutDesignator(parser, (PseudoTokens) pseudoTokens);
  }

  @Override
  public void setPointcutDesignatorOnAdvice(@NotNull MethodDeclaration adviceDecl, ASTNode des) {
    ((AdviceDeclaration) adviceDecl).pointcutDesignator = (PointcutDesignator) des;
  }

  @Override
  public void setPointcutDesignatorOnPointcut(@NotNull MethodDeclaration pcutDecl, ASTNode des) {
    ((PointcutDeclaration) pcutDecl).pointcutDesignator = (PointcutDesignator) des;
  }

  @Override
  public void setExtraArgument(@NotNull MethodDeclaration adviceDeclaration, Argument arg) {
    ((AdviceDeclaration) adviceDeclaration).extraArgument = arg;
  }

  @Override
  public boolean isAfterAdvice(@NotNull MethodDeclaration adviceDecl) {
    return ((AdviceDeclaration) adviceDecl).kind != AdviceKind.After;
  }

  @Override
  public void setAfterThrowingAdviceKind(@NotNull MethodDeclaration adviceDecl) {
    ((AdviceDeclaration) adviceDecl).kind = AdviceKind.AfterThrowing;
  }

  @Override
  public void setAfterReturningAdviceKind(@NotNull MethodDeclaration adviceDecl) {
    ((AdviceDeclaration) adviceDecl).kind = AdviceKind.AfterReturning;
  }

  @Override
  @NotNull
  public MethodDeclaration createDeclareDeclaration(@NotNull CompilationResult result, @NotNull ASTNode pseudoTokens, Parser parser) {
    final Declare declare = ((PseudoTokens) pseudoTokens).parseDeclare(parser);
    return new DeclareDeclaration(result, declare);
  }

  @Override
  @NotNull
  public MethodDeclaration createDeclareAnnotationDeclaration(@NotNull CompilationResult result, @NotNull ASTNode pseudoTokens,
                                                              Annotation annotation, Parser parser, char kind) {
    final DeclareAnnotation declare = (DeclareAnnotation) ((PseudoTokens) pseudoTokens).parseAnnotationDeclare(parser);
    if (declare != null) {
      if (kind == '-') {
        declare.setRemover(true);
      }
    }
    final DeclareAnnotationDeclaration decl = new DeclareAnnotationDeclaration(result, declare, annotation);
    return decl;
  }

  @Override
  @NotNull
  public MethodDeclaration createInterTypeFieldDeclaration(@NotNull CompilationResult result, @NotNull TypeReference onType) {
    return new InterTypeFieldDeclaration(result, onType);
  }

  @Override
  @NotNull
  public MethodDeclaration createInterTypeMethodDeclaration(@NotNull CompilationResult result) {
    return new InterTypeMethodDeclaration(result, null);
  }

  @Override
  @NotNull
  public MethodDeclaration createInterTypeConstructorDeclaration(@NotNull CompilationResult result) {
    return new InterTypeConstructorDeclaration(result, null);
  }

  @Override
  public void setSelector(@NotNull MethodDeclaration interTypeDecl, @NotNull char[] selector) {
    ((InterTypeDeclaration) interTypeDecl).setSelector(selector);
  }

  @Override
  public void setDeclaredModifiers(@NotNull MethodDeclaration interTypeDecl, int modifiers) {
    ((InterTypeDeclaration) interTypeDecl).setDeclaredModifiers(modifiers);
  }

  @Override
  public void setInitialization(@NotNull MethodDeclaration itdFieldDecl, Expression initialization) {
    ((InterTypeFieldDeclaration) itdFieldDecl).setInitialization(initialization);
  }

  @Override
  public void setOnType(@NotNull MethodDeclaration interTypeDecl, TypeReference onType) {
    ((InterTypeDeclaration) interTypeDecl).setOnType(onType);
  }

  @Override
  @NotNull
  public ASTNode createPseudoToken(Parser parser, String value, boolean isIdentifier) {
    return new PseudoToken(parser, value, isIdentifier);
  }

  @Override
  @NotNull
  public ASTNode createIfPseudoToken(Parser parser, Expression expr) {
    return new IfPseudoToken(parser, expr);
  }

  @Override
  public void setLiteralKind(@NotNull ASTNode pseudoToken, String string) {
    ((PseudoToken) pseudoToken).literalKind = string;
  }

  @Override
  public boolean shouldTryToRecover(ASTNode node) {
    return !(node instanceof AspectDeclaration || node instanceof PointcutDeclaration || node instanceof AdviceDeclaration);
  }

  @Override
  @NotNull
  public TypeDeclaration createIntertypeMemberClassDeclaration(CompilationResult compilationResult) {
    return new IntertypeMemberClassDeclaration(compilationResult);
  }

  @Override
  public void setOnType(@NotNull TypeDeclaration interTypeDecl, TypeReference onType) {
    ((IntertypeMemberClassDeclaration) interTypeDecl).setOnType(onType);
  }
}
