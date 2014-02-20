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

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author colyer
 *         <p/>
 *         TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code
 *         Templates
 */
public class AnnotationPatternList extends PatternNode {

  private final AnnotationTypePattern[] typePatterns;
  int ellipsisCount = 0;

  public static final AnnotationPatternList EMPTY = new AnnotationPatternList(new AnnotationTypePattern[]{});

  public static final AnnotationPatternList ANY = new AnnotationPatternList(
      new AnnotationTypePattern[]{AnnotationTypePattern.ELLIPSIS});

  public AnnotationPatternList() {
    typePatterns = new AnnotationTypePattern[0];
    ellipsisCount = 0;
  }

  public AnnotationPatternList(AnnotationTypePattern[] arguments) {
    this.typePatterns = arguments;
    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i] == AnnotationTypePattern.ELLIPSIS) {
        ellipsisCount++;
      }
    }
  }

  public AnnotationPatternList(List l) {
    this((AnnotationTypePattern[]) l.toArray(new AnnotationTypePattern[l.size()]));
  }

  protected AnnotationTypePattern[] getAnnotationPatterns() {
    return typePatterns;
  }

  public AnnotationPatternList parameterizeWith(Map typeVariableMap, World w) {
    final AnnotationTypePattern[] parameterizedPatterns = new AnnotationTypePattern[this.typePatterns.length];
    for (int i = 0; i < parameterizedPatterns.length; i++) {
      parameterizedPatterns[i] = this.typePatterns[i].parameterizeWith(typeVariableMap, w);
    }
    final AnnotationPatternList ret = new AnnotationPatternList(parameterizedPatterns);
    ret.copyLocationFrom(this);
    return ret;
  }

  public void resolve(World inWorld) {
    for (int i = 0; i < typePatterns.length; i++) {
      typePatterns[i].resolve(inWorld);
    }
  }

  public FuzzyBoolean matches(ResolvedType[] someArgs) {
    // do some quick length tests first
    final int numArgsMatchedByEllipsis = (someArgs.length + ellipsisCount) - typePatterns.length;
    if (numArgsMatchedByEllipsis < 0) {
      return FuzzyBoolean.NO;
    }
    if ((numArgsMatchedByEllipsis > 0) && (ellipsisCount == 0)) {
      return FuzzyBoolean.NO;
    }
    // now work through the args and the patterns, skipping at ellipsis
    FuzzyBoolean ret = FuzzyBoolean.YES;
    int argsIndex = 0;
    for (int i = 0; i < typePatterns.length; i++) {
      if (typePatterns[i] == AnnotationTypePattern.ELLIPSIS) {
        // match ellipsisMatchCount args
        argsIndex += numArgsMatchedByEllipsis;
      } else if (typePatterns[i] == AnnotationTypePattern.ANY) {
        argsIndex++;
      } else {
        // match the argument type at argsIndex with the ExactAnnotationTypePattern
        // we know it is exact because nothing else is allowed in args
        if (someArgs[argsIndex].isPrimitiveType()) {
          return FuzzyBoolean.NO; // can never match
        }
        final ExactAnnotationTypePattern ap = (ExactAnnotationTypePattern) typePatterns[i];
        final FuzzyBoolean matches = ap.matchesRuntimeType(someArgs[argsIndex]);
        if (matches == FuzzyBoolean.NO) {
          return FuzzyBoolean.MAYBE; // could still match at runtime
        } else {
          argsIndex++;
          ret = ret.and(matches);
        }
      }
    }
    return ret;
  }

  public int size() {
    return typePatterns.length;
  }

  public AnnotationTypePattern get(int index) {
    return typePatterns[index];
  }

  public AnnotationPatternList resolveBindings(IScope scope, Bindings bindings, boolean allowBinding) {
    for (int i = 0; i < typePatterns.length; i++) {
      final AnnotationTypePattern p = typePatterns[i];
      if (p != null) {
        typePatterns[i] = typePatterns[i].resolveBindings(scope, bindings, allowBinding);
      }
    }
    return this;
  }

  public AnnotationPatternList resolveReferences(IntMap bindings) {
    final int len = typePatterns.length;
    final AnnotationTypePattern[] ret = new AnnotationTypePattern[len];
    for (int i = 0; i < len; i++) {
      ret[i] = typePatterns[i].remapAdviceFormals(bindings);
    }
    return new AnnotationPatternList(ret);
  }

  public String toString() {
    final StringBuffer buf = new StringBuffer();
    buf.append("(");
    for (int i = 0, len = typePatterns.length; i < len; i++) {
      final AnnotationTypePattern type = typePatterns[i];
      if (i > 0) {
        buf.append(", ");
      }
      if (type == AnnotationTypePattern.ELLIPSIS) {
        buf.append("..");
      } else {
        final String annPatt = type.toString();
        buf.append(annPatt.startsWith("@") ? annPatt.substring(1) : annPatt);
      }
    }
    buf.append(")");
    return buf.toString();
  }

  public boolean equals(Object other) {
    if (!(other instanceof AnnotationPatternList)) {
      return false;
    }
    final AnnotationPatternList o = (AnnotationPatternList) other;
    final int len = o.typePatterns.length;
    if (len != this.typePatterns.length) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      if (!this.typePatterns[i].equals(o.typePatterns[i])) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int result = 41;
    for (int i = 0, len = typePatterns.length; i < len; i++) {
      result = 37 * result + typePatterns[i].hashCode();
    }
    return result;
  }

  public static AnnotationPatternList read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final short len = s.readShort();
    final AnnotationTypePattern[] arguments = new AnnotationTypePattern[len];
    for (int i = 0; i < len; i++) {
      arguments[i] = AnnotationTypePattern.read(s, context);
    }
    final AnnotationPatternList ret = new AnnotationPatternList(arguments);
    ret.readLocation(context, s);
    return ret;
  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeShort(typePatterns.length);
    for (int i = 0; i < typePatterns.length; i++) {
      typePatterns[i].write(s);
    }
    writeLocation(s);
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public Object traverse(PatternNodeVisitor visitor, Object data) {
    final Object ret = accept(visitor, data);
    for (int i = 0; i < typePatterns.length; i++) {
      typePatterns[i].traverse(visitor, ret);
    }
    return ret;
  }

}
