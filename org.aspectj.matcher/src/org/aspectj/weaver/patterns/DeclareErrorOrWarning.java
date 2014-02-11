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

import org.aspectj.weaver.CompressingDataOutputStream;
import org.aspectj.weaver.ISourceContext;
import org.aspectj.weaver.VersionedDataInputStream;
import org.aspectj.weaver.World;

import java.io.IOException;
import java.util.Map;

public class DeclareErrorOrWarning extends Declare {
  private final boolean isError;
  private Pointcut pointcut;
  private final String message;

  public DeclareErrorOrWarning(boolean isError, Pointcut pointcut, String message) {
    this.isError = isError;
    this.pointcut = pointcut;
    this.message = message;
  }

  /**
   * returns "declare warning: <message>" or "declare error: <message>"
   */
  public String toString() {
    final StringBuffer buf = new StringBuffer();
    buf.append("declare ");
    if (isError) {
      buf.append("error: ");
    } else {
      buf.append("warning: ");
    }
    buf.append(pointcut);
    buf.append(": ");
    buf.append("\"");
    buf.append(message);
    buf.append("\";");
    return buf.toString();
  }

  public boolean equals(Object other) {
    if (!(other instanceof DeclareErrorOrWarning)) {
      return false;
    }
    final DeclareErrorOrWarning o = (DeclareErrorOrWarning) other;
    return (o.isError == isError) && o.pointcut.equals(pointcut) && o.message.equals(message);
  }

  public int hashCode() {
    int result = isError ? 19 : 23;
    result = 37 * result + pointcut.hashCode();
    result = 37 * result + message.hashCode();
    return result;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    s.writeByte(Declare.ERROR_OR_WARNING);
    s.writeBoolean(isError);
    pointcut.write(s);
    s.writeUTF(message);
    writeLocation(s);
  }

  public static Declare read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final Declare ret = new DeclareErrorOrWarning(s.readBoolean(), Pointcut.read(s, context), s.readUTF());
    ret.readLocation(context, s);
    return ret;
  }

  public boolean isError() {
    return isError;
  }

  public String getMessage() {
    return message;
  }

  public Pointcut getPointcut() {
    return pointcut;
  }

  @Override
  public void resolve(IScope scope) {
    pointcut = pointcut.resolve(scope);
  }

  @Override
  public Declare parameterizeWith(Map typeVariableBindingMap, World w) {
    final Declare ret = new DeclareErrorOrWarning(isError, pointcut.parameterizeWith(typeVariableBindingMap, w), message);
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public boolean isAdviceLike() {
    return true;
  }

  @Override
  public String getNameSuffix() {
    return "eow";
  }

  /**
   * returns "declare warning" or "declare error"
   */
  public String getName() {
    final StringBuffer buf = new StringBuffer();
    buf.append("declare ");
    if (isError) {
      buf.append("error");
    } else {
      buf.append("warning");
    }
    return buf.toString();
  }
}
