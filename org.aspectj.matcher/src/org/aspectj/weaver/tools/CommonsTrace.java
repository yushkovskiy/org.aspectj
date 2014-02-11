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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CommonsTrace extends AbstractTrace {
  @NotNull
  private final Log log;
  @NotNull
  private final String className;

  public CommonsTrace(@NotNull Class clazz) {
    super(clazz);
    this.log = LogFactory.getLog(clazz);
    this.className = tracedClass.getName();
  }

  @Override
  public void enter(@NotNull String methodName, @Nullable Object thiz, @Nullable Object[] args) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage(">", className, methodName, thiz, args));
    }
  }

  @Override
  public void enter(@NotNull String methodName, @NotNull Object thiz) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage(">", className, methodName, thiz, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName, @Nullable Object ret) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage("<", className, methodName, ret, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName, @NotNull Throwable th) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage("<", className, methodName, th, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage("<", className, methodName, null, null));
    }
  }

  @Override
  public void event(@NotNull String methodName, Object thiz, Object[] args) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage("-", className, methodName, thiz, args));
    }
  }

  @Override
  public void event(@NotNull String methodName) {
    if (log.isDebugEnabled()) {
      log.debug(formatMessage("-", className, methodName, null, null));
    }
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public void setTraceEnabled(boolean b) {
  }

  @Override
  public void debug(@NotNull String message) {
    if (log.isDebugEnabled()) {
      log.debug(message);
    }
  }

  @Override
  public void info(@NotNull String message) {
    if (log.isInfoEnabled()) {
      log.info(message);
    }
  }

  @Override
  public void warn(@NotNull String message, @NotNull Throwable th) {
    if (log.isWarnEnabled()) {
      log.warn(message, th);
    }
  }

  @Override
  public void error(@NotNull String message, @NotNull Throwable th) {
    if (log.isErrorEnabled()) {
      log.error(message, th);
    }
  }

  @Override
  public void fatal(@NotNull String message, @NotNull Throwable th) {
    if (log.isFatalEnabled()) {
      log.fatal(message, th);
    }
  }

}
