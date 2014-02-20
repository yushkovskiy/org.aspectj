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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PerCflow extends PerClause {
  private final boolean isBelow;
  private final Pointcut entry;

  @NotNull
  public static PerClause readPerClause(@NotNull VersionedDataInputStream s, ISourceContext context) throws IOException {
    final PerCflow ret = new PerCflow(Pointcut.read(s, context), s.readBoolean());
    ret.readLocation(context, s);
    return ret;
  }

  public PerCflow(Pointcut entry, boolean isBelow) {
    this.entry = entry;
    this.isBelow = isBelow;
  }

  // -----

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS;
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return FuzzyBoolean.MAYBE;
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    // assert bindings == null;
    entry.resolve(scope);
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final PerCflow ret = new PerCflow(entry.parameterizeWith(typeVariableMap, w), isBelow);
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public PerClause concretize(ResolvedType inAspect) {
    final PerCflow ret = new PerCflow(entry, isBelow);
    ret.inAspect = inAspect;
    if (inAspect.isAbstract()) {
      return ret;
    }

    final Member cflowStackField = new ResolvedMemberImpl(Member.FIELD, inAspect, Modifier.STATIC | Modifier.PUBLIC | Modifier.FINAL,
        UnresolvedType.forName(NameMangler.CFLOW_STACK_TYPE), NameMangler.PERCFLOW_FIELD_NAME, UnresolvedType.NONE);

    final World world = inAspect.getWorld();

    final CrosscuttingMembers xcut = inAspect.crosscuttingMembers;

    final Collection<ShadowMunger> previousCflowEntries = xcut.getCflowEntries();
    final Pointcut concreteEntry = entry.concretize(inAspect, inAspect, 0, null); // IntMap
    // .
    // EMPTY
    // )
    // ;
    final List<ShadowMunger> innerCflowEntries = new ArrayList<ShadowMunger>(xcut.getCflowEntries());
    innerCflowEntries.removeAll(previousCflowEntries);

    xcut.addConcreteShadowMunger(Advice.makePerCflowEntry(world, concreteEntry, isBelow, cflowStackField, inAspect,
        innerCflowEntries));

    // ATAJ: add a munger to add the aspectOf(..) to the @AJ aspects
    if (inAspect.isAnnotationStyleAspect() && !inAspect.isAbstract()) {
      inAspect.crosscuttingMembers.addLateTypeMunger(inAspect.getWorld().getWeavingSupport()
          .makePerClauseAspect(inAspect, getKind()));
    }

    // ATAJ inline around advice support - don't use a late munger to allow
    // around inling for itself
    if (inAspect.isAnnotationStyleAspect() && !inAspect.getWorld().isXnoInline()) {
      inAspect.crosscuttingMembers.addTypeMunger(inAspect.getWorld().getWeavingSupport()
          .createAccessForInlineMunger(inAspect));
    }

    return ret;
  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    PERCFLOW.write(s);
    entry.write(s);
    s.writeBoolean(isBelow);
    writeLocation(s);
  }

  @Override
  public PerClause.Kind getKind() {
    return PERCFLOW;
  }

  public Pointcut getEntry() {
    return entry;
  }

  public String toString() {
    return "percflow(" + inAspect + " on " + entry + ")";
  }

  @Override
  public String toDeclarationString() {
    if (isBelow) {
      return "percflowbelow(" + entry + ")";
    }
    return "percflow(" + entry + ")";
  }

  public boolean equals(Object other) {
    if (!(other instanceof PerCflow)) {
      return false;
    }
    final PerCflow pc = (PerCflow) other;
    return (pc.isBelow && isBelow) && ((pc.inAspect == null) ? (inAspect == null) : pc.inAspect.equals(inAspect))
        && ((pc.entry == null) ? (entry == null) : pc.entry.equals(entry));
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + (isBelow ? 0 : 1);
    result = 37 * result + ((inAspect == null) ? 0 : inAspect.hashCode());
    result = 37 * result + ((entry == null) ? 0 : entry.hashCode());
    return result;
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    return FuzzyBoolean.YES;
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    final Expr myInstance = Expr.makeCallExpr(AjcMemberMaker.perCflowAspectOfMethod(inAspect), Expr.NONE, inAspect);
    state.setAspectInstance(myInstance);
    return Test.makeCall(AjcMemberMaker.perCflowHasAspectMethod(inAspect), Expr.NONE);
  }

}
