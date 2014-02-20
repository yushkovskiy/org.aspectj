/* *******************************************************************
 * Copyright (c) 2010 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Andy Clement - SpringSource
 * ******************************************************************/
package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.ajdt.internal.compiler.IOutputClassFileNameProvider;
import org.aspectj.asm.internal.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.patterns.ExactTypePattern;
import org.aspectj.weaver.patterns.TypePattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Collects up information about the application of ITDs and relevant declares - it can then output source code as if those ITDs had
 * been pushed in. Supports the simulated push-in of:
 * <ul>
 * <li>declare at_type
 * <li>itd method
 * <li>itd field
 * <li>itd ctor
 * <li>declare parents
 * </ul>
 *
 * @author Andy Clement
 * @since 1.6.9
 */
public final class PushinCollector {
  @NotNull
  private final static String OPTION_SUFFIX = "suffix";
  @NotNull
  private final static String OPTION_DIR = "dir";
  @NotNull
  private final static String OPTION_PKGDIRS = "packageDirs";
  @NotNull
  private final static String OPTION_DEBUG = "debug";
  @NotNull
  private final static String OPTION_LINENUMS = "lineNums";
  @NotNull
  private final static String OPTION_DUMPUNCHANGED = "dumpUnchanged";

  @NotNull
  private final World world;
  private boolean debug = false;
  private boolean dumpUnchanged = false;
  @Nullable
  private IOutputClassFileNameProvider outputFileNameProvider;
  @Nullable
  private final String specifiedOutputDirectory;
  private final boolean includePackageDirs;
  private final boolean includeLineNumberComments;
  @NotNull
  private final String suffix;

  // This first collection stores the 'text' for the declarations.
  @NotNull
  private final Map<AbstractMethodDeclaration, RepresentationAndLocation> codeRepresentation = new HashMap<AbstractMethodDeclaration, RepresentationAndLocation>();

  // This stores the new annotations
  @NotNull
  private final Map<SourceTypeBinding, List<String>> additionalAnnotations = new HashMap<SourceTypeBinding, List<String>>();

  // This stores the new parents
  @NotNull
  private final Map<SourceTypeBinding, List<ExactTypePattern>> additionalParents = new HashMap<SourceTypeBinding, List<ExactTypePattern>>();

  // This indicates which types are affected by which intertype declarations
  @NotNull
  private final Map<SourceTypeBinding, List<AbstractMethodDeclaration>> newDeclarations = new HashMap<SourceTypeBinding, List<AbstractMethodDeclaration>>();

  /**
   * Checks if the aspectj.pushin property is set - this is the main condition for triggering the creation of pushed-in source
   * files. If not set just to 'true', the value of the property is processed as configuration. Configurable options are:
   * <ul>
   * <li>dir=XXXX - to set the output directory for the pushed in files
   * <li>suffix=XXX - to set the suffix, can be blank to get just '.java'
   * </ul>
   */
  @Nullable
  public static PushinCollector createInstance(@NotNull World world) {
    try {
      final String property = System.getProperty("aspectj.pushin");
      if (property == null) {
        return null;
      }
      final Properties configuration = new Properties();
      final StringTokenizer tokenizer = new StringTokenizer(property, ",");
      while (tokenizer.hasMoreElements()) {
        final String token = tokenizer.nextToken();
        // Simplest thing to do is turn it on 'aspectj.pushin=true'
        if (token.equalsIgnoreCase("true")) {
          continue;
        }
        final int positionOfEquals = token.indexOf("=");
        if (positionOfEquals != -1) {
          // it is an option
          final String optionName = token.substring(0, positionOfEquals);
          final String optionValue = token.substring(positionOfEquals + 1);
          configuration.put(optionName, optionValue);
        } else {
          // it is a flag
          configuration.put(token, "true");
        }
      }
      return new PushinCollector(world, configuration);
    } catch (Exception e) {
      // unable to read system properties...
    }
    return null;
  }

  private PushinCollector(@NotNull World world, @NotNull Properties configuration) {
    this.world = world;

    // Configure the instance based on the input properties
    specifiedOutputDirectory = configuration.getProperty(OPTION_DIR);
    includePackageDirs = configuration.getProperty(OPTION_PKGDIRS, "true").equalsIgnoreCase("true");
    includeLineNumberComments = configuration.getProperty(OPTION_LINENUMS, "false").equalsIgnoreCase("true");
    debug = configuration.getProperty(OPTION_DEBUG, "false").equalsIgnoreCase("true");
    dumpUnchanged = configuration.getProperty(OPTION_DUMPUNCHANGED, "false").equalsIgnoreCase("true");
    final String specifiedSuffix = configuration.getProperty(OPTION_SUFFIX, "pushedin");
    if (specifiedSuffix.length() > 0) {
      final StringBuilder sb = new StringBuilder();
      sb.append(".").append(specifiedSuffix);
      suffix = sb.toString();
    } else {
      suffix = "";
    }
    if (debug) {
      System.out.println("Configured to create pushin side files:" + configuration);
      System.out.println("dumpUnchanged=" + dumpUnchanged + "\nincludePackageDirs=" + includePackageDirs);
    }

  }

