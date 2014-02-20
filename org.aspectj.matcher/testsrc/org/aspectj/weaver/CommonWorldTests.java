/* *******************************************************************
 * Copyright (c) 2002-2008 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 *     Andy Clement 
 * ******************************************************************/

package org.aspectj.weaver;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.aspectj.testing.util.TestUtil;

/**
 * An abstract set of tests that any World implementation should be able to pass. To run it against your World, subclass it and
 * implement getWorld().
 *
 * @author Andy Clement
 */
public abstract class CommonWorldTests extends TestCase {

  /**
   * @return an instance of the World to be tested
   */
  protected abstract World getWorld();

  private World world;

  @Override
  public void setUp() {
    world = getWorld();
  }

  private final UnresolvedType[] primitiveTypes = UnresolvedType.forSignatures(new String[]{"B", "S", "C", "I", "J", "F", "D",
      "V"});

  public void testPrimitiveTypes() {
    final ResolvedType[] primitives = world.resolve(primitiveTypes);
    for (int i = 0, len = primitives.length; i < len; i++) {
      final ResolvedType ty = primitives[i];
      modifiersTest(ty, Modifier.PUBLIC | Modifier.FINAL);
      fieldsTest(ty, ResolvedMember.NONE);
      methodsTest(ty, ResolvedMember.NONE);
      interfacesTest(ty, ResolvedType.NONE);
      superclassTest(ty, null);
      pointcutsTest(ty, ResolvedMember.NONE);
      isInterfaceTest(ty, false);
      isClassTest(ty, false);
      isAspectTest(ty, false);
      for (int j = 0; j < len; j++) {
        final ResolvedType ty1 = primitives[j];
        if (ty.equals(ty1)) {
          isCoerceableFromTest(ty, ty1, true);
        } else if (ty.equals(UnresolvedType.BOOLEAN) || ty1.equals(UnresolvedType.BOOLEAN)
            || ty.equals(UnresolvedType.VOID) || ty1.equals(UnresolvedType.VOID)) {
          isCoerceableFromTest(ty, ty1, false);
        } else {
          isCoerceableFromTest(ty, ty1, true);
        }
      }

      // Result of this depends on whether autoboxing is supported
      // isCoerceableFromTest(ty, UnresolvedType.OBJECT, getSupportsAutoboxing());

      primAssignTest("B", new String[]{});
      primAssignTest("S", new String[]{"B"});
      primAssignTest("C", new String[]{"B"});
      primAssignTest("I", new String[]{"B", "S", "C"});
      primAssignTest("J", new String[]{"B", "S", "C", "I"});
      primAssignTest("F", new String[]{"B", "S", "C", "I", "J"});
      primAssignTest("D", new String[]{"B", "S", "C", "I", "J", "F"});
      primAssignTest("Z", new String[]{});
      primAssignTest("V", new String[]{});

    }
  }

  private void primAssignTest(String sig, String[] lowers) {
    final ResolvedType[] primitives = world.resolve(primitiveTypes);
    final UnresolvedType tx = UnresolvedType.forSignature(sig);
    final ResolvedType ty = world.resolve(tx, true);
    assertTrue("Couldnt find type " + tx, !ty.isMissing());
    final ResolvedType[] lowerTyArray = world.resolve(UnresolvedType.forSignatures(lowers));
    final List<ResolvedType> lowerTys = new ArrayList<ResolvedType>(Arrays.asList(lowerTyArray));
    lowerTys.add(ty);
    final Set<ResolvedType> allLowerTys = new HashSet<ResolvedType>(lowerTys);
    final Set<ResolvedType> allUpperTys = new HashSet<ResolvedType>(Arrays.asList(primitives));
    allUpperTys.removeAll(allLowerTys);

    for (ResolvedType other : allLowerTys) {
      isAssignableFromTest(ty, other, true);
    }
    for (ResolvedType other : allUpperTys) {
      isAssignableFromTest(ty, other, false);
    }
  }

