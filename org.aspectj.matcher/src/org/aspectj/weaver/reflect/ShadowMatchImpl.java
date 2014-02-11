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
package org.aspectj.weaver.reflect;

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.ast.*;
import org.aspectj.weaver.internal.tools.MatchingContextBasedTest;
import org.aspectj.weaver.patterns.ExposedState;
import org.aspectj.weaver.tools.*;

import java.lang.reflect.Member;

/**
 * @author colyer Implementation of ShadowMatch for reflection based worlds.
 */
public class ShadowMatchImpl implements ShadowMatch {

  private final FuzzyBoolean match;
  private final ExposedState state;
  private final Test residualTest;
  private final PointcutParameter[] params;
  private Member withinCode;
  private Member subject;
  private Class<?> withinType;
  private MatchingContext matchContext = new DefaultMatchingContext();

  public ShadowMatchImpl(FuzzyBoolean match, Test test, ExposedState state, PointcutParameter[] params) {
    this.match = match;
    this.residualTest = test;
    this.state = state;
    this.params = params;
  }

  public void setWithinCode(Member aMember) {
    this.withinCode = aMember;
  }

  public void setSubject(Member aMember) {
    this.subject = aMember;
  }

  public void setWithinType(Class<?> aClass) {
    this.withinType = aClass;
  }

  @Override
  public boolean alwaysMatches() {
    return match.alwaysTrue();
  }

  @Override
  public boolean maybeMatches() {
    return match.maybeTrue();
  }

  @Override
  public boolean neverMatches() {
    return match.alwaysFalse();
  }

  @Override
  public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
    if (neverMatches()) {
      return JoinPointMatchImpl.NO_MATCH;
    }
    if (new RuntimeTestEvaluator(residualTest, thisObject, targetObject, args, this.matchContext).matches()) {
      return new JoinPointMatchImpl(getPointcutParameters(thisObject, targetObject, args));
    } else {
      return JoinPointMatchImpl.NO_MATCH;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.tools.ShadowMatch#setMatchingContext(org.aspectj.weaver.tools.MatchingContext)
   */
  @Override
  public void setMatchingContext(MatchingContext aMatchContext) {
    this.matchContext = aMatchContext;
  }

  private PointcutParameter[] getPointcutParameters(Object thisObject, Object targetObject, Object[] args) {
    final Var[] vars = state.vars;
    final PointcutParameterImpl[] bindings = new PointcutParameterImpl[params.length];
    for (int i = 0; i < bindings.length; i++) {
      bindings[i] = new PointcutParameterImpl(params[i].getName(), params[i].getType());
      bindings[i].setBinding(((ReflectionVar) vars[i]).getBindingAtJoinPoint(thisObject, targetObject, args, subject,
          withinCode, withinType));
    }
    return bindings;
  }

  private static class RuntimeTestEvaluator implements ITestVisitor {

    private boolean matches = true;
    private final Test test;
    private final Object thisObject;
    private final Object targetObject;
    private final Object[] args;
    private final MatchingContext matchContext;

    public RuntimeTestEvaluator(Test aTest, Object thisObject, Object targetObject, Object[] args, MatchingContext context) {
      this.test = aTest;
      this.thisObject = thisObject;
      this.targetObject = targetObject;
      this.args = args;
      this.matchContext = context;
    }

    public boolean matches() {
      test.accept(this);
      return matches;
    }

    @Override
    public void visit(And e) {
      final boolean leftMatches = new RuntimeTestEvaluator(e.getLeft(), thisObject, targetObject, args, matchContext).matches();
      if (!leftMatches) {
        matches = false;
      } else {
        matches = new RuntimeTestEvaluator(e.getRight(), thisObject, targetObject, args, matchContext).matches();
      }
    }

    @Override
    public void visit(Instanceof instanceofTest) {
      final ReflectionVar v = (ReflectionVar) instanceofTest.getVar();
      final Object value = v.getBindingAtJoinPoint(thisObject, targetObject, args);
      final World world = v.getType().getWorld();
      final ResolvedType desiredType = instanceofTest.getType().resolve(world);
      if (value == null) {
        matches = false;
      } else {
        final ResolvedType actualType = world.resolve(value.getClass().getName());
        matches = desiredType.isAssignableFrom(actualType);
      }
    }

    @Override
    public void visit(MatchingContextBasedTest matchingContextTest) {
      matches = matchingContextTest.matches(this.matchContext);
    }

    @Override
    public void visit(Not not) {
      matches = !new RuntimeTestEvaluator(not.getBody(), thisObject, targetObject, args, matchContext).matches();
    }

    @Override
    public void visit(Or or) {
      final boolean leftMatches = new RuntimeTestEvaluator(or.getLeft(), thisObject, targetObject, args, matchContext).matches();
      if (leftMatches) {
        matches = true;
      } else {
        matches = new RuntimeTestEvaluator(or.getRight(), thisObject, targetObject, args, matchContext).matches();
      }
    }

    @Override
    public void visit(Literal literal) {
      if (literal == Literal.FALSE) {
        matches = false;
      } else {
        matches = true;
      }
    }

    @Override
    public void visit(Call call) {
      throw new UnsupportedOperationException("Can't evaluate call test at runtime");
    }

    @Override
    public void visit(FieldGetCall fieldGetCall) {
      throw new UnsupportedOperationException("Can't evaluate fieldGetCall test at runtime");
    }

    @Override
    public void visit(HasAnnotation hasAnnotation) {
      final ReflectionVar v = (ReflectionVar) hasAnnotation.getVar();
      final Object value = v.getBindingAtJoinPoint(thisObject, targetObject, args);
      final World world = v.getType().getWorld();
      final ResolvedType actualVarType = world.resolve(value.getClass().getName());
      final ResolvedType requiredAnnotationType = hasAnnotation.getAnnotationType().resolve(world);
      matches = actualVarType.hasAnnotation(requiredAnnotationType);
    }

  }

}
