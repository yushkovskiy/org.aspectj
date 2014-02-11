/* *******************************************************************
 * Copyright (c) 2009 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement     initial implementation 
 * ******************************************************************/

package org.aspectj.weaver.patterns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.aspectj.weaver.CompressingDataOutputStream;
import org.aspectj.weaver.Member;
import org.aspectj.weaver.TestUtils;
import org.aspectj.weaver.VersionedDataInputStream;
import org.aspectj.weaver.World;
import org.aspectj.weaver.reflect.ReflectionWorld;

public class SignaturePatternMatchSpeedTestCase extends PatternsTestCase {

  Member stringReplaceFirstMethod;

  @Override
  public World getWorld() {
    return new ReflectionWorld(true, this.getClass().getClassLoader());
  }

  public void testPatternEllipsis() throws IOException {
    final String pattern = "* *(..))";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternOneArg() throws IOException {
    final String pattern = "* *(*))";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternNoArgs() throws IOException {
    final String pattern = "* *())";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternNotVoidReturn() throws IOException {
    final String pattern = "!void *(..))";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternVoidReturn() throws IOException {
    final String pattern = "void *(..))";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternWildcardedName() throws IOException {
    final String pattern = "* *a*b*()";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  public void testPatternWildcardedName2() throws IOException {
    final String pattern = "* *a*b*(..)";
    final SignaturePattern signaturePattern = toPattern(pattern);
    warmup(signaturePattern, stringReplaceFirstMethod, world);
    final long time = measure(signaturePattern, stringReplaceFirstMethod, world);
    System.out.println("Signature pattern [" + pattern + "] took " + time + "ms for 1,000,000");
  }

  // ---

  public void checkMatch(SignaturePattern p, Member[] yes, Member[] no) throws IOException {
    p = p.resolveBindings(new TestScope(world, new FormalBinding[0]), new Bindings(0));

    for (int i = 0; i < yes.length; i++) {
      checkMatch(p, yes[i], true);
    }

    for (int i = 0; i < no.length; i++) {
      checkMatch(p, no[i], false);
    }

    checkSerialization(p);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    stringReplaceFirstMethod = TestUtils.methodFromString(
        "java.lang.String java.lang.String.replaceFirst(java.lang.String,java.lang.String)").resolve(world);
  }

  /**
   * Run match 1000 times to warmup
   */
  private void warmup(SignaturePattern signaturePattern, Member method, World world) {
    for (int i = 0; i < 1000; i++) {
      signaturePattern.matches(method, world, false);
    }
  }

  /**
   * Run match 1000000 and return time taken in ms
   */
  private long measure(SignaturePattern signaturePattern, Member method, World world) {
    final long stime = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      signaturePattern.matches(method, world, false);
    }
    return (System.currentTimeMillis() - stime);
  }

  private SignaturePattern toPattern(String pattern) {
    SignaturePattern signaturePattern = makeMethodPat(pattern);
    signaturePattern = signaturePattern.resolveBindings(new TestScope(world, new FormalBinding[0]), new Bindings(0));
    return signaturePattern;
  }

  private void checkMatch(SignaturePattern p, Member member, boolean b) {
    final boolean matches = p.matches(member, world, false);
    assertEquals(p.toString() + " matches " + member.toString(), b, matches);
  }

  private SignaturePattern makeMethodPat(String pattern) {
    return new PatternParser(pattern).parseMethodOrConstructorSignaturePattern();
  }

  private SignaturePattern makeFieldPat(String pattern) {
    return new PatternParser(pattern).parseFieldSignaturePattern();
  }

  private void checkSerialization(SignaturePattern p) throws IOException {
    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
    final ConstantPoolSimulator cps = new ConstantPoolSimulator();
    final CompressingDataOutputStream out = new CompressingDataOutputStream(bo, cps);
    p.write(out);
    out.close();

    final ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    final VersionedDataInputStream in = new VersionedDataInputStream(bi, cps);
    final SignaturePattern newP = SignaturePattern.read(in, null);

    assertEquals("write/read", p, newP);
  }

}
