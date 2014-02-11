/* *******************************************************************
 * Copyright (c) 2003 Contributors.
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Isberg        initial implementation 
 * ******************************************************************/

package org.aspectj.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Load classes as File from File[] dirs or URL[] jars.
 */
public final class UtilClassLoader extends URLClassLoader {

  /**
   * seek classes in dirs first
   */
  @NotNull
  List<File> dirs;

  /**
   * save URL[] only for toString
   */
  @NotNull
  private final URL[] urlsForDebugString;

  public UtilClassLoader(@NotNull URL[] urls, @Nullable File[] dirs) {
    super(urls);
    LangUtil.throwIaxIfNotAssignable(dirs, File.class, "dirs");
    this.urlsForDebugString = urls;
    final ArrayList<File> dcopy = new ArrayList<File>();

    if (!LangUtil.isEmpty(dirs)) {
      dcopy.addAll(Arrays.asList(dirs));
    }
    this.dirs = Collections.unmodifiableList(dcopy);
  }

  @Override
  @Nullable
  public URL getResource(@NotNull String name) {
    return ClassLoader.getSystemResource(name);
  }

  @Override
  @Nullable
  public InputStream getResourceAsStream(@NotNull String name) {
    return ClassLoader.getSystemResourceAsStream(name);
  }

  @Override
  @NotNull
  public synchronized Class<?> loadClass(@NotNull String name, boolean resolve)
      throws ClassNotFoundException {
    // search the cache, our dirs (if maybe test), 
    // the system, the superclass (URL[]),
    // and our dirs again (if not maybe test)
    ClassNotFoundException thrown = null;
    Class<?> result = findLoadedClass(name);
    if (null != result) {
      resolve = false;
    } else {
      try {
        result = findSystemClass(name);
      } catch (ClassNotFoundException e) {
        thrown = e;
      }
    }
    if (null == result) {
      try {
        result = super.loadClass(name, resolve);
      } catch (ClassNotFoundException e) {
        thrown = e;
      }
      if (null != result) { // resolved by superclass
        return result;
      }
    }
    if (null == result) {
      final byte[] data = readClass(name);
      if (data != null) {
        result = defineClass(name, data, 0, data.length);
      } // handle ClassFormatError?            
    }

    if (null == result) {
      throw (null != thrown ? thrown : new ClassNotFoundException(name));
    }
    if (resolve) {
      resolveClass(result);
    }
    return result;
  }

  /**
   * @return String with debug info: urls and classes used
   */
  @Override
  public String toString() {
    return "UtilClassLoader(urls="
        + Arrays.asList(urlsForDebugString)
        + ", dirs="
        + dirs
        + ")";
  }

  /**
   * @return null if class not found or byte[] of class otherwise
   */
  @Nullable
  private byte[] readClass(@NotNull String className) throws ClassNotFoundException {
    final String fileName = className.replace('.', '/') + ".class";
    for (final Iterator<File> iter = dirs.iterator(); iter.hasNext(); ) {
      final File file = new File(iter.next(), fileName);
      if (file.canRead()) {
        return getClassData(file);
      }
    }
    return null;
  }

  @Nullable
  private static byte[] getClassData(@NotNull File f) {
    try {
      final FileInputStream stream = new FileInputStream(f);
      final ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
      final byte[] b = new byte[4096];
      int n;
      while ((n = stream.read(b)) != -1) {
        out.write(b, 0, n);
      }
      stream.close();
      out.close();
      return out.toByteArray();
    } catch (IOException e) {
    }
    return null;
  }
}

