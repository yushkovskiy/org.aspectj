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

import org.aspectj.bridge.*;
import org.aspectj.weaver.IHasPosition;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.World;

public class SimpleScope implements IScope {

  private static final String[] NoStrings = new String[0];
  private static final String[] javaLangPrefixArray = new String[]{"java.lang.",};

  private String[] importedPrefixes = javaLangPrefixArray;
  private String[] importedNames = NoStrings;
  private final World world;
  private ResolvedType enclosingType;
  protected FormalBinding[] bindings;

  public SimpleScope(World world, FormalBinding[] bindings) {
    super();
    this.world = world;
    this.bindings = bindings;
  }

  @Override
  public UnresolvedType lookupType(String name, IHasPosition location) {
    for (int i = 0; i < importedNames.length; i++) {
      final String importedName = importedNames[i];
      // make sure we're matching against the type name rather than part of it
      // if (importedName.endsWith("." + name)) {
      if (importedName.endsWith(name)) {
        return world.resolve(importedName);
      }
    }

    // Check for a primitive
    if (name.length() < 8 && Character.isLowerCase(name.charAt(0))) {
      // could be a primitive
      final int len = name.length();
      if (len == 3) {
        if (name.equals("int")) {
          return UnresolvedType.INT;
        }
      } else if (len == 4) {
        if (name.equals("void")) {
          return UnresolvedType.VOID;
        } else if (name.equals("byte")) {
          return UnresolvedType.BYTE;
        } else if (name.equals("char")) {
          return UnresolvedType.CHAR;
        } else if (name.equals("long")) {
          return UnresolvedType.LONG;
        }
      } else if (len == 5) {
        if (name.equals("float")) {
          return UnresolvedType.FLOAT;
        } else if (name.equals("short")) {
          return UnresolvedType.SHORT;
        }
      } else if (len == 6) {
        if (name.equals("double")) {
          return UnresolvedType.DOUBLE;
        }
      } else if (len == 7) {
        if (name.equals("boolean")) {
          return UnresolvedType.BOOLEAN;
        }
      }
    }

    // Is it fully qualified?
    if (name.indexOf('.') != -1) {
      return world.resolve(UnresolvedType.forName(name), true);
    }

    for (String importedPrefix : importedPrefixes) {
      final ResolvedType tryType = world.resolve(UnresolvedType.forName(importedPrefix + name), true);
      if (!tryType.isMissing()) {
        return tryType;
      }
    }

    return world.resolve(UnresolvedType.forName(name), true);
  }

  @Override
  public IMessageHandler getMessageHandler() {
    return world.getMessageHandler();
  }

  @Override
  public FormalBinding lookupFormal(String name) {
    for (int i = 0, len = bindings.length; i < len; i++) {
      if (bindings[i].getName().equals(name)) {
        return bindings[i];
      }
    }
    return null;
  }

  @Override
  public FormalBinding getFormal(int i) {
    return bindings[i];
  }

  @Override
  public int getFormalCount() {
    return bindings.length;
  }

  @Override
  public String[] getImportedNames() {
    return importedNames;
  }

  @Override
  public String[] getImportedPrefixes() {
    return importedPrefixes;
  }

  public void setImportedNames(String[] importedNames) {
    this.importedNames = importedNames;
  }

  public void setImportedPrefixes(String[] importedPrefixes) {
    this.importedPrefixes = importedPrefixes;
  }

  public static FormalBinding[] makeFormalBindings(UnresolvedType[] types, String[] names) {
    final int len = types.length;
    final FormalBinding[] bindings = new FormalBinding[len];
    for (int i = 0; i < len; i++) {
      bindings[i] = new FormalBinding(types[i], names[i], i);
    }
    return bindings;
  }

  public ISourceLocation makeSourceLocation(IHasPosition location) {
    return new SourceLocation(ISourceLocation.NO_FILE, 0);
  }

  @Override
  public void message(IMessage.Kind kind, IHasPosition location1, IHasPosition location2, String message) {
    message(kind, location1, message);
    message(kind, location2, message);
  }

  @Override
  public void message(IMessage.Kind kind, IHasPosition location, String message) {
    getMessageHandler().handleMessage(new Message(message, kind, null, makeSourceLocation(location)));
  }

  @Override
  public void message(IMessage aMessage) {
    getMessageHandler().handleMessage(aMessage);
  }

  @Override
  public World getWorld() {
    return world;
  }

  @Override
  public ResolvedType getEnclosingType() {
    return enclosingType;
  }

}
