/* *******************************************************************
 * Copyright (c) 1999-2001 Xerox Corporation, 
 *               2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Xerox/PARC     initial implementation 
 * ******************************************************************/

package org.aspectj.ajdt.ajc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ConfigParser {
  @NotNull
  protected static String CONFIG_MSG = "build config error: ";
  @Nullable
  Location location;
  @Nullable
  protected File relativeDirectory = null;
  @NotNull
  protected final List<File> files = new LinkedList<File>();
  @NotNull
  protected final List<File> xmlfiles = new ArrayList<File>();
  private boolean fileParsed = false;

  @NotNull
  public List<File> getFiles() {
    return files;
  }

  @NotNull
  public List<File> getXmlFiles() {
    return xmlfiles;
  }

  public void parseCommandLine(@NotNull String[] argsArray) throws ParseException {
    location = new CommandLineLocation();
    final LinkedList<Arg> args = new LinkedList<Arg>();
    for (int i = 0; i < argsArray.length; i++) {
      args.add(new Arg(argsArray[i], location));
    }
    parseArgs(args);
  }

  public void parseConfigFile(@NotNull File configFile) throws ParseException {
    if (fileParsed == true) {
      throw new ParseException(CONFIG_MSG + "The file has already been parsed.", null);
    } else {
      parseConfigFileHelper(configFile);
    }
  }

  @NotNull
  public File makeFile(@NotNull String name) {
    if (relativeDirectory != null) {
      return makeFile(relativeDirectory, name);
    } else {
      return makeFile(getCurrentDir(), name);
    }
  }

  /**
   * ??? We would like to call a showNonFatalError method here to show all errors in config files before aborting the compilation
   */
  protected void addFile(@NotNull File sourceFile) {
    if (!sourceFile.isFile()) {
      showError("source file does not exist: " + sourceFile.getPath());
    }
    files.add(sourceFile);
  }

  protected void addXmlFile(@NotNull File xmlFile) {
    if (!xmlFile.isFile()) {
      showError("XML file does not exist: " + xmlFile.getPath());
    }
    xmlfiles.add(xmlFile);
  }

  protected void parseOption(@NotNull String arg, @NotNull LinkedList<Arg> args) {
    showWarning("unrecognized option: " + arg);
  }

  protected void showWarning(@NotNull String message) {
    if (location != null) {
      message += " at " + location.toString();
    }
    System.err.println(CONFIG_MSG + message);
  }

  protected void showError(@NotNull String message) {
    throw new ParseException(CONFIG_MSG + message, location);
  }

  @Nullable
  protected Arg removeArg(@NotNull LinkedList<Arg> args) {
    if (args.size() == 0) {
      showError("value missing");
      return null;
    } else {
      return args.removeFirst();
    }
  }

  @Nullable
  protected String removeStringArg(@NotNull LinkedList<Arg> args) {
    final Arg arg = removeArg(args);
    if (arg == null) {
      return null;
    }
    return arg.getValue();
  }

  protected void parseImportedConfigFile(@NotNull String relativeFilePath) {
    parseConfigFileHelper(makeFile(relativeFilePath));
  }

  @NotNull
  File getCurrentDir() {
    return location.getDirectory();
  }

  @NotNull
  static String stripSingleLineComment(@NotNull String s, @NotNull String commentString) {
    final int commentStart = s.indexOf(commentString);
    if (commentStart == -1) {
      return s;
    } else {
      return s.substring(0, commentStart);
    }
  }

  @NotNull
  String stripWhitespaceAndComments(@NotNull String s) {
    s = stripSingleLineComment(s, "//");
    s = stripSingleLineComment(s, "#");
    s = s.trim();
    if (s.startsWith("\"") && s.endsWith("\"")) {
      if (s.length() == 1) {
        return "";
      } else {
        s = s.substring(1, s.length() - 1);
      }
    }
    return s;
  }

  void addFileOrPattern(@NotNull File sourceFile) {
    if (sourceFile.getName().charAt(0) == '*') {
      if (sourceFile.getName().equals("*.java")) {
        addFiles(sourceFile.getParentFile(), new FileFilter() {
          @Override
          public boolean accept(@NotNull File f) {
            return f != null && f.getName().endsWith(".java");
          }
        });
      } else if (sourceFile.getName().equals("*.aj")) {
        addFiles(sourceFile.getParentFile(), new FileFilter() {
          @Override
          public boolean accept(@NotNull File f) {
            return f != null && f.getName().endsWith(".aj");
          }
        });
      } else {
        addFile(sourceFile);
      }
    } else {
      addFile(sourceFile);
    }
  }

  void addFiles(@Nullable File dir, @NotNull FileFilter filter) {
    if (dir == null) {
      dir = new File(System.getProperty("user.dir"));
    }

    if (!dir.isDirectory()) {
      showError("can't find " + dir.getPath());
    } else {
      final File[] files = dir.listFiles(filter);
      if (files.length == 0) {
        showWarning("no matching files found in: " + dir);
      }

      for (int i = 0; i < files.length; i++) {
        addFile(files[i]);
      }
    }
  }

  void parseArgs(@NotNull LinkedList<Arg> args) {
    while (args.size() > 0) {
      parseOneArg(args);
    }
  }

  /**
   * aop.xml configuration files can be passed on the command line.
   */
  static boolean isXml(@NotNull String s) {
    return s.endsWith(".xml");
  }

  static boolean isSourceFileName(@NotNull String s) {
    if (s.endsWith(".java")) {
      return true;
    }
    if (s.endsWith(".aj")) {
      return true;
    }
    // if (s.endsWith(".ajava")) {
    // showWarning(".ajava is deprecated, replace with .aj or .java: " + s);
    // return true;
    // }
    return false;
  }

  void parseOneArg(@NotNull LinkedList<Arg> args) {
    final Arg arg = removeArg(args);
    final String v = arg.getValue();
    location = arg.getLocation();
    if (v.startsWith("@")) {
      parseImportedConfigFile(v.substring(1));
    } else if (v.equals("-argfile")) {
      parseConfigFileHelper(makeFile(removeArg(args).getValue()));
    } else if (isSourceFileName(v)) {
      addFileOrPattern(makeFile(v));
    } else if (isXml(v)) {
      addXmlFile(makeFile(v));
    } else {
      parseOption(arg.getValue(), args);
    }
  }

  /**
   * @throws ParseException if the config file has already been prased.
   */
  private void parseConfigFileHelper(@NotNull File configFile) {
    if (!configFile.exists()) {
      showError("file does not exist: " + configFile.getPath());
      return;
    }

    final LinkedList<Arg> args = new LinkedList<Arg>();
    int lineNum = 0;

    try {
      final BufferedReader stream = new BufferedReader(new FileReader(configFile));
      String line = null;
      while ((line = stream.readLine()) != null) {
        lineNum += 1;
        line = stripWhitespaceAndComments(line);
        if (line.length() == 0) {
          continue;
        }
        args.add(new Arg(line, new CPSourceLocation(configFile, lineNum)));
      }
      stream.close();
    } catch (IOException e) {
      location = new CPSourceLocation(configFile, lineNum);
      showError("error reading config file: " + e.toString());
    }
    final File oldRelativeDirectory = relativeDirectory; // for nested arg files;
    relativeDirectory = configFile.getParentFile();
    parseArgs(args);
    relativeDirectory = oldRelativeDirectory;
    fileParsed = true;
  }

  @NotNull
  private static File makeFile(@NotNull File dir, @NotNull String name) {
    name = name.replace('/', File.separatorChar);
    File ret = new File(name);
    final boolean isAbsolute = ret.isAbsolute() || (ret.exists() && ret.getPath().startsWith(File.separator));
    if (!isAbsolute && (dir != null)) {
      ret = new File(dir, name);
    }
    try {
      ret = ret.getCanonicalFile();
    } catch (IOException ioEx) {
      // proceed without canonicalization
      // so nothing to do here
    }
    return ret;
  }

  protected static class Arg {
    @NotNull
    private final Location location;
    @NotNull
    private final String value;

    public Arg(@NotNull String value, @NotNull Location location) {
      this.value = value;
      this.location = location;
    }

    @NotNull
    public String getValue() {
      return value;
    }

    @NotNull
    public Location getLocation() {
      return location;
    }
  }

  static abstract class Location {
    @NotNull
    public abstract File getFile();

    @NotNull
    public abstract File getDirectory();

    public abstract int getLine();

    @NotNull
    public abstract String toString();
  }

  static final class CPSourceLocation extends Location {
    private final int line;
    @NotNull
    private final File file;

    public CPSourceLocation(@NotNull File file, int line) {
      this.line = line;
      this.file = file;
    }

    @NotNull
    @Override
    public File getFile() {
      return file;
    }

    @NotNull
    @Override
    public File getDirectory() {
      return file.getParentFile();
    }

    @Override
    public int getLine() {
      return line;
    }

    @NotNull
    public String toString() {
      return file.getPath() + ":" + line;
    }
  }

  static final class CommandLineLocation extends Location {
    @NotNull
    @Override
    public File getFile() {
      return new File(System.getProperty("user.dir"));
    }

    @NotNull
    @Override
    public File getDirectory() {
      return new File(System.getProperty("user.dir"));
    }

    @Override
    public int getLine() {
      return -1;
    }

    @NotNull
    public String toString() {
      return "command-line";
    }
  }

  public static final class ParseException extends RuntimeException {
    @Nullable
    private final Location location;

    public ParseException(@NotNull String message, @Nullable Location location) {
      super(message);
      this.location = location;
    }

    public int getLine() {
      if (location == null) {
        return -1;
      }
      return location.getLine();
    }

    @Nullable
    public File getFile() {
      if (location == null) {
        return null;
      }
      return location.getFile();
    }
  }
}
