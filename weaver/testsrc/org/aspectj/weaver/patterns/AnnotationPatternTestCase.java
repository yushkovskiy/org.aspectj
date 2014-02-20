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

import junit.framework.TestCase;

import org.aspectj.bridge.AbortException;
import org.aspectj.util.LangUtil;
import org.aspectj.weaver.AnnotatedElement;
import org.aspectj.weaver.AnnotationAJ;
import org.aspectj.weaver.BcweaverTests;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.bcel.BcelWorld;

public class AnnotationPatternTestCase extends TestCase {

  public void testParseSimpleAnnotationPattern() {
    final PatternParser p = new PatternParser("@Foo");
    AnnotationTypePattern foo = p.maybeParseAnnotationPattern();
    foo = foo.resolveBindings(makeSimpleScope(), new Bindings(3), true);
    assertTrue("ExactAnnotationTypePattern", foo instanceof ExactAnnotationTypePattern);
    assertEquals("Foo", UnresolvedType.forSignature("LFoo;"), ((ExactAnnotationTypePattern) foo).annotationType);
  }

  public void testParseAndAnnotationPattern() {
    final PatternParser p = new PatternParser("@Foo @Goo");
    AnnotationTypePattern fooAndGoo = p.maybeParseAnnotationPattern();
    assertTrue("AndAnnotationTypePattern", fooAndGoo instanceof AndAnnotationTypePattern);
    assertEquals("@(Foo) @(Goo)", fooAndGoo.toString());
    fooAndGoo = fooAndGoo.resolveBindings(makeSimpleScope(), new Bindings(3), true);
    assertEquals("@Foo @Goo", fooAndGoo.toString());
    final AnnotationTypePattern left = ((AndAnnotationTypePattern) fooAndGoo).getLeft();
    final AnnotationTypePattern right = ((AndAnnotationTypePattern) fooAndGoo).getRight();
    assertEquals("Foo", UnresolvedType.forSignature("LFoo;"), ((ExactAnnotationTypePattern) left).annotationType);
    assertEquals("Goo", UnresolvedType.forSignature("LGoo;"), ((ExactAnnotationTypePattern) right).annotationType);
  }

  //
  // public void testParseOrAnnotationPattern() {
  // PatternParser p = new PatternParser("@Foo || @Goo");
  // AnnotationTypePattern fooOrGoo = p.parseAnnotationTypePattern();
  // assertTrue("OrAnnotationTypePattern",fooOrGoo instanceof
  // OrAnnotationTypePattern);
  // assertEquals("(@Foo || @Goo)",fooOrGoo.toString());
  // AnnotationTypePattern left =
  // ((OrAnnotationTypePattern)fooOrGoo).getLeft();
  // AnnotationTypePattern right =
  // ((OrAnnotationTypePattern)fooOrGoo).getRight();
  // assertEquals("Foo",UnresolvedType.forName("Foo"),((
  // ExactAnnotationTypePattern)left).annotationType);
  // assertEquals("Goo",UnresolvedType.forName("Goo"),((
  // ExactAnnotationTypePattern)right).annotationType);
  // }
  //
  public void testParseNotAnnotationPattern() {
    final PatternParser p = new PatternParser("!@Foo");
    AnnotationTypePattern notFoo = p.maybeParseAnnotationPattern();
    assertTrue("NotAnnotationTypePattern", notFoo instanceof NotAnnotationTypePattern);
    notFoo = notFoo.resolveBindings(makeSimpleScope(), new Bindings(3), true);
    assertEquals("!@Foo", notFoo.toString());
    final AnnotationTypePattern body = ((NotAnnotationTypePattern) notFoo).getNegatedPattern();
    assertEquals("Foo", UnresolvedType.forName("Foo"), ((ExactAnnotationTypePattern) body).annotationType);
  }

  public void testParseBracketedAnnotationPattern() {
    final PatternParser p = new PatternParser("(@Foo)");
    final AnnotationTypePattern foo = p.maybeParseAnnotationPattern();
    // cannot start with ( so, we get ANY
    assertEquals("ANY", AnnotationTypePattern.ANY, foo);
  }

  public void testParseFQAnnPattern() {
    final PatternParser p = new PatternParser("@org.aspectj.Foo");
    final AnnotationTypePattern foo = p.maybeParseAnnotationPattern();
    assertEquals("@(org.aspectj.Foo)", foo.toString());
  }

