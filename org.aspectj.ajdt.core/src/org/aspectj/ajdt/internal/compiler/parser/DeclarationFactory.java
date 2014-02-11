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

/**
 * @author Adrian Colyer
 * @author Andy Clement
 */
public class DeclarationFactory implements IDeclarationFactory {

  @Override
  public MethodDeclaration createMethodDeclaration(CompilationResult result) {
    return new AjMethodDeclaration(result);
  }

  @Override
  public ConstructorDeclaration createConstructorDeclaration(CompilationResult result) {
    return new AjConstructorDeclaration(result);
  }

  @Override
  public MessageSend createProceed(MessageSend m) {
    return new Proceed(m);
  }

  @Override
  public TypeDeclaration createAspect(CompilationResult result) {
    return new AspectDeclaration(result);
  }

  @Override
  public void setPrivileged(TypeDeclaration aspectDecl, boolean isPrivileged) {
    ((AspectDeclaration) aspectDecl).isPrivileged = isPrivileged;
  }

  @Override
  public void setPerClauseFrom(TypeDeclaration aspectDecl, ASTNode pseudoTokens, Parser parser) {
    final AspectDeclaration aspect = (AspectDeclaration) aspectDecl;
    final PseudoTokens tok = (PseudoTokens) pseudoTokens;
    aspect.perClause = tok.parsePerClause(parser);
    // For the ast support: currently the below line is not finished! The start is set incorrectly
    ((AspectDeclaration) aspectDecl).perClause.setLocation(null, 1, parser.getCurrentTokenStart() + 1);
  }

  @Override
  public void setDominatesPatternFrom(TypeDeclaration aspectDecl, ASTNode pseudoTokens, Parser parser) {
    final AspectDeclaration aspect = (AspectDeclaration) aspectDecl;
    final PseudoTokens tok = (PseudoTokens) pseudoTokens;
    aspect.dominatesPattern = tok.maybeParseDominatesPattern(parser);
  }

  @Override
  public ASTNode createPseudoTokensFrom(ASTNode[] tokens, CompilationResult result) {
    final PseudoToken[] psts = new PseudoToken[tokens.length];
    for (int i = 0; i < psts.length; i++) {
      psts[i] = (PseudoToken) tokens[i];
    }
    return new PseudoTokens(psts, new EclipseSourceContext(result));
  }

  @Override
  public MethodDeclaration createPointcutDeclaration(CompilationResult result) {
    return new PointcutDeclaration(result);
  }

  @Override
  public MethodDeclaration createAroundAdviceDeclaration(CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.Around);
  }

  @Override
  public MethodDeclaration createAfterAdviceDeclaration(CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.After);
  }

  @Override
  public MethodDeclaration createBeforeAdviceDeclaration(CompilationResult result) {
    return new AdviceDeclaration(result, AdviceKind.Before);
  }

  @Override
  public ASTNode createPointcutDesignator(Parser parser, ASTNode pseudoTokens) {
    return new PointcutDesignator(parser, (PseudoTokens) pseudoTokens);
  }

  @Override
  public void setPointcutDesignatorOnAdvice(MethodDeclaration adviceDecl, ASTNode des) {
    ((AdviceDeclaration) adviceDecl).pointcutDesignator = (PointcutDesignator) des;
  }

  @Override
  public void setPointcutDesignatorOnPointcut(MethodDeclaration pcutDecl, ASTNode des) {
    ((PointcutDeclaration) pcutDecl).pointcutDesignator = (PointcutDesignator) des;
  }

  @Override
  public void setExtraArgument(MethodDeclaration adviceDeclaration, Argument arg) {
    ((AdviceDeclaration) adviceDeclaration).extraArgument = arg;
  }

  @Override
  public boolean isAfterAdvice(MethodDeclaration adviceDecl) {
    return ((AdviceDeclaration) adviceDecl).kind != AdviceKind.After;
  }

  @Override
  public void setAfterThrowingAdviceKind(MethodDeclaration adviceDecl) {
    ((AdviceDeclaration) adviceDecl).kind = AdviceKind.AfterThrowing;
  }

  @Override
  public void setAfterReturningAdviceKind(MethodDeclaration adviceDecl) {
    ((AdviceDeclaration) adviceDecl).kind = AdviceKind.AfterReturning;
  }

  @Override
  public MethodDeclaration createDeclareDeclaration(CompilationResult result, ASTNode pseudoTokens, Parser parser) {
    final Declare declare = ((PseudoTokens) pseudoTokens).parseDeclare(parser);
    return new DeclareDeclaration(result, declare);
  }

  @Override
  public MethodDeclaration createDeclareAnnotationDeclaration(CompilationResult result, ASTNode pseudoTokens,
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
  public MethodDeclaration createInterTypeFieldDeclaration(CompilationResult result, TypeReference onType) {
    return new InterTypeFieldDeclaration(result, onType);
  }

  @Override
  public MethodDeclaration createInterTypeMethodDeclaration(CompilationResult result) {
    return new InterTypeMethodDeclaration(result, null);
  }

  @Override
  public MethodDeclaration createInterTypeConstructorDeclaration(CompilationResult result) {
    return new InterTypeConstructorDeclaration(result, null);
  }

  @Override
  public void setSelector(MethodDeclaration interTypeDecl, char[] selector) {
    ((InterTypeDeclaration) interTypeDecl).setSelector(selector);
  }

  @Override
  public void setDeclaredModifiers(MethodDeclaration interTypeDecl, int modifiers) {
    ((InterTypeDeclaration) interTypeDecl).setDeclaredModifiers(modifiers);
  }

  @Override
  public void setInitialization(MethodDeclaration itdFieldDecl, Expression initialization) {
    ((InterTypeFieldDeclaration) itdFieldDecl).setInitialization(initialization);
  }

  @Override
  public void setOnType(MethodDeclaration interTypeDecl, TypeReference onType) {
    ((InterTypeDeclaration) interTypeDecl).setOnType(onType);
  }

  @Override
  public ASTNode createPseudoToken(Parser parser, String value, boolean isIdentifier) {
    return new PseudoToken(parser, value, isIdentifier);
  }

  @Override
  public ASTNode createIfPseudoToken(Parser parser, Expression expr) {
    return new IfPseudoToken(parser, expr);
  }

  @Override
  public void setLiteralKind(ASTNode pseudoToken, String string) {
    ((PseudoToken) pseudoToken).literalKind = string;
  }

  @Override
  public boolean shouldTryToRecover(ASTNode node) {
    return !(node instanceof AspectDeclaration || node instanceof PointcutDeclaration || node instanceof AdviceDeclaration);
  }

  @Override
  public TypeDeclaration createIntertypeMemberClassDeclaration(CompilationResult compilationResult) {
    return new IntertypeMemberClassDeclaration(compilationResult);
  }

  @Override
  public void setOnType(TypeDeclaration interTypeDecl, TypeReference onType) {
    ((IntertypeMemberClassDeclaration) interTypeDecl).setOnType(onType);
  }
}
