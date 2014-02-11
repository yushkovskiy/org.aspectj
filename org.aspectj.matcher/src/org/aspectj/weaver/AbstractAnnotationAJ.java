/* *******************************************************************
 * Copyright (c) 2008 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * ******************************************************************/
package org.aspectj.weaver;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractAnnotationAJ implements AnnotationAJ {

  protected final ResolvedType type;

  private Set<String> supportedTargets = null; // @target meta annotation

  public AbstractAnnotationAJ(ResolvedType type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ResolvedType getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getTypeSignature() {
    return type.getSignature();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getTypeName() {
    return type.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean allowedOnAnnotationType() {
    ensureAtTargetInitialized();
    if (supportedTargets.isEmpty()) {
      return true;
    }
    return supportedTargets.contains("ANNOTATION_TYPE");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean allowedOnField() {
    ensureAtTargetInitialized();
    if (supportedTargets.isEmpty()) {
      return true;
    }
    return supportedTargets.contains("FIELD");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean allowedOnRegularType() {
    ensureAtTargetInitialized();
    if (supportedTargets.isEmpty()) {
      return true;
    }
    return supportedTargets.contains("TYPE");
  }

  /**
   * {@inheritDoc}
   */
  public final void ensureAtTargetInitialized() {
    if (supportedTargets == null) {
      final AnnotationAJ atTargetAnnotation = retrieveAnnotationOnAnnotation(UnresolvedType.AT_TARGET);
      if (atTargetAnnotation == null) {
        supportedTargets = Collections.emptySet();
      } else {
        supportedTargets = atTargetAnnotation.getTargets();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getValidTargets() {
    final StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (final Iterator<String> iter = supportedTargets.iterator(); iter.hasNext(); ) {
      final String evalue = iter.next();
      sb.append(evalue);
      if (iter.hasNext()) {
        sb.append(",");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean specifiesTarget() {
    ensureAtTargetInitialized();
    return !supportedTargets.isEmpty();
  }

  /**
   * Helper method to retrieve an annotation on an annotation e.g. retrieveAnnotationOnAnnotation(UnresolvedType.AT_TARGET)
   */
  private final AnnotationAJ retrieveAnnotationOnAnnotation(UnresolvedType requiredAnnotationSignature) {
    final AnnotationAJ[] annos = type.getAnnotations();
    for (int i = 0; i < annos.length; i++) {
      final AnnotationAJ a = annos[i];
      if (a.getTypeSignature().equals(requiredAnnotationSignature.getSignature())) {
        return annos[i];
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract boolean isRuntimeVisible();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract Set<String> getTargets();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract boolean hasNameValuePair(String name, String value);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract boolean hasNamedValue(String name);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract String stringify();

}
