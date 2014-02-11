/* *******************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *   Adrian Colyer			Initial implementation
 * ******************************************************************/
package org.aspectj.weaver.reflect;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.aspectj.bridge.IMessageHandler;
import org.aspectj.weaver.ReferenceType;
import org.aspectj.weaver.ResolvedMember;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.bcel.BcelWorld;

public class ReflectionBasedReferenceTypeDelegateTest extends TestCase {

  protected ReflectionWorld world;
  private ResolvedType objectType;
  private ResolvedType classType;

  public void testIsAspect() {
    assertFalse(objectType.isAspect());
  }

  public void testIsAnnotationStyleAspect() {
    assertFalse(objectType.isAnnotationStyleAspect());
  }

  public void testIsInterface() {
    assertFalse(objectType.isInterface());
    assertTrue(world.resolve("java.io.Serializable").isInterface());
  }

  public void testIsEnum() {
    assertFalse(objectType.isEnum());
  }

  public void testIsAnnotation() {
    assertFalse(objectType.isAnnotation());
  }

  public void testIsAnnotationWithRuntimeRetention() {
    assertFalse(objectType.isAnnotationWithRuntimeRetention());
  }

  public void testIsClass() {
    assertTrue(objectType.isClass());
    assertFalse(world.resolve("java.io.Serializable").isClass());
  }

  public void testIsGeneric() {
    assertFalse(objectType.isGenericType());
  }

  public void testIsExposedToWeaver() {
    assertFalse(objectType.isExposedToWeaver());
  }

  public void testHasAnnotation() {
    assertFalse(objectType.hasAnnotation(UnresolvedType.forName("Foo")));
  }

  public void testGetAnnotations() {
    assertEquals("no entries", 0, objectType.getAnnotations().length);
  }

  public void testGetAnnotationTypes() {
    assertEquals("no entries", 0, objectType.getAnnotationTypes().length);
  }

  public void testGetTypeVariables() {
    assertEquals("no entries", 0, objectType.getTypeVariables().length);
  }

  public void testGetPerClause() {
    assertNull(objectType.getPerClause());
  }

  public void testGetModifiers() {
    assertEquals(Object.class.getModifiers(), objectType.getModifiers());
  }

  public void testGetSuperclass() {
    assertTrue("Superclass of object should be null, but it is: " + objectType.getSuperclass(),
        objectType.getSuperclass() == null);
    assertEquals(objectType, world.resolve("java.lang.Class").getSuperclass());
    final ResolvedType d = world.resolve("reflect.tests.D");
    assertEquals(world.resolve("reflect.tests.C"), d.getSuperclass());
  }

  public void testArrayArgsSig() throws Exception {
    final Method invokeMethod = Method.class.getMethod("invoke", new Class[]{Object.class, Object[].class});
    final ResolvedMember reflectionMethod = ReflectionBasedReferenceTypeDelegateFactory.createResolvedMethod(invokeMethod, world);
    final String exp = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
    assertTrue("Expected: \n" + exp + "\n but got:\n" + reflectionMethod.getSignature(), reflectionMethod.getSignature()
        .equals(exp));
  }

  protected int findMethod(String name, ResolvedMember[] methods) {
    for (int i = 0; i < methods.length; i++) {
      if (name.equals(methods[i].getName())) {
        return i;
      }
    }
    return -1;
  }

  protected int findMethod(String name, int numArgs, ResolvedMember[] methods) {
    for (int i = 0; i < methods.length; i++) {
      if (name.equals(methods[i].getName()) && (methods[i].getParameterTypes().length == numArgs)) {
        return i;
      }
    }
    return -1;
  }