  public void testParseComboPattern() {
    // PatternParser p = new PatternParser("!((@Foo || @Goo) && !@Boo)");
    final PatternParser p = new PatternParser("@(Foo || Goo)!@Boo");
    AnnotationTypePattern ap = p.maybeParseAnnotationPattern();
    ap = ap.resolveBindings(makeSimpleScope(), new Bindings(3), true);
    final AndAnnotationTypePattern atp = (AndAnnotationTypePattern) ap;
    final NotAnnotationTypePattern notBoo = (NotAnnotationTypePattern) atp.getRight();
    // ExactAnnotationTypePattern boo = (ExactAnnotationTypePattern)
    notBoo.getNegatedPattern();
    // AnnotationTypePattern fooOrGoo = (AnnotationTypePattern)
    atp.getLeft();
    assertEquals("@((Foo || Goo)) !@Boo", ap.toString());
  }

  // public void testParseAndOrPattern() {
  // PatternParser p = new PatternParser("@Foo && @Boo || @Goo");
  // AnnotationTypePattern andOr = p.parseAnnotationTypePattern();
  // assertTrue("Should be or pattern",andOr instanceof
  // OrAnnotationTypePattern);
  // }
  //
  public void testParseBadPattern() {
    final PatternParser p = new PatternParser("@@Foo");
    try {
      p.maybeParseAnnotationPattern();
      fail("ParserException expected");
    } catch (ParserException pEx) {
      assertEquals("name pattern", pEx.getMessage());
    }
  }

  public void testParseBadPattern2() {
    final PatternParser p = new PatternParser("Foo");
    final AnnotationTypePattern bad = p.maybeParseAnnotationPattern();
    assertEquals("ANY", AnnotationTypePattern.ANY, bad);
  }

  public void testParseNameOrVarAnnotationPattern() {
    final PatternParser p = new PatternParser("Foo");
    final AnnotationTypePattern foo = p.parseAnnotationNameOrVarTypePattern();
    assertTrue("ExactAnnotationTypePattern expected", foo != null);
    assertEquals("Foo", UnresolvedType.forName("Foo"), ((ExactAnnotationTypePattern) foo).annotationType);
  }

  public void testParseNameOrVarAnnotationPatternWithNot() {
    final PatternParser p = new PatternParser("!@Foo");
    try {
      // AnnotationTypePattern bad =
      p.parseAnnotationNameOrVarTypePattern();
      fail("ParserException expected");
    } catch (ParserException pEx) {
      assertEquals("identifier", pEx.getMessage());
    }
  }

  public void testParseNameOrVarAnnotationPatternWithOr() {
    final PatternParser p = new PatternParser("Foo || Boo");
    final AnnotationTypePattern foo = p.parseAnnotationNameOrVarTypePattern();
    // rest of pattern not consumed...
    assertTrue("ExactAnnotationTypePattern", foo instanceof ExactAnnotationTypePattern);
    assertEquals("Foo", UnresolvedType.forName("Foo"), ((ExactAnnotationTypePattern) foo).annotationType);
  }

  public void testParseNameOrVarAnnotationWithBinding() {
    final PatternParser p = new PatternParser("foo");
    final AnnotationTypePattern foo = p.parseAnnotationNameOrVarTypePattern();
    assertTrue("ExactAnnotationTypePattern", foo instanceof ExactAnnotationTypePattern);
    assertEquals("@foo", ((ExactAnnotationTypePattern) foo).toString());
  }

  public void testParseNameOrVarAnnotationPatternWithAnd() {
    final PatternParser p = new PatternParser("Foo Boo");
    final AnnotationTypePattern foo = p.parseAnnotationNameOrVarTypePattern();
    // rest of pattern not consumed...
    assertEquals("@Foo", foo.toString());
  }

  public void testMaybeParseAnnotationPattern() {
    PatternParser p = new PatternParser("@Foo");
    AnnotationTypePattern a = p.maybeParseAnnotationPattern();
    assertNotNull("Should find annotation pattern", a);
    p = new PatternParser("Foo && Boo");
    a = p.maybeParseAnnotationPattern();
    assertEquals("Should be ANY pattern for a non-match", AnnotationTypePattern.ANY, a);
  }

  public void testParseTypePatternsWithAnnotations() {
    final PatternParser p = new PatternParser("@Foo *");
    final TypePattern t = p.parseTypePattern();
    assertTrue("AnyWithAnnotationTypePattern", t instanceof AnyWithAnnotationTypePattern);
    final AnnotationTypePattern atp = t.annotationPattern;
    assertEquals("@(Foo)", atp.toString());
    assertEquals("(@(Foo) *)", t.toString());
  }

