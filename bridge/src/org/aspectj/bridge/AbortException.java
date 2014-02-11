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

package org.aspectj.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Signal that a process was aborted before completion. This may contain a structured IMessage which indicates why the process was
 * aborted (e.g., the underlying exception). For processes using try/catch to complete a method abruptly but complete the process
 * normally (e.g., a test failure causes the test to abort but the reporting and testing continues normally), use the static methods
 * to borrow and return a "porter" to avoid the expense of constructing a stack trace each time. A porter stack trace is invalid,
 * and it should only be used to convey a message. E.g., to print the stack of the AbortException and any contained message:
 * <p/>
 * <pre>
 * catch (AbortException ae) {
 *     IMessage m = ae.getMessage();
 *     if (!ae.isPorter()) ae.printStackTrace(System.err);
 *     Throwable thrown = ae.getThrown();
 *     if (null != thrown) thrown.printStackTrace(System.err);
 * }
 * </pre>
 *
 * @author PARC
 * @author Andy Clement
 */
public final class AbortException extends RuntimeException { // XXX move porters out, handle proxy better

  private static final long serialVersionUID = -7211791639898586417L;

  /**
   * used when message text is null
   */
  @NotNull
  public static final String NO_MESSAGE_TEXT = "AbortException (no message)";
  @NotNull
  private static final ArrayList<AbortException> porters = new ArrayList<AbortException>();

  private boolean isSilent = false;

  /**
   * structured message abort
   */
  @Nullable
  protected IMessage message;

  /**
   * true if this is a porter exception - only used to hold message
   */
  protected boolean isPorter;

  /**
   * Get a porter exception from the pool. Porter exceptions do <b>not</b> have valid stack traces. They are used only to avoid
   * generating stack traces when using throw/catch to abruptly complete but continue.
   */
  @NotNull
  public static AbortException borrowPorter(@NotNull IMessage message) {
    final AbortException result;
    synchronized (porters) {
      if (porters.size() > 0) {
        result = porters.get(0);
      } else {
        result = new AbortException();
        result.setIsSilent(false);
      }
    }
    result.message = message;
    result.isPorter = true;
    return result;
  }

  /**
   * Return (or add) a porter exception to the pool.
   */
  public static void returnPorter(@NotNull AbortException porter) {
    synchronized (porters) {
      if (porters.contains(porter)) {
        throw new IllegalStateException("already have " + porter);
      } else {
        porters.add(porter);
      }
    }
  }

  /**
   * abort with default String message
   */
  public AbortException() {
    this("ABORT");
    isSilent = true;
  }

  /**
   * abort with message
   */
  public AbortException(@Nullable String s) {
    super(null != s ? s : NO_MESSAGE_TEXT);
    this.message = null;
  }

  /**
   * abort with structured message
   */
  public AbortException(@NotNull IMessage message) {
    super(extractMessage(message));
    this.message = message;
  }

  /**
   * @return IMessage structured message, if set
   */
  @Nullable
  public IMessage getIMessage() {
    return message;
  }

  /**
   * The stack trace of a porter is invalid; it is only used to carry a message (which may itself have a wrapped exception).
   *
   * @return true if this exception is only to carry exception
   */
  public boolean isPorter() {
    return isPorter;
  }

  /**
   * @return Throwable at bottom of IMessage chain, if any
   */
  @Nullable
  public Throwable getThrown() {
    Throwable result = null;
    final IMessage m = getIMessage();
    if (null != m) {
      result = m.getThrown();
      if (result instanceof AbortException) {
        return ((AbortException) result).getThrown();
      }
    }
    return result;
  }

  // ----------- proxy attempts

  /**
   * Get message for this AbortException, either associated explicitly as message or implicitly as IMessage message or its thrown
   * message.
   *
   * @see java.lang.Throwable#getMessage()
   */
  @Override
  @NotNull
  public String getMessage() {
    String message = super.getMessage();
    if ((null == message) || (NO_MESSAGE_TEXT == message)) {
      final IMessage m = getIMessage();
      if (null != m) {
        message = m.getMessage();
        if (null == message) {
          final Throwable thrown = m.getThrown();
          if (null != thrown) {
            message = thrown.getMessage();
          }
        }
      }
      if (null == message) {
        message = NO_MESSAGE_TEXT; // better than nothing
      }
    }
    return message;
  }

  /**
   * @see java.lang.Throwable#printStackTrace()
   */
  @Override
  public void printStackTrace() {
    printStackTrace(System.out);
  }

  /**
   * Print the stack trace of any enclosed thrown or this otherwise.
   *
   * @see java.lang.Throwable#printStackTrace(PrintStream)
   */
  @Override
  public void printStackTrace(@NotNull PrintStream s) {
    final IMessage m = getIMessage();
    final Throwable thrown = (null == m ? null : m.getThrown());
    if (!isPorter() || (null == thrown)) {
      s.println("Message: " + m);
      super.printStackTrace(s);
    } else {
      thrown.printStackTrace(s);
    }
  }

  /**
   * Print the stack trace of any enclosed thrown or this otherwise.
   *
   * @see java.lang.Throwable#printStackTrace(PrintWriter)
   */
  @Override
  public void printStackTrace(@NotNull PrintWriter s) {
    final IMessage m = getIMessage();
    final Throwable thrown = (null == m ? null : m.getThrown());
    if (null == thrown) { // Always print
      if (isPorter()) {
        s.println("(Warning porter AbortException without thrown)");
      }
      s.println("Message: " + m);
      super.printStackTrace(s);
    } else {
      thrown.printStackTrace(s);
    }
  }

  public boolean isSilent() {
    return isSilent;
  }

  public void setIsSilent(boolean isSilent) {
    this.isSilent = isSilent;
  }

  /**
   * @return NO_MESSAGE_TEXT or message.getMessage() if not null
   */
  @NotNull
  private static String extractMessage(@Nullable IMessage message) {
    if (null == message) {
      return NO_MESSAGE_TEXT;
    } else {
      final String m = message.getMessage();
      if (null == m) {
        return NO_MESSAGE_TEXT;
      } else {
        return m;
      }
    }
  }

}
