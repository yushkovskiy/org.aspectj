/*******************************************************************************
 * Copyright (c) 2004 IBM All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License
 * v1.0 which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Andy Clement - initial implementation
 ******************************************************************************/

package org.aspectj.apache.bcel.classfile.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aspectj.apache.bcel.Constants;
import org.aspectj.apache.bcel.classfile.ConstantPool;
import org.aspectj.apache.bcel.classfile.JavaClass;
import org.aspectj.apache.bcel.classfile.Method;
import org.aspectj.apache.bcel.classfile.annotation.AnnotationElementValue;
import org.aspectj.apache.bcel.classfile.annotation.AnnotationGen;
import org.aspectj.apache.bcel.classfile.annotation.ArrayElementValue;
import org.aspectj.apache.bcel.classfile.annotation.NameValuePair;
import org.aspectj.apache.bcel.classfile.annotation.ElementValue;
import org.aspectj.apache.bcel.classfile.annotation.SimpleElementValue;
import org.aspectj.apache.bcel.generic.ArrayType;
import org.aspectj.apache.bcel.generic.ClassGen;
import org.aspectj.apache.bcel.generic.InstructionBranch;
import org.aspectj.apache.bcel.generic.InstructionConstants;
import org.aspectj.apache.bcel.generic.InstructionFactory;
import org.aspectj.apache.bcel.generic.InstructionHandle;
import org.aspectj.apache.bcel.generic.InstructionList;
import org.aspectj.apache.bcel.generic.LocalVariableGen;
import org.aspectj.apache.bcel.generic.MethodGen;
import org.aspectj.apache.bcel.generic.ObjectType;
import org.aspectj.apache.bcel.generic.Type;
import org.aspectj.apache.bcel.util.SyntheticRepository;

/**
 * The program that some of the tests generate looks like this: public class HelloWorld { public static void main(String[] argv) {
 * BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); String name = null; try { name = "Andy"; }
 * catch(IOException e) { return; } System.out.println("Hello, " + name); } }
 */
