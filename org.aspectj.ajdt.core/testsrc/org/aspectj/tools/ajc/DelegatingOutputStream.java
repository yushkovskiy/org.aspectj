/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Webster - initial implementation
 *******************************************************************************/
package org.aspectj.tools.ajc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DelegatingOutputStream extends OutputStream {

  private boolean verbose = true;
  private final OutputStream target;
  private final List delegates;

  public DelegatingOutputStream(OutputStream os) {
    this.target = os;
    this.delegates = new LinkedList();
  }

  @Override
  public void close() throws IOException {
    target.close();

    for (final Iterator i = delegates.iterator(); i.hasNext(); ) {
      final OutputStream delegate = (OutputStream) i.next();
      delegate.close();
    }
  }

  @Override
  public void flush() throws IOException {
    target.flush();

    for (final Iterator i = delegates.iterator(); i.hasNext(); ) {
      final OutputStream delegate = (OutputStream) i.next();
      delegate.flush();
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (verbose) target.write(b, off, len);

    for (final Iterator i = delegates.iterator(); i.hasNext(); ) {
      final OutputStream delegate = (OutputStream) i.next();
      delegate.write(b, off, len);
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    if (verbose) target.write(b);

    for (final Iterator i = delegates.iterator(); i.hasNext(); ) {
      final OutputStream delegate = (OutputStream) i.next();
      delegate.write(b);
    }
  }

  @Override
  public void write(int b) throws IOException {
    if (verbose) target.write(b);

    for (final Iterator i = delegates.iterator(); i.hasNext(); ) {
      final OutputStream delegate = (OutputStream) i.next();
      delegate.write(b);
    }
  }

  public boolean add(OutputStream delegate) {
    return delegates.add(delegate);
  }

  public boolean remove(OutputStream delegate) {
    return delegates.remove(delegate);
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

}
