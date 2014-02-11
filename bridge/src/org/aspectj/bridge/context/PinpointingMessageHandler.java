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
package org.aspectj.bridge.context;

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHandler;
import org.aspectj.bridge.ISourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author colyer
 *         Facade for an IMessageHandler
 *         Extends message with details of exactly what the compiler / weaver was doing at the
 *         time. Use the -Xdev:Pinpoint option to turn this facility on.
 */
public class PinpointingMessageHandler implements IMessageHandler {
  @NotNull
  private final IMessageHandler delegate;

  public PinpointingMessageHandler(@NotNull IMessageHandler delegate) {
    this.delegate = delegate;
  }

  /* (non-Javadoc)
   * @see org.aspectj.bridge.IMessageHandler#handleMessage(org.aspectj.bridge.IMessage)
   */
  @Override
  public boolean handleMessage(@NotNull IMessage message) throws AbortException {
    if (!isIgnoring(message.getKind())) {
      final MessageIssued ex = new MessageIssued();
      ex.fillInStackTrace();
      final StringWriter sw = new StringWriter();
      ex.printStackTrace(new PrintWriter(sw));
      final StringBuilder sb = new StringBuilder();
      sb.append(CompilationAndWeavingContext.getCurrentContext());
      sb.append(sw.toString());
      final IMessage pinpointedMessage = new PinpointedMessage(message, sb.toString());
      return delegate.handleMessage(pinpointedMessage);
    } else {
      return delegate.handleMessage(message);
    }
  }

  /* (non-Javadoc)
   * @see org.aspectj.bridge.IMessageHandler#isIgnoring(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public boolean isIgnoring(Kind kind) {
    return delegate.isIgnoring(kind);
  }

  /* (non-Javadoc)
   * @see org.aspectj.bridge.IMessageHandler#dontIgnore(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void dontIgnore(Kind kind) {
    delegate.dontIgnore(kind);
  }


  /* (non-Javadoc)
   * @see org.aspectj.bridge.IMessageHandler#ignore(org.aspectj.bridge.IMessage.Kind)
   */
  @Override
  public void ignore(Kind kind) {
    delegate.ignore(kind);
  }

  private static class PinpointedMessage implements IMessage {
    @NotNull
    private final IMessage delegate;
    @NotNull
    private final String message;

    public PinpointedMessage(@NotNull IMessage delegate, @NotNull String pinpoint) {
      this.delegate = delegate;
      this.message = delegate.getMessage() + "\n" + pinpoint;
    }

    @Override
    @NotNull
    public String getMessage() {
      return this.message;
    }

    @Override
    @NotNull
    public Kind getKind() {
      return delegate.getKind();
    }

    @Override
    public boolean isError() {
      return delegate.isError();
    }

    @Override
    public boolean isWarning() {
      return delegate.isWarning();
    }

    @Override
    public boolean isDebug() {
      return delegate.isDebug();
    }

    @Override
    public boolean isInfo() {
      return delegate.isInfo();
    }

    @Override
    public boolean isAbort() {
      return delegate.isAbort();
    }

    @Override
    public boolean isTaskTag() {
      return delegate.isTaskTag();
    }

    @Override
    public boolean isFailed() {
      return delegate.isFailed();
    }

    @Override
    public boolean getDeclared() {
      return delegate.getDeclared();
    }

    @Override
    public int getID() {
      return delegate.getID();
    }

    @Override
    public int getSourceStart() {
      return delegate.getSourceStart();
    }

    @Override
    public int getSourceEnd() {
      return delegate.getSourceEnd();
    }

    @Nullable
    @Override
    public Throwable getThrown() {
      return delegate.getThrown();
    }

    @Nullable
    @Override
    public ISourceLocation getSourceLocation() {
      return delegate.getSourceLocation();
    }

    @Nullable
    @Override
    public String getDetails() {
      return delegate.getDetails();
    }

    @Override
    @NotNull
    public List<ISourceLocation> getExtraSourceLocations() {
      return delegate.getExtraSourceLocations();
    }
  }

  private static class MessageIssued extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Override
    public String getMessage() {
      return "message issued...";
    }
  }

}
