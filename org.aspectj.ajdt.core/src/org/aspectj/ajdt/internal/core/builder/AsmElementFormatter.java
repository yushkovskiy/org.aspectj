/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     PARC                     initial implementation
 *     Alexandre Vasseur        support for @AJ style
 * ******************************************************************/

package org.aspectj.ajdt.internal.core.builder;

import org.aspectj.ajdt.internal.compiler.ast.*;
import org.aspectj.ajdt.internal.compiler.lookup.AjLookupEnvironment;
import org.aspectj.asm.IProgramElement;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.aspectj.weaver.AdviceKind;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.model.AsmRelationshipUtils;
import org.aspectj.weaver.patterns.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mik Kersten
 */
public class AsmElementFormatter {

  public void genLabelAndKind(MethodDeclaration methodDeclaration, IProgramElement node) {

    if (methodDeclaration instanceof AdviceDeclaration) {
      final AdviceDeclaration ad = (AdviceDeclaration) methodDeclaration;
      node.setKind(IProgramElement.Kind.ADVICE);

      if (ad.kind == AdviceKind.Around) {
        node.setCorrespondingType(ad.returnType.toString()); // returnTypeToString(0));
      }

      final StringBuilder details = new StringBuilder();
      if (ad.pointcutDesignator != null) {
        details.append(AsmRelationshipUtils.genPointcutDetails(ad.pointcutDesignator.getPointcut()));
      } else {
        details.append(AsmRelationshipUtils.POINTCUT_ABSTRACT);
      }
      node.setName(ad.kind.toString());
      // if (details.length()!=0)
      node.setDetails(details.toString());
      setParameters(methodDeclaration, node);

    } else if (methodDeclaration instanceof PointcutDeclaration) {
      // PointcutDeclaration pd = (PointcutDeclaration) methodDeclaration;
      node.setKind(IProgramElement.Kind.POINTCUT);
      node.setName(translatePointcutName(new String(methodDeclaration.selector)));
      setParameters(methodDeclaration, node);

    } else if (methodDeclaration instanceof DeclareDeclaration) {
      final DeclareDeclaration declare = (DeclareDeclaration) methodDeclaration;
      String name = AsmRelationshipUtils.DEC_LABEL + " ";
      if (declare.declareDecl instanceof DeclareErrorOrWarning) {
        final DeclareErrorOrWarning deow = (DeclareErrorOrWarning) declare.declareDecl;

        if (deow.isError()) {
          node.setKind(IProgramElement.Kind.DECLARE_ERROR);
          name += AsmRelationshipUtils.DECLARE_ERROR;
        } else {
          node.setKind(IProgramElement.Kind.DECLARE_WARNING);
          name += AsmRelationshipUtils.DECLARE_WARNING;
        }
        node.setName(name);
        node.setDetails("\"" + AsmRelationshipUtils.genDeclareMessage(deow.getMessage()) + "\"");

      } else if (declare.declareDecl instanceof DeclareParents) {

        node.setKind(IProgramElement.Kind.DECLARE_PARENTS);
        final DeclareParents dp = (DeclareParents) declare.declareDecl;
        node.setName(name + AsmRelationshipUtils.DECLARE_PARENTS);

        String kindOfDP = null;
        final StringBuilder details = new StringBuilder("");
        final TypePattern[] newParents = dp.getParents().getTypePatterns();
        for (int i = 0; i < newParents.length; i++) {
          final TypePattern tp = newParents[i];
          final UnresolvedType tx = tp.getExactType();
          if (kindOfDP == null) {
            kindOfDP = "implements ";
            try {
              final ResolvedType rtx = tx.resolve(((AjLookupEnvironment) declare.scope.environment()).factory.getWorld());
              if (!rtx.isInterface()) {
                kindOfDP = "extends ";
              }
            } catch (Throwable t) {
              // What can go wrong???? who knows!
            }

          }
          String typename = tp.toString();
          if (typename.lastIndexOf(".") != -1) {
            typename = typename.substring(typename.lastIndexOf(".") + 1);
          }
          details.append(typename);
          if ((i + 1) < newParents.length) {
            details.append(",");
          }
        }
        node.setDetails(kindOfDP + details.toString());

      } else if (declare.declareDecl instanceof DeclareSoft) {
        node.setKind(IProgramElement.Kind.DECLARE_SOFT);
        final DeclareSoft ds = (DeclareSoft) declare.declareDecl;
        node.setName(name + AsmRelationshipUtils.DECLARE_SOFT);
        node.setDetails(genTypePatternLabel(ds.getException()));

      } else if (declare.declareDecl instanceof DeclarePrecedence) {
        node.setKind(IProgramElement.Kind.DECLARE_PRECEDENCE);
        final DeclarePrecedence ds = (DeclarePrecedence) declare.declareDecl;
        node.setName(name + AsmRelationshipUtils.DECLARE_PRECEDENCE);
        node.setDetails(genPrecedenceListLabel(ds.getPatterns()));

      } else if (declare.declareDecl instanceof DeclareAnnotation) {
        final DeclareAnnotation deca = (DeclareAnnotation) declare.declareDecl;
        final String thekind = deca.getKind().toString();
        node.setName(name + "@" + thekind.substring(3));

        if (deca.getKind() == DeclareAnnotation.AT_CONSTRUCTOR) {
          node.setKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_CONSTRUCTOR);
        } else if (deca.getKind() == DeclareAnnotation.AT_FIELD) {
          node.setKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_FIELD);
        } else if (deca.getKind() == DeclareAnnotation.AT_METHOD) {
          node.setKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_METHOD);
        } else if (deca.getKind() == DeclareAnnotation.AT_TYPE) {
          node.setKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_TYPE);
        }
        node.setDetails(genDecaLabel(deca));

      } else {
        node.setKind(IProgramElement.Kind.ERROR);
        node.setName(AsmRelationshipUtils.DECLARE_UNKNONWN);
      }

    } else if (methodDeclaration instanceof InterTypeDeclaration) {
      final InterTypeDeclaration itd = (InterTypeDeclaration) methodDeclaration;
      String fqname = itd.getOnType().toString();
      if (fqname.indexOf(".") != -1) {
        // TODO the string handling round here is embarrassing
        node.addFullyQualifiedName(fqname + "." + new String(itd.getDeclaredSelector()));
        fqname = fqname.substring(fqname.lastIndexOf(".") + 1);
      }
      final String name = fqname + "." + new String(itd.getDeclaredSelector());
      if (methodDeclaration instanceof InterTypeFieldDeclaration) {
        node.setKind(IProgramElement.Kind.INTER_TYPE_FIELD);
        node.setName(name);
      } else if (methodDeclaration instanceof InterTypeMethodDeclaration) {
        node.setKind(IProgramElement.Kind.INTER_TYPE_METHOD);
        node.setName(name);
      } else if (methodDeclaration instanceof InterTypeConstructorDeclaration) {
        node.setKind(IProgramElement.Kind.INTER_TYPE_CONSTRUCTOR);

        // StringBuffer argumentsSignature = new StringBuffer("fubar");
        // argumentsSignature.append("(");
        // if (methodDeclaration.arguments!=null && methodDeclaration.arguments.length>1) {
        //
        // for (int i = 1;i<methodDeclaration.arguments.length;i++) {
        // argumentsSignature.append(methodDeclaration.arguments[i]);
        // if (i+1<methodDeclaration.arguments.length) argumentsSignature.append(",");
        // }
        // }
        // argumentsSignature.append(")");
        // InterTypeConstructorDeclaration itcd = (InterTypeConstructorDeclaration)methodDeclaration;
        node.setName(itd.getOnType().toString() + "." + itd.getOnType().toString().replace('.', '_'));
      } else {
        node.setKind(IProgramElement.Kind.ERROR);
        node.setName(name);
      }
      node.setCorrespondingType(new String(itd.returnType.resolvedType.readableName()));
      if (node.getKind() != IProgramElement.Kind.INTER_TYPE_FIELD) {
        setParameters(methodDeclaration, node);
      }
    } else {
      if (methodDeclaration.isConstructor()) {
        node.setKind(IProgramElement.Kind.CONSTRUCTOR);
      } else {
        node.setKind(IProgramElement.Kind.METHOD);

        // TODO AV - could speed up if we could dig only for @Aspect declaring types (or aspect if mixed style allowed)
        // ??? how to : node.getParent().getKind().equals(IProgramElement.Kind.ASPECT)) {
        if (true && methodDeclaration != null && methodDeclaration.annotations != null && methodDeclaration.scope != null) {
          for (int i = 0; i < methodDeclaration.annotations.length; i++) {
            // Note: AV: implicit single advice type support here (should be enforced somewhere as well (APT etc))
            final Annotation annotation = methodDeclaration.annotations[i];
            final String annotationSig = new String(annotation.type.getTypeBindingPublic(methodDeclaration.scope).signature());
            if (annotationSig.charAt(1) == 'o') {
              if ("Lorg/aspectj/lang/annotation/Pointcut;".equals(annotationSig)) {
                node.setKind(IProgramElement.Kind.POINTCUT);
                node.setAnnotationStyleDeclaration(true); // pointcuts don't seem to get handled quite right...
                break;
              } else if ("Lorg/aspectj/lang/annotation/Before;".equals(annotationSig)
                  || "Lorg/aspectj/lang/annotation/After;".equals(annotationSig)
                  || "Lorg/aspectj/lang/annotation/AfterReturning;".equals(annotationSig)
                  || "Lorg/aspectj/lang/annotation/AfterThrowing;".equals(annotationSig)
                  || "Lorg/aspectj/lang/annotation/Around;".equals(annotationSig)) {
                node.setKind(IProgramElement.Kind.ADVICE);
                node.setAnnotationStyleDeclaration(true);
                // TODO AV - all are considered anonymous - is that ok?
                node.setDetails(AsmRelationshipUtils.POINTCUT_ANONYMOUS);
                break;
              }
            }
          }
        }
      }
      node.setName(new String(methodDeclaration.selector));
      setParameters(methodDeclaration, node);
    }
  }

  private static String genDecaLabel(DeclareAnnotation deca) {
    final StringBuilder sb = new StringBuilder("");
    sb.append(deca.getPatternAsString());
    sb.append(" : ");
    sb.append(deca.getAnnotationString());
    return sb.toString();
  }

  private String genPrecedenceListLabel(TypePatternList list) {
    String tpList = "";
    for (int i = 0; i < list.size(); i++) {
      tpList += genTypePatternLabel(list.get(i));
      if (i < list.size() - 1) {
        tpList += ", ";
      }
    }
    return tpList;
  }

  // private String genArguments(MethodDeclaration md) {
  // String args = "";
  // Argument[] argArray = md.arguments;
  // if (argArray == null) return args;
  // for (int i = 0; i < argArray.length; i++) {
  // String argName = new String(argArray[i].name);
  // String argType = argArray[i].type.toString();
  // if (acceptArgument(argName, argType)) {
  // args += argType + ", ";
  // }
  // }
  // int lastSepIndex = args.lastIndexOf(',');
  // if (lastSepIndex != -1 && args.endsWith(", ")) args = args.substring(0, lastSepIndex);
  // return args;
  // }

  private String handleSigForReference(TypeReference ref, TypeBinding tb, MethodScope scope) {
    try {
      final StringBuffer sb = new StringBuffer();
      createHandleSigForReference(ref, tb, scope, sb);
      return sb.toString();
    } catch (Throwable t) {
      System.err.println("Problem creating handle sig for this type reference " + ref);
      t.printStackTrace(System.err);
      return null;
    }
  }

  /**
   * Aim of this method is create the signature for a parameter that can be used in a handle such that JDT can interpret the
   * handle. Whether a type is qualified or unqualified in its source reference is actually reflected in the handle and this code
   * allows for that.
   */
  private static void createHandleSigForReference(TypeReference ref, TypeBinding tb, MethodScope scope, StringBuffer handleSig) {
    if (ref instanceof Wildcard) {
      final Wildcard w = (Wildcard) ref;
      if (w.bound == null) {
        handleSig.append('*');
      } else {
        handleSig.append('+');
        TypeBinding typeB = w.bound.resolvedType;
        if (typeB == null) {
          typeB = w.bound.resolveType(scope);
        }
        createHandleSigForReference(w.bound, typeB, scope, handleSig);
      }
    } else if (ref instanceof ParameterizedSingleTypeReference) {
      final ParameterizedSingleTypeReference pstr = (ParameterizedSingleTypeReference) ref;
      for (int i = pstr.dimensions(); i > 0; i--) {
        handleSig.append("\\[");
      }
      handleSig.append('Q').append(pstr.token);
      final TypeReference[] typeRefs = pstr.typeArguments;
      if (typeRefs != null && typeRefs.length > 0) {
        handleSig.append("\\<");
        for (int i = 0; i < typeRefs.length; i++) {
          final TypeReference typeR = typeRefs[i];
          TypeBinding typeB = typeR.resolvedType;
          if (typeB == null) {
            typeB = typeR.resolveType(scope);
          }
          createHandleSigForReference(typeR, typeB, scope, handleSig);
        }
        handleSig.append('>');
      }
      handleSig.append(';');
    } else if (ref instanceof ArrayTypeReference) {
      final ArrayTypeReference atr = (ArrayTypeReference) ref;
      for (int i = 0; i < atr.dimensions; i++) {
        handleSig.append("\\[");
      }
      TypeBinding typeB = atr.resolvedType;
      if (typeB == null) {
        typeB = atr.resolveType(scope);
      }
      if (typeB.leafComponentType().isBaseType()) {
        handleSig.append(tb.leafComponentType().signature());
      } else {
        handleSig.append('Q').append(atr.token).append(';');
      }
    } else if (ref instanceof SingleTypeReference) {
      final SingleTypeReference str = (SingleTypeReference) ref;
      if (tb.isBaseType()) {
        handleSig.append(tb.signature());
      } else {
        handleSig.append('Q').append(str.token).append(';');
      }
    } else if (ref instanceof ParameterizedQualifiedTypeReference) {
      final ParameterizedQualifiedTypeReference pstr = (ParameterizedQualifiedTypeReference) ref;
      final char[][] tokens = pstr.tokens;
      for (int i = pstr.dimensions(); i > 0; i--) {
        handleSig.append("\\[");
      }
      handleSig.append('Q');
      for (int i = 0; i < tokens.length; i++) {
        if (i > 0) {
          handleSig.append('.');
        }
        handleSig.append(tokens[i]);
        final TypeReference[] typeRefs = pstr.typeArguments[i];
        if (typeRefs != null && typeRefs.length > 0) {
          handleSig.append("\\<");
          for (int j = 0; j < typeRefs.length; j++) {
            final TypeReference typeR = typeRefs[j];
            TypeBinding typeB = typeR.resolvedType;
            if (typeB == null) {
              typeB = typeR.resolveType(scope);
            }
            createHandleSigForReference(typeR, typeB, scope, handleSig);
          }
          handleSig.append('>');
        }
      }
      handleSig.append(';');
    } else if (ref instanceof ArrayQualifiedTypeReference) {
      final ArrayQualifiedTypeReference atr = (ArrayQualifiedTypeReference) ref;
      for (int i = 0; i < atr.dimensions(); i++) {
        handleSig.append("\\[");
      }
      TypeBinding typeB = atr.resolvedType;
      if (typeB == null) {
        typeB = atr.resolveType(scope);
      }
      if (typeB.leafComponentType().isBaseType()) {
        handleSig.append(tb.leafComponentType().signature());
      } else {
        final char[][] tokens = atr.tokens;
        handleSig.append('Q');
        for (int i = 0; i < tokens.length; i++) {
          if (i > 0) {
            handleSig.append('.');
          }
          handleSig.append(tokens[i]);
        }
        handleSig.append(';');
      }
    } else if (ref instanceof QualifiedTypeReference) {
      final QualifiedTypeReference qtr = (QualifiedTypeReference) ref;
      final char[][] tokens = qtr.tokens;
      handleSig.append('Q');
      for (int i = 0; i < tokens.length; i++) {
        if (i > 0) {
          handleSig.append('.');
        }
        handleSig.append(tokens[i]);
      }
      handleSig.append(';');
    } else {
      throw new RuntimeException("Cant handle " + ref.getClass());
    }
  }

  public void setParameters(AbstractMethodDeclaration md, IProgramElement pe) {
    final Argument[] argArray = md.arguments;
    if (argArray == null) {
      pe.setParameterNames(Collections.EMPTY_LIST);
      pe.setParameterSignatures(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    } else {
      final List<String> names = new ArrayList<String>();
      final List<char[]> paramSigs = new ArrayList<char[]>();
      final List<String> paramSourceRefs = new ArrayList<String>();
      boolean problemWithSourceRefs = false;
      for (int i = 0; i < argArray.length; i++) {
        final String argName = new String(argArray[i].name);
        // String argType = "<UnknownType>"; // pr135052
        if (acceptArgument(argName, argArray[i].type.toString())) {
          final TypeReference typeR = argArray[i].type;
          if (typeR != null && md.scope != null) {
            TypeBinding typeB = typeR.resolvedType;
            if (typeB == null) {
              typeB = typeR.resolveType(md.scope);
            }
            // This code will conjure up a 'P' style signature:
            // EclipseFactory factory = EclipseFactory.fromScopeLookupEnvironment(md.scope);
            // UnresolvedType ut = factory.fromBinding(typeB);
            // paramSigs.add(ut.getSignature().toCharArray());
            paramSigs.add(typeB.genericTypeSignature());
            final String hsig = handleSigForReference(typeR, typeB, md.scope);
            if (hsig == null) {
              problemWithSourceRefs = true;
            } else {
              paramSourceRefs.add(hsig);
            }
          }
          names.add(argName);
        }
      }
      pe.setParameterNames(names);
      if (!paramSigs.isEmpty()) {
        pe.setParameterSignatures(paramSigs, (problemWithSourceRefs ? null : paramSourceRefs));
      }
    }
  }

  // TODO: fix this way of determing ajc-added arguments, make subtype of Argument with extra info
  private static boolean acceptArgument(String name, String type) {
    if (name.charAt(0) != 'a' && type.charAt(0) != 'o') {
      return true;
    }
    return !name.startsWith("ajc$this_") && !type.equals("org.aspectj.lang.JoinPoint.StaticPart")
        && !type.equals("org.aspectj.lang.JoinPoint") && !type.equals("org.aspectj.runtime.internal.AroundClosure");
  }

  public static String genTypePatternLabel(TypePattern tp) {
    final String TYPE_PATTERN_LITERAL = "<type pattern>";
    String label;
    final UnresolvedType typeX = tp.getExactType();

    if (!ResolvedType.isMissing(typeX)) {
      label = typeX.getName();
      if (tp.isIncludeSubtypes()) {
        label += "+";
      }
    } else {
      label = TYPE_PATTERN_LITERAL;
    }
    return label;

  }

  // // TODO:
  // private String translateAdviceName(String label) {
  // if (label.indexOf("before") != -1) return "before";
  // if (label.indexOf("returning") != -1) return "after returning";
  // if (label.indexOf("after") != -1) return "after";
  // if (label.indexOf("around") != -1) return "around";
  // else return "<advice>";
  // }

  // // !!! move or replace
  // private String translateDeclareName(String name) {
  // int colonIndex = name.indexOf(":");
  // if (colonIndex != -1) {
  // return name.substring(0, colonIndex);
  // } else {
  // return name;
  // }
  // }

  // !!! move or replace
  // private String translateInterTypeDecName(String name) {
  // int index = name.lastIndexOf('$');
  // if (index != -1) {
  // return name.substring(index+1);
  // } else {
  // return name;
  // }
  // }

  // !!! move or replace
  private static String translatePointcutName(String name) {
    final int index = name.indexOf("$$") + 2;
    final int endIndex = name.lastIndexOf('$');
    if (index != -1 && endIndex != -1) {
      return name.substring(index, endIndex);
    } else {
      return name;
    }
  }

}
