/* *******************************************************************
 * Copyright (c) 2004 IBM Corporation.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * ******************************************************************/
package org.aspectj.weaver.patterns;

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.AjAttribute.WeaverVersionInfo;
import org.aspectj.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class NotAnnotationTypePattern extends AnnotationTypePattern {

  AnnotationTypePattern negatedPattern;

  public NotAnnotationTypePattern(AnnotationTypePattern pattern) {
    this.negatedPattern = pattern;
    setLocation(pattern.getSourceContext(), pattern.getStart(), pattern.getEnd());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.AnnotationTypePattern#matches(org.aspectj.weaver.AnnotatedElement)
   */
  @Override
  public FuzzyBoolean matches(AnnotatedElement annotated) {
    return negatedPattern.matches(annotated).not();
  }

  @Override
  public FuzzyBoolean matches(AnnotatedElement annotated, ResolvedType[] parameterAnnotations) {
    return negatedPattern.matches(annotated, parameterAnnotations).not();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.AnnotationTypePattern#resolve(org.aspectj.weaver.World)
   */
  @Override
  public void resolve(World world) {
    negatedPattern.resolve(world);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.AnnotationTypePattern#resolveBindings(org.aspectj.weaver.patterns.IScope,
   * org.aspectj.weaver.patterns.Bindings, boolean)
   */
  @Override
  public AnnotationTypePattern resolveBindings(IScope scope, Bindings bindings, boolean allowBinding) {
    negatedPattern = negatedPattern.resolveBindings(scope, bindings, allowBinding);
    return this;
  }

  @Override
  public AnnotationTypePattern parameterizeWith(Map typeVariableMap, World w) {
    final AnnotationTypePattern newNegatedPattern = negatedPattern.parameterizeWith(typeVariableMap, w);
    final NotAnnotationTypePattern ret = new NotAnnotationTypePattern(newNegatedPattern);
    ret.copyLocationFrom(this);
    if (this.isForParameterAnnotationMatch()) {
      ret.setForParameterAnnotationMatch();
    }
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.PatternNode#write(java.io.DataOutputStream)
   */
  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(AnnotationTypePattern.NOT);
    negatedPattern.write(s);
    writeLocation(s);
    s.writeBoolean(isForParameterAnnotationMatch());
  }

  public static AnnotationTypePattern read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final AnnotationTypePattern ret = new NotAnnotationTypePattern(AnnotationTypePattern.read(s, context));
    ret.readLocation(context, s);
    if (s.getMajorVersion() >= WeaverVersionInfo.WEAVER_VERSION_MAJOR_AJ160) {
      if (s.readBoolean()) {
        ret.setForParameterAnnotationMatch();
      }
    }
    return ret;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof NotAnnotationTypePattern)) {
      return false;
    }
    final NotAnnotationTypePattern other = (NotAnnotationTypePattern) obj;
    return other.negatedPattern.equals(negatedPattern)
        && other.isForParameterAnnotationMatch() == isForParameterAnnotationMatch();
  }

  public int hashCode() {
    int result = 17 + 37 * negatedPattern.hashCode();
    result = 37 * result + (isForParameterAnnotationMatch() ? 0 : 1);
    return result;
  }

  public String toString() {
    return "!" + negatedPattern.toString();
  }

  public AnnotationTypePattern getNegatedPattern() {
    return negatedPattern;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public Object traverse(PatternNodeVisitor visitor, Object data) {
    final Object ret = accept(visitor, data);
    negatedPattern.traverse(visitor, ret);
    return ret;
  }

  @Override
  public void setForParameterAnnotationMatch() {
    negatedPattern.setForParameterAnnotationMatch();
  }
}
