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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class DefaultTraceFactory extends TraceFactory {

  @NotNull
  public final static String ENABLED_PROPERTY = "org.aspectj.tracing.enabled";
  @NotNull
  public final static String FILE_PROPERTY = "org.aspectj.tracing.file";

  private final boolean tracingEnabled = getBoolean(ENABLED_PROPERTY, false);
  @Nullable
  private PrintStream print;

  public DefaultTraceFactory() {
    final String filename = System.getProperty(FILE_PROPERTY);
    if (filename != null) {
      final File file = new File(filename);
      try {
        print = new PrintStream(new FileOutputStream(file));
      } catch (IOException ex) {
        if (debug) ex.printStackTrace();
      }
    }
  }

  public boolean isEnabled() {
    return tracingEnabled;
  }

  @Override
  @NotNull
  public Trace getTrace(@NotNull Class clazz) {
    final DefaultTrace trace = new DefaultTrace(clazz);
    trace.setTraceEnabled(tracingEnabled);
    if (print != null) trace.setPrintStream(print);
    return trace;
  }

}
