/* *******************************************************************
 * Copyright (c) 2005 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement   promoted member type from AjState
 * ******************************************************************/
package org.aspectj.ajdt.internal.core.builder;

import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.aspectj.org.eclipse.jdt.internal.compiler.env.*;

/**
 * Used to determine if a type has structurally changed during incremental compilation. At the end of compilation we create one of
 * these objects using the bytes for the class about to be woven. On a subsequent incremental compile we compare the new form of the
 * class with a previously stored CompactTypeStructureRepresentation instance. A structural change will indicate we need to do
 * recompile other dependent types.
 */
public class CompactTypeStructureRepresentation implements IBinaryType {
  static char[][] NoInterface = CharOperation.NO_CHAR_CHAR;
  static IBinaryNestedType[] NoNestedType = new IBinaryNestedType[0];
  static IBinaryField[] NoField = new IBinaryField[0];
  static IBinaryMethod[] NoMethod = new IBinaryMethod[0];

  // this is the core state for comparison
  char[] className;
  int modifiers;
  char[] genericSignature;
  char[] superclassName;
  char[][] interfaces;

  char[] enclosingMethod;

  char[][][] missingTypeNames;

  // this is the extra state that enables us to be an IBinaryType
  char[] enclosingTypeName;
  boolean isLocal, isAnonymous, isMember;
  char[] sourceFileName;
  char[] fileName;
  char[] sourceName;
  long tagBits;
  boolean isBinaryType;
  IBinaryField[] binFields;
  IBinaryMethod[] binMethods;
  IBinaryNestedType[] memberTypes;
  IBinaryAnnotation[] annotations;


  public CompactTypeStructureRepresentation(ClassFileReader cfr, boolean isAspect) {

    this.enclosingTypeName = cfr.getEnclosingTypeName();
    this.isLocal = cfr.isLocal();
    this.isAnonymous = cfr.isAnonymous();
    this.isMember = cfr.isMember();
    this.sourceFileName = cfr.sourceFileName();
    this.fileName = cfr.getFileName();
    this.missingTypeNames = cfr.getMissingTypeNames();
    this.tagBits = cfr.getTagBits();
    this.enclosingMethod = cfr.getEnclosingMethod();
    this.isBinaryType = cfr.isBinaryType();
    this.binFields = cfr.getFields();
    if (binFields == null) {
      binFields = NoField;
    }
    this.binMethods = cfr.getMethods();
    if (binMethods == null) {
      binMethods = NoMethod;
    }
    // If we are an aspect we (for now) need to grab even the malformed inner type info as it
    // may be there because it refers to an ITD'd innertype. This needs to be improved - perhaps
    // using a real attribute against which memberTypes can be compared to see which are just
    // references and which were real declarations
    this.memberTypes = cfr.getMemberTypes(isAspect);
    this.annotations = cfr.getAnnotations();
    this.sourceName = cfr.getSourceName();
    this.className = cfr.getName(); // slashes...
    this.modifiers = cfr.getModifiers();
    this.genericSignature = cfr.getGenericSignature();
    // if (this.genericSignature.length == 0) {
    // this.genericSignature = null;
    // }
    this.superclassName = cfr.getSuperclassName(); // slashes...
    interfaces = cfr.getInterfaceNames();

  }

  @Override
  public char[][][] getMissingTypeNames() {
    return missingTypeNames;
  }

  @Override
  public char[] getEnclosingTypeName() {
    return enclosingTypeName;
  }

  @Override
  public int getModifiers() {
    return modifiers;
  }

  @Override
  public char[] getGenericSignature() {
    return genericSignature;
  }

  @Override
  public char[] getEnclosingMethod() {
    return enclosingMethod;
  }

  @Override
  public char[][] getInterfaceNames() {
    return interfaces;
  }

  @Override
  public boolean isAnonymous() {
    return isAnonymous;
  }

  @Override
  public char[] sourceFileName() {
    return sourceFileName;
  }

  @Override
  public boolean isLocal() {
    return isLocal;
  }

  @Override
  public boolean isMember() {
    return isMember;
  }

  @Override
  public char[] getSuperclassName() {
    return superclassName;
  }

  @Override
  public char[] getFileName() {
    return fileName;
  }

  @Override
  public char[] getName() {
    return className;
  }

  @Override
  public long getTagBits() {
    return tagBits;
  }

  @Override
  public boolean isBinaryType() {
    return isBinaryType;
  }

  @Override
  public IBinaryField[] getFields() {
    return binFields;
  }

  @Override
  public IBinaryMethod[] getMethods() {
    return binMethods;
  }

  @Override
  public IBinaryNestedType[] getMemberTypes() {
    return memberTypes;
  }

  @Override
  public IBinaryAnnotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public char[] getSourceName() {
    return sourceName;
  }

}