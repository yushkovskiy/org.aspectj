/* *******************************************************************
 * Copyright (c) 2006 Contributors
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andy Clement                 initial implementation
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.apache.bcel.classfile.annotation.ElementValue;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.IntConstant;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.StringConstant;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.aspectj.weaver.*;

// not yet used...
public class EclipseAnnotationConvertor {
  /**
   * Convert one eclipse annotation into an AnnotationX object containing an AnnotationAJ object.
   * <p/>
   * This code and the helper methods used by it will go *BANG* if they encounter anything not currently supported - this is safer
   * than limping along with a malformed annotation. When the *BANG* is encountered the bug reporter should indicate the kind of
   * annotation they were working with and this code can be enhanced to support it.
   */
  public static AnnotationAJ convertEclipseAnnotation(Annotation eclipseAnnotation, World w, EclipseFactory factory) {
    // TODO if it is sourcevisible, we shouldn't let it through!!!!!!!!!
    // testcase!
    final ResolvedType annotationType = factory.fromTypeBindingToRTX(eclipseAnnotation.type.resolvedType);
    // long bs = (eclipseAnnotation.bits & TagBits.AnnotationRetentionMASK);
    final boolean isRuntimeVisible = (eclipseAnnotation.bits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
    final StandardAnnotation annotationAJ = new StandardAnnotation(annotationType, isRuntimeVisible);
    generateAnnotation(eclipseAnnotation, annotationAJ);
    return annotationAJ;
  }

  static class MissingImplementationException extends RuntimeException {
    MissingImplementationException(String reason) {
      super(reason);
    }
  }

  private static void generateAnnotation(Annotation annotation, StandardAnnotation annotationAJ) {
    if (annotation instanceof NormalAnnotation) {
      final NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
      final MemberValuePair[] memberValuePairs = normalAnnotation.memberValuePairs;
      if (memberValuePairs != null) {
        final int memberValuePairsLength = memberValuePairs.length;
        for (int i = 0; i < memberValuePairsLength; i++) {
          final MemberValuePair memberValuePair = memberValuePairs[i];
          final MethodBinding methodBinding = memberValuePair.binding;
          if (methodBinding == null) {
            // is this just a marker annotation?
            throw new MissingImplementationException(
                "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation
                    + "]");
          } else {
            final AnnotationValue av = generateElementValue(memberValuePair.value, methodBinding.returnType);
            final AnnotationNameValuePair anvp = new AnnotationNameValuePair(new String(memberValuePair.name), av);
            annotationAJ.addNameValuePair(anvp);
          }
        }
      } else {
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation + "]");
      }
    } else if (annotation instanceof SingleMemberAnnotation) {
      // this is a single member annotation (one member value)
      final SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
      final MethodBinding methodBinding = singleMemberAnnotation.memberValuePairs()[0].binding;
      if (methodBinding == null) {
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation + "]");
      } else {
        final AnnotationValue av = generateElementValue(singleMemberAnnotation.memberValue, methodBinding.returnType);
        annotationAJ.addNameValuePair(new AnnotationNameValuePair(new String(
            singleMemberAnnotation.memberValuePairs()[0].name), av));
      }
    } else if (annotation instanceof MarkerAnnotation) {
      final MarkerAnnotation markerAnnotation = (MarkerAnnotation) annotation;
    } else {
      // this is a marker annotation (no member value pairs)
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation [" + annotation + "]");
    }
  }

  private static AnnotationValue generateElementValue(Expression defaultValue, TypeBinding memberValuePairReturnType) {
    final Constant constant = defaultValue.constant;
    final TypeBinding defaultValueBinding = defaultValue.resolvedType;
    if (defaultValueBinding == null) {
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
              + "]");
    } else {
      if (memberValuePairReturnType.isArrayType() && !defaultValueBinding.isArrayType()) {
        if (constant != null && constant != Constant.NotAConstant) {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
          // generateElementValue(attributeOffset, defaultValue,
          // constant, memberValuePairReturnType.leafComponentType());
        } else {
          final AnnotationValue av = generateElementValueForNonConstantExpression(defaultValue, defaultValueBinding);
          return new ArrayAnnotationValue(new AnnotationValue[]{av});
        }
      } else {
        if (constant != null && constant != Constant.NotAConstant) {
          if (constant instanceof IntConstant || constant instanceof BooleanConstant
              || constant instanceof StringConstant) {
            final AnnotationValue av = generateElementValueForConstantExpression(defaultValue, defaultValueBinding);
            return av;
          }
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
          // generateElementValue(attributeOffset, defaultValue,
          // constant, memberValuePairReturnType.leafComponentType());
        } else {
          final AnnotationValue av = generateElementValueForNonConstantExpression(defaultValue, defaultValueBinding);
          return av;
        }
      }
    }
  }

  public static AnnotationValue generateElementValueForConstantExpression(Expression defaultValue, TypeBinding defaultValueBinding) {
    if (defaultValueBinding != null) {
      final Constant c = defaultValue.constant;
      if (c instanceof IntConstant) {
        final IntConstant iConstant = (IntConstant) c;
        return new SimpleAnnotationValue(ElementValue.PRIMITIVE_INT, new Integer(iConstant.intValue()));
      } else if (c instanceof BooleanConstant) {
        final BooleanConstant iConstant = (BooleanConstant) c;
        return new SimpleAnnotationValue(ElementValue.PRIMITIVE_BOOLEAN, new Boolean(iConstant.booleanValue()));
      } else if (c instanceof StringConstant) {
        final StringConstant sConstant = (StringConstant) c;
        return new SimpleAnnotationValue(ElementValue.STRING, sConstant.stringValue());
      }
    }
    return null;
  }

  private static AnnotationValue generateElementValueForNonConstantExpression(Expression defaultValue,
                                                                              TypeBinding defaultValueBinding) {
    if (defaultValueBinding != null) {
      if (defaultValueBinding.isEnum()) {
        FieldBinding fieldBinding = null;
        if (defaultValue instanceof QualifiedNameReference) {
          final QualifiedNameReference nameReference = (QualifiedNameReference) defaultValue;
          fieldBinding = (FieldBinding) nameReference.binding;
        } else if (defaultValue instanceof SingleNameReference) {
          final SingleNameReference nameReference = (SingleNameReference) defaultValue;
          fieldBinding = (FieldBinding) nameReference.binding;
        } else {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
        }
        if (fieldBinding != null) {
          final String sig = new String(fieldBinding.type.signature());
          final AnnotationValue enumValue = new EnumAnnotationValue(sig, new String(fieldBinding.name));
          return enumValue;
        }
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
                + "]");
      } else if (defaultValueBinding.isAnnotationType()) {
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
                + "]");
        // contents[contentsOffset++] = (byte) '@';
        // generateAnnotation((Annotation) defaultValue,
        // attributeOffset);
      } else if (defaultValueBinding.isArrayType()) {
        // array type
        if (defaultValue instanceof ArrayInitializer) {
          final ArrayInitializer arrayInitializer = (ArrayInitializer) defaultValue;
          final int arrayLength = arrayInitializer.expressions != null ? arrayInitializer.expressions.length : 0;
          final AnnotationValue[] values = new AnnotationValue[arrayLength];
          for (int i = 0; i < arrayLength; i++) {
            values[i] = generateElementValue(arrayInitializer.expressions[i], defaultValueBinding.leafComponentType());// ,
            // attributeOffset
            // )
            // ;
          }
          final ArrayAnnotationValue aav = new ArrayAnnotationValue(values);
          return aav;
        } else {
          throw new MissingImplementationException(
              "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value ["
                  + defaultValue + "]");
        }
      } else {
        // class type
        throw new MissingImplementationException(
            "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
                + "]");
        // if (contentsOffset + 3 >= this.contents.length) {
        // resizeContents(3);
        // }
        // contents[contentsOffset++] = (byte) 'c';
        // if (defaultValue instanceof ClassLiteralAccess) {
        // ClassLiteralAccess classLiteralAccess = (ClassLiteralAccess)
        // defaultValue;
        // final int classInfoIndex =
        // constantPool.literalIndex(classLiteralAccess
        // .targetType.signature());
        // contents[contentsOffset++] = (byte) (classInfoIndex >> 8);
        // contents[contentsOffset++] = (byte) classInfoIndex;
        // } else {
        // contentsOffset = attributeOffset;
        // }
      }
    } else {
      throw new MissingImplementationException(
          "Please raise an AspectJ bug.  AspectJ does not know how to convert this annotation value [" + defaultValue
              + "]");
      // contentsOffset = attributeOffset;
    }
  }

}
