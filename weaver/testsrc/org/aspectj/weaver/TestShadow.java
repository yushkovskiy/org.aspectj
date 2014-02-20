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
import org.aspectj.weaver.ast.Var;

public class TestShadow extends Shadow {

  private final World world;
  private final UnresolvedType thisType;

  public TestShadow(Kind kind, Member signature, UnresolvedType thisType, World world) {
    super(kind, signature, null);
    this.world = world;
    this.thisType = thisType;
  }

  @Override
  public World getIWorld() {
    return world;
  }

  /**
   * this is subtly wrong. ha ha
   */
  @Override
  public UnresolvedType getEnclosingType() {
    return thisType;
  }

  @Override
  public Var getThisVar() {
    // we should thorw if we don't have a this
    return new Var(getThisType().resolve(world));
  }

  @Override
  public Var getTargetVar() {
    if (!hasTarget())
      throw new RuntimeException("bad");
    return new Var(getTargetType().resolve(world));
  }

  @Override
  public Var getArgVar(int i) {
    return new Var(getArgType(i).resolve(world));
  }

  @Override
  public Var getThisEnclosingJoinPointStaticPartVar() {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Var getThisJoinPointStaticPartVar() {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Var getThisAspectInstanceVar(ResolvedType aspectType) {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Var getThisJoinPointVar() {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public ISourceLocation getSourceLocation() {
    throw new RuntimeException("unimplemented");
  }

  @Override
  public Member getEnclosingCodeSignature() {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getKindedAnnotationVar()
   */
  @Override
  public Var getKindedAnnotationVar(UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getWithinAnnotationVar()
   */
  @Override
  public Var getWithinAnnotationVar(UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getWithinCodeAnnotationVar()
   */
  @Override
  public Var getWithinCodeAnnotationVar(UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getThisAnnotationVar()
   */
  @Override
  public Var getThisAnnotationVar(UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getTargetAnnotationVar()
   */
  @Override
  public Var getTargetAnnotationVar(UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.aspectj.weaver.Shadow#getArgAnnotationVar(int)
   */
  @Override
  public Var getArgAnnotationVar(int i, UnresolvedType annotationType) {
    throw new RuntimeException("unimplemented");
  }

}
