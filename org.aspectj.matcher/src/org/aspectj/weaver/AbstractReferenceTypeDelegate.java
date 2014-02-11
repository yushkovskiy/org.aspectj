/* *******************************************************************
 * Copyright (c) 2002 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 *     Andy Clement - June 2005 - separated out from ResolvedType
 * ******************************************************************/
package org.aspectj.weaver;

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.util.GenericSignature;
import org.aspectj.util.GenericSignature.ClassSignature;
import org.aspectj.util.GenericSignatureParser;
import org.aspectj.weaver.AjAttribute.WeaverVersionInfo;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReferenceTypeDelegate implements ReferenceTypeDelegate {

  private String sourcefilename = UNKNOWN_SOURCE_FILE;
  private ISourceContext sourceContext = SourceContextImpl.UNKNOWN_SOURCE_CONTEXT;

  protected boolean exposedToWeaver;
  protected ReferenceType resolvedTypeX;
  protected ClassSignature cachedGenericClassTypeSignature;

  // Happens to match Bcel javaClass default of '<Unknown>'
  public final static String UNKNOWN_SOURCE_FILE = "<Unknown>";

  public AbstractReferenceTypeDelegate(ReferenceType resolvedTypeX, boolean exposedToWeaver) {
    this.resolvedTypeX = resolvedTypeX;
    this.exposedToWeaver = exposedToWeaver;
  }

  @Override
  public final boolean isClass() {
    return !isAspect() && !isInterface();
  }

  @Override
  public boolean isCacheable() {
    return false;
  }

  /**
   * Designed to be overriden by EclipseType to disable collection of shadow mungers during pre-weave compilation phase
   */
  @Override
  public boolean doesNotExposeShadowMungers() {
    return false;
  }

  @Override
  public boolean isExposedToWeaver() {
    return exposedToWeaver;
  }

  @Override
  public ReferenceType getResolvedTypeX() {
    return resolvedTypeX;
  }

  @Override
  public final String getSourcefilename() {
    return sourcefilename;
  }

  public final void setSourcefilename(String sourceFileName) {
    sourcefilename = sourceFileName;
    if (sourceFileName != null && sourceFileName.equals(AbstractReferenceTypeDelegate.UNKNOWN_SOURCE_FILE)) {
      sourcefilename = "Type '" + getResolvedTypeX().getName() + "' (no debug info available)";
    } else {
      final String pname = getResolvedTypeX().getPackageName();
      if (pname != null) {
        sourcefilename = pname.replace('.', '/') + '/' + sourceFileName;
      }
    }
    if (sourcefilename != null && sourceContext instanceof SourceContextImpl) {
      ((SourceContextImpl) sourceContext).setSourceFileName(sourcefilename);
    }
  }

  public ISourceLocation getSourceLocation() {
    return getSourceContext().makeSourceLocation(0, 0);
  }

  @Override
  public ISourceContext getSourceContext() {
    return sourceContext;
  }

  public void setSourceContext(ISourceContext isc) {
    sourceContext = isc;
  }

  public GenericSignature.ClassSignature getGenericClassTypeSignature() {
    if (cachedGenericClassTypeSignature == null) {
      final String sig = getDeclaredGenericSignature();
      if (sig != null) {
        final GenericSignatureParser parser = new GenericSignatureParser();
        cachedGenericClassTypeSignature = parser.parseAsClassSignature(sig);
      }
    }
    return cachedGenericClassTypeSignature;
  }

  protected GenericSignature.FormalTypeParameter[] getFormalTypeParametersFromOuterClass() {
    final List<GenericSignature.FormalTypeParameter> typeParameters = new ArrayList<GenericSignature.FormalTypeParameter>();
    final ResolvedType outerClassType = getOuterClass();
    if (!(outerClassType instanceof ReferenceType)) {
      throw new BCException("Whilst processing type '" + this.resolvedTypeX.getSignature()
          + "' - cannot cast the outer type to a reference type.  Signature=" + outerClassType.getSignature()
          + " toString()=" + outerClassType.toString());
    }
    final ReferenceType outer = (ReferenceType) outerClassType;
    final ReferenceTypeDelegate outerDelegate = outer.getDelegate();
    final AbstractReferenceTypeDelegate outerObjectType = (AbstractReferenceTypeDelegate) outerDelegate;
    if (outerObjectType.isNested()) {
      final GenericSignature.FormalTypeParameter[] parentParams = outerObjectType.getFormalTypeParametersFromOuterClass();
      for (int i = 0; i < parentParams.length; i++) {
        typeParameters.add(parentParams[i]);
      }
    }
    final GenericSignature.ClassSignature outerSig = outerObjectType.getGenericClassTypeSignature();
    if (outerSig != null) {
      for (int i = 0; i < outerSig.formalTypeParameters.length; i++) {
        typeParameters.add(outerSig.formalTypeParameters[i]);
      }
    }

    final GenericSignature.FormalTypeParameter[] ret = new GenericSignature.FormalTypeParameter[typeParameters.size()];
    typeParameters.toArray(ret);
    return ret;
  }

  @Override
  public boolean copySourceContext() {
    return true;
  }

  @Override
  public int getCompilerVersion() {
    return WeaverVersionInfo.getCurrentWeaverMajorVersion();
  }

  @Override
  public void ensureConsistent() {

  }

  @Override
  public boolean isWeavable() {
    return false;
  }

  @Override
  public boolean hasBeenWoven() {
    return false;
  }
}