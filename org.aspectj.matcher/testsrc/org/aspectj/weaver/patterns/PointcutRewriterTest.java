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

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.aspectj.weaver.Shadow;

/**
 * Testing the pointcut rewriter.
 *
 * @author Adrian Colyer
 * @author Andy Clement
 */
public class PointcutRewriterTest extends TestCase {

  private PointcutRewriter prw;

  public void testComplexRewrite1() {
    Pointcut p = getPointcut("(persingleton(org.eclipse.ajdt.internal.ui.ras.UIFFDC) && ((handler(java.lang.Throwable+) && args(arg1)) && ((within(org.eclipse.ajdt..*) && (!within(org.eclipse.ajdt.internal.ui.lazystart..*) && (!within(org.eclipse.ajdt.internal.ui.dialogs.OpenTypeSelectionDialog2) && !(within(org.eclipse.ajdt.internal.ui.editor.AspectJBreakpointRulerAction) && handler(org.eclipse.jface.text.BadLocationException))))) && (!(within(org.eclipse.ajdt.core.ras.FFDC+) || handler(org.eclipse.core.runtime.OperationCanceledException)) && !this(java.lang.Object)))))");
    checkMultipleRewrite(p);
    p = getPointcut("((((((((((!within(org.eclipse.ajdt.internal.ui.lazystart..*) && !within(org.eclipse.ajdt.internal.ui.dialogs.OpenTypeSelectionDialog2)) && !within(org.eclipse.ajdt.core.ras.FFDC+)) && within(org.eclipse.ajdt..*)) && !within(org.eclipse.ajdt.internal.ui.editor.AspectJBreakpointRulerAction)) && handler(java.lang.Throwable+)) && !handler(org.eclipse.core.runtime.OperationCanceledException)) && !this(java.lang.Object)) && args(arg1)) && persingleton(org.eclipse.ajdt.internal.ui.ras.UIFFDC)) || (((((((((!within(org.eclipse.ajdt.internal.ui.lazystart..*) && !within(org.eclipse.ajdt.internal.ui.dialogs.OpenTypeSelectionDialog2)) && !within(org.eclipse.ajdt.core.ras.FFDC+)) && within(org.eclipse.ajdt..*)) && !handler(org.eclipse.jface.text.BadLocationException)) && handler(java.lang.Throwable+)) && !handler(org.eclipse.core.runtime.OperationCanceledException)) && !this(java.lang.Object)) && args(arg1)) && persingleton(org.eclipse.ajdt.internal.ui.ras.UIFFDC)))");
    checkMultipleRewrite(p);
    p = getPointcut("(persingleton(Oranges) && ((handler(Apples+) && args(arg1)) && ((within(foo..*) && (!within(org.eclipse.ajdt.internal.ui.lazystart..*) && (!within(org.eclipse.ajdt.internal.ui.dialogs.OpenTypeSelectionDialog2) && !(within(org.eclipse.ajdt.internal.ui.editor.AspectJBreakpointRulerAction) && handler(org.eclipse.jface.text.BadLocationException))))) && (!(within(org.eclipse.ajdt.core.ras.FFDC+) || handler(org.eclipse.core.runtime.OperationCanceledException)) && !this(java.lang.Object)))))");
    checkMultipleRewrite(p);
    p = getPointcut("(((handler(Apples+)) && ((within(foo..*) && (!within(org.eclipse.ajdt.internal.ui.lazystart..*) && (!within(org.eclipse.ajdt.internal.ui.dialogs.OpenTypeSelectionDialog2) && !(within(org.eclipse.ajdt.internal.ui.editor.AspectJBreakpointRulerAction) && handler(org.eclipse.jface.text.BadLocationException))))) && (!(within(org.eclipse.ajdt.core.ras.FFDC+) || handler(org.eclipse.core.runtime.OperationCanceledException)) && !this(java.lang.Object)))))");
    checkMultipleRewrite(p);
    p = getPointcut("within(xxx..*) && within(XXY) && within(org.eclipse.AspectJBreakpoint)");
    checkMultipleRewrite(p);
  }

