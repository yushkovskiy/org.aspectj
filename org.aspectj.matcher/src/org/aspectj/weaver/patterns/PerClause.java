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

import org.aspectj.util.TypeSafeEnum;
import org.aspectj.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// PTWIMPL New kind added to this class, can be (de)serialized
public abstract class PerClause extends Pointcut {

  @NotNull
  public static final Kind SINGLETON = new Kind("issingleton", 1);
  @NotNull
  public static final Kind PERCFLOW = new Kind("percflow", 2);
  @NotNull
  public static final Kind PEROBJECT = new Kind("perobject", 3);
  @NotNull
  public static final Kind FROMSUPER = new Kind("fromsuper", 4);
  @NotNull
  public static final Kind PERTYPEWITHIN = new Kind("pertypewithin", 5);
  protected ResolvedType inAspect;

  @NotNull
  public static PerClause readPerClause(@NotNull VersionedDataInputStream s, ISourceContext context) throws IOException {
    final Kind kind = Kind.read(s);
    if (kind == SINGLETON) return PerSingleton.readPerClause(s, context);
    else if (kind == PERCFLOW) return PerCflow.readPerClause(s, context);
    else if (kind == PEROBJECT) return PerObject.readPerClause(s, context);
    else if (kind == FROMSUPER) return PerFromSuper.readPerClause(s, context);
    else if (kind == PERTYPEWITHIN) return PerTypeWithin.readPerClause(s, context);

    throw new BCException("unknown kind: " + kind);
  }

  @Override
  public final Pointcut concretize1(ResolvedType inAspect, ResolvedType declaringType, IntMap bindings) {
    throw new RuntimeException("unimplemented: wrong concretize");
  }

  public abstract PerClause concretize(ResolvedType inAspect);

  public abstract PerClause.Kind getKind();

  public abstract String toDeclarationString();

  public static class Kind extends TypeSafeEnum {

    public static Kind read(VersionedDataInputStream s) throws IOException {
      final int key = s.readByte();
      switch (key) {
        case 1:
          return SINGLETON;
        case 2:
          return PERCFLOW;
        case 3:
          return PEROBJECT;
        case 4:
          return FROMSUPER;
        case 5:
          return PERTYPEWITHIN;
      }
      throw new BCException("weird kind " + key);
    }

    public Kind(String name, int key) {
      super(name, key);
    }
  }

  public static class KindAnnotationPrefix extends TypeSafeEnum {

    public static final KindAnnotationPrefix PERCFLOW = new KindAnnotationPrefix("percflow(", 1);
    public static final KindAnnotationPrefix PERCFLOWBELOW = new KindAnnotationPrefix("percflowbelow(", 2);
    public static final KindAnnotationPrefix PERTHIS = new KindAnnotationPrefix("perthis(", 3);
    public static final KindAnnotationPrefix PERTARGET = new KindAnnotationPrefix("pertarget(", 4);
    public static final KindAnnotationPrefix PERTYPEWITHIN = new KindAnnotationPrefix("pertypewithin(", 5);

    private KindAnnotationPrefix(String name, int key) {
      super(name, key);
    }

    public String extractPointcut(String perClause) {
      final int from = getName().length();
      final int to = perClause.length() - 1;
      if (!perClause.startsWith(getName())
          || !perClause.endsWith(")")
          || from > perClause.length()) {
        throw new RuntimeException("cannot read perclause " + perClause);
      }

      return perClause.substring(from, to);
    }
  }
}
