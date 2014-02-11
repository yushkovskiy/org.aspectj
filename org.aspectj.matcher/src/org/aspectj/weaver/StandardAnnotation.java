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

import java.util.*;

/**
 * This type represents the weavers abstraction of an annotation - it is not tied to any underlying BCI toolkit. The weaver actualy
 * handles these through AnnotationX wrapper objects - until we start transforming the BCEL annotations into this form (expensive)
 * or offer a clever visitor mechanism over the BCEL annotation stuff that builds these annotation types directly.
 *
 * @author AndyClement
 */
public class StandardAnnotation extends AbstractAnnotationAJ {

  private final boolean isRuntimeVisible;

  private List<AnnotationNameValuePair> nvPairs = null;

  public StandardAnnotation(ResolvedType type, boolean isRuntimeVisible) {
    super(type);
    this.isRuntimeVisible = isRuntimeVisible;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeVisible() {
    return isRuntimeVisible;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String stringify() {
    final StringBuffer sb = new StringBuffer();
    sb.append("@").append(type.getClassName());
    if (hasNameValuePairs()) {
      sb.append("(");
      for (AnnotationNameValuePair nvPair : nvPairs) {
        sb.append(nvPair.stringify());
      }
      sb.append(")");
    }
    return sb.toString();
  }

  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Anno[" + getTypeSignature() + " " + (isRuntimeVisible ? "rVis" : "rInvis"));
    if (nvPairs != null) {
      sb.append(" ");
      for (final Iterator<AnnotationNameValuePair> iter = nvPairs.iterator(); iter.hasNext(); ) {
        final AnnotationNameValuePair element = iter.next();
        sb.append(element.toString());
        if (iter.hasNext()) {
          sb.append(",");
        }
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNamedValue(String n) {
    if (nvPairs == null) {
      return false;
    }
    for (int i = 0; i < nvPairs.size(); i++) {
      final AnnotationNameValuePair pair = nvPairs.get(i);
      if (pair.getName().equals(n)) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNameValuePair(String n, String v) {
    if (nvPairs == null) {
      return false;
    }
    for (int i = 0; i < nvPairs.size(); i++) {
      final AnnotationNameValuePair pair = nvPairs.get(i);
      if (pair.getName().equals(n)) {
        if (pair.getValue().stringify().equals(v)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getTargets() {
    if (!type.equals(UnresolvedType.AT_TARGET)) {
      return Collections.emptySet();
    }
    final AnnotationNameValuePair nvp = nvPairs.get(0);
    final ArrayAnnotationValue aav = (ArrayAnnotationValue) nvp.getValue();
    final AnnotationValue[] avs = aav.getValues();
    final Set<String> targets = new HashSet<String>();
    for (int i = 0; i < avs.length; i++) {
      final EnumAnnotationValue value = (EnumAnnotationValue) avs[i];
      targets.add(value.getValue());
    }
    return targets;
  }

  public List<AnnotationNameValuePair> getNameValuePairs() {
    return nvPairs;
  }

  public boolean hasNameValuePairs() {
    return nvPairs != null && nvPairs.size() != 0;
  }

  public void addNameValuePair(AnnotationNameValuePair pair) {
    if (nvPairs == null) {
      nvPairs = new ArrayList<AnnotationNameValuePair>();
    }
    nvPairs.add(pair);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStringFormOfValue(String name) {
    if (hasNameValuePairs()) {
      for (AnnotationNameValuePair nvPair : nvPairs) {
        if (nvPair.getName().equals(name)) {
          return nvPair.getValue().stringify();
        }
      }
    }
    return null;
  }
}
