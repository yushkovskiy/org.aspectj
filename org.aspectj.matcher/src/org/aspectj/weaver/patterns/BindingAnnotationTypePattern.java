/* *******************************************************************
 * Copyright (c) 2004 IBM Corporation.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * ******************************************************************/
package org.aspectj.weaver.patterns;

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageUtil;
import org.aspectj.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class BindingAnnotationTypePattern extends ExactAnnotationTypePattern implements BindingPattern {

  protected int formalIndex;

  /**
   * @param annotationType
   */
  public BindingAnnotationTypePattern(UnresolvedType annotationType, int index) {
    super(annotationType, null);
    this.formalIndex = index;
  }

  public BindingAnnotationTypePattern(FormalBinding binding) {
    this(binding.getType(), binding.getIndex());
  }

  public void resolveBinding(World world) {
    if (resolved) {
      return;
    }
    resolved = true;
    annotationType = annotationType.resolve(world);
    final ResolvedType resolvedAnnotationType = (ResolvedType) annotationType;
    if (!resolvedAnnotationType.isAnnotation()) {
      final IMessage m = MessageUtil.error(WeaverMessages.format(WeaverMessages.REFERENCE_TO_NON_ANNOTATION_TYPE, annotationType
          .getName()), getSourceLocation());
      world.getMessageHandler().handleMessage(m);
      resolved = false;
    }
    if (annotationType.isTypeVariableReference()) {
      return; // we'll deal with this next check when the type var is actually bound...
    }
    verifyRuntimeRetention(world, resolvedAnnotationType);
  }

  private void verifyRuntimeRetention(World world, ResolvedType resolvedAnnotationType) {
    if (!resolvedAnnotationType.isAnnotationWithRuntimeRetention()) { // default is class visibility
      // default is class visibility
      final IMessage m = MessageUtil.error(WeaverMessages.format(WeaverMessages.BINDING_NON_RUNTIME_RETENTION_ANNOTATION,
          annotationType.getName()), getSourceLocation());
      world.getMessageHandler().handleMessage(m);
      resolved = false;
    }
  }

  @Override
  public AnnotationTypePattern parameterizeWith(Map typeVariableMap, World w) {
    UnresolvedType newAnnotationType = annotationType;
    if (annotationType.isTypeVariableReference()) {
      final TypeVariableReference t = (TypeVariableReference) annotationType;
      final String key = t.getTypeVariable().getName();
      if (typeVariableMap.containsKey(key)) {
        newAnnotationType = (UnresolvedType) typeVariableMap.get(key);
      }
    } else if (annotationType.isParameterizedType()) {
      newAnnotationType = annotationType.parameterize(typeVariableMap);
    }
    final BindingAnnotationTypePattern ret = new BindingAnnotationTypePattern(newAnnotationType, this.formalIndex);
    if (newAnnotationType instanceof ResolvedType) {
      final ResolvedType rat = (ResolvedType) newAnnotationType;
      verifyRuntimeRetention(rat.getWorld(), rat);
    }
    ret.copyLocationFrom(this);
    return ret;
  }

  @Override
  public int getFormalIndex() {
    return formalIndex;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof BindingAnnotationTypePattern)) {
      return false;
    }
    final BindingAnnotationTypePattern btp = (BindingAnnotationTypePattern) obj;
    return (super.equals(btp) && (btp.formalIndex == formalIndex));
  }

  public int hashCode() {
    return super.hashCode() * 37 + formalIndex;
  }

  @Override
  public AnnotationTypePattern remapAdviceFormals(IntMap bindings) {
    if (!bindings.hasKey(formalIndex)) {
      return new ExactAnnotationTypePattern(annotationType, null);
    } else {
      final int newFormalIndex = bindings.get(formalIndex);
      return new BindingAnnotationTypePattern(annotationType, newFormalIndex);
    }
  }

  private static final byte VERSION = 1; // rev if serialised form changed

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    s.writeByte(AnnotationTypePattern.BINDING);
    s.writeByte(VERSION);
    annotationType.write(s);
    s.writeShort((short) formalIndex);
    writeLocation(s);
  }

  public static AnnotationTypePattern read(VersionedDataInputStream s, ISourceContext context) throws IOException {
    final byte version = s.readByte();
    if (version > VERSION) {
      throw new BCException("BindingAnnotationTypePattern was written by a more recent version of AspectJ");
    }
    final AnnotationTypePattern ret = new BindingAnnotationTypePattern(UnresolvedType.read(s), s.readShort());
    ret.readLocation(context, s);
    return ret;
  }
}
