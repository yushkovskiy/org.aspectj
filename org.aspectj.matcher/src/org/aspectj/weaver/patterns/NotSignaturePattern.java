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
import java.util.List;
import java.util.Map;

/**
 * Represents the NOT of a signature pattern
 *
 * @author Andy Clement
 * @since 1.6.9
 */
public class NotSignaturePattern extends AbstractSignaturePattern {

  private final ISignaturePattern negatedSp;

  public NotSignaturePattern(ISignaturePattern negatedSp) {
    this.negatedSp = negatedSp;
  }

  @Override
  public boolean couldEverMatch(ResolvedType type) {
    if (negatedSp.getExactDeclaringTypes().size() == 0) {
      return true;
    }
    return !negatedSp.couldEverMatch(type);
  }

  @Override
  public List<ExactTypePattern> getExactDeclaringTypes() {
    return negatedSp.getExactDeclaringTypes();
  }

  @Override
  public boolean isMatchOnAnyName() {
    return negatedSp.isMatchOnAnyName();
  }

  @Override
  public boolean isStarAnnotation() {
    return negatedSp.isStarAnnotation();
  }

  @Override
  public boolean matches(Member member, World world, boolean b) {
    return !negatedSp.matches(member, world, b);
  }

  @Override
  public ISignaturePattern parameterizeWith(Map<String, UnresolvedType> typeVariableBindingMap, World world) {
    return new NotSignaturePattern(negatedSp.parameterizeWith(typeVariableBindingMap, world));
  }

  @Override
  public ISignaturePattern resolveBindings(IScope scope, Bindings bindings) {
    // Whilst the real SignaturePattern returns 'this' we are safe to return 'this' here rather than build a new
    // AndSignaturePattern
    negatedSp.resolveBindings(scope, bindings);
    return this;
  }

  public static ISignaturePattern readNotSignaturePattern(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final NotSignaturePattern ret = new NotSignaturePattern(readCompoundSignaturePattern(s, context));
    // ret.readLocation(context, s); // TODO output position currently useless so dont need to do this
    s.readInt();
    s.readInt();
    return ret;
  }

  public ISignaturePattern getNegated() {
    return negatedSp;
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("!").append(negatedSp.toString());
    return sb.toString();
  }

}
