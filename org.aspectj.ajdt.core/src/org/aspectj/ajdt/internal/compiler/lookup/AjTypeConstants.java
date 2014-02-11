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


package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.jetbrains.annotations.NotNull;

public final class AjTypeConstants {
  @NotNull
  public static final char[] ORG = "org".toCharArray();
  @NotNull
  public static final char[] ASPECTJ = "aspectj".toCharArray();
  @NotNull
  public static final char[] RUNTIME = "runtime".toCharArray();
  @NotNull
  public static final char[] LANG = "lang".toCharArray();
  @NotNull
  public static final char[] INTERNAL = "internal".toCharArray();

  // Constant compound names
  @NotNull
  public static final char[][] ORG_ASPECTJ_LANG_JOINPOINT =
      new char[][]{ORG, ASPECTJ, LANG, "JoinPoint".toCharArray()};

  @NotNull
  public static final char[][] ORG_ASPECTJ_LANG_JOINPOINT_STATICPART =
      new char[][]{ORG, ASPECTJ, LANG, "JoinPoint".toCharArray(), "StaticPart".toCharArray()};

  @NotNull
  public static final char[][] ORG_ASPECTJ_RUNTIME_INTERNAL_AROUNDCLOSURE =
      new char[][]{ORG, ASPECTJ, RUNTIME, INTERNAL, "AroundClosure".toCharArray()};

  @NotNull
  public static final char[][] ORG_ASPECTJ_RUNTIME_INTERNAL_CONVERSIONS =
      new char[][]{ORG, ASPECTJ, RUNTIME, INTERNAL, "Conversions".toCharArray()};

  @NotNull
  public static TypeReference getJoinPointType() {
    return new QualifiedTypeReference(ORG_ASPECTJ_LANG_JOINPOINT, new long[10]);
  }

  @NotNull
  public static TypeReference getJoinPointStaticPartType() {
    return new QualifiedTypeReference(ORG_ASPECTJ_LANG_JOINPOINT_STATICPART, new long[10]);
  }

  @NotNull
  public static TypeReference getAroundClosureType() {
    return new QualifiedTypeReference(ORG_ASPECTJ_RUNTIME_INTERNAL_AROUNDCLOSURE, new long[10]);
  }

  @NotNull
  public static ReferenceBinding getConversionsType(@NotNull Scope scope) {
    return (ReferenceBinding) scope.getType(
        ORG_ASPECTJ_RUNTIME_INTERNAL_CONVERSIONS,
        ORG_ASPECTJ_RUNTIME_INTERNAL_CONVERSIONS.length);
  }

  @NotNull
  public static MethodBinding getConversionMethodToObject(@NotNull Scope scope, @NotNull TypeBinding fromType) {
    final String name = new String(fromType.sourceName()) + "Object";
    return getConversionsType(scope).getMethods(name.toCharArray())[0];
  }

  @NotNull
  public static MethodBinding getConversionMethodFromObject(@NotNull Scope scope, @NotNull TypeBinding toType) {
    final String name = new String(toType.sourceName()) + "Value";
    return getConversionsType(scope).getMethods(name.toCharArray())[0];
  }

}
