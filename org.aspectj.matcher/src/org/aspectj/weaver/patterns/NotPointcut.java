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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class NotPointcut extends Pointcut {
  private final Pointcut body;

  public NotPointcut(Pointcut negated) {
    super();
    this.body = negated;
    this.pointcutKind = NOT;
    setLocation(negated.getSourceContext(), negated.getStart(), negated.getEnd()); // should that be at least start-1?
  }

  public NotPointcut(Pointcut pointcut, int startPos) {
    this(pointcut);
    setLocation(pointcut.getSourceContext(), startPos, pointcut.getEnd());
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS;
  }

  public Pointcut getNegatedPointcut() {
    return body;
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return body.fastMatch(type).not();
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    return body.match(shadow).not();
  }

  public String toString() {
    return "!" + body.toString();

  }

  public boolean equals(Object other) {
    if (!(other instanceof NotPointcut)) {
      return false;
    }
    final NotPointcut o = (NotPointcut) other;
    return o.body.equals(body);
  }

  public int hashCode() {
    return 37 * 23 + body.hashCode();
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    // Bindings old = bindings.copy();

    // Bindings newBindings = new Bindings(bindings.size());

    body.resolveBindings(scope, null);

    // newBindings.checkEmpty(scope, "negation does not allow binding");
    // bindings.checkEquals(old, scope);

  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.NOT);
    body.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final NotPointcut ret = new NotPointcut(Pointcut.read(s, context));
    ret.readLocation(context, s);
    return ret;
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    return Test.makeNot(body.findResidue(shadow, state));
  }

  @Override
  public Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    final Pointcut ret = new NotPointcut(body.concretize(inAspect, declaringType, bindings));
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final Pointcut ret = new NotPointcut(body.parameterizeWith(typeVariableMap, w));
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public Object traverse(PatternNodeVisitor visitor, Object data) {
    final Object ret = accept(visitor, data);
    this.body.traverse(visitor, ret);
    return ret;
  }

}
