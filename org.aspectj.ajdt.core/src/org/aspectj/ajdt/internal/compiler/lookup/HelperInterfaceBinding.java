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


package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.org.eclipse.jdt.internal.compiler.ClassFile;
import org.aspectj.org.eclipse.jdt.internal.compiler.CompilationResult;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.ResolvedMember;
import org.aspectj.weaver.UnresolvedType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HelperInterfaceBinding extends SourceTypeBinding {
  private UnresolvedType typeX;
  SourceTypeBinding enclosingType;
  List methods = new ArrayList();

  public HelperInterfaceBinding(SourceTypeBinding enclosingType, UnresolvedType typeX) {
    super();
    this.fPackage = enclosingType.fPackage;
    //this.fileName = scope.referenceCompilationUnit().getFileName();
    this.modifiers = ClassFileConstants.AccPublic | ClassFileConstants.AccInterface | ClassFileConstants.AccAbstract;
    this.sourceName = enclosingType.scope.referenceContext.name;
    this.enclosingType = enclosingType;
    this.typeX = typeX;
    this.typeVariables = Binding.NO_TYPE_VARIABLES;
    this.scope = enclosingType.scope;
    this.superInterfaces = new ReferenceBinding[0];
  }

  public HelperInterfaceBinding(
      char[][] compoundName,
      PackageBinding fPackage,
      ClassScope scope) {
    super(compoundName, fPackage, scope);
  }

  @Override
  public char[] getFileName() {
    return enclosingType.getFileName();
  }

  public UnresolvedType getTypeX() {
    return typeX;
  }

  public void addMethod(EclipseFactory world, ResolvedMember member) {
    final MethodBinding binding = world.makeMethodBinding(member);
    this.methods.add(binding);
  }

  @Override
  @NotNull
  public FieldBinding[] fields() {
    return new FieldBinding[0];
  }

  @Override
  @NotNull
  public MethodBinding[] methods() {
    return new MethodBinding[0];
  }

  @Override
  @NotNull
  public char[] constantPoolName() {
    final String sig = typeX.getSignature();
    return sig.substring(1, sig.length() - 1).toCharArray();
  }

  public void generateClass(CompilationResult result, ClassFile enclosingClassFile) {
    final ClassFile classFile = new ClassFile(this);
    classFile.initialize(this, enclosingClassFile, false);
    classFile.recordInnerClasses(this);

    //classFile.addFieldInfos();
    classFile.contents[classFile.contentsOffset++] = (byte) 0;
    classFile.contents[classFile.contentsOffset++] = (byte) 0;

    classFile.setForMethodInfos();
    for (final Iterator i = methods.iterator(); i.hasNext(); ) {
      final MethodBinding b = (MethodBinding) i.next();
      generateMethod(classFile, b);
    }

    classFile.addAttributes();

    result.record(this.constantPoolName(), classFile);
  }


  private static void generateMethod(ClassFile classFile, MethodBinding binding) {
    classFile.generateMethodInfoHeader(binding);
    final int methodAttributeOffset = classFile.contentsOffset;
    final int attributeNumber = classFile.generateMethodInfoAttributes(binding);
    classFile.completeMethodInfo(binding, methodAttributeOffset, attributeNumber);
  }


  @Override
  public ReferenceBinding[] superInterfaces() {
    return new ReferenceBinding[0];
  }

}
