/* *******************************************************************
 * Copyright (c) 2004 IBM Corporation.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * ******************************************************************/
package org.aspectj.weaver.internal.tools;

import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.Shadow;
import org.aspectj.weaver.World;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.patterns.*;
import org.aspectj.weaver.reflect.ReflectionFastMatchInfo;
import org.aspectj.weaver.reflect.ReflectionShadow;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Map from weaver.tools interface to internal Pointcut implementation...
 */
public class PointcutExpressionImpl implements PointcutExpression {

  private final static boolean MATCH_INFO = false;

  private final World world;
  private final Pointcut pointcut;
  private final String expression;
  private PointcutParameter[] parameters;
  private MatchingContext matchContext = new DefaultMatchingContext();

  public PointcutExpressionImpl(Pointcut pointcut, String expression, PointcutParameter[] params, World inWorld) {
    this.pointcut = pointcut;
    this.expression = expression;
    this.world = inWorld;
    this.parameters = params;
    if (this.parameters == null) {
      this.parameters = new PointcutParameter[0];
    }
  }

  public Pointcut getUnderlyingPointcut() {
    return this.pointcut;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.tools.PointcutExpression#setMatchingContext(org.aspectj.weaver.tools.MatchingContext)
   */
  @Override
  public void setMatchingContext(MatchingContext aMatchContext) {
    this.matchContext = aMatchContext;
  }

  @Override
  public boolean couldMatchJoinPointsInType(Class aClass) {
    final ResolvedType matchType = world.resolve(aClass.getName());
    final ReflectionFastMatchInfo info = new ReflectionFastMatchInfo(matchType, null, this.matchContext, world);
    final boolean couldMatch = pointcut.fastMatch(info).maybeTrue();
    if (MATCH_INFO) {
      System.out.println("MATCHINFO: fast match for '" + this.expression + "' against '" + aClass.getName() + "': "
          + couldMatch);
    }
    return couldMatch;
  }

  @Override
  public boolean mayNeedDynamicTest() {
    final HasPossibleDynamicContentVisitor visitor = new HasPossibleDynamicContentVisitor();
    pointcut.traverse(visitor, null);
    return visitor.hasDynamicContent();
  }

  private ExposedState getExposedState() {
    return new ExposedState(parameters.length);
  }

  @Override
  public ShadowMatch matchesMethodExecution(Method aMethod) {
    final ShadowMatch match = matchesExecution(aMethod);
    if (MATCH_INFO && match.maybeMatches()) {
      System.out.println("MATCHINFO: method execution match on '" + aMethod + "' for '" + this.expression + "': "
          + (match.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return match;
  }

  @Override
  public ShadowMatch matchesConstructorExecution(Constructor aConstructor) {
    final ShadowMatch match = matchesExecution(aConstructor);
    if (MATCH_INFO && match.maybeMatches()) {
      System.out.println("MATCHINFO: constructor execution match on '" + aConstructor + "' for '" + this.expression + "': "
          + (match.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return match;
  }

  private ShadowMatch matchesExecution(Member aMember) {
    final Shadow s = ReflectionShadow.makeExecutionShadow(world, aMember, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aMember);
    sm.setWithinCode(null);
    sm.setWithinType(aMember.getDeclaringClass());
    return sm;
  }

  @Override
  public ShadowMatch matchesStaticInitialization(Class aClass) {
    final Shadow s = ReflectionShadow.makeStaticInitializationShadow(world, aClass, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(null);
    sm.setWithinCode(null);
    sm.setWithinType(aClass);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: static initialization match on '" + aClass.getName() + "' for '" + this.expression
          + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesAdviceExecution(Method aMethod) {
    final Shadow s = ReflectionShadow.makeAdviceExecutionShadow(world, aMethod, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aMethod);
    sm.setWithinCode(null);
    sm.setWithinType(aMethod.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: advice execution match on '" + aMethod + "' for '" + this.expression + "': "
          + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesInitialization(Constructor aConstructor) {
    final Shadow s = ReflectionShadow.makeInitializationShadow(world, aConstructor, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aConstructor);
    sm.setWithinCode(null);
    sm.setWithinType(aConstructor.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: initialization match on '" + aConstructor + "' for '" + this.expression + "': "
          + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesPreInitialization(Constructor aConstructor) {
    final Shadow s = ReflectionShadow.makePreInitializationShadow(world, aConstructor, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aConstructor);
    sm.setWithinCode(null);
    sm.setWithinType(aConstructor.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: preinitialization match on '" + aConstructor + "' for '" + this.expression + "': "
          + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesMethodCall(Method aMethod, Member withinCode) {
    final Shadow s = ReflectionShadow.makeCallShadow(world, aMethod, withinCode, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aMethod);
    sm.setWithinCode(withinCode);
    sm.setWithinType(withinCode.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: method call match on '" + aMethod + "' withinCode='" + withinCode + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesMethodCall(Method aMethod, Class callerType) {
    final Shadow s = ReflectionShadow.makeCallShadow(world, aMethod, callerType, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aMethod);
    sm.setWithinCode(null);
    sm.setWithinType(callerType);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: method call match on '" + aMethod + "' callerType='" + callerType.getName() + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesConstructorCall(Constructor aConstructor, Class callerType) {
    final Shadow s = ReflectionShadow.makeCallShadow(world, aConstructor, callerType, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aConstructor);
    sm.setWithinCode(null);
    sm.setWithinType(callerType);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: constructor call match on '" + aConstructor + "' callerType='" + callerType.getName()
          + "' for '" + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesConstructorCall(Constructor aConstructor, Member withinCode) {
    final Shadow s = ReflectionShadow.makeCallShadow(world, aConstructor, withinCode, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aConstructor);
    sm.setWithinCode(withinCode);
    sm.setWithinType(withinCode.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: constructor call match on '" + aConstructor + "' withinCode='" + withinCode + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesHandler(Class exceptionType, Class handlingType) {
    final Shadow s = ReflectionShadow.makeHandlerShadow(world, exceptionType, handlingType, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(null);
    sm.setWithinCode(null);
    sm.setWithinType(handlingType);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: handler match on '" + exceptionType.getName() + "' handlingType='" + handlingType
          + "' for '" + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesHandler(Class exceptionType, Member withinCode) {
    final Shadow s = ReflectionShadow.makeHandlerShadow(world, exceptionType, withinCode, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(null);
    sm.setWithinCode(withinCode);
    sm.setWithinType(withinCode.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: handler match on '" + exceptionType.getName() + "' withinCode='" + withinCode
          + "' for '" + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesFieldGet(Field aField, Class withinType) {
    final Shadow s = ReflectionShadow.makeFieldGetShadow(world, aField, withinType, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aField);
    sm.setWithinCode(null);
    sm.setWithinType(withinType);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: field get match on '" + aField + "' withinType='" + withinType.getName() + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesFieldGet(Field aField, Member withinCode) {
    final Shadow s = ReflectionShadow.makeFieldGetShadow(world, aField, withinCode, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aField);
    sm.setWithinCode(withinCode);
    sm.setWithinType(withinCode.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: field get match on '" + aField + "' withinCode='" + withinCode + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesFieldSet(Field aField, Class withinType) {
    final Shadow s = ReflectionShadow.makeFieldSetShadow(world, aField, withinType, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aField);
    sm.setWithinCode(null);
    sm.setWithinType(withinType);
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: field set match on '" + aField + "' withinType='" + withinType.getName() + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  @Override
  public ShadowMatch matchesFieldSet(Field aField, Member withinCode) {
    final Shadow s = ReflectionShadow.makeFieldSetShadow(world, aField, withinCode, this.matchContext);
    final ShadowMatchImpl sm = getShadowMatch(s);
    sm.setSubject(aField);
    sm.setWithinCode(withinCode);
    sm.setWithinType(withinCode.getDeclaringClass());
    if (MATCH_INFO && sm.maybeMatches()) {
      System.out.println("MATCHINFO: field set match on '" + aField + "' withinCode='" + withinCode + "' for '"
          + this.expression + "': " + (sm.alwaysMatches() ? "YES" : "MAYBE"));
    }
    return sm;
  }

  private ShadowMatchImpl getShadowMatch(Shadow forShadow) {
    final org.aspectj.util.FuzzyBoolean match = pointcut.match(forShadow);
    Test residueTest = Literal.TRUE;
    final ExposedState state = getExposedState();
    if (match.maybeTrue()) {
      residueTest = pointcut.findResidue(forShadow, state);
    }
    final ShadowMatchImpl sm = new ShadowMatchImpl(match, residueTest, state, parameters);
    sm.setMatchingContext(this.matchContext);
    return sm;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.tools.PointcutExpression#getPointcutExpression()
   */
  @Override
  public String getPointcutExpression() {
    return expression;
  }

  private static class HasPossibleDynamicContentVisitor extends AbstractPatternNodeVisitor {
    private boolean hasDynamicContent = false;

    public boolean hasDynamicContent() {
      return hasDynamicContent;
    }

    @Override
    public Object visit(WithinAnnotationPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(WithinCodeAnnotationPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(AnnotationPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(ArgsAnnotationPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(ArgsPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(CflowPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(IfPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(NotAnnotationTypePattern node, Object data) {
      return node.getNegatedPattern().accept(this, data);
    }

    @Override
    public Object visit(NotPointcut node, Object data) {
      return node.getNegatedPointcut().accept(this, data);
    }

    @Override
    public Object visit(ThisOrTargetAnnotationPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

    @Override
    public Object visit(ThisOrTargetPointcut node, Object data) {
      hasDynamicContent = true;
      return null;
    }

  }

  public static class Handler implements Member {

    private final Class decClass;
    private final Class exType;

    public Handler(Class decClass, Class exType) {
      this.decClass = decClass;
      this.exType = exType;
    }

    @Override
    public int getModifiers() {
      return 0;
    }

    @Override
    public Class getDeclaringClass() {
      return decClass;
    }

    @Override
    public String getName() {
      return null;
    }

    public Class getHandledExceptionType() {
      return exType;
    }

    @Override
    public boolean isSynthetic() {
      return false;
    }
  }
}
