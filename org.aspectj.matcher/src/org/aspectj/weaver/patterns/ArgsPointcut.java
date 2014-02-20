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

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.ISourceLocation;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Test;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * args(arguments)
 *
 * @author Erik Hilsdale
 * @author Jim Hugunin
 */
public class ArgsPointcut extends NameBindingPointcut {
  private static final String ASPECTJ_JP_SIGNATURE_PREFIX = "Lorg/aspectj/lang/JoinPoint";
  private static final String ASPECTJ_SYNTHETIC_SIGNATURE_PREFIX = "Lorg/aspectj/runtime/internal/";

  private final TypePatternList arguments;
  private final String stringRepresentation;

  public ArgsPointcut(TypePatternList arguments) {
    this.arguments = arguments;
    this.pointcutKind = ARGS;
    this.stringRepresentation = "args" + arguments.toString() + "";
  }

  public TypePatternList getArguments() {
    return arguments;
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    final ArgsPointcut ret = new ArgsPointcut(this.arguments.parameterizeWith(typeVariableMap, w));
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS; // empty args() matches jps with no args
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    return FuzzyBoolean.MAYBE;
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    final ResolvedType[] argumentsToMatchAgainst = getArgumentsToMatchAgainst(shadow);
    final FuzzyBoolean ret = arguments.matches(argumentsToMatchAgainst, TypePattern.DYNAMIC);
    return ret;
  }

  private static ResolvedType[] getArgumentsToMatchAgainst(Shadow shadow) {

    if (shadow.isShadowForArrayConstructionJoinpoint()) {
      return shadow.getArgumentTypesForArrayConstructionShadow();
    }

    ResolvedType[] argumentsToMatchAgainst = shadow.getIWorld().resolve(shadow.getGenericArgTypes());

    // special treatment for adviceexecution which may have synthetic arguments we
    // want to ignore.
    if (shadow.getKind() == Shadow.AdviceExecution) {
      int numExtraArgs = 0;
      for (int i = 0; i < argumentsToMatchAgainst.length; i++) {
        final String argumentSignature = argumentsToMatchAgainst[i].getSignature();
        if (argumentSignature.startsWith(ASPECTJ_JP_SIGNATURE_PREFIX)
            || argumentSignature.startsWith(ASPECTJ_SYNTHETIC_SIGNATURE_PREFIX)) {
          numExtraArgs++;
        } else {
          // normal arg after AJ type means earlier arg was NOT synthetic
          numExtraArgs = 0;
        }
      }
      if (numExtraArgs > 0) {
        final int newArgLength = argumentsToMatchAgainst.length - numExtraArgs;
        final ResolvedType[] argsSubset = new ResolvedType[newArgLength];
        System.arraycopy(argumentsToMatchAgainst, 0, argsSubset, 0, newArgLength);
        argumentsToMatchAgainst = argsSubset;
      }
    } else if (shadow.getKind() == Shadow.ConstructorExecution) {
      if (shadow.getMatchingSignature().getParameterTypes().length < argumentsToMatchAgainst.length) {
        // there are one or more synthetic args on the end, caused by non-public itd constructor
        final int newArgLength = shadow.getMatchingSignature().getParameterTypes().length;
        final ResolvedType[] argsSubset = new ResolvedType[newArgLength];
        System.arraycopy(argumentsToMatchAgainst, 0, argsSubset, 0, newArgLength);
        argumentsToMatchAgainst = argsSubset;
      }
    }

    return argumentsToMatchAgainst;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.NameBindingPointcut#getBindingAnnotationTypePatterns()
   */
  @Override
  public List getBindingAnnotationTypePatterns() {
    return Collections.EMPTY_LIST;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.NameBindingPointcut#getBindingTypePatterns()
   */
  @Override
  public List getBindingTypePatterns() {
    final List l = new ArrayList();
    final TypePattern[] pats = arguments.getTypePatterns();
    for (int i = 0; i < pats.length; i++) {
      if (pats[i] instanceof BindingTypePattern) {
        l.add(pats[i]);
      }
    }
    return l;
  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(Pointcut.ARGS);
    arguments.write(s);
    writeLocation(s);
  }

  public static Pointcut read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final ArgsPointcut ret = new ArgsPointcut(TypePatternList.read(s, context));
    ret.readLocation(context, s);
    return ret;
  }

  public boolean equals(Object other) {
    if (!(other instanceof ArgsPointcut)) {
      return false;
    }
    final ArgsPointcut o = (ArgsPointcut) other;
    return o.arguments.equals(this.arguments);
  }

  public int hashCode() {
    return arguments.hashCode();
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    arguments.resolveBindings(scope, bindings, true, true);
    if (arguments.ellipsisCount > 1) {
      scope.message(IMessage.ERROR, this, "uses more than one .. in args (compiler limitation)");
    }
  }

  @Override
  public void postRead(ResolvedType enclosingType) {
    arguments.postRead(enclosingType);
  }

  @Override
  public Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    if (isDeclare(bindings.getEnclosingAdvice())) {
      // Enforce rule about which designators are supported in declare
      inAspect.getWorld().showMessage(IMessage.ERROR, WeaverMessages.format(WeaverMessages.ARGS_IN_DECLARE),
          bindings.getEnclosingAdvice().getSourceLocation(), null);
      return Pointcut.makeMatchesNothing(Pointcut.CONCRETE);
    }
    final TypePatternList args = arguments.resolveReferences(bindings);
    if (inAspect.crosscuttingMembers != null) {
      inAspect.crosscuttingMembers.exposeTypes(args.getExactTypes());
    }
    final Pointcut ret = new ArgsPointcut(args);
    ret.copyLocationFrom(this);
    return ret;
  }