  /**
   * Rewrites a pointcut twice and checks the format is stable
   */
  private void checkMultipleRewrite(Pointcut p) {
    final Pointcut rewrittenPointcut = prw.rewrite(p, false);
    final String rewrite = rewrittenPointcut.toString();
    final Pointcut rewriteOfRewrittenPointcut = prw.rewrite(rewrittenPointcut, false);
    final String rewriteOfRewritten = rewriteOfRewrittenPointcut.toString();
    assertEquals(rewrite, rewriteOfRewritten);
  }

  public void testDistributeNot() {
    final Pointcut plain = getPointcut("this(Foo)");
    assertEquals("Unchanged", plain, prw.rewrite(plain));
    final Pointcut not = getPointcut("!this(Foo)");
    assertEquals("Unchanged", not, prw.rewrite(not));
    final Pointcut notNot = getPointcut("!!this(Foo)");
    assertEquals("this(Foo)", prw.rewrite(notNot).toString());
    final Pointcut notNotNOT = getPointcut("!!!this(Foo)");
    assertEquals("!this(Foo)", prw.rewrite(notNotNOT).toString());
    final Pointcut and = getPointcut("!(this(Foo) && this(Goo))");
    assertEquals("(!this(Foo) || !this(Goo))", prw.rewrite(and, true).toString());
    final Pointcut or = getPointcut("!(this(Foo) || this(Goo))");
    assertEquals("(!this(Foo) && !this(Goo))", prw.rewrite(or, true).toString());
    final Pointcut nestedNot = getPointcut("!(this(Foo) && !this(Goo))");
    assertEquals("(!this(Foo) || this(Goo))", prw.rewrite(nestedNot, true).toString());
  }

  public void testPullUpDisjunctions() {
    final Pointcut aAndb = getPointcut("this(Foo) && this(Goo)");
    assertEquals("Unchanged", aAndb, prw.rewrite(aAndb));
    final Pointcut aOrb = getPointcut("this(Foo) || this(Moo)");
    assertEquals("Unchanged", aOrb, prw.rewrite(aOrb));

    final Pointcut leftOr = getPointcut("this(Foo) || (this(Goo) && this(Boo))");
    assertEquals("or%anyorder%this(Foo)%and%anyorder%this(Boo)%this(Goo)", prw.rewrite(leftOr));
    // assertEquals("(this(Foo) || (this(Boo) && this(Goo)))",prw.rewrite(leftOr).toString());

    final Pointcut rightOr = getPointcut("(this(Goo) && this(Boo)) || this(Foo)");
    // assertEquals("(this(Foo) || (this(Boo) && this(Goo)))",prw.rewrite(rightOr).toString());
    assertEquals("or%anyorder%this(Foo)%and%anyorder%this(Goo)%this(Boo)", prw.rewrite(rightOr));

    final Pointcut leftAnd = getPointcut("this(Foo) && (this(Goo) || this(Boo))");
    // assertEquals("((this(Boo) && this(Foo)) || (this(Foo) && this(Goo)))",prw.rewrite(leftAnd).toString());
    assertEquals("or%anyorder%and%anyorder%this(Boo)%this(Foo)%and%anyorder%this(Foo)%this(Goo)", prw.rewrite(leftAnd));

    final Pointcut rightAnd = getPointcut("(this(Goo) || this(Boo)) && this(Foo)");
    // assertEquals("((this(Boo) && this(Foo)) || (this(Foo) && this(Goo)))",prw.rewrite(rightAnd).toString());
    assertEquals("or%anyorder%and%anyorder%this(Boo)%this(Foo)%and%anyorder%this(Foo)%this(Goo)", prw.rewrite(rightAnd));

    final Pointcut nestedOrs = getPointcut("this(Foo) || this(Goo) || this(Boo)");
    // assertEquals("((this(Boo) || this(Foo)) || this(Goo))",prw.rewrite(nestedOrs).toString());
    assertEquals("or%anyorder%this(Goo)%or%anyorder%this(Boo)%this(Foo)", prw.rewrite(nestedOrs));

    final Pointcut nestedAnds = getPointcut("(this(Foo) && (this(Boo) && (this(Goo) || this(Moo))))");
    // t(F) && (t(B) && (t(G) || t(M)))
    // ==> t(F) && ((t(B) && t(G)) || (t(B) && t(M)))
    // ==> (t(F) && (t(B) && t(G))) || (t(F) && (t(B) && t(M)))
    // assertEquals("(((this(Boo) && this(Foo)) && this(Goo)) || ((this(Boo) && this(Foo)) && this(Moo)))",
    // prw.rewrite(nestedAnds).toString());
    assertEquals(
        "or%anyorder%and%anyorder%and%anyorder%this(Boo)%this(Foo)%this(Goo)%and%anyorder%and%anyorder%this(Boo)%this(Foo)%this(Moo)",
        prw.rewrite(nestedAnds));
  }

