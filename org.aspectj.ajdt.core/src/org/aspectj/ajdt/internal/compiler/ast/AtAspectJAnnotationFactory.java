/* *******************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *   Adrian Colyer			Initial implementation
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.jetbrains.annotations.NotNull;

/**
 * @author colyer
 *         Creates @AspectJ annotations for use by AtAspectJVisitor
 */
public final class AtAspectJAnnotationFactory {

  @NotNull
  static final char[] org = "org".toCharArray();
  @NotNull
  static final char[] aspectj = "aspectj".toCharArray();
  @NotNull
  static final char[] lang = "lang".toCharArray();
  @NotNull
  static final char[] internal = "internal".toCharArray();
  @NotNull
  static final char[] annotation = "annotation".toCharArray();
  @NotNull
  static final char[] value = "value".toCharArray();

  @NotNull
  static final char[] aspect = "Aspect".toCharArray();
  @NotNull
  static final char[] privileged = "ajcPrivileged".toCharArray();
  @NotNull
  static final char[] before = "Before".toCharArray();
  @NotNull
  static final char[] after = "After".toCharArray();
  @NotNull
  static final char[] afterReturning = "AfterReturning".toCharArray();
  @NotNull
  static final char[] afterThrowing = "AfterThrowing".toCharArray();
  @NotNull
  static final char[] around = "Around".toCharArray();
  @NotNull
  static final char[] pointcut = "Pointcut".toCharArray();
  @NotNull
  static final char[] declareErrorOrWarning = "ajcDeclareEoW".toCharArray();
  @NotNull
  static final char[] declareParents = "ajcDeclareParents".toCharArray();
  @NotNull
  static final char[] declareSoft = "ajcDeclareSoft".toCharArray();
  @NotNull
  static final char[] declarePrecedence = "ajcDeclarePrecedence".toCharArray();
  @NotNull
  static final char[] declareAnnotation = "ajcDeclareAnnotation".toCharArray();
  @NotNull
  static final char[] itdAnnotation = "ajcITD".toCharArray();