  private Test findResidueNoEllipsis(Shadow shadow, ExposedState state, TypePattern[] patterns) {
    final ResolvedType[] argumentsToMatchAgainst = getArgumentsToMatchAgainst(shadow);
    final int len = argumentsToMatchAgainst.length;
    // System.err.println("boudn to : " + len + ", " + patterns.length);
    if (patterns.length != len) {
      return Literal.FALSE;
    }

    Test ret = Literal.TRUE;

    for (int i = 0; i < len; i++) {
      final UnresolvedType argType = shadow.getGenericArgTypes()[i];
      final TypePattern type = patterns[i];
      final ResolvedType argRTX = shadow.getIWorld().resolve(argType, true);
      if (!(type instanceof BindingTypePattern)) {
        if (argRTX.isMissing()) {
          shadow.getIWorld().getLint().cantFindType.signal(new String[]{WeaverMessages.format(
              WeaverMessages.CANT_FIND_TYPE_ARG_TYPE, argType.getName())}, shadow.getSourceLocation(),
              new ISourceLocation[]{getSourceLocation()});
          // IMessage msg = new Message(
          // WeaverMessages.format(WeaverMessages.CANT_FIND_TYPE_ARG_TYPE,argType.getName()),
          // "",IMessage.ERROR,shadow.getSourceLocation(),null,new ISourceLocation[]{getSourceLocation()});
          // shadow.getIWorld().getMessageHandler().handleMessage(msg);
        }
        if (type.matchesInstanceof(argRTX).alwaysTrue()) {
          continue;
        }
      }

      final World world = shadow.getIWorld();
      final ResolvedType typeToExpose = type.getExactType().resolve(world);
      if (typeToExpose.isParameterizedType()) {
        final boolean inDoubt = (type.matchesInstanceof(argRTX) == FuzzyBoolean.MAYBE);
        if (inDoubt && world.getLint().uncheckedArgument.isEnabled()) {
          String uncheckedMatchWith = typeToExpose.getSimpleBaseName();
          if (argRTX.isParameterizedType() && (argRTX.getRawType() == typeToExpose.getRawType())) {
            uncheckedMatchWith = argRTX.getSimpleName();
          }
          if (!isUncheckedArgumentWarningSuppressed()) {
            world.getLint().uncheckedArgument.signal(new String[]{typeToExpose.getSimpleName(), uncheckedMatchWith,
                typeToExpose.getSimpleBaseName(), shadow.toResolvedString(world)}, getSourceLocation(),
                new ISourceLocation[]{shadow.getSourceLocation()});
          }
        }
      }

      ret = Test.makeAnd(ret, exposeStateForVar(shadow.getArgVar(i), type, state, shadow.getIWorld()));
    }

    return ret;
  }

  /**
   * We need to find out if someone has put the @SuppressAjWarnings{"uncheckedArgument"} annotation somewhere. That somewhere is
   * going to be an a piece of advice that uses this pointcut. But how do we find it???
   *
   * @return
   */
  private static boolean isUncheckedArgumentWarningSuppressed() {
    return false;
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    final ResolvedType[] argsToMatch = getArgumentsToMatchAgainst(shadow);
    if (arguments.matches(argsToMatch, TypePattern.DYNAMIC).alwaysFalse()) {
      return Literal.FALSE;
    }
    final int ellipsisCount = arguments.ellipsisCount;
    if (ellipsisCount == 0) {
      return findResidueNoEllipsis(shadow, state, arguments.getTypePatterns());
    } else if (ellipsisCount == 1) {
      final TypePattern[] patternsWithEllipsis = arguments.getTypePatterns();
      final TypePattern[] patternsWithoutEllipsis = new TypePattern[argsToMatch.length];
      final int lenWithEllipsis = patternsWithEllipsis.length;
      final int lenWithoutEllipsis = patternsWithoutEllipsis.length;
      // l1+1 >= l0
      int indexWithEllipsis = 0;
      int indexWithoutEllipsis = 0;
      while (indexWithoutEllipsis < lenWithoutEllipsis) {
        final TypePattern p = patternsWithEllipsis[indexWithEllipsis++];
        if (p == TypePattern.ELLIPSIS) {
          final int newLenWithoutEllipsis = lenWithoutEllipsis - (lenWithEllipsis - indexWithEllipsis);
          while (indexWithoutEllipsis < newLenWithoutEllipsis) {
            patternsWithoutEllipsis[indexWithoutEllipsis++] = TypePattern.ANY;
          }
        } else {
          patternsWithoutEllipsis[indexWithoutEllipsis++] = p;
        }
      }
      return findResidueNoEllipsis(shadow, state, patternsWithoutEllipsis);
    } else {
      throw new BCException("unimplemented");
    }
  }

  public String toString() {
    return this.stringRepresentation;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