  /**
   * spec is reverse polish notation with operators and, or , not, anyorder, delimiter is "%" (not whitespace).
   *
   * @param spec
   * @param pc
   */
  private void assertEquals(String spec, Pointcut pc) {
    final StringTokenizer strTok = new StringTokenizer(spec, "%");
    final String[] tokens = new String[strTok.countTokens()];
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = strTok.nextToken();
    }
    tokenIndex = 0;
    assertTrue(spec, equals(pc, tokens));
  }

  private int tokenIndex = 0;

  private boolean equals(Pointcut pc, String[] tokens) {
    if (tokens[tokenIndex].equals("and")) {
      tokenIndex++;
      if (!(pc instanceof AndPointcut)) {
        return false;
      }
      final AndPointcut apc = (AndPointcut) pc;
      final Pointcut left = apc.getLeft();
      final Pointcut right = apc.getRight();
      if (tokens[tokenIndex].equals("anyorder")) {
        tokenIndex++;
        final int restorePoint = tokenIndex;
        final boolean leftMatchFirst = equals(left, tokens) && equals(right, tokens);
        if (leftMatchFirst) {
          return true;
        }
        tokenIndex = restorePoint;
        final boolean rightMatchFirst = equals(right, tokens) && equals(left, tokens);
        return rightMatchFirst;
      } else {
        return equals(left, tokens) && equals(right, tokens);
      }
    } else if (tokens[tokenIndex].equals("or")) {
      tokenIndex++;
      if (!(pc instanceof OrPointcut)) {
        return false;
      }
      final OrPointcut opc = (OrPointcut) pc;
      final Pointcut left = opc.getLeft();
      final Pointcut right = opc.getRight();
      if (tokens[tokenIndex].equals("anyorder")) {
        tokenIndex++;
        final int restorePoint = tokenIndex;
        final boolean leftMatchFirst = equals(left, tokens) && equals(right, tokens);
        if (leftMatchFirst) {
          return true;
        }
        tokenIndex = restorePoint;
        final boolean rightMatchFirst = equals(right, tokens) && equals(left, tokens);
        return rightMatchFirst;
      } else {
        return equals(left, tokens) && equals(right, tokens);
      }

    } else if (tokens[tokenIndex].equals("not")) {
      if (!(pc instanceof NotPointcut)) {
        return false;
      }
      tokenIndex++;
      final NotPointcut np = (NotPointcut) pc;
      return equals(np.getNegatedPointcut(), tokens);
    } else {
      return tokens[tokenIndex++].equals(pc.toString());
    }
  }

  // public void testSplitOutWithins() {
  // Pointcut simpleExecution = getPointcut("execution(* *.*(..))");
  // assertEquals("Unchanged",simpleExecution,prw.rewrite(simpleExecution));
  // Pointcut simpleWithinCode = getPointcut("withincode(* *.*(..))");
  // assertEquals("Unchanged",simpleWithinCode,prw.rewrite(simpleWithinCode));
  // Pointcut execution = getPointcut("execution(@Foo Foo (@Goo org.xyz..*).m*(Foo,Boo))");
  // assertEquals("(within((@(Goo) org.xyz..*)) && execution(@(Foo) Foo m*(Foo, Boo)))",
  // prw.rewrite(execution).toString());
  // Pointcut withincode = getPointcut("withincode(@Foo Foo (@Goo org.xyz..*).m*(Foo,Boo))");
  // assertEquals("(within((@(Goo) org.xyz..*)) && withincode(@(Foo) Foo m*(Foo, Boo)))",
  // prw.rewrite(withincode).toString());
  // Pointcut notExecution = getPointcut("!execution(Foo BankAccount+.*(..))");
  // assertEquals("(!within(BankAccount+) || !execution(Foo *(..)))",
  // prw.rewrite(notExecution).toString());
  // Pointcut andWithincode = getPointcut("withincode(Foo.new(..)) && this(Foo)");
  // assertEquals("((within(Foo) && withincode(new(..))) && this(Foo))",
  // prw.rewrite(andWithincode).toString());
  // Pointcut orExecution = getPointcut("this(Foo) || execution(Goo Foo.moo(Baa))");
  // assertEquals("((within(Foo) && execution(Goo moo(Baa))) || this(Foo))",
  // prw.rewrite(orExecution).toString());
  // }

  public void testRemoveDuplicatesInAnd() {
    final Pointcut dupAnd = getPointcut("this(Foo) && this(Foo)");
    assertEquals("this(Foo)", prw.rewrite(dupAnd).toString());
    final Pointcut splitdupAnd = getPointcut("(this(Foo) && target(Boo)) && this(Foo)");
    assertEquals("(target(Boo) && this(Foo))", prw.rewrite(splitdupAnd).toString());
  }

  public void testNotRemoveNearlyDuplicatesInAnd() {
    final Pointcut toAndto = getPointcut("this(Object+) && this(Object)");
    // Pointcut rewritten =
    prw.rewrite(toAndto);
  }

  public void testAAndNotAinAnd() {
    final Pointcut aAndNota = getPointcut("this(Foo)&& !this(Foo)");
    assertEquals("Matches nothing", "", prw.rewrite(aAndNota).toString());
    final Pointcut aAndBAndNota = getPointcut("this(Foo) && execution(* *.*(..)) && !this(Foo)");
    assertEquals("Matches nothing", "", prw.rewrite(aAndBAndNota).toString());
  }

  public void testIfFalseInAnd() {
    final Pointcut ifFalse = IfPointcut.makeIfFalsePointcut(Pointcut.CONCRETE);
    final Pointcut p = getPointcut("this(A)");
    assertEquals("Matches nothing", "", prw.rewrite(new AndPointcut(ifFalse, p)).toString());
  }

  public void testMatchesNothinginAnd() {
    final Pointcut nothing = Pointcut.makeMatchesNothing(Pointcut.CONCRETE);
    final Pointcut p = getPointcut("this(A)");
    assertEquals("Matches nothing", "", prw.rewrite(new AndPointcut(nothing, p)).toString());
  }

  public void testMixedKindsInAnd() {
    final Pointcut mixedKinds = getPointcut("call(* *(..)) && execution(* *(..))");
    assertEquals("Matches nothing", "", prw.rewrite(mixedKinds).toString());
    final Pointcut ok = getPointcut("this(Foo) && call(* *(..))");
    assertEquals(ok, prw.rewrite(ok));
  }

  public void testDetermineKindSetOfAnd() {
    final Pointcut oneKind = getPointcut("execution(* foo(..)) && this(Boo)");
    final AndPointcut rewritten = (AndPointcut) prw.rewrite(oneKind);
    assertEquals("Only one kind", 1, Shadow.howMany(rewritten.couldMatchKinds()));
    assertTrue("It's Shadow.MethodExecution", Shadow.MethodExecution.isSet(rewritten.couldMatchKinds()));
  }

  public void testKindSetOfExecution() {
    Pointcut p = getPointcut("execution(* foo(..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.MethodExecution", Shadow.MethodExecution.isSet(p.couldMatchKinds()));
    p = getPointcut("execution(new(..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.ConstructorExecution", Shadow.ConstructorExecution.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfCall() {
    Pointcut p = getPointcut("call(* foo(..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.MethodCall", Shadow.MethodCall.isSet(p.couldMatchKinds()));
    p = getPointcut("call(new(..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.ConstructorCall", Shadow.ConstructorCall.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfAdviceExecution() {
    final Pointcut p = getPointcut("adviceexecution()");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.AdviceExecution", Shadow.AdviceExecution.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfGet() {
    final Pointcut p = getPointcut("get(* *)");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.FieldGet", Shadow.FieldGet.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfSet() {
    final Pointcut p = getPointcut("set(* *)");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.FieldSet", Shadow.FieldSet.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfHandler() {
    final Pointcut p = getPointcut("handler(*)");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.ExceptionHandler", Shadow.ExceptionHandler.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfInitialization() {
    final Pointcut p = getPointcut("initialization(new (..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.Initialization", Shadow.Initialization.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfPreInitialization() {
    final Pointcut p = getPointcut("preinitialization(new (..))");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.PreInitialization", Shadow.PreInitialization.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfStaticInitialization() {
    final Pointcut p = getPointcut("staticinitialization(*)");
    assertEquals("Only one kind", 1, Shadow.howMany(p.couldMatchKinds()));
    assertTrue("It's Shadow.StaticInitialization", Shadow.StaticInitialization.isSet(p.couldMatchKinds()));
  }

  public void testKindSetOfThis() {
    Pointcut p = getPointcut("this(Foo)");
    Set matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that don't have a this", kind.neverHasThis());
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].neverHasThis()) {
        assertTrue("All kinds that do have this", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
    // + @
    p = getPointcut("@this(Foo)");
    matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that don't have a this", kind.neverHasThis());
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].neverHasThis()) {
        assertTrue("All kinds that do have this", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
  }

  public void testKindSetOfTarget() {
    Pointcut p = getPointcut("target(Foo)");
    Set matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that don't have a target", kind.neverHasTarget());
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].neverHasTarget()) {
        assertTrue("All kinds that do have target", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
    // + @
    p = getPointcut("@target(Foo)");
    matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that don't have a target", kind.neverHasTarget());
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].neverHasTarget()) {
        assertTrue("All kinds that do have target", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
  }

  public void testKindSetOfArgs() {
    Pointcut p = getPointcut("args(..)");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
    // + @
    p = getPointcut("@args(..)");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
  }

  public void testKindSetOfAnnotation() {
    final Pointcut p = getPointcut("@annotation(Foo)");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
  }

  public void testKindSetOfWithin() {
    Pointcut p = getPointcut("within(*)");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
    // + @
    p = getPointcut("@within(Foo)");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
  }

  public void testKindSetOfWithinCode() {
    Pointcut p = getPointcut("withincode(* foo(..))");
    Set matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that are themselves enclosing",
          (kind.isEnclosingKind() && kind != Shadow.ConstructorExecution && kind != Shadow.Initialization));
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].isEnclosingKind()) {
        assertTrue("All kinds that are not enclosing", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
    assertTrue("Need cons-exe for inlined field inits", matches.contains(Shadow.ConstructorExecution));
    assertTrue("Need init for inlined field inits", matches.contains(Shadow.Initialization));
    // + @
    p = getPointcut("@withincode(Foo)");
    matches = Shadow.toSet(p.couldMatchKinds());
    for (final Iterator iter = matches.iterator(); iter.hasNext(); ) {
      final Shadow.Kind kind = (Shadow.Kind) iter.next();
      assertFalse("No kinds that are themselves enclosing", kind.isEnclosingKind());
    }
    for (int i = 0; i < Shadow.SHADOW_KINDS.length; i++) {
      if (!Shadow.SHADOW_KINDS[i].isEnclosingKind()) {
        assertTrue("All kinds that are not enclosing", matches.contains(Shadow.SHADOW_KINDS[i]));
      }
    }
  }

  public void testKindSetOfIf() {
    Pointcut p = new IfPointcut(null, 0);
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
    p = IfPointcut.makeIfTruePointcut(Pointcut.CONCRETE);
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
    p = IfPointcut.makeIfFalsePointcut(Pointcut.CONCRETE);
    assertTrue("Nothing", p.couldMatchKinds() == Shadow.NO_SHADOW_KINDS_BITS);
  }

  public void testKindSetOfCflow() {
    Pointcut p = getPointcut("cflow(this(Foo))");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
    // [below]
    p = getPointcut("cflowbelow(this(Foo))");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
  }

  public void testKindSetInNegation() {
    final Pointcut p = getPointcut("!execution(new(..))");
    assertTrue("All kinds", p.couldMatchKinds() == Shadow.ALL_SHADOW_KINDS_BITS);
  }

  public void testKindSetOfOr() {
    final Pointcut p = getPointcut("execution(new(..)) || get(* *)");
    final Set matches = Shadow.toSet(p.couldMatchKinds());
    assertEquals("2 kinds", 2, matches.size());
    assertTrue("ConstructorExecution", matches.contains(Shadow.ConstructorExecution));
    assertTrue("FieldGet", matches.contains(Shadow.FieldGet));
  }

  public void testOrderingInAnd() {
    final Pointcut bigLongPC = getPointcut("cflow(this(Foo)) && @args(X) && args(X) && @this(Foo) && @target(Boo) && this(Moo) && target(Boo) && @annotation(Moo) && @withincode(Boo) && withincode(new(..)) && set(* *)&& @within(Foo) && within(Foo)");
    checkMultipleRewrite(bigLongPC);
    final Pointcut rewritten = prw.rewrite(bigLongPC);
    assertEquals(
        "((((((((((((within(Foo) && @within(Foo)) && set(* *)) && withincode(new(..))) && @withincode(Boo)) && target(Boo)) && this(Moo)) && @annotation(Moo)) && @this(Foo)) && @target(Boo)) && args(X)) && @args(X)) && cflow(this(Foo)))",
        rewritten.toString());
  }

  public void testOrderingInSimpleOr() {
    final OrPointcut opc = (OrPointcut) getPointcut("execution(new(..)) || get(* *)");
    assertEquals("reordered", "(get(* *) || execution(new(..)))", prw.rewrite(opc).toString());
  }

  public void testOrderingInNestedOrs() {
    final OrPointcut opc = (OrPointcut) getPointcut("(execution(new(..)) || get(* *)) || within(abc)");
    assertEquals("reordered", "((within(abc) || get(* *)) || execution(new(..)))", prw.rewrite(opc).toString());
  }

  public void testOrderingInOrsWithNestedAnds() {
    final OrPointcut opc = (OrPointcut) getPointcut("get(* *) || (execution(new(..)) && within(abc))");
    assertEquals("reordered", "((within(abc) && execution(new(..))) || get(* *))", prw.rewrite(opc).toString());
  }

  private Pointcut getPointcut(String s) {
    return new PatternParser(s).parsePointcut();
  }

  /*
   * @see TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    prw = new PointcutRewriter();
  }

}
