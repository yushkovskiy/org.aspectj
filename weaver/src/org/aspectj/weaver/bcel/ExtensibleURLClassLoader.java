/* *******************************************************************
 * Copyright (c) 2004 IBM Corporation
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Matthew Webster, Adrian Colyer, 
 *     Martin Lippert     initial implementation 
 * ******************************************************************/

package org.aspectj.weaver.bcel;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;

import org.aspectj.util.FileUtil;
import org.aspectj.weaver.BCException;
import org.aspectj.weaver.UnresolvedType;

public abstract class ExtensibleURLClassLoader extends URLClassLoader {

  private ClassPathManager classPath;

  public ExtensibleURLClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);

    // System.err.println("? ExtensibleURLClassLoader.<init>() path=" + WeavingAdaptor.makeClasspath(urls));
    try {
      classPath = new ClassPathManager(FileUtil.makeClasspath(urls), null);
    } catch (ExceptionInInitializerError ex) {
      ex.printStackTrace(System.out);
      throw ex;
    }
  }

  @Override
  protected void addURL(URL url) {
    super.addURL(url); // amc - this call was missing and is needed in
    // WeavingURLClassLoader chains
    classPath.addPath(url.getPath(), null);
  }

  @Override
  protected Class findClass(String name) throws ClassNotFoundException {
    // System.err.println("? ExtensibleURLClassLoader.findClass(" + name + ")");
    try {
      final byte[] bytes = getBytes(name);
      if (bytes != null) {
        return defineClass(name, bytes);
      } else {
        throw new ClassNotFoundException(name);
      }
    } catch (IOException ex) {
      throw new ClassNotFoundException(name);
    }
  }

  protected Class defineClass(String name, byte[] b, CodeSource cs) throws IOException {
    // System.err.println("? ExtensibleURLClassLoader.defineClass(" + name + ",[" + b.length + "])");
    return defineClass(name, b, 0, b.length, cs);
  }

  protected byte[] getBytes(String name) throws IOException {
    byte[] b = null;
    UnresolvedType unresolvedType = null;
    try {
      unresolvedType = UnresolvedType.forName(name);
    } catch (BCException bce) {
      if (bce.getMessage().indexOf("nameToSignature") == -1) {
        bce.printStackTrace(System.err);
      }
      return null;
    }
    final ClassPathManager.ClassFile classFile = classPath.find(unresolvedType);
    if (classFile != null) {
      try {
        b = FileUtil.readAsByteArray(classFile.getInputStream());
      } finally {
        classFile.close();
      }
    }
    return b;
  }

  private Class defineClass(String name, byte[] bytes /* ClassPathManager.ClassFile classFile */) throws IOException {
    final String packageName = getPackageName(name);
    if (packageName != null) {
      final Package pakkage = getPackage(packageName);
      if (pakkage == null) {
        definePackage(packageName, null, null, null, null, null, null, null);
      }
    }

    return defineClass(name, bytes, null);
  }

  private String getPackageName(String className) {
    final int offset = className.lastIndexOf('.');
    return (offset == -1) ? null : className.substring(0, offset);
  }

}
