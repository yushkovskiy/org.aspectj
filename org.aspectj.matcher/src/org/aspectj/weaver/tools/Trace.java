/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Webster - initial implementation
 *******************************************************************************/
package org.aspectj.weaver.tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Trace {

  public void enter(@NotNull String methodName, @Nullable Object thiz, @Nullable Object[] args);

  public void enter(@NotNull String methodName, @NotNull Object thiz);

  public void exit(@NotNull String methodName, @Nullable Object ret);

  public void exit(@NotNull String methodName, @NotNull Throwable th);

  public void exit(@NotNull String methodName);

  public void event(@NotNull String methodName);

  public void event(@NotNull String methodName, @Nullable Object thiz, @Nullable Object[] args);

  public void debug(@NotNull String message);

  public void info(@NotNull String message);

  public void warn(@NotNull String message);

  public void warn(@NotNull String message, @NotNull Throwable th);

  public void error(@NotNull String message);

  public void error(@NotNull String message, @NotNull Throwable th);

  public void fatal(@NotNull String message);

  public void fatal(@NotNull String message, @NotNull Throwable th);


  /*
   * Convenience methods
   */
  public void enter(@NotNull String methodName, @Nullable Object thiz, @Nullable Object arg);

  public void enter(@NotNull String methodName, Object thiz, boolean z);

  public void exit(@NotNull String methodName, boolean b);

  public void exit(@NotNull String methodName, int i);

  public void event(@NotNull String methodName, @Nullable Object thiz, @Nullable Object arg);

  public boolean isTraceEnabled();

  public void setTraceEnabled(boolean b);
}