/* *******************************************************************
 * Copyright (c) 2010 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement - SpringSource
 * ******************************************************************/
package org.aspectj.weaver.patterns;

import org.aspectj.weaver.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the OR of two other signature patterns.
 *
 * @author Andy Clement
 * @since 1.6.9
 */
public class OrSignaturePattern extends AbstractSignaturePattern {

  private final ISignaturePattern leftSp;
  private final ISignaturePattern rightSp;
  private List<ExactTypePattern> exactDeclaringTypes;

  public OrSignaturePattern(ISignaturePattern leftSp, ISignaturePattern rightSp) {
    this.leftSp = leftSp;
    this.rightSp = rightSp;
  }

  @Override
  public boolean couldEverMatch(ResolvedType type) {
    return leftSp.couldEverMatch(type) || rightSp.couldEverMatch(type);
  }

  @Override
  public List<ExactTypePattern> getExactDeclaringTypes() {
    if (exactDeclaringTypes == null) {
      exactDeclaringTypes = new ArrayList<ExactTypePattern>();
      exactDeclaringTypes.addAll(leftSp.getExactDeclaringTypes());
      exactDeclaringTypes.addAll(rightSp.getExactDeclaringTypes());
    }
    return exactDeclaringTypes;
  }

  @Override
  public boolean isMatchOnAnyName() {
    return leftSp.isMatchOnAnyName() || rightSp.isMatchOnAnyName();
  }

  @Override
  public boolean isStarAnnotation() {
    return leftSp.isStarAnnotation() || rightSp.isStarAnnotation();
  }

  @Override
  public boolean matches(Member member, World world, boolean b) {
    return (leftSp.matches(member, world, b)) || (rightSp.matches(member, world, b));
  }

  @Override
  public ISignaturePattern parameterizeWith(Map<String, UnresolvedType> typeVariableBindingMap, World world) {
    return new OrSignaturePattern(leftSp.parameterizeWith(typeVariableBindingMap, world), rightSp.parameterizeWith(
        typeVariableBindingMap, world));
  }

  @Override
  public ISignaturePattern resolveBindings(IScope scope, Bindings bindings) {
    // Whilst the real SignaturePattern returns 'this' we are safe to return 'this' here rather than build a new
    // AndSignaturePattern
    leftSp.resolveBindings(scope, bindings);
    rightSp.resolveBindings(scope, bindings);
    return this;
  }

  public static ISignaturePattern readOrSignaturePattern(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final OrSignaturePattern ret = new OrSignaturePattern(readCompoundSignaturePattern(s, context), readCompoundSignaturePattern(s,
        context));
    s.readInt();
    s.readInt();
    // ret.readLocation(context, s); // TODO output position currently useless so dont need to do this
    return ret;
  }

  public ISignaturePattern getLeft() {
    return leftSp;
  }

  public ISignaturePattern getRight() {
    return rightSp;
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(leftSp.toString()).append(" || ").append(rightSp.toString());
    return sb.toString();
  }

}
