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

package org.aspectj.weaver.patterns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.aspectj.weaver.CompressingDataOutputStream;
import org.aspectj.weaver.VersionedDataInputStream;

/**
 * @author hugunin
 *         <p/>
 *         To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable
 *         and disable the creation of type comments go to Window>Preferences>Java>Code Generation.
 */
public class NamePatternTestCase extends TestCase {
  static String[] matchAll = new String[]{"*****", "*"};

  static String[] match1 = new String[]{"abcde", "abc*", "abcd*", "abcde*", "*e", "*cde", "*abcde", "a*e", "ab*e", "abc*de",
      "*a*b*c*d*e*", "a*c*e", "a***bcde", "*d*",};

  static String[] match2 = new String[]{"abababab", "aba*", "abab*", "abababab*", "*b", "*ab", "*ababab", "*abababab", "a*b",
      "ab*b", "abab*abab", "*a*b*a*b*a*b*a*b*", "a*****b", "a**b", "ab*b*b",};

  /**
   * Constructor for PatternTestCase.
   *
   * @param name
   */
  public NamePatternTestCase(String name) {
    super(name);
  }

  public void testMatch() {
    checkMatch("abcde", matchAll, true);
    checkMatch("abcde", match1, true);
    checkMatch("abcde", match2, false);

    checkMatch("abababab", matchAll, true);
    checkMatch("abababab", match1, false);
    checkMatch("abababab", match2, true);

  }

  /**
   * Method checkMatch.
   *
   * @param string
   * @param matchAll
   * @param b
   */
  private void checkMatch(String string, String[] patterns, boolean shouldMatch) {
    for (int i = 0, len = patterns.length; i < len; i++) {
      final NamePattern p = new NamePattern(patterns[i]);
      checkMatch(string, p, shouldMatch);
    }
  }

  private static void checkMatch(String string, NamePattern p, boolean shouldMatch) {
    final String msg = "matching " + string + " to " + p;
    assertEquals(msg, shouldMatch, p.matches(string));
  }

  public void testSerialization() throws IOException {
    checkSerialization(matchAll);
    checkSerialization(match1);
    checkSerialization(match2);
  }

  private void checkSerialization(String[] patterns) throws IOException {
    for (int i = 0, len = patterns.length; i < len; i++) {
      final NamePattern p = new NamePattern(patterns[i]);
      checkSerialization(p);
    }
  }

  private static void checkSerialization(NamePattern p) throws IOException {
    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
    final ConstantPoolSimulator cps = new ConstantPoolSimulator();
    final CompressingDataOutputStream out = new CompressingDataOutputStream(bo, cps);
    p.write(out);
    out.close();

    final ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    final VersionedDataInputStream in = new VersionedDataInputStream(bi, cps);
    final NamePattern newP = NamePattern.read(in);

    assertEquals("write/read", p, newP);
  }

}
