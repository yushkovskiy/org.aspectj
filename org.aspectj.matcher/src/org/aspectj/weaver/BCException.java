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

package org.aspectj.weaver;

import org.aspectj.bridge.context.CompilationAndWeavingContext;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception to use inside the bcweaver.
 */
public class BCException extends RuntimeException {
  Throwable thrown;

  public BCException() {
    super();
  }

  public BCException(String s) {
    super(s + "\n" + CompilationAndWeavingContext.getCurrentContext());
  }

  public BCException(String s, Throwable thrown) {
    this(s);
    this.thrown = thrown;
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.err);
  }

  @Override
  public void printStackTrace(PrintStream s) {
    printStackTrace(new PrintWriter(s));
  }

  @Override
  public void printStackTrace(PrintWriter s) {
    super.printStackTrace(s);
    if (null != thrown) {
      s.print("Caused by: ");
      s.print(thrown.getClass().getName());
      final String message = thrown.getMessage();
      if (null != message) {
        s.print(": ");
        s.print(message);
      }
      s.println();
      thrown.printStackTrace(s);
    }
  }

}
