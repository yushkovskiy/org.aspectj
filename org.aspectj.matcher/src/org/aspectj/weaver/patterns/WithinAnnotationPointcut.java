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

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.bridge.MessageUtil;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.ast.Var;

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
public class WithinAnnotationPointcut extends NameBindingPointcut {

  private AnnotationTypePattern annotationTypePattern;
  private String declarationText;

  /**
   *
   */
  public WithinAnnotationPointcut(AnnotationTypePattern type) {
    super();
    this.annotationTypePattern = type;
    this.pointcutKind = ATWITHIN;
    buildDeclarationText();
  }

  public WithinAnnotationPointcut(AnnotationTypePattern type, ShadowMunger munger) {
    this(type);
    this.pointcutKind = ATWITHIN;
  }

  public AnnotationTypePattern getAnnotationTypePattern() {
    return annotationTypePattern;
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final WithinAnnotationPointcut ret = new WithinAnnotationPointcut(this.annotationTypePattern.parameterizeWith(typeVariableMap, w));
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
    return annotationTypePattern.fastMatches(info.getType());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#match(org.aspectj.weaver.Shadow)
   */
  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    final ResolvedType enclosingType = shadow.getIWorld().resolve(shadow.getEnclosingType(), true);
    if (enclosingType.isMissing()) {
      shadow.getIWorld().getLint().cantFindType.signal(new String[]{WeaverMessages.format(
          WeaverMessages.CANT_FIND_TYPE_WITHINPCD, shadow.getEnclosingType().getName())}, shadow.getSourceLocation(),
          new ISourceLocation[]{getSourceLocation()});
      // IMessage msg = new Message(
      // WeaverMessages.format(WeaverMessages.CANT_FIND_TYPE_WITHINPCD,
      // shadow.getEnclosingType().getName()),
      // shadow.getSourceLocation(),true,new ISourceLocation[]{getSourceLocation()});
      // shadow.getIWorld().getMessageHandler().handleMessage(msg);
    }
    annotationTypePattern.resolve(shadow.getIWorld());
    return annotationTypePattern.matches(enclosingType);
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
      scope.message(MessageUtil.error(WeaverMessages.format(WeaverMessages.ATWITHIN_ONLY_SUPPORTED_AT_JAVA5_LEVEL),
          getSourceLocation()));
      return;
    }
    annotationTypePattern = annotationTypePattern.resolveBindings(scope, bindings, true);
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
    final Pointcut ret = new WithinAnnotationPointcut(newType, bindings.getEnclosingAdvice());
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
      final Var var = shadow.getWithinAnnotationVar(annotationType);

      // This should not happen, we shouldn't have gotten this far
      // if we weren't going to find the annotation
      if (var == null) {
        throw new BCException("Impossible! annotation=[" + annotationType + "]  shadow=[" + shadow + " at "
            + shadow.getSourceLocation() + "]    pointcut is at [" + getSourceLocation() + "]");
      }

      state.set(btp.getFormalIndex(), var);
    }
    return match(shadow).alwaysTrue() ? Literal.TRUE : Literal.FALSE;
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
  public void write(CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.ATWITHIN);
    annotationTypePattern.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final AnnotationTypePattern type = AnnotationTypePattern.read(s, context);
    final WithinAnnotationPointcut ret = new WithinAnnotationPointcut(type);
    ret.readLocation(context, s);
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WithinAnnotationPointcut)) {
      return false;
    }
    final WithinAnnotationPointcut other = (WithinAnnotationPointcut) obj;
    return other.annotationTypePattern.equals(this.annotationTypePattern);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 17 + 19 * annotationTypePattern.hashCode();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  private void buildDeclarationText() {
    final StringBuffer buf = new StringBuffer();
    buf.append("@within(");
    final String annPatt = annotationTypePattern.toString();
    buf.append(annPatt.startsWith("@") ? annPatt.substring(1) : annPatt);
    buf.append(")");
    this.declarationText = buf.toString();
  }

  @Override
  public String toString() {
    return this.declarationText;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
