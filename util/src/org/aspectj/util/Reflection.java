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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;

public final class Reflection {
  @NotNull
  public static final Class<?>[] MAIN_PARM_TYPES = new Class[]{String[].class};

  @Nullable
  public static Object invokestaticN(@NotNull Class<?> class_, @NotNull String name, @NotNull Object[] args) {
    return invokeN(class_, name, null, args);
  }

  @Nullable
  public static Object invoke(@NotNull Class<?> class_, @NotNull Object target, @NotNull String name, @Nullable Object arg1, @Nullable Object arg2) {
    return invokeN(class_, name, target, new Object[]{arg1, arg2});
  }

  @Nullable
  public static Object invoke(@NotNull Class<?> class_, @NotNull Object target, @NotNull String name, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3) {
    return invokeN(class_, name, target, new Object[]{arg1, arg2, arg3});
  }


  @Nullable
  public static Object invokeN(@NotNull Class<?> class_, @NotNull String name, @Nullable Object target, @NotNull Object[] args) {
    final Method meth = getMatchingMethod(class_, name, args);
    try {
      return meth.invoke(target, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.toString());
    } catch (InvocationTargetException e) {
      final Throwable t = e.getTargetException();
      if (t instanceof Error) throw (Error) t;
      if (t instanceof RuntimeException) throw (RuntimeException) t;
      t.printStackTrace();
      throw new RuntimeException(t.toString());
    }
  }

  @Nullable
  public static Method getMatchingMethod(@NotNull Class<?> class_, @NotNull String name, @NotNull Object[] args) {
    final Method[] meths = class_.getMethods();
    for (int i = 0; i < meths.length; i++) {
      final Method meth = meths[i];
      if (meth.getName().equals(name) && isCompatible(meth, args)) {
        return meth;
      }
    }
    return null;
  }

  @Nullable
  public static Object getStaticField(@NotNull Class<?> class_, @NotNull String name) {
    try {
      return class_.getField(name).get(null);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("unimplemented");
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("unimplemented");
    }
  }

  public static void runMainInSameVM(@NotNull String classpath, @NotNull String className, @NotNull String[] args)
      throws SecurityException, NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException, ClassNotFoundException {

    LangUtil.throwIaxIfNull(className, "class name");
    if (LangUtil.isEmpty(classpath)) {
      final Class<?> mainClass = Class.forName(className);
      runMainInSameVM(mainClass, args);
      return;
    }
    final ArrayList<File> dirs = new ArrayList<File>();
    final ArrayList<File> libs = new ArrayList<File>();
    final ArrayList<URL> urls = new ArrayList<URL>();
    final String[] entries = LangUtil.splitClasspath(classpath);
    for (int i = 0; i < entries.length; i++) {
      final String entry = entries[i];
      final URL url = makeURL(entry);
      if (null != url) {
        urls.add(url);
      }
      final File file = new File(entries[i]);
// tolerate bad entries b/c bootclasspath sometimes has them
//            if (!file.canRead()) {
//                throw new IllegalArgumentException("cannot read " + file);
//            }
      if (FileUtil.isZipFile(file)) {
        libs.add(file);
      } else if (file.isDirectory()) {
        dirs.add(file);
      } else {
        // not URL, zip, or dir - unsure what to do
      }
    }
    final File[] dirRa = (File[]) dirs.toArray(new File[0]);
    final File[] libRa = (File[]) libs.toArray(new File[0]);
    final URL[] urlRa = (URL[]) urls.toArray(new URL[0]);
    runMainInSameVM(urlRa, libRa, dirRa, className, args);
  }

  public static void runMainInSameVM(@NotNull URL[] urls, @NotNull File[] libs, @NotNull File[] dirs, @NotNull String className, @NotNull String[] args)
      throws SecurityException, NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException, ClassNotFoundException {

    LangUtil.throwIaxIfNull(className, "class name");
    LangUtil.throwIaxIfNotAssignable(libs, File.class, "jars");
    LangUtil.throwIaxIfNotAssignable(dirs, File.class, "dirs");
    final URL[] libUrls = FileUtil.getFileURLs(libs);
    if (!LangUtil.isEmpty(libUrls)) {
      if (!LangUtil.isEmpty(urls)) {
        final URL[] temp = new URL[libUrls.length + urls.length];
        System.arraycopy(urls, 0, temp, 0, urls.length);
        System.arraycopy(urls, 0, temp, libUrls.length, urls.length);
        urls = temp;
      } else {
        urls = libUrls;
      }
    }
    final UtilClassLoader loader = new UtilClassLoader(urls, dirs);
    Class<?> targetClass = null;
    try {
      targetClass = loader.loadClass(className);
    } catch (ClassNotFoundException e) {
      final String s = "unable to load class " + className
          + " using class loader " + loader;
      throw new ClassNotFoundException(s);
    }
    final Method main = targetClass.getMethod("main", MAIN_PARM_TYPES);
    main.invoke(null, new Object[]{args});
  }

  public static void runMainInSameVM(@NotNull Class<?> mainClass, @NotNull String[] args) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    LangUtil.throwIaxIfNull(mainClass, "main class");
    final Method main = mainClass.getMethod("main", MAIN_PARM_TYPES);
    main.invoke(null, new Object[]{args});
  }

  private Reflection() {
  }

  private static boolean isCompatible(@NotNull Method meth, @NotNull Object[] args) {
    // ignore methods with overloading other than lengths
    return meth.getParameterTypes().length == args.length;
  }

  /**
   * @return URL if the input is valid as such
   */
  @Nullable
  private static URL makeURL(@NotNull String s) {
    try {
      return new URL(s);
    } catch (Throwable t) {
      return null;
    }
  }

}