  public void testParseTypePatternsWithAnnotationsComplex() {
    final PatternParser p = new PatternParser("(@(Foo || Boo) (Foo || Boo))");
    final TypePattern t = p.parseTypePattern();
    assertTrue("OrTypePattern", t instanceof OrTypePattern);
    assertEquals("((@((Foo || Boo)) Foo) || (@((Foo || Boo)) Boo))", t.toString());
  }

  public void testNotSyntax() {
    final PatternParser p = new PatternParser("!@Foo (Foo || Boo))");
    final TypePattern t = p.parseTypePattern();
    assertTrue("OrTypePattern", t instanceof OrTypePattern);
    assertEquals("((!@(Foo) Foo) || (!@(Foo) Boo))", t.toString());
  }

  public void testParseMethodOrConstructorSigNoAP() {
    final PatternParser p = new PatternParser("* *.*(..)");
    final SignaturePattern s = p.parseMethodOrConstructorSignaturePattern();
    assertEquals("Any annotation", AnnotationTypePattern.ANY, s.getAnnotationPattern());
    assertEquals("Any return", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("* *.*(..)", s.toString());
  }

  public void testParseMethodOrConstructorSigSimpleAP() {
    final PatternParser p = new PatternParser("@Foo * *.*(..)");
    final SignaturePattern s = p.parseMethodOrConstructorSignaturePattern();
    assertEquals("@(Foo) annotation", "@(Foo)", s.getAnnotationPattern().toString());
    assertEquals("Any return", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("@(Foo) * *.*(..)", s.toString());
  }

  public void testParseMethodOrConstructorSigComplexAP() {
    final PatternParser p = new PatternParser("!@(Foo || Goo) * *.*(..)");
    final SignaturePattern s = p.parseMethodOrConstructorSignaturePattern();
    assertEquals("complex annotation", "!@((Foo || Goo))", s.getAnnotationPattern().toString());
    assertEquals("Any return", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("!@((Foo || Goo)) * *.*(..)", s.toString());
  }

  public void testParseMethodFieldSigNoAP() {
    final PatternParser p = new PatternParser("* *.*");
    final SignaturePattern s = p.parseFieldSignaturePattern();
    assertEquals("Any annotation", AnnotationTypePattern.ANY, s.getAnnotationPattern());
    assertEquals("Any field type", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("* *.*", s.toString());
  }

  public void testParseFieldSigSimpleAP() {
    final PatternParser p = new PatternParser("@Foo * *.*");
    final SignaturePattern s = p.parseFieldSignaturePattern();
    assertEquals("@Foo annotation", "@(Foo)", s.getAnnotationPattern().toString());
    assertEquals("Any field type", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("@(Foo) * *.*", s.toString());
  }

  public void testParseFieldSigComplexAP() {
    final PatternParser p = new PatternParser("!@(Foo || Goo) * *.*");
    final SignaturePattern s = p.parseFieldSignaturePattern();
    assertEquals("complex annotation", "!@((Foo || Goo))", s.getAnnotationPattern().toString());
    assertEquals("Any field type", "*", s.getReturnType().toString());
    assertEquals("Any dec type", "*", s.getDeclaringType().toString());
    assertEquals("Any name", "*", s.getName().toString());
    assertEquals("!@((Foo || Goo)) * *.*", s.toString());
  }

  public void testExactAnnotationPatternMatching() {
    if (LangUtil.is15VMOrGreater()) {
      final PatternParser p = new PatternParser("@Foo");
      AnnotationTypePattern ap = p.maybeParseAnnotationPattern();
      ap = ap.resolveBindings(makeSimpleScope(), new Bindings(3), true);
      final AnnotatedElementImpl ae = new AnnotatedElementImpl(new String[]{"Foo"});
      assertTrue("matches element with Foo", ap.matches(ae).alwaysTrue());
      final AnnotatedElementImpl ae2 = new AnnotatedElementImpl(new String[]{"Boo"});
      assertTrue("does not match element with Boo", ap.matches(ae2).alwaysFalse());
    }
  }

  public void testBindingAnnotationPatternMatching() {
    if (LangUtil.is15VMOrGreater()) {
      final PatternParser p = new PatternParser("foo");
      AnnotationTypePattern ap = p.parseAnnotationNameOrVarTypePattern();
      try {
        ap = ap.resolveBindings(makeSimpleScope(), new Bindings(3), true);
      } catch (AbortException abEx) {
        assertEquals("Binding not supported in @pcds (1.5.0 M1 limitation): null", abEx.getMessage());
      }
      // uncomment these next lines once binding is supported
      // AnnotatedElementImpl ae = new AnnotatedElementImpl(new
      // String[]{"Foo"});
      // assertTrue("matches element with Foo",ap.matches(ae).alwaysTrue())
      // ;
      // AnnotatedElementImpl ae2 = new AnnotatedElementImpl(new
      // String[]{"Boo"});
      // assertTrue("does not match element with Boo",ap.matches(ae2).
      // alwaysFalse());
    }
  }

  public void testAndAnnotationPatternMatching() {
    if (LangUtil.is15VMOrGreater()) {
      final PatternParser p = new PatternParser("@Foo @Boo");
      AnnotationTypePattern ap = p.maybeParseAnnotationPattern();
      ap = ap.resolveBindings(makeSimpleScope(), new Bindings(3), true);
      AnnotatedElementImpl ae = new AnnotatedElementImpl(new String[]{"Foo", "Boo"});
      assertTrue("matches foo and boo", ap.matches(ae).alwaysTrue());
      ae = new AnnotatedElementImpl(new String[]{"Foo"});
      assertTrue("does not match foo", ap.matches(ae).alwaysFalse());
      ae = new AnnotatedElementImpl(new String[]{"Boo"});
      assertTrue("does not match boo", ap.matches(ae).alwaysFalse());
      ae = new AnnotatedElementImpl(new String[]{"Goo"});
      assertTrue("does not match goo", ap.matches(ae).alwaysFalse());
    }
  }

  //
  // public void testOrAnnotationPatternMatching() {
  // PatternParser p = new PatternParser("@Foo || @Boo");
  // AnnotationTypePattern ap = p.parseAnnotationTypePattern();
  // ap = ap.resolveBindings(makeSimpleScope(),new Bindings(3),true);
  // AnnotatedElementImpl ae = new AnnotatedElementImpl(new String[]
  // {"Foo","Boo"});
  // assertTrue("matches foo and boo",ap.matches(ae).alwaysTrue());
  // ae = new AnnotatedElementImpl(new String[] {"Foo"});
  // assertTrue("matches foo",ap.matches(ae).alwaysTrue());
  // ae = new AnnotatedElementImpl(new String[] {"Boo"});
  // assertTrue("matches boo",ap.matches(ae).alwaysTrue());
  // ae = new AnnotatedElementImpl(new String[] {"Goo"});
  // assertTrue("does not match goo",ap.matches(ae).alwaysFalse());
  // }
  //
  public void testNotAnnotationPatternMatching() {
    if (LangUtil.is15VMOrGreater()) {
      final PatternParser p = new PatternParser("!@Foo");
      AnnotationTypePattern ap = p.maybeParseAnnotationPattern();
      ap = ap.resolveBindings(makeSimpleScope(), new Bindings(3), true);
      AnnotatedElementImpl ae = new AnnotatedElementImpl(new String[]{"Foo", "Boo"});
      assertTrue("does not match foo and boo", ap.matches(ae).alwaysFalse());
      ae = new AnnotatedElementImpl(new String[]{"Boo"});
      assertTrue("matches boo", ap.matches(ae).alwaysTrue());
    }
  }

  public void testAnyAnnotationPatternMatching() {
    AnnotatedElementImpl ae = new AnnotatedElementImpl(new String[]{"Foo", "Boo"});
    assertTrue("always matches", AnnotationTypePattern.ANY.matches(ae).alwaysTrue());
    ae = new AnnotatedElementImpl(new String[]{});
    assertTrue("always matches", AnnotationTypePattern.ANY.matches(ae).alwaysTrue());
  }

  public TestScope makeSimpleScope() {
    final BcelWorld bWorld = new BcelWorld(BcweaverTests.TESTDATA_PATH + "/testcode.jar"); // testcode contains Foo/Boo/Goo/etc
    bWorld.setBehaveInJava5Way(true);
    return new TestScope(new String[]{"int", "java.lang.String", "Foo", "Boo", "Goo"}, new String[]{"a", "b", "foo",
        "boo", "goo"}, bWorld);
  }

  // put test cases for AnnotationPatternList matching in separate test
  // class...

  static class AnnotatedElementImpl implements AnnotatedElement {

    private String[] annotationTypes;

    public AnnotatedElementImpl(String[] annotationTypes) {
      this.annotationTypes = annotationTypes;
    }

    @Override
    public boolean hasAnnotation(UnresolvedType ofType) {
      for (int i = 0; i < annotationTypes.length; i++) {
        if (annotationTypes[i].equals(ofType.getName())) {
          return true;
        }
      }
      return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.aspectj.weaver.AnnotatedElement#getAnnotationTypes()
     */
    @Override
    public ResolvedType[] getAnnotationTypes() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public AnnotationAJ getAnnotationOfType(UnresolvedType ofType) {
      // TODO Auto-generated method stub
      return null;
    }

  }
}
