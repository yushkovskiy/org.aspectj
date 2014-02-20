/* *******************************************************************
 * Copyright (c) 2010 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement     initial implementation 
 * ******************************************************************/

package org.aspectj.weaver.patterns;

import org.aspectj.weaver.CompressingDataOutputStream;
import org.aspectj.weaver.ISourceContext;
import org.aspectj.weaver.VersionedDataInputStream;
import org.aspectj.weaver.World;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * For a declare error/warning that specified a type pattern rather than a pointcut.
 *
 * @author Andy Clement
 * @since 1.6.9
 */
public class DeclareTypeErrorOrWarning extends Declare {
  private final boolean isError;
  private final TypePattern typePattern;
  private final String message;

  public DeclareTypeErrorOrWarning(boolean isError, TypePattern typePattern, String message) {
    this.isError = isError;
    this.typePattern = typePattern;
    this.message = message;
  }

  /**
   * returns "declare warning: <typepattern>: <message>" or "declare error: <typepattern>: <message>"
   */
  public String toString() {
    final StringBuffer buf = new StringBuffer();
    buf.append("declare ");
    if (isError) {
      buf.append("error: ");
    } else {
      buf.append("warning: ");
    }
    buf.append(typePattern);
    buf.append(": ");
    buf.append("\"");
    buf.append(message);
    buf.append("\";");
    return buf.toString();
  }

  public boolean equals(Object other) {
    if (!(other instanceof DeclareTypeErrorOrWarning)) {
      return false;
    }
    final DeclareTypeErrorOrWarning o = (DeclareTypeErrorOrWarning) other;
    return (o.isError == isError) && o.typePattern.equals(typePattern) && o.message.equals(message);
  }

  public int hashCode() {
    int result = isError ? 19 : 23;
    result = 37 * result + typePattern.hashCode();
    result = 37 * result + message.hashCode();
    return result;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(Declare.TYPE_ERROR_OR_WARNING);
    s.writeBoolean(isError);
    typePattern.write(s);
    s.writeUTF(message);
    writeLocation(s);
  }

  public static Declare read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final Declare ret = new DeclareTypeErrorOrWarning(s.readBoolean(), TypePattern.read(s, context), s.readUTF());
    ret.readLocation(context, s);
    return ret;
  }

  public boolean isError() {
    return isError;
  }

  public String getMessage() {
    return message;
  }

  public TypePattern getTypePattern() {
    return typePattern;
  }

  @Override
  public void resolve(IScope scope) {
    typePattern.resolve(scope.getWorld());
  }

  @Override
  public Declare parameterizeWith(Map typeVariableBindingMap, World w) {
    final Declare ret = new DeclareTypeErrorOrWarning(isError, typePattern.parameterizeWith(typeVariableBindingMap, w), message);
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public boolean isAdviceLike() {
    return false;
  }

  @Override
  public String getNameSuffix() {
    return "teow";
  }

  /**
   * returns "declare type warning" or "declare type error"
   */
  public String getName() {
    final StringBuffer buf = new StringBuffer();
    buf.append("declare type ");
    if (isError) {
      buf.append("error");
    } else {
      buf.append("warning");
    }
    return buf.toString();
  }
}
