/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Xerox/PARC     initial implementation 
 * ******************************************************************/

package org.aspectj.util;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class TypeSafeEnum {
  private final byte key;
  @NotNull
  private final String name;

  public TypeSafeEnum(@NotNull String name, int key) {
    this.name = name;
    if (key > Byte.MAX_VALUE || key < Byte.MIN_VALUE) {
      throw new IllegalArgumentException("key doesn't fit into a byte: " + key);
    }
    this.key = (byte) key;
  }

  public String toString() {
    return name;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public byte getKey() {
    return key;
  }

  public void write(@NotNull DataOutputStream s) throws IOException {
    s.writeByte(key);
  }
}
