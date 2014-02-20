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

import org.aspectj.bridge.IMessage.Kind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public abstract class AbstractTrace implements Trace {
  @Nullable
  private static SimpleDateFormat timeFormat;
  @NotNull
  protected Class tracedClass;

  protected AbstractTrace(@NotNull Class clazz) {
    this.tracedClass = clazz;
  }

  @Override
  public abstract void enter(@NotNull String methodName, @Nullable Object thiz, @Nullable Object[] args);

  @Override
  public abstract void enter(@NotNull String methodName, @NotNull Object thiz);

  @Override
  public abstract void exit(@NotNull String methodName, @Nullable Object ret);

  @Override
  public abstract void exit(@NotNull String methodName, @NotNull Throwable th);

  /*
   * Convenience methods
   */
  public void enter(@NotNull String methodName) {
    enter(methodName, null, null);
  }

  @Override
  public void enter(@NotNull String methodName, Object thiz, Object arg) {
    enter(methodName, thiz, new Object[]{arg});
  }

  @Override
  public void enter(@NotNull String methodName, Object thiz, boolean z) {
    enter(methodName, thiz, new Boolean(z));
  }

  @Override
  public void exit(@NotNull String methodName, boolean b) {
    exit(methodName, new Boolean(b));
  }

  @Override
  public void exit(@NotNull String methodName, int i) {
    exit(methodName, new Integer(i));
  }

  @Override
  public void event(@NotNull String methodName, Object thiz, Object arg) {
    event(methodName, thiz, new Object[]{arg});
  }

  @Override
  public void warn(@NotNull String message) {
    warn(message, null);
  }

  @Override
  public void error(@NotNull String message) {
    error(message, null);
  }

  @Override
  public void fatal(@NotNull String message) {
    fatal(message, null);
  }

  /*
   * Formatting
   */
  @NotNull
  protected String formatMessage(@NotNull String kind, @NotNull String className, @NotNull String methodName, @Nullable Object thiz, @Nullable Object[] args) {
    final StringBuilder message = new StringBuilder();
    final Date now = new Date();
    message.append(formatDate(now)).append(" ");
    message.append(Thread.currentThread().getName()).append(" ");
    message.append(kind).append(" ");
    message.append(className);
    message.append(".").append(methodName);
    if (thiz != null) message.append(" ").append(formatObj(thiz));
    if (args != null) message.append(" ").append(formatArgs(args));
    return message.toString();
  }

  @NotNull
  protected String formatMessage(@NotNull String kind, @NotNull String text, @Nullable Throwable th) {
    final StringBuilder message = new StringBuilder();
    final Date now = new Date();
    message.append(formatDate(now)).append(" ");
    message.append(Thread.currentThread().getName()).append(" ");
    message.append(kind).append(" ");
    message.append(text);
    if (th != null) message.append(" ").append(formatObj(th));
    return message.toString();
  }

  /**
   * Format objects safely avoiding toString which can cause recursion,
   * NullPointerExceptions or highly verbose results.
   *
   * @param obj parameter to be formatted
   * @return the formated parameter
   */
  @Nullable
  protected Object formatObj(@Nullable Object obj) {
    /* These classes have a safe implementation of toString() */
    if (obj == null
        || obj instanceof String
        || obj instanceof Number
        || obj instanceof Boolean
        || obj instanceof Exception
        || obj instanceof Character
        || obj instanceof Class
        || obj instanceof File
        || obj instanceof StringBuffer
        || obj instanceof URL
        || obj instanceof Kind
        ) return obj;
    else if (obj.getClass().isArray()) {
      return formatArray(obj);
    } else if (obj instanceof Collection) {
      return formatCollection((Collection) obj);
    } else try {
      /* Classes can provide an alternative implementation of toString() */
      if (obj instanceof Traceable) {
        final Traceable t = (Traceable) obj;
        return t.toTraceString();
      }

			/* Use classname@hashcode */
      else return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));

		/* Object.hashCode() can be override and may thow an exception */
    } catch (Exception ex) {
      return obj.getClass().getName() + "@FFFFFFFF";
    }
  }

  @NotNull
  protected static String formatArray(@NotNull Object obj) {
    return obj.getClass().getComponentType().getName() + "[" + Array.getLength(obj) + "]";
  }

  @NotNull
  protected static String formatCollection(@NotNull Collection c) {
    return c.getClass().getName() + "(" + c.size() + ")";
  }

  /**
   * Format arguments into a comma separated list
   *
   * @param args array of arguments
   * @return the formated list
   */
  @NotNull
  protected String formatArgs(@NotNull Object[] args) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      sb.append(formatObj(args[i]));
      if (i < args.length - 1) sb.append(", ");
    }
    return sb.toString();
  }

  @NotNull
  protected Object[] formatObjects(@NotNull Object[] args) {
    for (int i = 0; i < args.length; i++) {
      args[i] = formatObj(args[i]);
    }
    return args;
  }

  @NotNull
  private static String formatDate(@NotNull Date date) {
    if (timeFormat == null) {
      timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    }
    return timeFormat.format(date);
  }
}