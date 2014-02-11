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
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;

import java.io.IOException;
import java.util.Map;

public class PerSingleton extends PerClause {

  private ResolvedMember perSingletonAspectOfMethod;

  public PerSingleton() {
  }

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
    return FuzzyBoolean.YES;
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    return FuzzyBoolean.YES;
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    // this method intentionally left blank
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    return this;
  }

  @Override
  public Test findResidueInternal(Shadow shadow, ExposedState state) {
    // TODO: the commented code is for slow Aspects.aspectOf() style - keep
    // or remove
    //
    // Expr myInstance =
    // Expr.makeCallExpr(AjcMemberMaker.perSingletonAspectOfMethod(inAspect),
    // Expr.NONE, inAspect);
    //
    // state.setAspectInstance(myInstance);
    //
    // // we have no test
    // // a NoAspectBoundException will be thrown if we need an instance of
    // this
    // // aspect before we are bound
    // return Literal.TRUE;
    // if (!Ajc5MemberMaker.isSlowAspect(inAspect)) {
    if (perSingletonAspectOfMethod == null) {
      // Build this just once
      perSingletonAspectOfMethod = AjcMemberMaker.perSingletonAspectOfMethod(inAspect);
    }
    final Expr myInstance = Expr.makeCallExpr(perSingletonAspectOfMethod, Expr.NONE, inAspect);

    state.setAspectInstance(myInstance);

    // we have no test
    // a NoAspectBoundException will be thrown if we need an instance of
    // this
    // aspect before we are bound
    return Literal.TRUE;
    // } else {
    // CallExpr callAspectOf =Expr.makeCallExpr(
    // Ajc5MemberMaker.perSingletonAspectOfMethod(inAspect),
    // new Expr[]{
    // Expr.makeStringConstantExpr(inAspect.getName(), inAspect),
    // //FieldGet is using ResolvedType and I don't need that here
    // new FieldGetOn(Member.ajClassField, shadow.getEnclosingType())
    // },
    // inAspect
    // );
    // Expr castedCallAspectOf = new CastExpr(callAspectOf,
    // inAspect.getName());
    // state.setAspectInstance(castedCallAspectOf);
    // return Literal.TRUE;
    // }
  }

  @Override
  public PerClause concretize(ResolvedType inAspect) {
    final PerSingleton ret = new PerSingleton();

    ret.copyLocationFrom(this);

    final World world = inAspect.getWorld();

    ret.inAspect = inAspect;

    // ATAJ: add a munger to add the aspectOf(..) to the @AJ aspects
    if (inAspect.isAnnotationStyleAspect() && !inAspect.isAbstract()) {
      // TODO will those change be ok if we add a serializable aspect ?
      // dig:
      // "can't be Serializable/Cloneable unless -XserializableAspects"
      if (getKind() == SINGLETON) { // pr149560
        inAspect.crosscuttingMembers.addTypeMunger(world.getWeavingSupport().makePerClauseAspect(inAspect, getKind()));
      } else {
        inAspect.crosscuttingMembers.addLateTypeMunger(world.getWeavingSupport().makePerClauseAspect(inAspect, getKind()));
      }
    }

    // ATAJ inline around advice support
    if (inAspect.isAnnotationStyleAspect() && !inAspect.getWorld().isXnoInline()) {
      inAspect.crosscuttingMembers.addTypeMunger(world.getWeavingSupport().createAccessForInlineMunger(inAspect));
    }

    return ret;
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    SINGLETON.write(s);
    writeLocation(s);
  }

  public static PerClause readPerClause(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final PerSingleton ret = new PerSingleton();
    ret.readLocation(context, s);
    return ret;
  }

  @Override
  public PerClause.Kind getKind() {
    return SINGLETON;
  }

  public String toString() {
    return "persingleton(" + inAspect + ")";
  }

  @Override
  public String toDeclarationString() {
    return "";
  }

  public boolean equals(Object other) {
    if (!(other instanceof PerSingleton)) {
      return false;
    }
    final PerSingleton pc = (PerSingleton) other;
    return ((pc.inAspect == null) ? (inAspect == null) : pc.inAspect.equals(inAspect));
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + ((inAspect == null) ? 0 : inAspect.hashCode());
    return result;
  }

}
