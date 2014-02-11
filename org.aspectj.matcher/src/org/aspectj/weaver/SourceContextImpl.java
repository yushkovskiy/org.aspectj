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

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.bridge.SourceLocation;

import java.io.File;
import java.util.Arrays;

public class SourceContextImpl implements ISourceContext {

  private int[] lineBreaks;
  String sourceFilename;

  public SourceContextImpl(AbstractReferenceTypeDelegate delegate) {
    sourceFilename = delegate.getSourcefilename();
  }

  public void configureFromAttribute(String name, int[] linebreaks) {
    this.sourceFilename = name;
    this.lineBreaks = linebreaks;
  }

  public void setSourceFileName(String name) {
    sourceFilename = name;
  }

  private File getSourceFile() {
    return new File(sourceFilename);
  }

  @Override
  public void tidy() {
  }

  @Override
  public int getOffset() {
    return 0;
  }

  @Override
  public ISourceLocation makeSourceLocation(IHasPosition position) {
    if (lineBreaks != null) {
      int line = Arrays.binarySearch(lineBreaks, position.getStart());
      if (line < 0) {
        line = -line;
      }
      return new SourceLocation(getSourceFile(), line); // ??? have more info
    } else {
      return new SourceLocation(getSourceFile(), 0);
    }
  }

  @Override
  public ISourceLocation makeSourceLocation(int line, int offset) {
    if (line < 0) {
      line = 0;
    }
    final SourceLocation sl = new SourceLocation(getSourceFile(), line);
    if (offset > 0) {
      sl.setOffset(offset);
    } else {
      if (lineBreaks != null) {
        int likelyOffset = 0;
        if (line > 0 && line < lineBreaks.length) {
          // 1st char of given line is next char after previous end of line
          likelyOffset = lineBreaks[line - 1] + 1;
        }
        sl.setOffset(likelyOffset);
      }
    }
    return sl;
  }

  public final static ISourceContext UNKNOWN_SOURCE_CONTEXT = new ISourceContext() {
    @Override
    public ISourceLocation makeSourceLocation(IHasPosition position) {
      return null;
    }

    @Override
    public ISourceLocation makeSourceLocation(int line, int offset) {
      return null;
    }

    @Override
    public int getOffset() {
      return 0;
    }

    @Override
    public void tidy() {
    }
  };
}
