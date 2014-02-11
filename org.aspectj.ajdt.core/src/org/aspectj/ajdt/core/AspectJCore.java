/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.aspectj.ajdt.core;

import org.aspectj.ajdt.internal.core.builder.AjCompilerOptions;
import org.aspectj.org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This is the plugin class for AspectJ.
 */
public final class AspectJCore extends JavaCore {
  @NotNull
  public static final String COMPILER_PB_INVALID_ABSOLUTE_TYPE_NAME = AjCompilerOptions.OPTION_ReportInvalidAbsoluteTypeName;
  @NotNull
  public static final String COMPILER_PB_INVALID_WILDCARD_TYPE_NAME = AjCompilerOptions.OPTION_ReportInvalidWildcardTypeName;
  @NotNull
  public static final String COMPILER_PB_UNRESOLVABLE_MEMBER = AjCompilerOptions.OPTION_ReportUnresolvableMember;
  @NotNull
  public static final String COMPILER_PB_TYPE_NOT_EXPOSED_TO_WEAVER = AjCompilerOptions.OPTION_ReportTypeNotExposedToWeaver;
  @NotNull
  public static final String COMPILER_PB_SHADOW_NOT_IN_STRUCTURE = AjCompilerOptions.OPTION_ReportShadowNotInStructure;
  @NotNull
  public static final String COMPILER_PB_UNMATCHED_SUPERTYPE_IN_CALL = AjCompilerOptions.OPTION_ReportUnmatchedSuperTypeInCall;
  @NotNull
  public static final String COMPILER_PB_CANNOT_IMPLEMENT_LAZY_TJP = AjCompilerOptions.OPTION_ReportCannotImplementLazyTJP;
  @NotNull
  public static final String COMPILER_PB_NEED_SERIAL_VERSION_UID = AjCompilerOptions.OPTION_ReportNeedSerialVersionUIDField;
  @NotNull
  public static final String COMPILER_PB_INCOMPATIBLE_SERIAL_VERSION = AjCompilerOptions.OPTION_ReportIncompatibleSerialVersion;

  @NotNull
  public static final String COMPILER_TERMINATE_AFTER_COMPILATION = AjCompilerOptions.OPTION_TerminateAfterCompilation;
  @NotNull
  public static final String COMPILER_SERIALIZABLE_ASPECTS = AjCompilerOptions.OPTION_XSerializableAspects;
  @NotNull
  public static final String COMPILER_LAZY_TJP = AjCompilerOptions.OPTION_XLazyThisJoinPoint;
  @NotNull
  public static final String COMPILER_NO_ADVICE_INLINE = AjCompilerOptions.OPTION_XNoInline;
  @NotNull
  public static final String COMPILER_NOT_REWEAVABLE = AjCompilerOptions.OPTION_XNotReweavable;

  @NotNull
  public static AspectJCore getAspectJCore() {
    return (AspectJCore) getPlugin();
  }

  public AspectJCore() {
    super();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jdt.core.JavaCore#getCompilerOptions()
   */
  @NotNull
  protected static Map getCompilerOptions() {
    return new AjCompilerOptions().getMap();
  }
}
