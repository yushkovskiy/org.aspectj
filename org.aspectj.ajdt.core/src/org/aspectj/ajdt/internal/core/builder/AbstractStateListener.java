/**
 * Copyright (c) 2005 IBM and other contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 *     Andy Clement     initial implementation 
 * ******************************************************************/
package org.aspectj.ajdt.internal.core.builder;

import java.io.File;
import java.util.List;

/**
 * Subtypes can override whatever they want...
 *
 * @author AndyClement
 */
public abstract class AbstractStateListener implements IStateListener {

  @Override
  public void detectedClassChangeInThisDir(File f) {
  }

  @Override
  public void aboutToCompareClasspaths(List oldClasspath, List newClasspath) {
  }

  @Override
  public void pathChangeDetected() {
  }

  @Override
  public void detectedAspectDeleted(File f) {
  }

  @Override
  public void buildSuccessful(boolean wasFullBuild) {
  }

  @Override
  public void recordDecision(String decision) {
  }

  @Override
  public void recordInformation(String info) {
  }

}