public class GeneratingAnnotatedClassesTest extends BcelTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /*
   * Steps in the test: 1) Programmatically construct the HelloWorld program 2) Add two simple annotations at the class level 3)
   * Save the class to disk 4) Reload the class using the 'static' variant of the BCEL classes 5) Check the attributes are OK
   */
  public void testGenerateClassLevelAnnotations() throws ClassNotFoundException {

    // Create HelloWorld
    final ClassGen cg = createClassGen("HelloWorld");
    final ConstantPool cp = cg.getConstantPool();
    final InstructionList il = new InstructionList();

    cg.addAnnotation(createSimpleVisibleAnnotation(cp));
    cg.addAnnotation(createSimpleInvisibleAnnotation(cp));

    buildClassContents(cg, cp, il);

    dumpClass(cg, "HelloWorld.class");

    final JavaClass jc = getClassFrom(".", "HelloWorld");

    final AnnotationGen[] as = jc.getAnnotations();
    assertTrue("Should be two annotations but found " + as.length, as.length == 2);
    final AnnotationGen one = as[0];
    final AnnotationGen two = as[1];
    assertTrue("Name of annotation 1 should be SimpleAnnotation but it is " + as[0].getTypeName(), as[0].getTypeName().equals(
        "SimpleAnnotation"));
    assertTrue("Name of annotation 2 should be SimpleAnnotation but it is " + as[1].getTypeName(), as[1].getTypeName().equals(
        "SimpleAnnotation"));
    final List<NameValuePair> vals = as[0].getValues();
    final NameValuePair nvp = vals.get(0);
    assertTrue("Name of element in SimpleAnnotation should be 'id' but it is " + nvp.getNameString(), nvp.getNameString()
        .equals("id"));
    final ElementValue ev = nvp.getValue();
    assertTrue("Type of element value should be int but it is " + ev.getElementValueType(),
        ev.getElementValueType() == ElementValue.PRIMITIVE_INT);
    assertTrue("Value of element should be 4 but it is " + ev.stringifyValue(), ev.stringifyValue().equals("4"));
    assertTrue(createTestdataFile("HelloWorld.class").delete());
  }

  /**
   * Just check that we can dump a class that has a method annotation on it and it is still there when we read it back in
   */
  public void testGenerateMethodLevelAnnotations1() throws ClassNotFoundException {
    // Create HelloWorld
    final ClassGen cg = createClassGen("HelloWorld");
    final ConstantPool cp = cg.getConstantPool();
    final InstructionList il = new InstructionList();

    buildClassContentsWithAnnotatedMethods(cg, cp, il);

    // Check annotation is OK
    int i = cg.getMethods()[0].getAnnotations().length;
    assertTrue("Prior to dumping, main method should have 1 annotation but has " + i, i == 1);

    dumpClass(cg, "temp1" + File.separator + "HelloWorld.class");

    final JavaClass jc2 = getClassFrom("temp1", "HelloWorld");

    // Check annotation is OK
    i = jc2.getMethods()[0].getAnnotations().length;
    assertTrue("JavaClass should say 1 annotation on main method but says " + i, i == 1);

    final ClassGen cg2 = new ClassGen(jc2);

    // Check it now it is a ClassGen
    final Method[] m = cg2.getMethods();
    i = m[0].getAnnotations().length;
    assertTrue("The main 'Method' should have one annotation but has " + i, i == 1);
    final MethodGen mg = new MethodGen(m[0], cg2.getClassName(), cg2.getConstantPool());

    // Check it finally when the Method is changed to a MethodGen
    i = mg.getAnnotations().size();
    assertTrue("The main 'MethodGen' should have one annotation but has " + i, i == 1);

    assertTrue(wipe("temp1" + File.separator + "HelloWorld.class"));

  }

  /**
   * Going further than the last test - when we reload the method back in, let's change it (adding a new annotation) and then
   * store that, read it back in and verify both annotations are there !
   */
  public void testGenerateMethodLevelAnnotations2() throws ClassNotFoundException {
    // Create HelloWorld
    final ClassGen cg = createClassGen("HelloWorld");
    final ConstantPool cp = cg.getConstantPool();
    final InstructionList il = new InstructionList();

    buildClassContentsWithAnnotatedMethods(cg, cp, il);

    dumpClass(cg, "temp2", "HelloWorld.class");

    final JavaClass jc2 = getClassFrom("temp2", "HelloWorld");

    final ClassGen cg2 = new ClassGen(jc2);

    // Main method after reading the class back in
    final Method mainMethod1 = jc2.getMethods()[0];
    assertTrue("The 'Method' should have one annotations but has " + mainMethod1.getAnnotations().length, mainMethod1
        .getAnnotations().length == 1);

    final MethodGen mainMethod2 = new MethodGen(mainMethod1, cg2.getClassName(), cg2.getConstantPool());

    assertTrue("The 'MethodGen' should have one annotations but has " + mainMethod2.getAnnotations().size(), mainMethod2
        .getAnnotations().size() == 1);

    mainMethod2.addAnnotation(createFruitAnnotation(cg2.getConstantPool(), "Pear"));

    cg2.removeMethod(mainMethod1);
    cg2.addMethod(mainMethod2.getMethod());

    dumpClass(cg2, "temp3", "HelloWorld.class");

    final JavaClass jc3 = getClassFrom("temp3", "HelloWorld");

    final ClassGen cg3 = new ClassGen(jc3);

    final Method mainMethod3 = cg3.getMethods()[1];
    final int i = mainMethod3.getAnnotations().length;
    assertTrue("The 'Method' should now have two annotations but has " + i, i == 2);

    assertTrue(wipe("temp2", "HelloWorld.class"));
    assertTrue(wipe("temp3", "HelloWorld.class"));
  }

  // J5TODO: Need to add deleteFile calls to many of these tests

  /**
   * Transform simple class from an immutable to a mutable object.
   */
  public void testTransformClassToClassGen_SimpleTypes() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "SimpleAnnotatedClass");
    final ClassGen cgen = new ClassGen(jc);

    // Check annotations are correctly preserved
    final AnnotationGen[] annotations = cgen.getAnnotations();
    assertTrue("Expected one annotation but found " + annotations.length, annotations.length == 1);
  }

  /**
   * Transform simple class from an immutable to a mutable object. The class is annotated with an annotation that uses an enum.
   */
  public void testTransformClassToClassGen_EnumType() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "AnnotatedWithEnumClass");
    final ClassGen cgen = new ClassGen(jc);

    // Check annotations are correctly preserved
    final AnnotationGen[] annotations = cgen.getAnnotations();
    assertTrue("Expected one annotation but found " + annotations.length, annotations.length == 1);
  }

  /**
   * Transform simple class from an immutable to a mutable object. The class is annotated with an annotation that uses an array of
   * SimpleAnnotations.
   */
  public void testTransformClassToClassGen_ArrayAndAnnotationTypes() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "AnnotatedWithCombinedAnnotation");
    final ClassGen cgen = new ClassGen(jc);

    // Check annotations are correctly preserved
    final AnnotationGen[] annotations = cgen.getAnnotations();
    assertTrue("Expected one annotation but found " + annotations.length, annotations.length == 1);
    final AnnotationGen a = annotations[0];
    assertTrue("That annotation should only have one value but has " + a.getValues().size(), a.getValues().size() == 1);
    final NameValuePair nvp = a.getValues().get(0);
    final ElementValue value = nvp.getValue();
    assertTrue("Value should be ArrayElementValueGen but is " + value, value instanceof ArrayElementValue);
    final ArrayElementValue arrayValue = (ArrayElementValue) value;
    assertTrue("Array value should be size one but is " + arrayValue.getElementValuesArraySize(), arrayValue
        .getElementValuesArraySize() == 1);
    final ElementValue innerValue = arrayValue.getElementValuesArray()[0];
    assertTrue("Value in the array should be AnnotationElementValueGen but is " + innerValue,
        innerValue instanceof AnnotationElementValue);
    final AnnotationElementValue innerAnnotationValue = (AnnotationElementValue) innerValue;
    assertTrue("Should be called LSimpleAnnotation; but is called: " + innerAnnotationValue.getAnnotation().getTypeName(),
        innerAnnotationValue.getAnnotation().getTypeSignature().equals("LSimpleAnnotation;"));
  }

  /**
   * Transform complex class from an immutable to a mutable object.
   */
  public void testTransformComplexClassToClassGen() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "ComplexAnnotatedClass");
    final ClassGen cgen = new ClassGen(jc);

    // Check annotations are correctly preserved
    final AnnotationGen[] annotations = cgen.getAnnotations();
    assertTrue("Expected one annotation but found " + annotations.length, annotations.length == 1);
    final List<NameValuePair> l = annotations[0].getValues();
    boolean found = false;
    for (final Iterator<NameValuePair> iter = l.iterator(); iter.hasNext(); ) {
      final NameValuePair element = iter.next();
      if (element.getNameString().equals("dval")) {
        if (((SimpleElementValue) element.getValue()).stringifyValue().equals("33.4"))
          found = true;
      }
    }
    assertTrue("Did not find double annotation value with value 33.4", found);
  }

  /**
   * Load a class in and modify it with a new attribute - A SimpleAnnotation annotation
   */
  public void testModifyingClasses1() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "SimpleAnnotatedClass");
    final ClassGen cgen = new ClassGen(jc);
    final ConstantPool cp = cgen.getConstantPool();
    cgen.addAnnotation(createFruitAnnotation(cp, "Pineapple"));
    assertTrue("Should now have two annotations but has " + cgen.getAnnotations().length, cgen.getAnnotations().length == 2);
    dumpClass(cgen, "SimpleAnnotatedClass.class");
    assertTrue(wipe("SimpleAnnotatedClass.class"));
  }

  /**
   * Load a class in and modify it with a new attribute - A ComplexAnnotation annotation
   */
  public void testModifyingClasses2() throws ClassNotFoundException {
    final JavaClass jc = getClassFrom("testcode.jar", "SimpleAnnotatedClass");
    final ClassGen cgen = new ClassGen(jc);
    final ConstantPool cp = cgen.getConstantPool();
    cgen.addAnnotation(createCombinedAnnotation(cp));
    assertTrue("Should now have two annotations but has " + cgen.getAnnotations().length, cgen.getAnnotations().length == 2);
    dumpClass(cgen, "SimpleAnnotatedClass.class");
    final JavaClass jc2 = getClassFrom(".", "SimpleAnnotatedClass");
    jc2.getAnnotations();
    assertTrue(wipe("SimpleAnnotatedClass.class"));
    // System.err.println(jc2.toString());
  }

  private void dumpClass(ClassGen cg, String fname) {
    try {
      final File f = createTestdataFile(fname);
      cg.getJavaClass().dump(f);
    } catch (java.io.IOException e) {
      System.err.println(e);
    }
  }

  private void dumpClass(ClassGen cg, String dir, String fname) {
    dumpClass(cg, dir + File.separator + fname);
  }

  private void buildClassContentsWithAnnotatedMethods(ClassGen cg, ConstantPool cp, InstructionList il) {
    // Create method 'public static void main(String[]argv)'
    final MethodGen mg = createMethodGen("main", il, cp);
    final InstructionFactory factory = new InstructionFactory(cg);
    mg.addAnnotation(createSimpleVisibleAnnotation(mg.getConstantPool()));
    // We now define some often used types:

    final ObjectType i_stream = new ObjectType("java.io.InputStream");
    final ObjectType p_stream = new ObjectType("java.io.PrintStream");

    // Create variables in and name : We call the constructors, i.e.,
    // execute BufferedReader(InputStreamReader(System.in)) . The reference
    // to the BufferedReader object stays on top of the stack and is stored
    // in the newly allocated in variable.

    il.append(factory.createNew("java.io.BufferedReader"));
    il.append(InstructionConstants.DUP); // Use predefined constant
    il.append(factory.createNew("java.io.InputStreamReader"));
    il.append(InstructionConstants.DUP);
    il.append(factory.createFieldAccess("java.lang.System", "in", i_stream, Constants.GETSTATIC));
    il.append(factory.createInvoke("java.io.InputStreamReader", "<init>", Type.VOID, new Type[]{i_stream},
        Constants.INVOKESPECIAL));
    il.append(factory.createInvoke("java.io.BufferedReader", "<init>", Type.VOID,
        new Type[]{new ObjectType("java.io.Reader")}, Constants.INVOKESPECIAL));

    LocalVariableGen lg = mg.addLocalVariable("in", new ObjectType("java.io.BufferedReader"), null, null);
    final int in = lg.getIndex();
    lg.setStart(il.append(InstructionFactory.createASTORE(in))); // "in" valid from here

    // Create local variable name and initialize it to null

    lg = mg.addLocalVariable("name", Type.STRING, null, null);
    final int name = lg.getIndex();
    il.append(InstructionConstants.ACONST_NULL);
    lg.setStart(il.append(InstructionFactory.createASTORE(name))); // "name" valid from here

    // Create try-catch block: We remember the start of the block, read a
    // line from the standard input and store it into the variable name .

    // InstructionHandle try_start = il.append(factory.createFieldAccess(
    // "java.lang.System", "out", p_stream, Constants.GETSTATIC));

    // il.append(new PUSH(cp, "Please enter your name> "));
    // il.append(factory.createInvoke("java.io.PrintStream", "print",
    // Type.VOID, new Type[] { Type.STRING },
    // Constants.INVOKEVIRTUAL));
    // il.append(new ALOAD(in));
    // il.append(factory.createInvoke("java.io.BufferedReader", "readLine",
    // Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
    final InstructionHandle try_start = il.append(InstructionFactory.PUSH(cp, "Andy"));
    il.append(InstructionFactory.createASTORE(name));

    // Upon normal execution we jump behind exception handler, the target
    // address is not known yet.

    final InstructionBranch g = new InstructionBranch(Constants.GOTO);
    final InstructionHandle try_end = il.append(g);

    // We add the exception handler which simply returns from the method.

    final LocalVariableGen var_ex = mg.addLocalVariable("ex", Type.getType("Ljava.io.IOException;"), null, null);
    final int var_ex_slot = var_ex.getIndex();

    final InstructionHandle handler = il.append(InstructionFactory.createASTORE(var_ex_slot));
    var_ex.setStart(handler);
    var_ex.setEnd(il.append(InstructionConstants.RETURN));

    mg.addExceptionHandler(try_start, try_end, handler, new ObjectType("java.io.IOException"));

    // "Normal" code continues, now we can set the branch target of the GOTO
    // .

    final InstructionHandle ih = il.append(factory.createFieldAccess("java.lang.System", "out", p_stream, Constants.GETSTATIC));
    g.setTarget(ih);

    // Printing "Hello": String concatenation compiles to StringBuffer
    // operations.

    il.append(factory.createNew(Type.STRINGBUFFER));
    il.append(InstructionConstants.DUP);
    il.append(InstructionFactory.PUSH(cp, "Hello, "));
    il.append(factory.createInvoke("java.lang.StringBuffer", "<init>", Type.VOID, new Type[]{Type.STRING},
        Constants.INVOKESPECIAL));
    il.append(InstructionFactory.createALOAD(name));
    il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[]{Type.STRING},
        Constants.INVOKEVIRTUAL));
    il.append(factory.createInvoke("java.lang.StringBuffer", "toString", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

    il.append(factory.createInvoke("java.io.PrintStream", "println", Type.VOID, new Type[]{Type.STRING},
        Constants.INVOKEVIRTUAL));
    il.append(InstructionConstants.RETURN);

    // Finalization: Finally, we have to set the stack size, which normally
    // would have to be computed on the fly and add a default constructor
    // method to the class, which is empty in this case.

    mg.setMaxStack();
    mg.setMaxLocals();
    cg.addMethod(mg.getMethod());
    il.dispose(); // Allow instruction handles to be reused
    cg.addEmptyConstructor(Constants.ACC_PUBLIC);
  }

  private void buildClassContents(ClassGen cg, ConstantPool cp, InstructionList il) {
    // Create method 'public static void main(String[]argv)'
    final MethodGen mg = createMethodGen("main", il, cp);
    final InstructionFactory factory = new InstructionFactory(cg);
    // We now define some often used types:

    final ObjectType i_stream = new ObjectType("java.io.InputStream");
    final ObjectType p_stream = new ObjectType("java.io.PrintStream");

    // Create variables in and name : We call the constructors, i.e.,
    // execute BufferedReader(InputStreamReader(System.in)) . The reference
    // to the BufferedReader object stays on top of the stack and is stored
    // in the newly allocated in variable.

    il.append(factory.createNew("java.io.BufferedReader"));
    il.append(InstructionConstants.DUP); // Use predefined constant
    il.append(factory.createNew("java.io.InputStreamReader"));
    il.append(InstructionConstants.DUP);
    il.append(factory.createFieldAccess("java.lang.System", "in", i_stream, Constants.GETSTATIC));
    il.append(factory.createInvoke("java.io.InputStreamReader", "<init>", Type.VOID, new Type[]{i_stream},
        Constants.INVOKESPECIAL));
    il.append(factory.createInvoke("java.io.BufferedReader", "<init>", Type.VOID,
        new Type[]{new ObjectType("java.io.Reader")}, Constants.INVOKESPECIAL));

    LocalVariableGen lg = mg.addLocalVariable("in", new ObjectType("java.io.BufferedReader"), null, null);
    final int in = lg.getIndex();
    lg.setStart(il.append(InstructionFactory.createASTORE(in))); // "in" valid from here

    // Create local variable name and initialize it to null

    lg = mg.addLocalVariable("name", Type.STRING, null, null);
    final int name = lg.getIndex();
    il.append(InstructionConstants.ACONST_NULL);
    lg.setStart(il.append(InstructionFactory.createASTORE(name))); // "name" valid from here

    // Create try-catch block: We remember the start of the block, read a
    // line from the standard input and store it into the variable name .

    // InstructionHandle try_start = il.append(factory.createFieldAccess(
    // "java.lang.System", "out", p_stream, Constants.GETSTATIC));

    // il.append(new PUSH(cp, "Please enter your name> "));
    // il.append(factory.createInvoke("java.io.PrintStream", "print",
    // Type.VOID, new Type[] { Type.STRING },
    // Constants.INVOKEVIRTUAL));
    // il.append(new ALOAD(in));
    // il.append(factory.createInvoke("java.io.BufferedReader", "readLine",
    // Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
    final InstructionHandle try_start = il.append(InstructionFactory.PUSH(cp, "Andy"));
    il.append(InstructionFactory.createASTORE(name));

    // Upon normal execution we jump behind exception handler, the target
    // address is not known yet.

    final InstructionBranch g = new InstructionBranch(Constants.GOTO);
    final InstructionHandle try_end = il.append(g);

    // We add the exception handler which simply returns from the method.

    final LocalVariableGen var_ex = mg.addLocalVariable("ex", Type.getType("Ljava.io.IOException;"), null, null);
    final int var_ex_slot = var_ex.getIndex();

    final InstructionHandle handler = il.append(InstructionFactory.createASTORE(var_ex_slot));
    var_ex.setStart(handler);
    var_ex.setEnd(il.append(InstructionConstants.RETURN));

    mg.addExceptionHandler(try_start, try_end, handler, new ObjectType("java.io.IOException"));

    // "Normal" code continues, now we can set the branch target of the GOTO
    // .

    final InstructionHandle ih = il.append(factory.createFieldAccess("java.lang.System", "out", p_stream, Constants.GETSTATIC));
    g.setTarget(ih);

    // Printing "Hello": String concatenation compiles to StringBuffer
    // operations.

    il.append(factory.createNew(Type.STRINGBUFFER));
    il.append(InstructionConstants.DUP);
    il.append(InstructionFactory.PUSH(cp, "Hello, "));
    il.append(factory.createInvoke("java.lang.StringBuffer", "<init>", Type.VOID, new Type[]{Type.STRING},
        Constants.INVOKESPECIAL));
    il.append(InstructionFactory.createALOAD(name));
    il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[]{Type.STRING},
        Constants.INVOKEVIRTUAL));
    il.append(factory.createInvoke("java.lang.StringBuffer", "toString", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

    il.append(factory.createInvoke("java.io.PrintStream", "println", Type.VOID, new Type[]{Type.STRING},
        Constants.INVOKEVIRTUAL));
    il.append(InstructionConstants.RETURN);

    // Finalization: Finally, we have to set the stack size, which normally
    // would have to be computed on the fly and add a default constructor
    // method to the class, which is empty in this case.

    mg.setMaxStack();
    mg.setMaxLocals();
    cg.addMethod(mg.getMethod());
    il.dispose(); // Allow instruction handles to be reused
    cg.addEmptyConstructor(Constants.ACC_PUBLIC);
  }

  private JavaClass getClassFrom(String where, String clazzname) throws ClassNotFoundException {
    final SyntheticRepository repos = createRepos(where);
    return repos.loadClass(clazzname);
  }

  // helper methods

  private ClassGen createClassGen(String classname) {
    return new ClassGen(classname, "java.lang.Object", "<generated>", Constants.ACC_PUBLIC | Constants.ACC_SUPER, null);
  }

  private MethodGen createMethodGen(String methodname, InstructionList il, ConstantPool cp) {
    return new MethodGen(Constants.ACC_STATIC | Constants.ACC_PUBLIC, // access flags
        Type.VOID, // return type
        new Type[]{new ArrayType(Type.STRING, 1)}, // argument types
        new String[]{"argv"}, // arg names
        methodname, "HelloWorld", // method, class
        il, cp);
  }

  public AnnotationGen createSimpleVisibleAnnotation(ConstantPool cp) {
    final SimpleElementValue evg = new SimpleElementValue(ElementValue.PRIMITIVE_INT, cp, 4);

    final NameValuePair nvGen = new NameValuePair("id", evg, cp);

    final ObjectType t = new ObjectType("SimpleAnnotation");

    final List<NameValuePair> elements = new ArrayList<NameValuePair>();
    elements.add(nvGen);

    final AnnotationGen a = new AnnotationGen(t, elements, true, cp);
    return a;
  }

  public AnnotationGen createFruitAnnotation(ConstantPool cp, String aFruit) {
    final SimpleElementValue evg = new SimpleElementValue(ElementValue.STRING, cp, aFruit);
    final NameValuePair nvGen = new NameValuePair("fruit", evg, cp);
    final ObjectType t = new ObjectType("SimpleStringAnnotation");
    final List<NameValuePair> elements = new ArrayList<NameValuePair>();
    elements.add(nvGen);
    return new AnnotationGen(t, elements, true, cp);
  }

  public AnnotationGen createCombinedAnnotation(ConstantPool cp) {
    // Create an annotation instance
    final AnnotationGen a = createSimpleVisibleAnnotation(cp);
    final ArrayElementValue array = new ArrayElementValue(cp);
    array.addElement(new AnnotationElementValue(a, cp));
    final NameValuePair nvp = new NameValuePair("value", array, cp);
    final List<NameValuePair> elements = new ArrayList<NameValuePair>();
    elements.add(nvp);
    return new AnnotationGen(new ObjectType("CombinedAnnotation"), elements, true, cp);
  }

  public AnnotationGen createSimpleInvisibleAnnotation(ConstantPool cp) {
    final SimpleElementValue evg = new SimpleElementValue(ElementValue.PRIMITIVE_INT, cp, 4);

    final NameValuePair nvGen = new NameValuePair("id", evg, cp);

    final ObjectType t = new ObjectType("SimpleAnnotation");

    final List<NameValuePair> elements = new ArrayList<NameValuePair>();
    elements.add(nvGen);

    final AnnotationGen a = new AnnotationGen(t, elements, false, cp);
    return a;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

}