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

import java.io.PrintStream;


public class DefaultTrace extends AbstractTrace {

  private boolean traceEnabled = false;
  private PrintStream print = System.err;

  public DefaultTrace(@NotNull Class clazz) {
    super(clazz);
  }

  @Override
  public boolean isTraceEnabled() {
    return traceEnabled;
  }

  @Override
  public void setTraceEnabled(boolean b) {
    traceEnabled = b;
  }

  @Override
  public void enter(@NotNull String methodName, Object thiz, Object[] args) {
    if (traceEnabled) {
      println(formatMessage(">", tracedClass.getName(), methodName, thiz, args));
    }
  }

  @Override
  public void enter(@NotNull String methodName, @NotNull Object thiz) {
    if (traceEnabled) {
      println(formatMessage(">", tracedClass.getName(), methodName, thiz, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName, Object ret) {
    if (traceEnabled) {
      println(formatMessage("<", tracedClass.getName(), methodName, ret, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName) {
    if (traceEnabled) {
      println(formatMessage("<", tracedClass.getName(), methodName, null, null));
    }
  }

  @Override
  public void exit(@NotNull String methodName, @NotNull Throwable th) {
    if (traceEnabled) {
      println(formatMessage("<", tracedClass.getName(), methodName, th, null));
    }
  }

  @Override
  public void event(@NotNull String methodName, Object thiz, Object[] args) {
    if (traceEnabled) {
      println(formatMessage("-", tracedClass.getName(), methodName, thiz, args));
    }
  }

  @Override
  public void event(@NotNull String methodName) {
    if (traceEnabled) {
      println(formatMessage("-", tracedClass.getName(), methodName, null, null));
    }
  }

  @Override
  public void debug(@NotNull String message) {
    println(formatMessage("?", message, null));
  }

  @Override
  public void info(@NotNull String message) {
    println(formatMessage("I", message, null));
  }

  @Override
  public void warn(@NotNull String message, @NotNull Throwable th) {
    println(formatMessage("W", message, th));
    if (th != null) th.printStackTrace();
  }


  @Override
  public void error(@NotNull String message, @NotNull Throwable th) {
    println(formatMessage("E", message, th));
    if (th != null) th.printStackTrace();
  }

  @Override
  public void fatal(@NotNull String message, @NotNull Throwable th) {
    println(formatMessage("X", message, th));
    if (th != null) th.printStackTrace();
  }

  /**
   * Template method that allows choice of destination for output
   *
   * @param s message to be traced
   */
  protected void println(String s) {
    print.println(s);
  }

  public void setPrintStream(PrintStream printStream) {
    this.print = printStream;
  }

//	private static boolean isTracingEnabled = getBoolean("org.aspectj.weaver.tools.tracing",false);
//
//	private static boolean getBoolean (String name, boolean def) {
//		String defaultValue = String.valueOf(def);
//		String value = System.getProperty(name,defaultValue);
//		return Boolean.valueOf(value).booleanValue();
//	}

}