  /**
   * Produce the modified source that looks like the itds and declares have been applied.
   */
  public void dump(@NotNull CompilationUnitDeclaration compilationUnitDeclaration, String outputFileLocation) {
    if (compilationUnitDeclaration.scope.topLevelTypes == null || compilationUnitDeclaration.scope.topLevelTypes.length == 0) {
      return;
    }
    final SourceTypeBinding[] types = compilationUnitDeclaration.scope.topLevelTypes;
    if (types == null || types.length == 0) {
      return;
    }

    // Process all types working from end to start as whatever we do (insert-wise) will affect locations later in the file
    final StringBuffer sourceContents = new StringBuffer();
    // put the whole original file in the buffer
    boolean changed = false;
    sourceContents.append(compilationUnitDeclaration.compilationResult.compilationUnit.getContents());
    for (int t = types.length - 1; t >= 0; t--) {
      final SourceTypeBinding sourceTypeBinding = compilationUnitDeclaration.scope.topLevelTypes[t];
      if (!hasChanged(sourceTypeBinding)) {
        if (debug) {
          System.out.println(getName(compilationUnitDeclaration) + " has nothing applied");
        }
        continue;
      }
      changed = true;
      final int bodyEnd = sourceTypeBinding.scope.referenceContext.bodyEnd; // last '}' of the type
      final List<AbstractMethodDeclaration> declarations = newDeclarations.get(sourceTypeBinding);
      if (declarations != null) {
        for (AbstractMethodDeclaration md : declarations) {
          final RepresentationAndLocation ral = codeRepresentation.get(md);
          if (ral != null) {
            final String s = ral.textualRepresentation;
            sourceContents.insert(bodyEnd, "\n" + s + "\n");
            if (includeLineNumberComments && ral.linenumber != -1) {
              sourceContents.insert(bodyEnd, "\n    // " + ral.linenumber);
            }
          }
        }
      }

      // fix up declare parents - may need to attach them to existing ones
      final TypeReference sr = sourceTypeBinding.scope.referenceContext.superclass;
      final TypeReference[] trs = sourceTypeBinding.scope.referenceContext.superInterfaces;
      final List<ExactTypePattern> newParents = additionalParents.get(sourceTypeBinding);
      final StringBuilder extendsString = new StringBuilder();
      final StringBuilder implementsString = new StringBuilder();
      if (newParents != null && newParents.size() > 0) {
        for (ExactTypePattern newParent : newParents) {
          final ResolvedType newParentType = newParent.getExactType().resolve(world);
          if (newParentType.isInterface()) {
            if (implementsString.length() > 0) {
              implementsString.append(",");
            }
            implementsString.append(newParentType.getName());
          } else {
            extendsString.append(newParentType.getName());
          }
        }
        if (trs == null && sr == null) {
          // nothing after the class declaration, let's insert what we need to
          // Find the position just before the type opening '{'
          final int beforeOpeningCurly = sourceTypeBinding.scope.referenceContext.bodyStart - 1;
          if (implementsString.length() != 0) {
            implementsString.insert(0, "implements ");
            implementsString.append(" ");
            sourceContents.insert(beforeOpeningCurly, implementsString);
          }
          if (extendsString.length() != 0) {
            extendsString.insert(0, "extends ");
            extendsString.append(" ");
            sourceContents.insert(beforeOpeningCurly, extendsString);
          }
        }
      }
      final List<String> annos = additionalAnnotations.get(sourceTypeBinding);
      if (annos != null && annos.size() > 0) {
        for (String anno : annos) {
          sourceContents.insert(sourceTypeBinding.scope.referenceContext.declarationSourceStart, anno + " ");
        }
      }
    }
    if (changed || (!changed && dumpUnchanged)) {
      try {
        if (debug) {
          System.out.println("Pushed in output file being written to " + outputFileLocation);
          System.out.println(sourceContents);
        }
        final FileWriter fos = new FileWriter(new File(outputFileLocation));
        fos.write(sourceContents.toString());
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void recordInterTypeMethodDeclarationCode(AbstractMethodDeclaration md, String s, int line) {
    codeRepresentation.put(md, new RepresentationAndLocation(s, line));
  }

  public void recordInterTypeFieldDeclarationCode(AbstractMethodDeclaration md, String s, int line) {
    codeRepresentation.put(md, new RepresentationAndLocation(s, line));
  }

  public void recordInterTypeConstructorDeclarationCode(AbstractMethodDeclaration md, String s, int line) {
    codeRepresentation.put(md, new RepresentationAndLocation(s, line));
  }

  // public void recordDeclareAnnotationDeclarationCode(AbstractMethodDeclaration md, String value) {
  // codeRepresentation.put(md, new RepresentationAndLocation(value, -1));
  // }

  public void tagAsMunged(SourceTypeBinding sourceType, AbstractMethodDeclaration sourceMethod) {
    if (sourceMethod == null) {
      // seen when an ITD field is made onto an interface. It matches, but the sourceMethod is null.
      // can be null for binary weave (there is no source method)
      return;
    }
    List<AbstractMethodDeclaration> amds = newDeclarations.get(sourceType);
    if (amds == null) {
      amds = new ArrayList<AbstractMethodDeclaration>();
      newDeclarations.put(sourceType, amds);
    }
    amds.add(sourceMethod);
  }

  public void tagAsMunged(SourceTypeBinding sourceType, String annotationString) {
    List<String> annos = additionalAnnotations.get(sourceType);
    if (annos == null) {
      annos = new ArrayList<String>();
      additionalAnnotations.put(sourceType, annos);
    }
    annos.add(annotationString);
  }

  public void dump(@NotNull CompilationUnitDeclaration unit) {
    final String outputFile = getOutputFileFor(unit);
    if (debug) {
      System.out
          .println("Output location is " + outputFile + " for " + new String(unit.scope.referenceContext.getFileName()));
    }
    dump(unit, outputFile);
  }

  public void tagAsMunged(SourceTypeBinding sourceType, TypePattern typePattern) {
    if (typePattern instanceof ExactTypePattern) {
      List<ExactTypePattern> annos = additionalParents.get(sourceType);
      if (annos == null) {
        annos = new ArrayList<ExactTypePattern>();
        additionalParents.put(sourceType, annos);
      }
      annos.add((ExactTypePattern) typePattern);
    }
  }

  public void setOutputFileNameProvider(@Nullable IOutputClassFileNameProvider outputFileNameProvider) {
    this.outputFileNameProvider = outputFileNameProvider;
  }

  @NotNull
  private static String getName(@Nullable CompilationUnitDeclaration cud) {
    if (cud == null) {
      return "UNKNOWN";
    }
    if (cud.scope == null) {
      return "UNKNOWN";
    }
    if (cud.scope.referenceContext == null) {
      return "UNKNOWN";
    }
    return new String(cud.scope.referenceContext.getFileName());
  }

  /**
   * @return true if the type is affected by something (itd/declare anno/declare parent)
   */
  private boolean hasChanged(SourceTypeBinding stb) {
    return newDeclarations.get(stb) != null || additionalParents.get(stb) != null || additionalAnnotations.get(stb) != null;
  }

  @NotNull
  private String getOutputFileFor(@NotNull CompilationUnitDeclaration unit) {
    final StringBuilder sb = new StringBuilder();

    // Create the directory portion of the output location
    if (specifiedOutputDirectory != null) {
      sb.append(specifiedOutputDirectory).append(File.separator);
    } else {
      final String sss = outputFileNameProvider.getOutputClassFileName("A".toCharArray(), unit.compilationResult);
      sb.append(sss, 0, sss.length() - 7);
    }

    // Create the subdirectory structure matching the package declaration
    if (includePackageDirs) {
      final char[][] packageName = unit.compilationResult.packageName;
      if (packageName != null) {
        sb.append(CharOperation.concatWith(unit.compilationResult.packageName, File.separatorChar));
        sb.append(File.separator);
      }
    }

    new File(sb.toString()).mkdirs();

    // Create the filename portion
    final String filename = new String(unit.getFileName()); // gives 'n:\A.java'
    final int index = filename.lastIndexOf('/');
    final int index2 = filename.lastIndexOf('\\');
    if (index > index2) {
      sb.append(filename.substring(index + 1));
    } else if (index2 > index) {
      sb.append(filename.substring(index2 + 1));
    } else {
      sb.append(filename);
    }

    // Add the suffix (may be an empty string)
    sb.append(suffix);
    return sb.toString();
  }

  /**
   * Encapsulates a text representation (source code) for a member and the line where it was declared.
   */
  private static class RepresentationAndLocation {

    String textualRepresentation;
    int linenumber;

    public RepresentationAndLocation(String textualRepresentation, int linenumber) {
      this.textualRepresentation = textualRepresentation;
      this.linenumber = linenumber;
    }
  }
}