  /**
   * Create an @Aspect annotation for a code style aspect declaration starting at
   * the given position in the source file
   */
  @NotNull
  public static Annotation createAspectAnnotation(@NotNull String perclause, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, aspect};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference orgAspectJLangAnnotationAspect = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation atAspectAnnotation = new NormalAnnotation(orgAspectJLangAnnotationAspect, pos);
    if (!perclause.equals("")) {
      // we have to set the value
      final Expression perclauseExpr = new StringLiteral(perclause.toCharArray(), pos, pos, 1);
      final MemberValuePair[] mvps = new MemberValuePair[1];
      mvps[0] = new MemberValuePair(value, pos, pos, perclauseExpr);
      atAspectAnnotation.memberValuePairs = mvps;
    }
    return atAspectAnnotation;
  }

  @NotNull
  public static Annotation createPrivilegedAnnotation(int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, privileged};
    final long[] positions = new long[]{pos, pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    return ann;
  }

  @NotNull
  public static Annotation createBeforeAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, before};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[2];
    mvps[0] = new MemberValuePair("value".toCharArray(), pos, pos, pcExpr);
    final Expression argNamesExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("argNames".toCharArray(), pos, pos, argNamesExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createAfterAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, after};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[2];
    mvps[0] = new MemberValuePair("value".toCharArray(), pos, pos, pcExpr);
    final Expression argNamesExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("argNames".toCharArray(), pos, pos, argNamesExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createAfterReturningAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, @NotNull String extraArgumentName, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, afterReturning};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("pointcut".toCharArray(), pos, pos, pcExpr);
    final Expression argExpr = new StringLiteral(extraArgumentName.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("returning".toCharArray(), pos, pos, argExpr);
    final Expression argNamesExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[2] = new MemberValuePair("argNames".toCharArray(), pos, pos, argNamesExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createAfterThrowingAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, @NotNull String extraArgumentName, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, afterThrowing};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("pointcut".toCharArray(), pos, pos, pcExpr);
    final Expression argExpr = new StringLiteral(extraArgumentName.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("throwing".toCharArray(), pos, pos, argExpr);
    final Expression argNamesExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[2] = new MemberValuePair("argNames".toCharArray(), pos, pos, argNamesExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createAroundAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, around};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[2];
    mvps[0] = new MemberValuePair("value".toCharArray(), pos, pos, pcExpr);
    final Expression argNamesExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("argNames".toCharArray(), pos, pos, argNamesExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createPointcutAnnotation(@NotNull String pointcutExpression, @NotNull String argNames, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, lang, annotation, pointcut};
    final long[] positions = new long[]{pos, pos, pos, pos, pos};
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[2];
    mvps[0] = new MemberValuePair("value".toCharArray(), pos, pos, pcExpr);
    final Expression argExpr = new StringLiteral(argNames.toCharArray(), pos, pos, 1);
    mvps[1] = new MemberValuePair("argNames".toCharArray(), pos, pos, argExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createDeclareErrorOrWarningAnnotation(@NotNull String pointcutExpression, @NotNull String message, boolean isError, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, declareErrorOrWarning};
    final long[] positions = new long[typeName.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcutExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final Expression msgExpr = new StringLiteral(message.toCharArray(), pos, pos, 1);
    final Expression isErrorExpr;
    if (isError) {
      isErrorExpr = new TrueLiteral(pos, pos);
    } else {
      isErrorExpr = new FalseLiteral(pos, pos);
    }
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("pointcut".toCharArray(), pos, pos, pcutExpr);
    mvps[1] = new MemberValuePair("message".toCharArray(), pos, pos, msgExpr);
    mvps[2] = new MemberValuePair("isError".toCharArray(), pos, pos, isErrorExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createDeclareParentsAnnotation(@NotNull String childPattern, @NotNull String parentPatterns, boolean isExtends, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, declareParents};
    final long[] positions = new long[typeName.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression targetExpression = new StringLiteral(childPattern.toCharArray(), pos, pos, 1);
    final Expression parentsExpression = new StringLiteral(parentPatterns.toCharArray(), pos, pos, 1);
    final Expression isExtendsExpression;
    if (isExtends) {
      isExtendsExpression = new TrueLiteral(pos, pos);
    } else {
      isExtendsExpression = new FalseLiteral(pos, pos);
    }
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("targetTypePattern".toCharArray(), pos, pos, targetExpression);
    mvps[1] = new MemberValuePair("parentTypes".toCharArray(), pos, pos, parentsExpression);
    mvps[2] = new MemberValuePair("isExtends".toCharArray(), pos, pos, isExtendsExpression);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createDeclareSoftAnnotation(@NotNull String pointcutExpression, @NotNull String exceptionType, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, declareSoft};
    final long[] positions = new long[typeName.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pcutExpr = new StringLiteral(pointcutExpression.toCharArray(), pos, pos, 1);
    final Expression exExpr = new StringLiteral(exceptionType.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[2];
    mvps[0] = new MemberValuePair("pointcut".toCharArray(), pos, pos, pcutExpr);
    mvps[1] = new MemberValuePair("exceptionType".toCharArray(), pos, pos, exExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createDeclareAnnAnnotation(@NotNull String patternString, @NotNull String annString, @NotNull String kind, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, declareAnnotation};
    final long[] positions = new long[typeName.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression pattExpr = new StringLiteral(patternString.toCharArray(), pos, pos, 1);
    final Expression annExpr = new StringLiteral(annString.toCharArray(), pos, pos, 1);
    final Expression kindExpr = new StringLiteral(kind.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("pattern".toCharArray(), pos, pos, pattExpr);
    mvps[1] = new MemberValuePair("annotation".toCharArray(), pos, pos, annExpr);
    mvps[2] = new MemberValuePair("kind".toCharArray(), pos, pos, kindExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createITDAnnotation(@NotNull char[] targetTypeName, int modifiers, @NotNull char[] name, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, itdAnnotation};
    final long[] positions = new long[typeName.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(typeName, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression targetExpr = new StringLiteral(targetTypeName, pos, pos, 1);
    final Expression nameExpr = new StringLiteral(name, pos, pos, 1);
    final Expression modsExpr = IntLiteral.buildIntLiteral(Integer.toString(modifiers).toCharArray(), pos, pos);
    final MemberValuePair[] mvps = new MemberValuePair[3];
    mvps[0] = new MemberValuePair("targetType".toCharArray(), pos, pos, targetExpr);
    mvps[1] = new MemberValuePair("name".toCharArray(), pos, pos, nameExpr);
    mvps[2] = new MemberValuePair("modifiers".toCharArray(), pos, pos, modsExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

  @NotNull
  public static Annotation createDeclarePrecedenceAnnotation(@NotNull String pointcutExpression, int pos) {
    final char[][] typeName = new char[][]{org, aspectj, internal, lang, annotation, declarePrecedence};
    return makeSingleStringMemberAnnotation(typeName, pos, pointcutExpression);

  }

  public static void addAnnotation(@NotNull AjMethodDeclaration decl, @NotNull Annotation annotation, @NotNull BlockScope scope) {
    if (decl.annotations == null) {
      decl.annotations = new Annotation[]{annotation};
    } else {
      final Annotation[] old = decl.annotations;
      decl.annotations = new Annotation[old.length + 1];
      System.arraycopy(old, 0, decl.annotations, 0, old.length);
      decl.annotations[old.length] = annotation;
    }
    if (decl.binding != null) {
      if ((decl.binding.tagBits & TagBits.AnnotationResolved) != 0) {
        annotation.resolve(scope);
      }
    }
  }

  @NotNull
  private static Annotation makeSingleStringMemberAnnotation(@NotNull char[][] name, int pos, @NotNull String annValue) {
    final long[] positions = new long[name.length];
    for (int i = 0; i < positions.length; i++) positions[i] = pos;
    final TypeReference annType = new QualifiedTypeReference(name, positions);
    final NormalAnnotation ann = new NormalAnnotation(annType, pos);
    final Expression valueExpr = new StringLiteral(annValue.toCharArray(), pos, pos, 1);
    final MemberValuePair[] mvps = new MemberValuePair[1];
    mvps[0] = new MemberValuePair(value, pos, pos, valueExpr);
    ann.memberValuePairs = mvps;
    return ann;
  }

}
