/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 * ******************************************************************/

package org.aspectj.weaver.patterns;

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Test;

import java.io.IOException;
import java.util.Map;

public class OrPointcut extends Pointcut {
  Pointcut left, right;
  private final int couldMatchKinds;

  public OrPointcut(Pointcut left, Pointcut right) {
    super();
    this.left = left;
    this.right = right;
    setLocation(left.getSourceContext(), left.getStart(), right.getEnd());
    this.pointcutKind = OR;
    this.couldMatchKinds = left.couldMatchKinds() | right.couldMatchKinds();
  }

  @Override
  public int couldMatchKinds() {
    return couldMatchKinds;
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return left.fastMatch(type).or(right.fastMatch(type));
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    final FuzzyBoolean leftMatch = left.match(shadow);
    if (leftMatch.alwaysTrue()) {
      return leftMatch;
    }
    return leftMatch.or(right.match(shadow));
  }

  public String toString() {
    return "(" + left.toString() + " || " + right.toString() + ")";
  }

  public boolean equals(Object other) {
    if (!(other instanceof OrPointcut)) {
      return false;
    }
    final OrPointcut o = (OrPointcut) other;
    return o.left.equals(left) && o.right.equals(right);
  }

  public int hashCode() {
    int result = 31;
    result = 37 * result + left.hashCode();
    result = 37 * result + right.hashCode();
    return result;
  }

  /**
   * @see org.aspectj.weaver.patterns.Pointcut#resolveBindings(IScope, Bindings)
   */
  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    final Bindings old = bindings == null ? null : bindings.copy();

    left.resolveBindings(scope, bindings);
    right.resolveBindings(scope, old);
    if (bindings != null) {
      bindings.checkEquals(old, scope);
    }

  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.OR);
    left.write(s);
    right.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final OrPointcut ret = new OrPointcut(Pointcut.read(s, context), Pointcut.read(s, context));
    ret.readLocation(context, s);
    return ret;

  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    return Test.makeOr(left.findResidue(shadow, state), right.findResidue(shadow, state));
  }

  @Override
  public Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    final Pointcut ret = new OrPointcut(left.concretize(inAspect, declaringType, bindings), right.concretize(inAspect, declaringType,
        bindings));
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final Pointcut ret = new OrPointcut(left.parameterizeWith(typeVariableMap, w), right.parameterizeWith(typeVariableMap, w));
    ret.copyLocationFrom(this);
    return ret;
  }

  public Pointcut getLeft() {
    return left;
  }

  public Pointcut getRight() {
    return right;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public Object traverse(PatternNodeVisitor visitor, Object data) {
    final Object ret = accept(visitor, data);
    left.traverse(visitor, ret);
    right.traverse(visitor, ret);
    return ret;
  }
}
