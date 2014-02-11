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

import org.aspectj.bridge.MessageUtil;
import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.*;
import org.aspectj.weaver.ast.Test;

import java.io.IOException;
import java.util.Map;

public class PerFromSuper extends PerClause {
  private final PerClause.Kind kind;

  public PerFromSuper(PerClause.Kind kind) {
    this.kind = kind;
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public int couldMatchKinds() {
    return Shadow.ALL_SHADOW_KINDS_BITS;
  }

  @Override
  public FuzzyBoolean fastMatch(FastMatchInfo type) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  protected FuzzyBoolean matchInternal(Shadow shadow) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public void resolveBindings(IScope scope, Bindings bindings) {
    // this method intentionally left blank
  }

  @Override
  protected Test findResidueInternal(Shadow shadow, ExposedState state) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public PerClause concretize(ResolvedType inAspect) {
    final PerClause p = lookupConcretePerClause(inAspect.getSuperclass());
    if (p == null) {
      inAspect.getWorld().getMessageHandler().handleMessage(
          MessageUtil.error(WeaverMessages.format(WeaverMessages.MISSING_PER_CLAUSE, inAspect.getSuperclass()),
              getSourceLocation()));
      return new PerSingleton().concretize(inAspect);// AV: fallback on something else NPE in AJDT
    } else {
      if (p.getKind() != kind) {
        inAspect.getWorld().getMessageHandler().handleMessage(
            MessageUtil.error(WeaverMessages.format(WeaverMessages.WRONG_PER_CLAUSE, kind, p.getKind()),
                getSourceLocation()));
      }
      return p.concretize(inAspect);
    }
  }

  @Override
  public Pointcut parameterizeWith(Map typeVariableMap, World w) {
    return this;
  }

  public PerClause lookupConcretePerClause(ResolvedType lookupType) {
    final PerClause ret = lookupType.getPerClause();
    if (ret == null) {
      return null;
    }
    if (ret instanceof PerFromSuper) {
      return lookupConcretePerClause(lookupType.getSuperclass());
    }
    return ret;
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    FROMSUPER.write(s);
    kind.write(s);
    writeLocation(s);
  }

  public static PerClause readPerClause(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final PerFromSuper ret = new PerFromSuper(Kind.read(s));
    ret.readLocation(context, s);
    return ret;
  }

  public String toString() {
    return "perFromSuper(" + kind + ", " + inAspect + ")";
  }

  @Override
  public String toDeclarationString() {
    return "";
  }

  @Override
  public PerClause.Kind getKind() {
    return kind;
  }

  public boolean equals(Object other) {
    if (!(other instanceof PerFromSuper)) {
      return false;
    }
    final PerFromSuper pc = (PerFromSuper) other;
    return pc.kind.equals(kind) && ((pc.inAspect == null) ? (inAspect == null) : pc.inAspect.equals(inAspect));
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + kind.hashCode();
    result = 37 * result + ((inAspect == null) ? 0 : inAspect.hashCode());
    return result;
  }

}