  public void testGetDeclaredMethods() {
    ResolvedMember[] methods = objectType.getDeclaredMethods();
    assertEquals(Object.class.getDeclaredMethods().length + Object.class.getDeclaredConstructors().length, methods.length);

    final ResolvedType c = world.resolve("reflect.tests.C");
    methods = c.getDeclaredMethods();
    assertEquals(3, methods.length);
    final int idx = findMethod("foo", methods);
    assertTrue(idx > -1);

    assertEquals(world.resolve("java.lang.String"), methods[idx].getReturnType());
    assertEquals(1, methods[idx].getParameterTypes().length);
    assertEquals(objectType, methods[idx].getParameterTypes()[0]);
    assertEquals(1, methods[idx].getExceptions().length);
    assertEquals(world.resolve("java.lang.Exception"), methods[idx].getExceptions()[0]);
    final int baridx = findMethod("bar", methods);
    final int initidx = findMethod("<init>", methods);
    assertTrue(baridx > -1);
    assertTrue(initidx > -1);
    assertTrue(baridx != initidx && baridx != idx && idx <= 2 && initidx <= 2 && baridx <= 2);

    final ResolvedType d = world.resolve("reflect.tests.D");
    methods = d.getDeclaredMethods();
    assertEquals(2, methods.length);

    classType = world.resolve("java.lang.Class");
    methods = classType.getDeclaredMethods();
    assertEquals(Class.class.getDeclaredMethods().length + Class.class.getDeclaredConstructors().length, methods.length);
  }

  public void testGetDeclaredFields() {
    ResolvedMember[] fields = objectType.getDeclaredFields();
    assertEquals(0, fields.length);

    final ResolvedType c = world.resolve("reflect.tests.C");
    fields = c.getDeclaredFields();

    assertEquals(2, fields.length);
    assertEquals("f", fields[0].getName());
    assertEquals("s", fields[1].getName());
    assertEquals(UnresolvedType.INT, fields[0].getReturnType());
    assertEquals(world.resolve("java.lang.String"), fields[1].getReturnType());
  }

  public void testGetDeclaredInterfaces() {
    ResolvedType[] interfaces = objectType.getDeclaredInterfaces();
    assertEquals(0, interfaces.length);

    final ResolvedType d = world.resolve("reflect.tests.D");
    interfaces = d.getDeclaredInterfaces();
    assertEquals(1, interfaces.length);
    assertEquals(world.resolve("java.io.Serializable"), interfaces[0]);
  }

  public void testGetDeclaredPointcuts() {
    final ResolvedMember[] pointcuts = objectType.getDeclaredPointcuts();
    assertEquals(0, pointcuts.length);
  }

  public void testSerializableSuperclass() {
    final ResolvedType serializableType = world.resolve("java.io.Serializable");
    final ResolvedType superType = serializableType.getSuperclass();
    assertTrue("Superclass of serializable should be Object but was " + superType, superType.equals(UnresolvedType.OBJECT));

    final BcelWorld bcelworld = new BcelWorld();
    bcelworld.setBehaveInJava5Way(true);
    final ResolvedType bcelSupertype = bcelworld.resolve(UnresolvedType.SERIALIZABLE).getSuperclass();
    assertTrue("Should be null but is " + bcelSupertype, bcelSupertype.equals(UnresolvedType.OBJECT));
  }

  public void testSubinterfaceSuperclass() {
    final ResolvedType ifaceType = world.resolve("java.security.Key");
    final ResolvedType superType = ifaceType.getSuperclass();
    assertTrue("Superclass should be Object but was " + superType, superType.equals(UnresolvedType.OBJECT));

    final BcelWorld bcelworld = new BcelWorld();
    bcelworld.setBehaveInJava5Way(true);
    final ResolvedType bcelSupertype = bcelworld.resolve("java.security.Key").getSuperclass();
    assertTrue("Should be null but is " + bcelSupertype, bcelSupertype.equals(UnresolvedType.OBJECT));
  }

  public void testVoidSuperclass() {
    final ResolvedType voidType = world.resolve(Void.TYPE);
    final ResolvedType superType = voidType.getSuperclass();
    assertNull(superType);

    final BcelWorld bcelworld = new BcelWorld();
    bcelworld.setBehaveInJava5Way(true);
    final ResolvedType bcelSupertype = bcelworld.resolve("void").getSuperclass();
    assertTrue("Should be null but is " + bcelSupertype, bcelSupertype == null);
  }

  public void testIntSuperclass() {
    final ResolvedType voidType = world.resolve(Integer.TYPE);
    final ResolvedType superType = voidType.getSuperclass();
    assertNull(superType);

    final BcelWorld bcelworld = new BcelWorld();
    bcelworld.setBehaveInJava5Way(true);
    final ResolvedType bcelSupertype = bcelworld.resolve("int").getSuperclass();
    assertTrue("Should be null but is " + bcelSupertype, bcelSupertype == null);
  }

