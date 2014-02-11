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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Wrap message with any associated throwable or source location.
 */
public interface IMessage {
  /**
   * no messages
   */
  @NotNull
  public static final IMessage[] RA_IMessage = new IMessage[0];

  // int values must sync with KINDS order below
  @NotNull
  public static final Kind WEAVEINFO = new Kind("weaveinfo", 5);
  @NotNull
  public static final Kind INFO = new Kind("info", 10);
  @NotNull
  public static final Kind DEBUG = new Kind("debug", 20);
  @NotNull
  public static final Kind TASKTAG = new Kind("task", 25); // represents a 'TODO' from eclipse - producted by the compiler and
  // consumed by AJDT
  @NotNull
  public static final Kind WARNING = new Kind("warning", 30);
  @NotNull
  public static final Kind ERROR = new Kind("error", 40);
  @NotNull
  public static final Kind FAIL = new Kind("fail", 50);
  @NotNull
  public static final Kind ABORT = new Kind("abort", 60);
  // XXX prefer another Kind to act as selector for "any",
  // but can't prohibit creating messages with it.
  // public static final Kind ANY = new Kind("any-selector", 0);

  /**
   * list of Kind in precedence order. 0 is less than IMessage.Kind.COMPARATOR.compareTo(KINDS.get(i), KINDS.get(i + 1))
   */
  @NotNull
  public static final List<Kind> KINDS = Collections.unmodifiableList(Arrays.asList(
      WEAVEINFO, INFO, DEBUG, TASKTAG, WARNING, ERROR, FAIL, ABORT
  ));

  /**
   * @return non-null String with simple message
   */
  @NotNull
  String getMessage();

  /**
   * @return the kind of this message
   */
  @NotNull
  Kind getKind();

  /**
   * @return true if this is an error
   */
  boolean isError();

  /**
   * @return true if this is a warning
   */
  boolean isWarning();

  /**
   * @return true if this is an internal debug message
   */
  boolean isDebug();

  /**
   * @return true if this is information for the user
   */
  boolean isInfo();

  /**
   * @return true if the process is aborting
   */
  boolean isAbort(); // XXX ambiguous

  /**
   * @return true if this is a task tag message
   */
  boolean isTaskTag();

  /**
   * @return true if something failed
   */
  boolean isFailed();

  /**
   * Caller can verify if this message came about because of a DEOW
   */
  boolean getDeclared();

  /**
   * Return the ID of the message where applicable, see IProblem for list of valid IDs
   */
  int getID();

  /**
   * Return the start position of the problem (inclusive), or -1 if unknown.
   */
  int getSourceStart();

  /**
   * Return the end position of the problem (inclusive), or -1 if unknown.
   */
  int getSourceEnd();

  /**
   * @return Throwable associated with this message, or null if none
   */
  @Nullable
  Throwable getThrown();

  /**
   * @return source location associated with this message, or null if none
   */
  @Nullable
  ISourceLocation getSourceLocation();

  /**
   * @return Detailed information about the message. For example, for declare error/warning messages this returns information
   * about the corresponding join point's static part.
   */
  @Nullable
  public String getDetails();

  /**
   * @return List of <code>ISourceLocation</code> instances that indicate additional source locations relevent to this message as
   * specified by the message creator. The list should not include the primary source location associated with the message
   * which can be obtained from <code>getSourceLocation()<code>.
   * <p/>
   * An example of using extra locations would be in a warning message that
   * flags all shadow locations that will go unmatched due to a pointcut definition
   * being based on a subtype of a defining type.
   * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=41952">AspectJ bug 41952</a>
   */
  @NotNull
  public List<ISourceLocation> getExtraSourceLocations();

  public static final class Kind implements Comparable<IMessage.Kind> {
    @NotNull
    public static final Comparator<IMessage.Kind> COMPARATOR = new Comparator<IMessage.Kind>() {
      @Override
      public int compare(@Nullable Kind one, @Nullable Kind two) {
        if (null == one) {
          return (null == two ? 0 : -1);
        } else if (null == two) {
          return 1;
        } else if (one == two) {
          return 0;
        } else {
          return (one.precedence - two.precedence);
        }
      }
    };

    private final int precedence;
    @NotNull
    private final String name;

    private Kind(@NotNull String name, int precedence) {
      this.name = name;
      this.precedence = precedence;
    }

    /**
     * @param kind the Kind floor
     * @return false if kind is null or this has less precedence than kind, true otherwise.
     */
    public boolean isSameOrLessThan(@Nullable Kind kind) {
      return (0 >= COMPARATOR.compare(this, kind));
    }

    @Override
    public int compareTo(@Nullable Kind other) {
      return COMPARATOR.compare(this, other);
    }

    @Override
    @NotNull
    public String toString() {
      return name;
    }
  }
}
