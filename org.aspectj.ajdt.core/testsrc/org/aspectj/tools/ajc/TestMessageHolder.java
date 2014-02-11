/* *******************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *   Adrian Colyer			Initial implementation
 * ******************************************************************/
package org.aspectj.tools.ajc;

import java.util.List;

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHolder;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrian
 *         <p/>
 *         Deliberately empty implementation of IMessageHolder
 */
public class TestMessageHolder implements IMessageHolder {

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHolder#hasAnyMessage(org.aspectj.bridge.IMessage.Kind, boolean)
   */
  @Override
  public boolean hasAnyMessage(Kind kind, boolean orGreater) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHolder#numMessages(org.aspectj.bridge.IMessage.Kind, boolean)
   */
  @Override
  public int numMessages(Kind kind, boolean orGreater) {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHolder#getMessages(org.aspectj.bridge.IMessage.Kind, boolean)
   */
  @Override
  @NotNull
  public IMessage[] getMessages(Kind kind, boolean orGreater) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHolder#getUnmodifiableListView()
   */
  @Override
  @NotNull
  public List<IMessage> getUnmodifiableListView() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHolder#clearMessages()
   */
  @Override
  public void clearMessages() throws UnsupportedOperationException {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHandler#handleMessage(org.aspectj.bridge.IMessage)
   */
  @Override
  public boolean handleMessage(@NotNull IMessage message) throws AbortException {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHandler#isIgnoring(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public boolean isIgnoring(Kind kind) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHandler#dontIgnore(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void dontIgnore(Kind kind) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.bridge.IMessageHandler#ignore(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void ignore(Kind kind) {
    // TODO Auto-generated method stub
  }

}
