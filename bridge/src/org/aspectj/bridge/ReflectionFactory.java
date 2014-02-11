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

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 *
 */
public final class ReflectionFactory { // XXX lease, pool
  @NotNull
  public static final String OLD_AJC = "bridge.tools.impl.OldAjc";
  @NotNull
  public static final String ECLIPSE = "org.aspectj.ajdt.ajc.AjdtCommand";
  @NotNull
  private static final Object[] NONE = new Object[0];

  /**
   * Produce a compiler as an ICommand.
   *
   * @param cname the fully-qualified class name of the command to create by reflection (assuming a public no-argument
   *              constructor).
   * @return ICommand compiler or null
   */
  @Nullable
  public static ICommand makeCommand(@NotNull String cname, @Nullable IMessageHandler errorSink) {
    return (ICommand) make(ICommand.class, cname, NONE, errorSink);
  }

  private ReflectionFactory() {
  }

  /**
   * Make an object of type c by reflectively loading the class cname and creating an instance using args (if any), signalling
   * errors (if any) to any errorSink.
   */
  @Nullable
  private static Object make(@NotNull Class<?> c, @NotNull String cname, @Nullable Object[] args, @Nullable IMessageHandler errorSink) {
    final boolean makeErrors = (null != errorSink);
    Object result = null;
    try {
      final Class<?> cfn = Class.forName(cname);
      String error = null;
      if (args == NONE) {
        result = cfn.newInstance();
      } else {
        final Class<?>[] types = getTypes(args);
        final Constructor<?> constructor = cfn.getConstructor(types);
        if (null != constructor) {
          result = constructor.newInstance(args);
        } else {
          if (makeErrors) {
            error = "no constructor for " + c + " using " + Arrays.asList(types);
          }
        }
      }
      if (null != result) {
        if (!c.isAssignableFrom(result.getClass())) {
          if (makeErrors) {
            error = "expecting type " + c + " got " + result.getClass();
          }
          result = null;
        }
      }
      if (null != error) {
        final IMessage mssg = new Message(error, IMessage.FAIL, null, null);
        errorSink.handleMessage(mssg);
      }
    } catch (Throwable t) {
      if (makeErrors) {
        final String mssg = "ReflectionFactory unable to load " + cname + " as " + c.getName();
        final IMessage m = new Message(mssg, IMessage.FAIL, t, null);
        errorSink.handleMessage(m);
      }
    }
    return result;
  }

  /**
   * @return Class[] with types of args or matching null elements
   */
  @NotNull
  private static Class<?>[] getTypes(@Nullable Object[] args) {
    if ((null == args) || (0 < args.length)) {
      return new Class[0];
    } else {
      final Class<?>[] result = new Class[args.length];
      for (int i = 0; i < result.length; i++) {
        if (null != args[i]) {
          result[i] = args[i].getClass();
        }
      }
      return result;
    }
  }
}