  public void testGenericInterfaceSuperclass_BcelWorldResolution() {
    final BcelWorld bcelworld = new BcelWorld();
    bcelworld.setBehaveInJava5Way(true);

    final UnresolvedType javaUtilMap = UnresolvedType.forName("java.util.Map");

    final ReferenceType rawType = (ReferenceType) bcelworld.resolve(javaUtilMap);
    assertTrue("Should be the raw type ?!? " + rawType.getTypekind(), rawType.isRawType());

    final ReferenceType genericType = (ReferenceType) rawType.getGenericType();
    assertTrue("Should be the generic type ?!? " + genericType.getTypekind(), genericType.isGenericType());

    final ResolvedType rt = rawType.getSuperclass();
    assertTrue("Superclass for Map raw type should be Object but was " + rt, rt.equals(UnresolvedType.OBJECT));

    final ResolvedType rt2 = genericType.getSuperclass();
    assertTrue("Superclass for Map generic type should be Object but was " + rt2, rt2.equals(UnresolvedType.OBJECT));
  }

  // FIXME asc maybe. The reflection list of methods returned doesn't include <clinit> (the static initializer) ... is that really
  // a problem.
  public void testCompareSubclassDelegates() {

    final boolean barfIfClinitMissing = false;
    world.setBehaveInJava5Way(true);

    final BcelWorld bcelWorld = new BcelWorld(getClass().getClassLoader(), IMessageHandler.THROW, null);
    bcelWorld.setBehaveInJava5Way(true);
    final UnresolvedType javaUtilHashMap = UnresolvedType.forName("java.util.HashMap");
    final ReferenceType rawType = (ReferenceType) bcelWorld.resolve(javaUtilHashMap);

    final ReferenceType rawReflectType = (ReferenceType) world.resolve(javaUtilHashMap);
    final ResolvedMember[] rms1 = rawType.getDelegate().getDeclaredMethods();
    final ResolvedMember[] rms2 = rawReflectType.getDelegate().getDeclaredMethods();
    final StringBuilder errors = new StringBuilder();
    final Set one = new HashSet();
    for (int i = 0; i < rms1.length; i++) {
      one.add(rms1[i].toString());
    }
    final Set two = new HashSet();
    for (int i = 0; i < rms2.length; i++) {
      two.add(rms2[i].toString());
    }
    for (int i = 0; i < rms2.length; i++) {
      if (!one.contains(rms2[i].toString())) {
        errors.append("Couldn't find " + rms2[i].toString() + " in the bcel set\n");
      }
    }
    for (int i = 0; i < rms1.length; i++) {
      if (!two.contains(rms1[i].toString())) {
        if (!barfIfClinitMissing && rms1[i].getName().equals("<clinit>"))
          continue;
        errors.append("Couldn't find " + rms1[i].toString() + " in the reflection set\n");
      }
    }
    assertTrue("Errors:" + errors.toString(), errors.length() == 0);

    // the good old ibm vm seems to offer clinit through its reflection support (see pr145322)
    if (rms1.length == rms2.length)
      return;
    if (barfIfClinitMissing) {
      // the numbers must be exact
      assertEquals(rms1.length, rms2.length);
    } else {
      // the numbers can be out by one in favour of bcel
      if (rms1.length != (rms2.length + 1)) {
        for (int i = 0; i < rms1.length; i++) {
          System.err.println("bcel" + i + " is " + rms1[i]);
        }
        for (int i = 0; i < rms2.length; i++) {
          System.err.println("refl" + i + " is " + rms2[i]);
        }
      }
      assertTrue("Should be one extra (clinit) in BCEL case, but bcel=" + rms1.length + " reflect=" + rms2.length,
          rms1.length == rms2.length + 1);
    }
  }

  // todo: array of int

  protected void setUp() throws Exception {
    world = new ReflectionWorld(getClass().getClassLoader());
    objectType = world.resolve("java.lang.Object");
  }
}
