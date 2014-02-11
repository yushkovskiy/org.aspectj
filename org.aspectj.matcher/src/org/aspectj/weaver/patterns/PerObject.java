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
import org.aspectj.weaver.ast.Expr;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.ast.Var;

import java.io.IOException;
import java.util.Map;

public class PerObject extends PerClause {
  private final boolean isThis;
  private final Pointcut entry;

  private static final int thisKindSet;
  private static final int targetKindSet;

  static {
    int thisFlags = Shadow.ALL_SHADOW_KINDS_BITS;
    int targFlags = Shadow.ALL_SHADOW_KINDS_BITS;
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      final Shadow.Kind kind = Shadow.SHADOW_KINDS[i];
      if (kind.neverHasThis()) {
        thisFlags -= kind.bit;
      }
      if (kind.neverHasTarget()) {
        targFlags -= kind.bit;
      }
    }
    thisKindSet = thisFlags;
    targetKindSet = targFlags;
  }

  public PerObject(Pointcut entry, boolean isThis) {
    this.entry = entry;
    this.isThis = isThis;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public int couldMatchKinds() {
    return isThis ? thisKindSet : targetKindSet;
  }

  // -----
  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return FuzzyBoolean.MAYBE;
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    // System.err.println("matches " + this + " ? " + shadow + ", " +
    // shadow.hasTarget());
    // ??? could probably optimize this better by testing could match
    if (isThis) {
      return FuzzyBoolean.fromBoolean(shadow.hasThis());
    } else {
      return FuzzyBoolean.fromBoolean(shadow.hasTarget());
    }
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    // assert bindings == null;
    entry.resolve(scope);
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final PerObject ret = new PerObject(entry.parameterizeWith(typeVariableMap, w), isThis);
    ret.copyLocationFrom(this);
    return ret;
  }

  private Var getVar(Shadow shadow) {
    return isThis ? shadow.getThisVar() : shadow.getTargetVar();
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    final Expr myInstance = Expr.makeCallExpr(AjcMemberMaker.perObjectAspectOfMethod(inAspect), new Expr[]{getVar(shadow)},
        inAspect);
    state.setAspectInstance(myInstance);
    return Test.makeCall(AjcMemberMaker.perObjectHasAspectMethod(inAspect), new Expr[]{getVar(shadow)});
  }

  @Override
  public PerClause concretize(ResolvedType inAspect) {
    final PerObject ret = new PerObject(entry, isThis);

    ret.inAspect = inAspect;
    if (inAspect.isAbstract()) {
      return ret;
    }

    final World world = inAspect.getWorld();

    final Pointcut concreteEntry = entry.concretize(inAspect, inAspect, 0, null);
    // concreteEntry = new AndPointcut(this, concreteEntry);
    // concreteEntry.state = Pointcut.CONCRETE;
    inAspect.crosscuttingMembers.addConcreteShadowMunger(Advice.makePerObjectEntry(world, concreteEntry, isThis, inAspect));

    // FIXME AV - don't use lateMunger here due to test
    // "inheritance, around advice and abstract pointcuts"
    // see #75442 thread. Issue with weaving order.
    final ResolvedTypeMunger munger = new PerObjectInterfaceTypeMunger(inAspect, concreteEntry);
    inAspect.crosscuttingMembers.addLateTypeMunger(world.getWeavingSupport().concreteTypeMunger(munger, inAspect));

    // ATAJ: add a munger to add the aspectOf(..) to the @AJ aspects
    if (inAspect.isAnnotationStyleAspect() && !inAspect.isAbstract()) {
      inAspect.crosscuttingMembers.addLateTypeMunger(inAspect.getWorld().getWeavingSupport().makePerClauseAspect(inAspect,
          getKind()));
    }

    // ATAJ inline around advice support - don't use a late munger to allow
    // around inling for itself
    if (inAspect.isAnnotationStyleAspect() && !inAspect.getWorld().isXnoInline()) {
      inAspect.crosscuttingMembers.addTypeMunger(world.getWeavingSupport().createAccessForInlineMunger(inAspect));
    }

    return ret;
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    PEROBJECT.write(s);
    entry.write(s);
    s.writeBoolean(isThis);
    writeLocation(s);
  }

  public static PerClause readPerClause(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final PerClause ret = new PerObject(Pointcut.read(s, context), s.readBoolean());
    ret.readLocation(context, s);
    return ret;
  }

  @Override
  public PerClause.Kind getKind() {
    return PEROBJECT;
  }

  public boolean isThis() {
    return isThis;
  }

  public String toString() {
    return "per" + (isThis ? "this" : "target") + "(" + entry + ")";
  }

  @Override
  public String toDeclarationString() {
    return toString();
  }

  public Pointcut getEntry() {
    return entry;
  }

  public boolean equals(Object other) {
    if (!(other instanceof PerObject)) {
      return false;
    }
    final PerObject pc = (PerObject) other;
    return (pc.isThis && isThis) && ((pc.inAspect == null) ? (inAspect == null) : pc.inAspect.equals(inAspect))
        && ((pc.entry == null) ? (entry == null) : pc.entry.equals(entry));
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + (isThis ? 0 : 1);
    result = 37 * result + ((inAspect == null) ? 0 : inAspect.hashCode());
    result = 37 * result + ((entry == null) ? 0 : entry.hashCode());
    return result;
  }
}
