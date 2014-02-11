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

import org.aspectj.bridge.MessageUtil;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;

import java.io.IOException;
import java.util.Map;

public class WithincodePointcut extends Pointcut {
  private SignaturePattern signature;
  private static final int matchedShadowKinds;

  static {
    int flags = Shadow.ALL_SHADOW_KINDS_BITS;
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (Shadow.SHADOW_KINDS[i].isEnclosingKind()) {
        flags -= Shadow.SHADOW_KINDS[i].bit;
      }
    }
    // these next two are needed for inlining of field initializers
    flags |= Shadow.ConstructorExecution.bit;
    flags |= Shadow.Initialization.bit;
    matchedShadowKinds = flags;
  }

  public WithincodePointcut(SignaturePattern signature) {
    this.signature = signature;
    this.pointcutKind = WITHINCODE;
  }

  public SignaturePattern getSignature() {
    return signature;
  }

  @Override
  public int couldMatchKinds() {
    return matchedShadowKinds;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final WithincodePointcut ret = new WithincodePointcut(signature.parameterizeWith(typeVariableMap, w));
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return FuzzyBoolean.MAYBE;
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    // This will not match code in local or anonymous classes as if
    // they were withincode of the outer signature
    return FuzzyBoolean.fromBoolean(signature.matches(shadow.getEnclosingCodeSignature(), shadow.getIWorld(), false));
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.WITHINCODE);
    signature.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final WithincodePointcut ret = new WithincodePointcut(SignaturePattern.read(s, context));
    ret.readLocation(context, s);
    return ret;
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    signature = signature.resolveBindings(scope, bindings);

    // look for inappropriate use of parameterized types and tell user...
    HasThisTypePatternTriedToSneakInSomeGenericOrParameterizedTypePatternMatchingStuffAnywhereVisitor visitor = new HasThisTypePatternTriedToSneakInSomeGenericOrParameterizedTypePatternMatchingStuffAnywhereVisitor();
    signature.getDeclaringType().traverse(visitor, null);
    if (visitor.wellHasItThen/* ? */()) {
      scope.message(MessageUtil.error(WeaverMessages
          .format(WeaverMessages.WITHINCODE_DOESNT_SUPPORT_PARAMETERIZED_DECLARING_TYPES), getSourceLocation()));
    }

    visitor = new HasThisTypePatternTriedToSneakInSomeGenericOrParameterizedTypePatternMatchingStuffAnywhereVisitor();
    signature.getThrowsPattern().traverse(visitor, null);
    if (visitor.wellHasItThen/* ? */()) {
      scope.message(MessageUtil.error(WeaverMessages.format(WeaverMessages.NO_GENERIC_THROWABLES), getSourceLocation()));
    }
  }

  @Override
  public void postRead(ResolvedType enclosingType) {
    signature.postRead(enclosingType);
  }

  public boolean equals(Object other) {
    if (!(other instanceof WithincodePointcut)) {
      return false;
    }
    final WithincodePointcut o = (WithincodePointcut) other;
    return o.signature.equals(this.signature);
  }

  public int hashCode() {
    int result = 43;
    result = 37 * result + signature.hashCode();
    return result;
  }

  public String toString() {
    return "withincode(" + signature + ")";
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    return match(shadow).alwaysTrue() ? Literal.TRUE : Literal.FALSE;
  }

  @Override
  public Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    final Pointcut ret = new WithincodePointcut(signature);
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
