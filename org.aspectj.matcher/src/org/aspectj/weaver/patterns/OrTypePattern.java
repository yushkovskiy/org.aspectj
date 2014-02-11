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

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;

import java.io.IOException;
import java.util.Map;

/**
 * left || right
 * <p/>
 * <p/>
 * any binding to formals is explicitly forbidden for any composite by the language
 *
 * @author Erik Hilsdale
 * @author Jim Hugunin
 */
public class OrTypePattern extends TypePattern {
  private TypePattern left, right;

  public OrTypePattern(TypePattern left, TypePattern right) {
    super(false, false); // ??? we override all methods that care about includeSubtypes
    this.left = left;
    this.right = right;
    setLocation(left.getSourceContext(), left.getStart(), right.getEnd());
  }

  public TypePattern getRight() {
    return right;
  }

  public TypePattern getLeft() {
    return left;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.patterns.TypePattern#couldEverMatchSameTypesAs(org.aspectj.weaver.patterns.TypePattern)
   */
  @Override
  protected boolean couldEverMatchSameTypesAs(TypePattern other) {
    return true; // don't dive at the moment...
  }

  @Override
  public FuzzyBoolean matchesInstanceof(ResolvedType type) {
    return left.matchesInstanceof(type).or(right.matchesInstanceof(type));
  }

  @Override
  protected boolean matchesExactly(ResolvedType type) {
    // ??? if these had side-effects, this sort-circuit could be a mistake
    return left.matchesExactly(type) || right.matchesExactly(type);
  }

  @Override
  protected boolean matchesExactly(ResolvedType type, ResolvedType annotatedType) {
    // ??? if these had side-effects, this sort-circuit could be a mistake
    return left.matchesExactly(type, annotatedType) || right.matchesExactly(type, annotatedType);
  }

  @Override
  public boolean matchesStatically(ResolvedType type) {
    return left.matchesStatically(type) || right.matchesStatically(type);
  }

  @Override
  public void setIsVarArgs(boolean isVarArgs) {
    this.isVarArgs = isVarArgs;
    left.setIsVarArgs(isVarArgs);
    right.setIsVarArgs(isVarArgs);
  }

  @Override
  public void setAnnotationTypePattern(AnnotationTypePattern annPatt) {
    if (annPatt == AnnotationTypePattern.ANY) {
      return;
    }
    if (left.annotationPattern == AnnotationTypePattern.ANY) {
      left.setAnnotationTypePattern(annPatt);
    } else {
      left.setAnnotationTypePattern(new AndAnnotationTypePattern(left.annotationPattern, annPatt));
    }
    if (right.annotationPattern == AnnotationTypePattern.ANY) {
      right.setAnnotationTypePattern(annPatt);
    } else {
      right.setAnnotationTypePattern(new AndAnnotationTypePattern(right.annotationPattern, annPatt));
    }
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    s.writeByte(TypePattern.OR);
    left.write(s);
    right.write(s);
    writeLocation(s);
  }

  public static TypePattern read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final OrTypePattern ret = new OrTypePattern(TypePattern.read(s, context), TypePattern.read(s, context));
    ret.readLocation(context, s);
    if (ret.left.isVarArgs && ret.right.isVarArgs) {
      ret.isVarArgs = true;
    }
    return ret;
  }

  @Override
  public TypePattern resolveBindings(IScope scope, Bindings bindings, boolean allowBinding, boolean requireExactType) {
    if (requireExactType) {
      return notExactType(scope);
    }
    left = left.resolveBindings(scope, bindings, false, false);
    right = right.resolveBindings(scope, bindings, false, false);
    return this;
  }

  @Override
  public TypePattern parameterizeWith(Map<String, UnresolvedType> typeVariableMap, World w) {
    final TypePattern newLeft = left.parameterizeWith(typeVariableMap, w);
    final TypePattern newRight = right.parameterizeWith(typeVariableMap, w);
    final OrTypePattern ret = new OrTypePattern(newLeft, newRight);
    ret.copyLocationFrom(this);
    return ret;
  }

  public String toString() {
    final StringBuffer buff = new StringBuffer();
    if (annotationPattern != AnnotationTypePattern.ANY) {
      buff.append('(');
      buff.append(annotationPattern.toString());
      buff.append(' ');
    }
    buff.append('(');
    buff.append(left.toString());
    buff.append(" || ");
    buff.append(right.toString());
    buff.append(')');
    if (annotationPattern != AnnotationTypePattern.ANY) {
      buff.append(')');
    }
    return buff.toString();
  }

  @Override
  public boolean isStarAnnotation() {
    return left.isStarAnnotation() || right.isStarAnnotation();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof OrTypePattern)) {
      return false;
    }
    final OrTypePattern other = (OrTypePattern) obj;
    return left.equals(other.left) && right.equals(other.right);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    int ret = 17;
    ret = ret + 37 * left.hashCode();
    ret = ret + 37 * right.hashCode();
    return ret;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public Object traverse(PatternNodeVisitor visitor, Object data) {
    final Object ret = accept(visitor, data);
    left.traverse(visitor, ret);
    right.traverse(visitor, ret);
    return ret;
  }

}
