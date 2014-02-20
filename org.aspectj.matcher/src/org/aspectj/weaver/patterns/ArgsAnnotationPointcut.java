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

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.ISourceLocation;
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
public class ArgsAnnotationPointcut extends NameBindingPointcut {

  private final AnnotationPatternList arguments;
  private String declarationText;

  /**
   *
   */
  public ArgsAnnotationPointcut(AnnotationPatternList arguments) {
    super();
    this.arguments = arguments;
    this.pointcutKind = ATARGS;
    buildDeclarationText();
  }

  public AnnotationPatternList getArguments() {
    return arguments;
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS; // empty args() matches jps with no args
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final ArgsAnnotationPointcut ret = new ArgsAnnotationPointcut(arguments.parameterizeWith(typeVariableMap, w));
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
    arguments.resolve(shadow.getIWorld());
    final FuzzyBoolean ret = arguments.matches(shadow.getIWorld().resolve(shadow.getArgTypes()));
    return ret;
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
      scope.message(MessageUtil.error(WeaverMessages.format(WeaverMessages.ATARGS_ONLY_SUPPORTED_AT_JAVA5_LEVEL),
          getSourceLocation()));
      return;
    }
    arguments.resolveBindings(scope, bindings, true);
    if (arguments.ellipsisCount > 1) {
      scope.message(IMessage.ERROR, this, "uses more than one .. in args (compiler limitation)");
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.Pointcut#concretize1(org.aspectj.weaver.ResolvedType, org.aspectj.weaver.IntMap)
   */
  @Override
  protected Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    if (isDeclare(bindings.getEnclosingAdvice())) {
      // Enforce rule about which designators are supported in declare
      inAspect.getWorld().showMessage(IMessage.ERROR, WeaverMessages.format(WeaverMessages.ARGS_IN_DECLARE),
          bindings.getEnclosingAdvice().getSourceLocation(), null);
      return Pointcut.makeMatchesNothing(Pointcut.CONCRETE);
    }
    final AnnotationPatternList list = arguments.resolveReferences(bindings);
    final Pointcut ret = new ArgsAnnotationPointcut(list);
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
    final int len = shadow.getArgCount();

    // do some quick length tests first
    final int numArgsMatchedByEllipsis = (len + arguments.ellipsisCount) - arguments.size();
    if (numArgsMatchedByEllipsis < 0) {
      return Literal.FALSE; // should never happen
    }
    if ((numArgsMatchedByEllipsis > 0) && (arguments.ellipsisCount == 0)) {
      return Literal.FALSE; // should never happen
    }
    // now work through the args and the patterns, skipping at ellipsis
    Test ret = Literal.TRUE;
    int argsIndex = 0;
    for (int i = 0; i < arguments.size(); i++) {
      if (arguments.get(i) == AnnotationTypePattern.ELLIPSIS) {
        // match ellipsisMatchCount args
        argsIndex += numArgsMatchedByEllipsis;
      } else if (arguments.get(i) == AnnotationTypePattern.ANY) {
        argsIndex++;
      } else {
        // match the argument type at argsIndex with the ExactAnnotationTypePattern
        // we know it is exact because nothing else is allowed in args
        final ExactAnnotationTypePattern ap = (ExactAnnotationTypePattern) arguments.get(i);
        final UnresolvedType argType = shadow.getArgType(argsIndex);
        final ResolvedType rArgType = argType.resolve(shadow.getIWorld());
        if (rArgType.isMissing()) {
          shadow.getIWorld().getLint().cantFindType.signal(new String[]{WeaverMessages.format(
              WeaverMessages.CANT_FIND_TYPE_ARG_TYPE, argType.getName())}, shadow.getSourceLocation(),
              new ISourceLocation[]{getSourceLocation()});
          // IMessage msg = new Message(
          // WeaverMessages.format(WeaverMessages.CANT_FIND_TYPE_ARG_TYPE,argType.getName()),
          // "",IMessage.ERROR,shadow.getSourceLocation(),null,new ISourceLocation[]{getSourceLocation()});
        }

        final ResolvedType rAnnType = ap.getAnnotationType().resolve(shadow.getIWorld());
        if (ap instanceof BindingAnnotationTypePattern) {
          final BindingAnnotationTypePattern btp = (BindingAnnotationTypePattern) ap;
          final Var annvar = shadow.getArgAnnotationVar(argsIndex, rAnnType);
          state.set(btp.getFormalIndex(), annvar);
        }
        if (!ap.matches(rArgType).alwaysTrue()) {
          // we need a test...
          ret = Test.makeAnd(ret, Test.makeHasAnnotation(shadow.getArgVar(argsIndex), rAnnType));
        }
        argsIndex++;
      }
    }
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.NameBindingPointcut#getBindingAnnotationTypePatterns()
   */
  @Override
  public List getBindingAnnotationTypePatterns() {
    final List l = new ArrayList();
    final AnnotationTypePattern[] pats = arguments.getAnnotationPatterns();
    for (int i = 0; i < pats.length; i++) {
      if (pats[i] instanceof BindingAnnotationTypePattern) {
        l.add(pats[i]);
      }
    }
    return l;
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
    s.writeByte(Pointcut.ATARGS);
    arguments.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final AnnotationPatternList annotationPatternList = AnnotationPatternList.read(s, context);
    final ArgsAnnotationPointcut ret = new ArgsAnnotationPointcut(annotationPatternList);
    ret.readLocation(context, s);
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof ArgsAnnotationPointcut)) {
      return false;
    }
    final ArgsAnnotationPointcut other = (ArgsAnnotationPointcut) obj;
    return other.arguments.equals(arguments);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 17 + 37 * arguments.hashCode();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  private void buildDeclarationText() {
    final StringBuffer buf = new StringBuffer("@args");
    buf.append(arguments.toString());
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