  public void testPrimitiveArrays() {
    final ResolvedType[] primitives = world.resolve(primitiveTypes);
    for (int i = 0, len = primitives.length; i < len; i++) {
      final ResolvedType ty = primitives[i];
      UnresolvedType tx = UnresolvedType.forSignature("[" + ty.getSignature());
      final ResolvedType aty = world.resolve(tx, true);
      assertTrue("Couldnt find type " + tx, !aty.isMissing());
      modifiersTest(aty, Modifier.PUBLIC | Modifier.FINAL);
      fieldsTest(aty, ResolvedMember.NONE);
      methodsTest(aty, ResolvedMember.NONE);
      interfaceTest(
          aty,
          new ResolvedType[]{world.getCoreType(UnresolvedType.CLONEABLE),
              world.getCoreType(UnresolvedType.SERIALIZABLE)});
      superclassTest(aty, UnresolvedType.OBJECT);

      pointcutsTest(aty, ResolvedMember.NONE);
      isInterfaceTest(aty, false);
      isClassTest(aty, false);
      isAspectTest(aty, false);
      for (int j = 0; j < len; j++) {
        final ResolvedType ty1 = primitives[j];
        isCoerceableFromTest(aty, ty1, false);
        tx = UnresolvedType.forSignature("[" + ty1.getSignature());
        final ResolvedType aty1 = getWorld().resolve(tx, true);
        assertTrue("Couldnt find type " + tx, !aty1.isMissing());
        if (ty.equals(ty1)) {
          isCoerceableFromTest(aty, aty1, true);
          isAssignableFromTest(aty, aty1, true);
        } else {
          isCoerceableFromTest(aty, aty1, false);
          isAssignableFromTest(aty, aty1, false);
        }
      }
    }
    // double dimension arrays
    for (int i = 0, len = primitives.length; i < len; i++) {
      final ResolvedType ty = primitives[i];
      UnresolvedType tx = UnresolvedType.forSignature("[[" + ty.getSignature());
      final ResolvedType aty = world.resolve(tx, true);
      assertTrue("Couldnt find type " + tx, !aty.isMissing());
      modifiersTest(aty, Modifier.PUBLIC | Modifier.FINAL);
      fieldsTest(aty, ResolvedMember.NONE);
      methodsTest(aty, ResolvedMember.NONE);
      interfaceTest(
          aty,
          new ResolvedType[]{world.getCoreType(UnresolvedType.CLONEABLE),
              world.getCoreType(UnresolvedType.SERIALIZABLE)});
      superclassTest(aty, UnresolvedType.OBJECT);

      pointcutsTest(aty, ResolvedMember.NONE);
      isInterfaceTest(aty, false);
      isClassTest(aty, false);
      isAspectTest(aty, false);
      for (int j = 0; j < len; j++) {
        final ResolvedType ty1 = primitives[j];
        isCoerceableFromTest(aty, ty1, false);
        tx = UnresolvedType.forSignature("[[" + ty1.getSignature());
        final ResolvedType aty1 = getWorld().resolve(tx, true);
        assertTrue("Couldnt find type " + tx, !aty1.isMissing());
        if (ty.equals(ty1)) {
          isCoerceableFromTest(aty, aty1, true);
          isAssignableFromTest(aty, aty1, true);
        } else {
          isCoerceableFromTest(aty, aty1, false);
          isAssignableFromTest(aty, aty1, false);
        }
      }
    }
  }

  // ---- tests for parts of ResolvedType objects

  protected static void modifiersTest(ResolvedType ty, int mods) {
    assertEquals(ty + " modifiers:", Modifier.toString(mods), Modifier.toString(ty.getModifiers()));
  }

  protected static void fieldsTest(ResolvedType ty, Member[] x) {
    TestUtil.assertSetEquals(ty + " fields:", x, ty.getDeclaredJavaFields());
  }

  protected static void methodsTest(ResolvedType ty, Member[] x) {
    TestUtil.assertSetEquals(ty + " methods:", x, ty.getDeclaredJavaMethods());
  }

  protected static void mungersTest(ResolvedType ty, ShadowMunger[] x) {
    final List l = (List) ty.getDeclaredShadowMungers();
    final ShadowMunger[] array = (ShadowMunger[]) l.toArray(new ShadowMunger[l.size()]);
    TestUtil.assertSetEquals(ty + " mungers:", x, array);
  }

  protected static void interfaceTest(ResolvedType type, ResolvedType[] expectedInterfaces) {
    final ResolvedType[] interfaces = type.getDeclaredInterfaces();
    for (int i = 0; i < expectedInterfaces.length; i++) {
      boolean wasMissing = true;
      for (int j = 0; j < interfaces.length; j++) {
        if (interfaces[j].getSignature().equals(expectedInterfaces[i].getSignature())) {
          wasMissing = false;
        }
      }
      if (wasMissing) {
        fail("Expected declared interface " + expectedInterfaces[i] + " but it wasn't found in "
            + Arrays.asList(interfaces));
      }
    }
  }

  protected static void interfacesTest(ResolvedType ty, ResolvedType[] x) {
    TestUtil.assertArrayEquals(ty + " interfaces:", x, ty.getDeclaredInterfaces());
  }

  protected static void superclassTest(ResolvedType ty, UnresolvedType x) {
    assertEquals(ty + " superclass:", x, ty.getSuperclass());
  }

  protected static void pointcutsTest(ResolvedType ty, Member[] x) {
    TestUtil.assertSetEquals(ty + " pointcuts:", x, ty.getDeclaredPointcuts());
  }

  protected static void isInterfaceTest(ResolvedType ty, boolean x) {
    assertEquals(ty + " is interface:", x, ty.isInterface());
  }

  protected static void isAspectTest(ResolvedType ty, boolean x) {
    assertEquals(ty + " is aspect:", x, ty.isAspect());
  }

  protected static void isClassTest(ResolvedType ty, boolean x) {
    assertEquals(ty + " is class:", x, ty.isClass());
  }

  protected void isCoerceableFromTest(UnresolvedType ty0, UnresolvedType ty1, boolean x) {
    assertEquals(ty0 + " is coerceable from " + ty1, ty0.resolve(world).isCoerceableFrom(ty1.resolve(world)), x);
    assertEquals(ty1 + " is coerceable from " + ty0, ty1.resolve(world).isCoerceableFrom(ty0.resolve(world)), x);
  }

  protected void isAssignableFromTest(UnresolvedType ty0, UnresolvedType ty1, boolean x) {
    final ResolvedType rty0 = ty0.resolve(world);
    final ResolvedType rty1 = ty1.resolve(world);
    final boolean result = rty0.isAssignableFrom(rty1);
    assertEquals(ty0 + " is assignable from " + ty1, result, x);
  }

  // ---- tests for parts of ResolvedMethod objects

  protected static void modifiersTest(ResolvedMember m, int mods) {
    assertEquals(m + " modifiers:", Modifier.toString(mods), Modifier.toString(m.getModifiers()));
  }

  protected static void exceptionsTest(ResolvedMember m, UnresolvedType[] exns) {
    TestUtil.assertSetEquals(m + " exceptions:", exns, m.getExceptions());
  }

}
