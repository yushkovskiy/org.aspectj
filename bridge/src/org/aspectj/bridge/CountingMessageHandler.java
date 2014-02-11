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

import org.aspectj.util.LangUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Wrap an IMessageHandler to count messages handled. Messages being ignored by the delegate IMessageHandler are not counted.
 */
public final class CountingMessageHandler implements IMessageHandler {
  @NotNull
  public final IMessageHandler delegate;
  @Nullable
  public final CountingMessageHandler proxy;
  @NotNull
  private final Hashtable<IMessage.Kind, IntHolder> counters;

  @NotNull
  public static CountingMessageHandler makeCountingMessageHandler(@NotNull IMessageHandler handler) {
    if (handler instanceof CountingMessageHandler) {
      return (CountingMessageHandler) handler;
    } else {
      return new CountingMessageHandler(handler);
    }
  }

  public CountingMessageHandler(@NotNull IMessageHandler delegate) {
    LangUtil.throwIaxIfNull(delegate, "delegate");
    this.delegate = delegate;
    this.counters = new Hashtable<IMessage.Kind, IntHolder>();
    proxy = (delegate instanceof CountingMessageHandler ? (CountingMessageHandler) delegate : null);
  }

  /**
   * @return delegate.handleMessage(IMessage)
   */
  @Override
  public boolean handleMessage(@NotNull IMessage message) throws AbortException {
    if (null != proxy) {
      return proxy.handleMessage(message);
    }
    if (null != message) {
      final IMessage.Kind kind = message.getKind();
      if (!isIgnoring(kind)) {
        increment(kind);
      }
    }
    return delegate.handleMessage(message);
  }

  /**
   * @return delegate.isIgnoring(IMessage.Kind)
   */
  @Override
  public boolean isIgnoring(IMessage.Kind kind) {
    return delegate.isIgnoring(kind);
  }

  /**
   * Delegate
   *
   * @param kind
   * @see org.aspectj.bridge.IMessageHandler#isIgnoring(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void dontIgnore(IMessage.Kind kind) {
    delegate.dontIgnore(kind);
  }

  /**
   * Delegate
   *
   * @param kind
   * @see org.aspectj.bridge.IMessageHandler#ignore(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void ignore(IMessage.Kind kind) {
    delegate.ignore(kind);
  }

  /**
   * @return delegate.toString()
   */
  public String toString() {
    return delegate.toString();
  }

  /**
   * Return count of messages seen through this interface.
   *
   * @param kind      the IMessage.Kind of the messages to count (if null, count all)
   * @param orGreater if true, then count this kind and any considered greater by the ordering of IMessage.Kind.COMPARATOR
   * @return number of messages of this kind (optionally or greater)
   * @see IMessage.Kind.COMPARATOR
   */
  public int numMessages(@Nullable IMessage.Kind kind, boolean orGreater) {
    if (null != proxy) {
      return proxy.numMessages(kind, orGreater);
    }
    int result = 0;
    if (null == kind) {
      for (final Enumeration<IntHolder> enu = counters.elements(); enu.hasMoreElements(); ) {
        result += (enu.nextElement()).count;
      }
    } else if (!orGreater) {
      result = numMessages(kind);
    } else {
      for (IMessage.Kind k : IMessage.KINDS) {
        if (kind.isSameOrLessThan(k)) {
          result += numMessages(k);
        }
      }
    }
    return result;
  }

  /**
   * @return true if 0 is less than <code>numMessages(IMessage.ERROR, true)</code>
   */
  public boolean hasErrors() {
    return (0 < numMessages(IMessage.ERROR, true));
  }

  public void reset() {
    if (proxy != null) {
      proxy.reset();
    }
    counters.clear();
  }

  private int numMessages(@NotNull IMessage.Kind kind) {
    if (null != proxy) {
      return proxy.numMessages(kind);
    }
    final IntHolder counter = counters.get(kind);
    return (null == counter ? 0 : counter.count);
  }

  private void increment(@NotNull IMessage.Kind kind) {
    if (null != proxy) {
      throw new IllegalStateException("not called when proxying");
    }

    IntHolder counter = counters.get(kind);
    if (null == counter) {
      counter = new IntHolder();
      counters.put(kind, counter);
    }
    counter.count++;
  }

  private static class IntHolder {
    int count;
  }

}
