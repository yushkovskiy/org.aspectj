/* *******************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *   Adrian Colyer			Initial implementation
 * ******************************************************************/
package org.aspectj.weaver.patterns;

import org.aspectj.weaver.patterns.Pointcut.MatchesNothingPointcut;

/**
 * @author colyer
 */
public abstract class AbstractPatternNodeVisitor implements PatternNodeVisitor {

  @Override
  public Object visit(AnyTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(NoTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(EllipsisTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(AnyWithAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(AnyAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(EllipsisAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(AndAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(AndPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(AndTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(AnnotationPatternList node, Object data) {
    return node;
  }

  @Override
  public Object visit(AnnotationPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ArgsAnnotationPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ArgsPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(BindingAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(BindingTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(CflowPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ConcreteCflowPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(DeclareAnnotation node, Object data) {
    return node;
  }

  @Override
  public Object visit(DeclareErrorOrWarning node, Object data) {
    return node;
  }

  @Override
  public Object visit(DeclareParents node, Object data) {
    return node;
  }

  @Override
  public Object visit(DeclarePrecedence node, Object data) {
    return node;
  }

  @Override
  public Object visit(DeclareSoft node, Object data) {
    return node;
  }

  @Override
  public Object visit(ExactAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(ExactTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(HandlerPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(IfPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(KindedPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ModifiersPattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(NamePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(NotAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(NotPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(NotTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(OrAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(OrPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(OrTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(PerCflow node, Object data) {
    return node;
  }

  @Override
  public Object visit(PerFromSuper node, Object data) {
    return node;
  }

  @Override
  public Object visit(PerObject node, Object data) {
    return node;
  }

  @Override
  public Object visit(PerSingleton node, Object data) {
    return node;
  }

  @Override
  public Object visit(PerTypeWithin node, Object data) {
    return node;
  }

  @Override
  public Object visit(PatternNode node, Object data) {
    return node;
  }

  @Override
  public Object visit(ReferencePointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(SignaturePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(ThisOrTargetAnnotationPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ThisOrTargetPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(ThrowsPattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(TypePatternList node, Object data) {
    return node;
  }

  @Override
  public Object visit(WildAnnotationTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(WildTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(WithinAnnotationPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(WithinCodeAnnotationPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(WithinPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(WithincodePointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(MatchesNothingPointcut node, Object data) {
    return node;
  }

  @Override
  public Object visit(TypeVariablePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(TypeVariablePatternList node, Object data) {
    return node;
  }

  @Override
  public Object visit(HasMemberTypePattern node, Object data) {
    return node;
  }

  @Override
  public Object visit(TypeCategoryTypePattern node, Object data) {
    return node;
  }
}
