/* *******************************************************************
 * Copyright (c) 1999-2001 Xerox Corporation, 
 *               2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * ******************************************************************/
package org.aspectj.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulate generic signature parsing
 *
 * @author Adrian Colyer
 * @author Andy Clement
 */
public final class GenericSignature {

  /**
   * structure holding a parsed class signature
   */
  public static class ClassSignature {
    @NotNull
    public FormalTypeParameter[] formalTypeParameters = FormalTypeParameter.NONE;
    @NotNull
    public ClassTypeSignature superclassSignature;
    @NotNull
    public ClassTypeSignature[] superInterfaceSignatures = ClassTypeSignature.NONE;

    public String toString() {
      final StringBuilder ret = new StringBuilder();
      ret.append(formalTypeParameters.toString());
      ret.append(superclassSignature.toString());
      for (int i = 0; i < superInterfaceSignatures.length; i++) {
        ret.append(superInterfaceSignatures[i].toString());
      }
      return ret.toString();
    }
  }

  public static class MethodTypeSignature {
    @NotNull
    public FormalTypeParameter[] formalTypeParameters = new FormalTypeParameter[0];
    @NotNull
    public TypeSignature[] parameters = new TypeSignature[0];
    @Nullable
    public TypeSignature returnType;
    @NotNull
    public FieldTypeSignature[] throwsSignatures = new FieldTypeSignature[0];

    public MethodTypeSignature(@NotNull FormalTypeParameter[] aFormalParameterList, @NotNull TypeSignature[] aParameterList,
                               @Nullable TypeSignature aReturnType, @NotNull FieldTypeSignature[] aThrowsSignatureList) {
      this.formalTypeParameters = aFormalParameterList;
      this.parameters = aParameterList;
      this.returnType = aReturnType;
      this.throwsSignatures = aThrowsSignatureList;
    }

    public String toString() {
      final StringBuilder sb = new StringBuilder();
      if (formalTypeParameters.length > 0) {
        sb.append("<");
        for (int i = 0; i < formalTypeParameters.length; i++) {
          sb.append(formalTypeParameters[i].toString());
        }
        sb.append(">");
      }
      sb.append("(");
      for (int i = 0; i < parameters.length; i++) {
        sb.append(parameters[i].toString());
      }
      sb.append(")");
      sb.append(returnType);
      for (int i = 0; i < throwsSignatures.length; i++) {
        sb.append("^");
        sb.append(throwsSignatures[i].toString());
      }
      return sb.toString();
    }
  }

  /**
   * structure capturing a FormalTypeParameter from the Signature grammar
   */
  public static final class FormalTypeParameter {
    @NotNull
    public static final FormalTypeParameter[] NONE = new FormalTypeParameter[0];
    @Nullable
    public String identifier;
    @Nullable
    public FieldTypeSignature classBound;
    @Nullable
    public FieldTypeSignature[] interfaceBounds;

    public String toString() {
      final StringBuilder ret = new StringBuilder();
      ret.append("T");
      ret.append(identifier);
      ret.append(":");
      ret.append(classBound);
      if (interfaceBounds == null)
        return ret.toString();
      for (int i = 0; i < interfaceBounds.length; i++) {
        ret.append(":");
        ret.append(interfaceBounds[i].toString());
      }
      return ret.toString();
    }
  }

  public static abstract class TypeSignature {
    public boolean isBaseType() {
      return false;
    }
  }

  public static class BaseTypeSignature extends TypeSignature {
    @NotNull
    private final String sig;

    public BaseTypeSignature(@NotNull String aPrimitiveType) {
      sig = aPrimitiveType;
    }

    @Override
    public boolean isBaseType() {
      return true;
    }

    public String toString() {
      return sig;
    }
  }

  public static abstract class FieldTypeSignature extends TypeSignature {
    public boolean isClassTypeSignature() {
      return false;
    }

    public boolean isTypeVariableSignature() {
      return false;
    }

    public boolean isArrayTypeSignature() {
      return false;
    }
  }

  public static final class ClassTypeSignature extends FieldTypeSignature {
    @NotNull
    public static final ClassTypeSignature[] NONE = new ClassTypeSignature[0];
    @NotNull
    public String classSignature;
    @NotNull
    public SimpleClassTypeSignature outerType;
    @NotNull
    public SimpleClassTypeSignature[] nestedTypes;

    public ClassTypeSignature(@NotNull String sig, @NotNull String identifier) {
      this.classSignature = sig;
      this.outerType = new SimpleClassTypeSignature(identifier);
      this.nestedTypes = new SimpleClassTypeSignature[0];
    }

    public ClassTypeSignature(@NotNull String sig, @NotNull SimpleClassTypeSignature outer, @NotNull SimpleClassTypeSignature[] inners) {
      this.classSignature = sig;
      this.outerType = outer;
      this.nestedTypes = inners;
    }

    @Override
    public boolean isClassTypeSignature() {
      return true;
    }

    public String toString() {
      return classSignature;
    }
  }

  public static class TypeVariableSignature extends FieldTypeSignature {
    @NotNull
    public String typeVariableName;

    public TypeVariableSignature(@NotNull String typeVarToken) {
      this.typeVariableName = typeVarToken.substring(1);
    }

    @Override
    public boolean isTypeVariableSignature() {
      return true;
    }

    public String toString() {
      return "T" + typeVariableName + ";";
    }
  }

  public static class ArrayTypeSignature extends FieldTypeSignature {
    @NotNull
    public TypeSignature typeSig;

    public ArrayTypeSignature(@NotNull TypeSignature aTypeSig) {
      this.typeSig = aTypeSig;
    }

    @Override
    public boolean isArrayTypeSignature() {
      return true;
    }

    public String toString() {
      return "[" + typeSig.toString();
    }
  }

  public static class SimpleClassTypeSignature {
    @NotNull
    public String identifier;
    @NotNull
    public TypeArgument[] typeArguments;

    public SimpleClassTypeSignature(@NotNull String identifier) {
      this.identifier = identifier;
      this.typeArguments = new TypeArgument[0];
    }

    public SimpleClassTypeSignature(@NotNull String identifier, @NotNull TypeArgument[] args) {
      this.identifier = identifier;
      this.typeArguments = args;
    }

    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(identifier);
      if (typeArguments.length > 0) {
        sb.append("<");
        for (int i = 0; i < typeArguments.length; i++) {
          sb.append(typeArguments[i].toString());
        }
        sb.append(">");
      }
      return sb.toString();
    }
  }

  public static class TypeArgument {
    public boolean isWildcard = false;
    public boolean isPlus = false;
    public boolean isMinus = false;
    @Nullable
    public FieldTypeSignature signature; // null if isWildcard

    public TypeArgument() {
      isWildcard = true;
    }

    public TypeArgument(boolean plus, boolean minus, @Nullable FieldTypeSignature aSig) {
      this.isPlus = plus;
      this.isMinus = minus;
      this.signature = aSig;
    }

    public String toString() {
      if (isWildcard)
        return "*";
      final StringBuilder sb = new StringBuilder();
      if (isPlus)
        sb.append("+");
      if (isMinus)
        sb.append("-");
      sb.append(signature);
      return sb.toString();
    }
  }
}
