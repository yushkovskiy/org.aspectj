/* *******************************************************************
 * Copyright (c) 2004 IBM
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement -     initial implementation 
 * ******************************************************************/

package org.aspectj.apache.bcel.classfile.tests;

import java.io.File;
import java.io.IOException;

import org.aspectj.apache.bcel.classfile.Attribute;
import org.aspectj.apache.bcel.classfile.ConstantPool;
import org.aspectj.apache.bcel.classfile.EnclosingMethod;
import org.aspectj.apache.bcel.classfile.JavaClass;
import org.aspectj.apache.bcel.util.SyntheticRepository;

public class EnclosingMethodAttributeTest extends BcelTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Verify for an inner class declared inside the 'main' method that the enclosing method attribute is set correctly.
   */
  public void testCheckMethodLevelNamedInnerClass() throws ClassNotFoundException {
    final SyntheticRepository repos = createRepos("testcode.jar");
    final JavaClass clazz = repos.loadClass("AttributeTestClassEM01$1S");
    final ConstantPool pool = clazz.getConstantPool();
    final Attribute[] encMethodAttrs = findAttribute("EnclosingMethod", clazz);
    assertTrue("Expected 1 EnclosingMethod attribute but found " + encMethodAttrs.length, encMethodAttrs.length == 1);
    final EnclosingMethod em = (EnclosingMethod) encMethodAttrs[0];
    final String enclosingClassName = em.getEnclosingClass().getClassname(pool);
    final String enclosingMethodName = em.getEnclosingMethod().getName(pool);
    assertTrue("Expected class name to be 'AttributeTestClassEM01' but was " + enclosingClassName, enclosingClassName
        .equals("AttributeTestClassEM01"));
    assertTrue("Expected method name to be 'main' but was " + enclosingMethodName, enclosingMethodName.equals("main"));
  }

  /**
   * Verify for an inner class declared at the type level that the EnclosingMethod attribute is set correctly (i.e. to a null
   * value)
   */
  public void testCheckClassLevelNamedInnerClass() throws ClassNotFoundException {
    final SyntheticRepository repos = createRepos("testcode.jar");
    final JavaClass clazz = repos.loadClass("AttributeTestClassEM02$1");
    final ConstantPool pool = clazz.getConstantPool();
    final Attribute[] encMethodAttrs = findAttribute("EnclosingMethod", clazz);
    assertTrue("Expected 1 EnclosingMethod attribute but found " + encMethodAttrs.length, encMethodAttrs.length == 1);
    final EnclosingMethod em = (EnclosingMethod) encMethodAttrs[0];
    final String enclosingClassName = em.getEnclosingClass().getClassname(pool);
    assertTrue("The class is not within a method, so method_index should be null, but it is " + em.getEnclosingMethodIndex(),
        em.getEnclosingMethodIndex() == 0);
    assertTrue("Expected class name to be 'AttributeTestClassEM02' but was " + enclosingClassName, enclosingClassName
        .equals("AttributeTestClassEM02"));
  }

  /**
   * Check that we can save and load the attribute correctly.
   */
  public void testAttributeSerializtion() throws ClassNotFoundException, IOException {
    // Read in the class
    final SyntheticRepository repos = createRepos("testcode.jar");
    final JavaClass clazz = repos.loadClass("AttributeTestClassEM02$1");
    final ConstantPool pool = clazz.getConstantPool();
    final Attribute[] encMethodAttrs = findAttribute("EnclosingMethod", clazz);
    assertTrue("Expected 1 EnclosingMethod attribute but found " + encMethodAttrs.length, encMethodAttrs.length == 1);

    // Write it out
    final File tfile = createTestdataFile("AttributeTestClassEM02$1.class");
    clazz.dump(tfile);

    // Read in the new version and check it is OK
    final SyntheticRepository repos2 = createRepos(".");
    final JavaClass clazz2 = repos2.loadClass("AttributeTestClassEM02$1");
    final EnclosingMethod em = (EnclosingMethod) encMethodAttrs[0];
    final String enclosingClassName = em.getEnclosingClass().getClassname(pool);
    assertTrue("The class is not within a method, so method_index should be null, but it is " + em.getEnclosingMethodIndex(),
        em.getEnclosingMethodIndex() == 0);
    assertTrue("Expected class name to be 'AttributeTestClassEM02' but was " + enclosingClassName, enclosingClassName
        .equals("AttributeTestClassEM02"));
    assertTrue(tfile.delete());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
