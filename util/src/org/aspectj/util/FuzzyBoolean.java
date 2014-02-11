/* *******************************************************************
 * Copyright (c) 1999-2001 Xerox Corporation, 
 *               2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Xerox/PARC     initial implementation 
 * ******************************************************************/
package org.aspectj.util;

import org.jetbrains.annotations.NotNull;

/**
 * This class implements boolean that include a "maybe"
 */
public abstract class FuzzyBoolean {

  @NotNull
  public static final FuzzyBoolean YES = new YesFuzzyBoolean();
  @NotNull
  public static final FuzzyBoolean NO = new NoFuzzyBoolean();
  @NotNull
  public static final FuzzyBoolean MAYBE = new MaybeFuzzyBoolean();
  @NotNull
  public static final FuzzyBoolean NEVER = new NeverFuzzyBoolean();

  @NotNull
  public static final FuzzyBoolean fromBoolean(boolean b) {
    return b ? YES : NO;
  }

  public abstract boolean alwaysTrue();

  public abstract boolean alwaysFalse();

  public abstract boolean maybeTrue();

  public abstract boolean maybeFalse();

  @NotNull
  public abstract FuzzyBoolean and(@NotNull FuzzyBoolean other);

  @NotNull
  public abstract FuzzyBoolean or(@NotNull FuzzyBoolean other);

  @NotNull
  public abstract FuzzyBoolean not();

  private static class YesFuzzyBoolean extends FuzzyBoolean {
    @Override
    public boolean alwaysFalse() {
      return false;
    }

    @Override
    public boolean alwaysTrue() {
      return true;
    }

    @Override
    public boolean maybeFalse() {
      return false;
    }

    @Override
    public boolean maybeTrue() {
      return true;
    }

    @Override
    @NotNull
    public FuzzyBoolean and(@NotNull FuzzyBoolean other) {
      return other;
    }

    @Override
    @NotNull
    public FuzzyBoolean not() {
      return FuzzyBoolean.NO;
    }

    @Override
    @NotNull
    public FuzzyBoolean or(@NotNull FuzzyBoolean other) {
      return this;
    }

    public String toString() {
      return "YES";
    }
  }

  private static class NoFuzzyBoolean extends FuzzyBoolean {
    @Override
    public boolean alwaysFalse() {
      return true;
    }

    @Override
    public boolean alwaysTrue() {
      return false;
    }


    @Override
    public boolean maybeFalse() {
      return true;
    }

    @Override
    public boolean maybeTrue() {
      return false;
    }

    @NotNull
    @Override
    public FuzzyBoolean and(@NotNull FuzzyBoolean other) {
      return this;
    }

    @NotNull
    @Override
    public FuzzyBoolean not() {
      return FuzzyBoolean.YES;
    }

    @NotNull
    @Override
    public FuzzyBoolean or(@NotNull FuzzyBoolean other) {
      return other;
    }

    public String toString() {
      return "NO";
    }
  }

  private static class NeverFuzzyBoolean extends FuzzyBoolean {
    @Override
    public boolean alwaysFalse() {
      return true;
    }

    @Override
    public boolean alwaysTrue() {
      return false;
    }


    @Override
    public boolean maybeFalse() {
      return true;
    }

    @Override
    public boolean maybeTrue() {
      return false;
    }

    @NotNull
    @Override
    public FuzzyBoolean and(@NotNull FuzzyBoolean other) {
      return this;
    }

    @NotNull
    @Override
    public FuzzyBoolean not() {
      return this;
    }

    @NotNull
    @Override
    public FuzzyBoolean or(@NotNull FuzzyBoolean other) {
      return this;
    }

    public String toString() {
      return "NEVER";
    }
  }

  private static class MaybeFuzzyBoolean extends FuzzyBoolean {
    @Override
    public boolean alwaysFalse() {
      return false;
    }

    @Override
    public boolean alwaysTrue() {
      return false;
    }


    @Override
    public boolean maybeFalse() {
      return true;
    }

    @Override
    public boolean maybeTrue() {
      return true;
    }

    @NotNull
    @Override
    public FuzzyBoolean and(@NotNull FuzzyBoolean other) {
      return other.alwaysFalse() ? other : this;
    }

    @NotNull
    @Override
    public FuzzyBoolean not() {
      return this;
    }

    @NotNull
    @Override
    public FuzzyBoolean or(@NotNull FuzzyBoolean other) {
      return other.alwaysTrue() ? other : this;
    }

    public String toString() {
      return "MAYBE";
    }
  }

}
