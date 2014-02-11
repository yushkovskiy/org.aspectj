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

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.bridge.Message;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Expr;
import org.aspectj.weaver.ast.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConcreteCflowPointcut extends Pointcut {
  private final Member cflowField;
  List/* Slot */slots; // exposed for testing
  boolean usesCounter;
  ResolvedType aspect;

  // Can either use a counter or a stack to implement cflow.
  public ConcreteCflowPointcut(ResolvedType aspect, Member cflowField, List slots, boolean usesCounter) {
    this.aspect = aspect;
    this.cflowField = cflowField;
    this.slots = slots;
    this.usesCounter = usesCounter;
    this.pointcutKind = CFLOW;
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
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    // ??? this is not maximally efficient
    // Check we'll be able to do the residue!

    // this bit is for pr145693 - we cannot match at all if one of the types is missing, we will be unable
    // to create the residue
    if (slots != null) {
      for (final Iterator i = slots.iterator(); i.hasNext(); ) {
        final Slot slot = (Slot) i.next();
        final ResolvedType rt = slot.formalType;
        if (rt.isMissing()) {
          final ISourceLocation[] locs = new ISourceLocation[]{getSourceLocation()};
          final Message m = new Message(WeaverMessages.format(WeaverMessages.MISSING_TYPE_PREVENTS_MATCH, rt.getName()), "",
              Message.WARNING, shadow.getSourceLocation(), null, locs);
          rt.getWorld().getMessageHandler().handleMessage(m);
          return FuzzyBoolean.NO;
        }
      }
    }
    return FuzzyBoolean.MAYBE;
  }

  // used by weaver when validating bindings
  public int[] getUsedFormalSlots() {
    if (slots == null) {
      return new int[0];
    }
    final int[] indices = new int[slots.size()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = ((Slot) slots.get(i)).formalIndex;
    }
    return indices;
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    throw new RuntimeException("unimplemented");
  }

  public boolean equals(Object other) {
    if (!(other instanceof ConcreteCflowPointcut)) {
      return false;
    }
    final ConcreteCflowPointcut o = (ConcreteCflowPointcut) other;
    return o.cflowField.equals(this.cflowField);
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + cflowField.hashCode();
    return result;
  }

  public String toString() {
    return "concretecflow(" + cflowField + ")";
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    // System.out.println("find residue: " + this);
    if (usesCounter) {
      return Test.makeFieldGetCall(cflowField, cflowCounterIsValidMethod, Expr.NONE);
    } else {
      if (slots != null) { // null for cflows managed by counters
        for (final Iterator i = slots.iterator(); i.hasNext(); ) {
          final Slot slot = (Slot) i.next();
          // System.out.println("slot: " + slot.formalIndex);
          state.set(slot.formalIndex,
              aspect.getWorld().getWeavingSupport().makeCflowAccessVar(slot.formalType, cflowField, slot.arrayIndex));
        }
      }
      return Test.makeFieldGetCall(cflowField, cflowStackIsValidMethod, Expr.NONE);
    }
  }

  private static final Member cflowStackIsValidMethod = MemberImpl.method(NameMangler.CFLOW_STACK_UNRESOLVEDTYPE, 0,
      UnresolvedType.BOOLEAN, "isValid", UnresolvedType.NONE);

  private static final Member cflowCounterIsValidMethod = MemberImpl.method(NameMangler.CFLOW_COUNTER_UNRESOLVEDTYPE, 0,
      UnresolvedType.BOOLEAN, "isValid", UnresolvedType.NONE);

  @Override
  public Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public static class Slot {
    int formalIndex;
    ResolvedType formalType;
    int arrayIndex;

    public Slot(int formalIndex, ResolvedType formalType, int arrayIndex) {
      this.formalIndex = formalIndex;
      this.formalType = formalType;
      this.arrayIndex = arrayIndex;
    }

    public boolean equals(Object other) {
      if (!(other instanceof Slot)) {
        return false;
      }

      final Slot o = (Slot) other;
      return o.formalIndex == this.formalIndex && o.arrayIndex == this.arrayIndex && o.formalType.equals(this.formalType);
    }

    public int hashCode() {
      int result = 19;
      result = 37 * result + formalIndex;
      result = 37 * result + arrayIndex;
      result = 37 * result + formalType.hashCode();
      return result;
    }

    public String toString() {
      return "Slot(" + formalIndex + ", " + formalType + ", " + arrayIndex + ")";
    }
  }

}
