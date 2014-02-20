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

import org.aspectj.weaver.CompressingDataOutputStream;
import org.aspectj.weaver.VersionedDataInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ModifiersPattern extends PatternNode {
  private final int requiredModifiers;
  private final int forbiddenModifiers;

  public static final ModifiersPattern ANY = new ModifiersPattern(0, 0);

  private static Map<String, Integer> modifierFlags = null;

  static {
    modifierFlags = new HashMap<String, Integer>();
    int flag = 1;
    while (flag <= Modifier.STRICT) {
      final String flagName = Modifier.toString(flag);
      modifierFlags.put(flagName, new Integer(flag));
      flag = flag << 1;
    }
    modifierFlags.put("synthetic", new Integer(0x1000 /* Modifier.SYNTHETIC */));
  }

  public ModifiersPattern(int requiredModifiers, int forbiddenModifiers) {
    this.requiredModifiers = requiredModifiers;
    this.forbiddenModifiers = forbiddenModifiers;
  }

  public String toString() {
    if (this == ANY) {
      return "";
    }

    final String ret = Modifier.toString(requiredModifiers);
    if (forbiddenModifiers == 0) {
      return ret;
    } else {
      return ret + " !" + Modifier.toString(forbiddenModifiers);
    }
  }

  public boolean equals(Object other) {
    if (!(other instanceof ModifiersPattern)) {
      return false;
    }
    final ModifiersPattern o = (ModifiersPattern) other;
    return o.requiredModifiers == this.requiredModifiers && o.forbiddenModifiers == this.forbiddenModifiers;
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + requiredModifiers;
    result = 37 * result + forbiddenModifiers;
    return result;
  }

  public boolean matches(int modifiers) {
    return ((modifiers & requiredModifiers) == requiredModifiers) && ((modifiers & forbiddenModifiers) == 0);
  }

  public static ModifiersPattern read(VersionedDataInputStream s) throws IOException {
    final int requiredModifiers = s.readShort();
    final int forbiddenModifiers = s.readShort();
    if (requiredModifiers == 0 && forbiddenModifiers == 0) {
      return ANY;
    }
    return new ModifiersPattern(requiredModifiers, forbiddenModifiers);
  }

  @Override
  public void write(@NotNull CompressingDataOutputStream s) throws IOException {
    // s.writeByte(MODIFIERS_PATTERN);
    s.writeShort(requiredModifiers);
    s.writeShort(forbiddenModifiers);
  }

  public static int getModifierFlag(String name) {
    final Integer flag = modifierFlags.get(name);
    if (flag == null) {
      return -1;
    }
    return flag.intValue();
  }

  @Override
  public Object accept(PatternNodeVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
