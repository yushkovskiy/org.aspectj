/* *******************************************************************
 * Copyright (c) 2004, 2013 IBM, VMware
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement -     initial implementation {date}
 * ******************************************************************/

package org.aspectj.apache.bcel.classfile.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.aspectj.apache.bcel.classfile.Attribute;
import org.aspectj.apache.bcel.classfile.ConstantPool;
import org.aspectj.apache.bcel.classfile.Field;
import org.aspectj.apache.bcel.classfile.JavaClass;
import org.aspectj.apache.bcel.classfile.Method;
import org.aspectj.apache.bcel.classfile.annotation.AnnotationGen;
import org.aspectj.apache.bcel.classfile.annotation.NameValuePair;
import org.aspectj.apache.bcel.classfile.annotation.ElementValue;
import org.aspectj.apache.bcel.classfile.annotation.SimpleElementValue;
import org.aspectj.apache.bcel.generic.ObjectType;
import org.aspectj.apache.bcel.util.ClassPath;
import org.aspectj.apache.bcel.util.SyntheticRepository;

/**
 * Super class for the Java5 tests, includes various helper methods.
 */

public class BcelTestCase extends TestCase {

  private final boolean verbose = false;

  protected File createTestdataFile(String name) {
    return new File("testdata" + File.separator + name);
  }

  protected JavaClass getClassFromJar(String clazzname) throws ClassNotFoundException {
    final SyntheticRepository repos = createRepos("testcode.jar");
    return repos.loadClass(clazzname);
  }

  protected JavaClass getClassFromJava8Jar(String clazzname) throws ClassNotFoundException {
    final SyntheticRepository repos = createRepos("java8testcode.jar");
    return repos.loadClass(clazzname);
  }

  protected Method getMethod(JavaClass cl, String methodname) {
    final Method[] methods = cl.getMethods();
    for (int i = 0; i < methods.length; i++) {
      final Method m = methods[i];
      if (m.getName().equals(methodname)) {
        return m;
      }
    }
    return null;
  }

  protected Field getField(JavaClass cl, String fieldname) {
    final Field[] fields = cl.getFields();
    for (int i = 0; i < fields.length; i++) {
      final Field f = fields[i];
      if (f.getName().equals(fieldname)) {
        return f;
      }
    }
    return null;
  }

  protected boolean wipe(String name) {
    return new File("testdata" + File.separator + name).delete();
  }

  protected boolean wipe(String dir, String name) {
    final boolean b = wipe(dir + File.separator + name);
    final String[] files = new File(dir).list();
    if (files == null || files.length == 0) {
      new File(dir).delete(); // Why does this not succeed? stupid thing
    }
    return b;
  }

  public SyntheticRepository createRepos(String cpentry) {
    final ClassPath cp = new ClassPath("testdata" + File.separator + cpentry + File.pathSeparator
        + System.getProperty("java.class.path"));
    return SyntheticRepository.getInstance(cp);
  }

  protected Attribute[] findAttribute(String name, JavaClass clazz) {
    final Attribute[] all = clazz.getAttributes();
    final List<Attribute> chosenAttrsList = new ArrayList<Attribute>();
    for (int i = 0; i < all.length; i++) {
      if (verbose)
        System.err.println("Attribute: " + all[i].getName());
      if (all[i].getName().equals(name))
        chosenAttrsList.add(all[i]);
    }
    return chosenAttrsList.toArray(new Attribute[]{});
  }

  protected Attribute findAttribute(String name, Attribute[] all) {
    final List<Attribute> chosenAttrsList = new ArrayList<Attribute>();
    for (int i = 0; i < all.length; i++) {
      if (verbose)
        System.err.println("Attribute: " + all[i].getName());
      if (all[i].getName().equals(name))
        chosenAttrsList.add(all[i]);
    }
    assertTrue("Should be one match: " + chosenAttrsList.size(), chosenAttrsList.size() == 1);
    return chosenAttrsList.get(0);
  }

  protected String dumpAnnotations(AnnotationGen[] as) {
    final StringBuilder result = new StringBuilder();
    result.append("[");
    for (int i = 0; i < as.length; i++) {
      final AnnotationGen annotation = as[i];
      result.append(annotation.toShortString());
      if (i + 1 < as.length)
        result.append(",");
    }
    result.append("]");
    return result.toString();
  }

  protected String dumpAnnotations(List<AnnotationGen> as) {
    final StringBuilder result = new StringBuilder();
    result.append("[");
    for (int i = 0; i < as.size(); i++) {
      final AnnotationGen annotation = as.get(i);
      result.append(annotation.toShortString());
      if (i + 1 < as.size())
        result.append(",");
    }
    result.append("]");
    return result.toString();
  }

  protected String dumpAttributes(Attribute[] as) {
    final StringBuilder result = new StringBuilder();
    result.append("AttributeArray:[");
    for (int i = 0; i < as.length; i++) {
      final Attribute attr = as[i];
      result.append(attr.toString());
      if (i + 1 < as.length)
        result.append(",");
    }
    result.append("]");
    return result.toString();
  }

  public AnnotationGen createFruitAnnotation(ConstantPool cp, String aFruit, boolean visibility) {
    final SimpleElementValue evg = new SimpleElementValue(ElementValue.STRING, cp, aFruit);
    final NameValuePair nvGen = new NameValuePair("fruit", evg, cp);
    final ObjectType t = new ObjectType("SimpleStringAnnotation");
    final List<NameValuePair> elements = new ArrayList<NameValuePair>();
    elements.add(nvGen);
    return new AnnotationGen(t, elements, visibility, cp);
  }

  public Attribute getAttribute(Attribute[] attrs, byte tag) {
    for (Attribute attr : attrs) {
      if (attr.getTag() == tag) {
        return attr;
      }
    }
    return null;
  }

}
