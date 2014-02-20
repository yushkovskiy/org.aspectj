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

import org.aspectj.bridge.MessageUtil;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.ast.Var;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author colyer
 *         <p/>
 *         TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code
 *         Templates
 */
public class WithinCodeAnnotationPointcut extends NameBindingPointcut {

  private ExactAnnotationTypePattern annotationTypePattern;
  private String declarationText;

  private static final int matchedShadowKinds;

  static {
    int flags = Shadow.ALL_SHADOW_KINDS_BITS;
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (Shadow.SHADOW_KINDS[i].isEnclosingKind()) {
        flags -= Shadow.SHADOW_KINDS[i].bit;
      }
    }
    matchedShadowKinds = flags;
  }

  public WithinCodeAnnotationPointcut(ExactAnnotationTypePattern type) {
    super();
    this.annotationTypePattern = type;
    this.pointcutKind = Pointcut.ATWITHINCODE;
    buildDeclarationText();
  }

  public WithinCodeAnnotationPointcut(ExactAnnotationTypePattern type, ShadowMunger munger) {
    this(type);
    this.pointcutKind = Pointcut.ATWITHINCODE;
  }

  public ExactAnnotationTypePattern getAnnotationTypePattern() {
    return annotationTypePattern;
  }

  @Override
  public int couldMatchKinds() {
    return matchedShadowKinds;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final WithinCodeAnnotationPointcut ret = new WithinCodeAnnotationPointcut((ExactAnnotationTypePattern) this.annotationTypePattern
        .parameterizeWith(typeVariableMap, w));
    ret.copyLocationFrom(this);
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#fastMatch(org.aspectj.weaver.patterns.FastMatchInfo)
   */
  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo info) {
    return FuzzyBoolean.MAYBE;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#match(org.aspectj.weaver.Shadow)
   */
  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    final Member member = shadow.getEnclosingCodeSignature();
    final ResolvedMember rMember = member.resolve(shadow.getIWorld());

    if (rMember == null) {
      if (member.getName().startsWith(NameMangler.PREFIX)) {
        return FuzzyBoolean.NO;
      }
      shadow.getIWorld().getLint().unresolvableMember.signal(member.toString(), getSourceLocation());
      return FuzzyBoolean.NO;
    }

    annotationTypePattern.resolve(shadow.getIWorld());
    return annotationTypePattern.matches(rMember);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#resolveBindings(org.aspectj.weaver.patterns.IScope,
   * org.aspectj.weaver.patterns.Bindings)
   */
  @Override
  protected void resolveBindings(IScope scope, Bindings bindings) {
    if (!scope.getWorld().isInJava5Mode()) {
      scope.message(MessageUtil.error(WeaverMessages.format(WeaverMessages.ATWITHINCODE_ONLY_SUPPORTED_AT_JAVA5_LEVEL),
          getSourceLocation()));
      return;
    }
    annotationTypePattern = (ExactAnnotationTypePattern) annotationTypePattern.resolveBindings(scope, bindings, true);
    // must be either a Var, or an annotation type pattern
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#concretize1(org.aspectj.weaver.ResolvedType, org.aspectj.weaver.IntMap)
   */
  @Override
  protected Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    final ExactAnnotationTypePattern newType = (ExactAnnotationTypePattern) annotationTypePattern.remapAdviceFormals(bindings);
    final Pointcut ret = new WithinCodeAnnotationPointcut(newType, bindings.getEnclosingAdvice());
    ret.copyLocationFrom(this);
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#findResidue(org.aspectj.weaver.Shadow, org.aspectj.weaver.patterns.ExposedState)
   */
  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {

    if (annotationTypePattern instanceof BindingAnnotationTypePattern) {
      final BindingAnnotationTypePattern btp = (BindingAnnotationTypePattern) annotationTypePattern;
      final UnresolvedType annotationType = btp.annotationType;
      final Var var = shadow.getWithinCodeAnnotationVar(annotationType);

      // This should not happen, we shouldn't have gotten this far
      // if we weren't going to find the annotation
      if (var == null) {
        throw new BCException("Impossible! annotation=[" + annotationType + "]  shadow=[" + shadow + " at "
            + shadow.getSourceLocation() + "]    pointcut is at [" + getSourceLocation() + "]");
      }

      state.set(btp.getFormalIndex(), var);
    }
    if (matchInternal(shadow).alwaysTrue()) {
      return Literal.TRUE;
    } else {
      return Literal.FALSE;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.NameBindingPointcut#getBindingAnnotationTypePatterns()
   */
  @Override
  public List getBindingAnnotationTypePatterns() {
    if (annotationTypePattern instanceof BindingAnnotationTypePattern) {
      final List l = new ArrayList();
      l.add(annotationTypePattern);
      return l;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.NameBindingPointcut#getBindingTypePatterns()
   */
  @Override
  public List getBindingTypePatterns() {
    return Collections.EMPTY_LIST;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.PatternNode#write(java.io.DataOutputStream)
   */
  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.ATWITHINCODE);
    annotationTypePattern.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final AnnotationTypePattern type = AnnotationTypePattern.read(s, context);
    final WithinCodeAnnotationPointcut ret = new WithinCodeAnnotationPointcut((ExactAnnotationTypePattern) type);
    ret.readLocation(context, s);
    return ret;
  }

  public boolean equals(Object other) {
    if (!(other instanceof WithinCodeAnnotationPointcut)) {
      return false;
    }
    final WithinCodeAnnotationPointcut o = (WithinCodeAnnotationPointcut) other;
    return o.annotationTypePattern.equals(this.annotationTypePattern);
  }

  public int hashCode() {
    int result = 17;
    result = 23 * result + annotationTypePattern.hashCode();
    return result;
  }

  private void buildDeclarationText() {
    final StringBuffer buf = new StringBuffer();
    buf.append("@withincode(");
    final String annPatt = annotationTypePattern.toString();
    buf.append(annPatt.startsWith("@") ? annPatt.substring(1) : annPatt);
    buf.append(")");
    this.declarationText = buf.toString();
  }

  public String toString() {
    return this.declarationText;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
