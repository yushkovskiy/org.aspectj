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

package org.aspectj.ajdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.AjAttribute;
import org.aspectj.weaver.patterns.WildTypePattern;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class AstUtil {

  private AstUtil() {
  }

  public static void addMethodBinding(SourceTypeBinding sourceType, MethodBinding method) {
    final int len = sourceType.methods.length;
    final MethodBinding[] temp = new MethodBinding[len + 1];
    System.arraycopy(sourceType.methods, 0, temp, 0, len);
    temp[len] = method;
    sourceType.methods = temp;
  }

  public static void addMethodDeclaration(TypeDeclaration typeDec, AbstractMethodDeclaration dec) {
    final AbstractMethodDeclaration[] methods = typeDec.methods;
    final int len = methods.length;
    final AbstractMethodDeclaration[] newMethods = new AbstractMethodDeclaration[len + 1];
    System.arraycopy(methods, 0, newMethods, 0, len);
    newMethods[len] = dec;
    typeDec.methods = newMethods;
  }

  public static Argument makeFinalArgument(char[] name, TypeBinding typeBinding) {
    final long pos = 0; // XXX encode start and end location
    final LocalVariableBinding binding = new LocalVariableBinding(name, typeBinding, Modifier.FINAL, true);
    final Argument ret = new Argument(name, pos, makeTypeReference(typeBinding), Modifier.FINAL);
    ret.binding = binding;
    return ret;
  }

  public static TypeReference makeTypeReference(TypeBinding binding) {
    // ??? does this work for primitives
    final QualifiedTypeReference ref = new QualifiedTypeReference(new char[][]{binding.sourceName()}, new long[]{0}); // ???
    ref.resolvedType = binding;
    ref.constant = Constant.NotAConstant;
    return ref;
  }

  public static NameReference makeNameReference(TypeBinding binding) {

    final char[][] name = new char[][]{binding.sourceName()};
    final long[] dummyPositions = new long[name.length];
    final QualifiedNameReference ref = new QualifiedNameReference(name, dummyPositions, 0, 0);
    ref.binding = binding;
    ref.constant = Constant.NotAConstant;
    return ref;
  }

  public static ReturnStatement makeReturnStatement(Expression expr) {
    return new ReturnStatement(expr, 0, 0);
  }

  public static MethodDeclaration makeMethodDeclaration(MethodBinding binding) {
    final MethodDeclaration ret = new MethodDeclaration(null);
    ret.binding = binding;
    final int nargs = binding.parameters.length;
    ret.arguments = new Argument[nargs];
    for (int i = 0; i < nargs; i++) {
      ret.arguments[i] = makeFinalArgument(("arg" + i).toCharArray(), binding.parameters[i]);
    }
    return ret;
  }

  public static void setStatements(MethodDeclaration ret, List statements) {
    ret.statements = (Statement[]) statements.toArray(new Statement[statements.size()]);
  }

  public static SingleNameReference makeLocalVariableReference(LocalVariableBinding binding) {
    final SingleNameReference ret = new SingleNameReference(binding.name, 0);
    ret.binding = binding;
//		ret.codegenBinding = binding;
    ret.constant = Constant.NotAConstant;
    ret.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
    ret.bits |= Binding.VARIABLE;
    return ret;
  }

  public static SingleNameReference makeResolvedLocalVariableReference(LocalVariableBinding binding) {
    final SingleNameReference ret = new SingleNameReference(binding.name, 0);
    ret.binding = binding;
//		ret.codegenBinding = binding;
    ret.constant = Constant.NotAConstant;
    ret.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
    ret.bits |= Binding.LOCAL;
    return ret;
  }

  public static int makePublic(int modifiers) {
    return makePackageVisible(modifiers) | ClassFileConstants.AccPublic;
  }

  public static int makePackageVisible(int modifiers) {
    modifiers &= ~(ClassFileConstants.AccPublic | ClassFileConstants.AccPrivate | ClassFileConstants.AccProtected);
    return modifiers;
  }

  public static CompilationUnitScope getCompilationUnitScope(Scope scope) {
    if (scope instanceof CompilationUnitScope) {
      return (CompilationUnitScope) scope;
    }
    return getCompilationUnitScope(scope.parent);
  }

  public static void generateParameterLoads(TypeBinding[] parameters, CodeStream codeStream) {
    int paramIndex = 0;
    int varIndex = 0;
    while (paramIndex < parameters.length) {
      final TypeBinding param = parameters[paramIndex++];
      codeStream.load(param, varIndex);
      varIndex += slotsNeeded(param);
    }
  }

  public static void generateParameterLoads(TypeBinding[] parameters, CodeStream codeStream, int offset) {
    int paramIndex = 0;
    int varIndex = offset;
    while (paramIndex < parameters.length) {
      final TypeBinding param = parameters[paramIndex++];
      codeStream.load(param, varIndex);
      varIndex += slotsNeeded(param);
    }
  }

  public static void generateReturn(TypeBinding returnType, CodeStream codeStream) {
    if (returnType.id == TypeIds.T_void) {
      codeStream.return_();
    } else if (returnType.isBaseType()) {
      switch (returnType.id) {
        case TypeIds.T_boolean:
        case TypeIds.T_int:
        case TypeIds.T_byte:
        case TypeIds.T_short:
        case TypeIds.T_char:
          codeStream.ireturn();
          break;
        case TypeIds.T_float:
          codeStream.freturn();
          break;
        case TypeIds.T_long:
          codeStream.lreturn();
          break;
        case TypeIds.T_double:
          codeStream.dreturn();
          break;
        default:
          throw new RuntimeException("huh");
      }
    } else {
      codeStream.areturn();
    }
  }

  // XXX this could be inconsistent for wierd case, i.e. a class named "java_lang_String"
  public static char[] makeMangledName(ReferenceBinding type) {
    return CharOperation.concatWith(type.compoundName, '_');
  }

  public static final char[] PREFIX = "ajc".toCharArray();

  // XXX not efficient
  public static char[] makeAjcMangledName(char[] kind, ReferenceBinding type, char[] name) {
    return CharOperation.concat(CharOperation.concat(PREFIX, new char[]{'$'}, kind), '$', makeMangledName(type), '$', name);
  }

  public static char[] makeAjcMangledName(char[] kind, char[] p, char[] name) {
    return CharOperation.concat(CharOperation.concat(PREFIX, new char[]{'$'}, kind), '$', p, '$', name);
  }

  public static List getAjSyntheticAttribute() {
    final ArrayList ret = new ArrayList(1);
    ret.add(new EclipseAttributeAdapter(new AjAttribute.AjSynthetic()));
    return ret;
  }

  public static long makeLongPos(int start, int end) {
    return (long) end | ((long) start << 32);
  }

  public static char[][] getCompoundName(String string) {
    return WildTypePattern.splitNames(string, true);
  }

  public static TypeBinding[] insert(TypeBinding first, TypeBinding[] rest) {
    if (rest == null) {
      return new TypeBinding[]{first};
    }

    final int len = rest.length;
    final TypeBinding[] ret = new TypeBinding[len + 1];
    ret[0] = first;
    System.arraycopy(rest, 0, ret, 1, len);
    return ret;
  }

  public static Argument[] insert(Argument first, Argument[] rest) {
    if (rest == null) {
      return new Argument[]{first};
    }

    final int len = rest.length;
    final Argument[] ret = new Argument[len + 1];
    ret[0] = first;
    System.arraycopy(rest, 0, ret, 1, len);
    return ret;
  }

  public static TypeParameter[] insert(TypeParameter first, TypeParameter[] rest) {
    if (rest == null) {
      return new TypeParameter[]{first};
    }
    final int len = rest.length;
    final TypeParameter[] ret = new TypeParameter[len + 1];
    ret[0] = first;
    System.arraycopy(rest, 0, ret, 1, len);
    return ret;
  }

  public static TypeVariableBinding[] insert(TypeVariableBinding first, TypeVariableBinding[] rest) {
    if (rest == null) {
      return new TypeVariableBinding[]{first};
    }
    final int len = rest.length;
    final TypeVariableBinding[] ret = new TypeVariableBinding[len + 1];
    ret[0] = first;
    System.arraycopy(rest, 0, ret, 1, len);
    return ret;
  }

  public static TypeVariableBinding[] insert(TypeVariableBinding[] first, TypeVariableBinding[] rest) {
    if (rest == null) {
      final TypeVariableBinding[] ret = new TypeVariableBinding[first.length];
      System.arraycopy(first, 0, ret, 0, first.length);
      return ret;
    }
    final int len = rest.length;
    final TypeVariableBinding[] ret = new TypeVariableBinding[first.length + len];
    System.arraycopy(first, 0, ret, 0, first.length);
    System.arraycopy(rest, 0, ret, first.length, len);
    return ret;
  }

  public static Expression[] insert(Expression first, Expression[] rest) {
    if (rest == null) {
      return new Expression[]{first};
    }

    final int len = rest.length;
    final Expression[] ret = new Expression[len + 1];
    ret[0] = first;
    System.arraycopy(rest, 0, ret, 1, len);
    return ret;
  }

  public static Argument[] copyArguments(Argument[] inArgs) {
    // Lets do a proper copy
    if (inArgs == null)
      return new Argument[]{};
    final Argument[] outArgs = new Argument[inArgs.length];
    for (int i = 0; i < inArgs.length; i++) {
      final Argument argument = inArgs[i];
      outArgs[i] = new Argument(argument.name, 0, argument.type, argument.modifiers);
    }
    return outArgs;

    // if (inArgs == null) return new Argument[] {};
    // int len = inArgs.length;
    // Argument[] outArgs = new Argument[len];
    // //??? we're not sure whether or not copying these is okay
    // System.arraycopy(inArgs, 0, outArgs, 0, len);
    // return outArgs;

  }

  public static Statement[] remove(int i, Statement[] statements) {
    final int len = statements.length;
    final Statement[] ret = new Statement[len - 1];
    System.arraycopy(statements, 0, ret, 0, i);
    System.arraycopy(statements, i + 1, ret, i, len - i - 1);
    return ret;
  }

  public static int slotsNeeded(TypeBinding type) {
    if (type == TypeBinding.DOUBLE || type == TypeBinding.LONG)
      return 2;
    else
      return 1;
  }

  public static void replaceMethodBinding(MessageSend send, MethodBinding newBinding) {
    send.binding =/* send.codegenBinding =*/ newBinding;
    send.setActualReceiverType(newBinding.declaringClass);

  }
}
