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


package org.aspectj.ajdt.ajc;

import org.aspectj.ajdt.internal.core.builder.AjBuildConfig;
import org.aspectj.ajdt.internal.core.builder.AjBuildManager;
import org.aspectj.bridge.*;
import org.aspectj.org.eclipse.jdt.internal.core.builder.MissingSourceFileException;
import org.aspectj.weaver.Dump;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ICommand adapter for the AspectJ compiler.
 * Not thread-safe.
 */
public final class AjdtCommand implements ICommand {

  /**
   * Message String for any AbortException thrown from ICommand API's
   */
  @NotNull
  public static final String ABORT_MESSAGE = "ABORT";

//    private boolean canRepeatCommand = true;

  @Nullable
  AjBuildManager buildManager = null;
  @Nullable
  String[] savedArgs = null;

  /**
   * This creates a build configuration for the arguments.
   * Errors reported to the handler:
   * <ol>
   * <li>The parser detects some directly</li>
   * <li>The parser grabs some from the error stream
   * emitted by its superclass</li>
   * <li>The configuration has a self-test</li>
   * </ol>
   * In the latter two cases, the errors do not have
   * a source location context for locating the error.
   */
  @NotNull
  public static AjBuildConfig genBuildConfig(@NotNull String[] args, @NotNull CountingMessageHandler handler) {
    final BuildArgParser parser = new BuildArgParser(handler);
    final AjBuildConfig config = parser.genBuildConfig(args);

    ISourceLocation location = null;
    if (config.getConfigFile() != null) {
      location = new SourceLocation(config.getConfigFile(), 0);
    }

    final String message = parser.getOtherMessages(true);
    if (null != message) {
      final IMessage.Kind kind = inferKind(message);
      final IMessage m = new Message(message, kind, null, location);
      handler.handleMessage(m);
    }
//        message = config.configErrors();
//        if (null != message) {
//            IMessage.Kind kind = inferKind(message);
//            IMessage m = new Message(message, kind, null, location);            
//            handler.handleMessage(m);
//        }
    return config;
  }

  /**
   * Run AspectJ compiler, wrapping any exceptions thrown as
   * ABORT messages (containing ABORT_MESSAGE String).
   *
   * @param args    the String[] for the compiler
   * @param handler the IMessageHandler for any messages
   * @return false if handler has errors or the command failed
   * @see org.aspectj.bridge.ICommand#runCommand(String[], IMessageHandler)
   */
  @Override
  public boolean runCommand(@NotNull String[] args, @NotNull IMessageHandler handler) {
    buildManager = new AjBuildManager(handler);
    savedArgs = new String[args.length];
    System.arraycopy(args, 0, savedArgs, 0, savedArgs.length);
    for (int i = 0; i < args.length; i++) {
// AMC - PR58681. No need to abort on -help as the Eclipse compiler does the right thing.
//            if ("-help".equals(args[i])) {
//                // should be info, but handler usually suppresses
//                MessageUtil.abort(handler, BuildArgParser.getUsage());
//                return true;
//            } else 
      if ("-X".equals(args[i])) {
        // should be info, but handler usually suppresses
        MessageUtil.abort(handler, BuildArgParser.getXOptionUsage());
        return true;
      }
    }
    return doCommand(handler, false);
  }

  /**
   * Run AspectJ compiler, wrapping any exceptions thrown as
   * ABORT messages (containing ABORT_MESSAGE String).
   *
   * @param handler the IMessageHandler for any messages
   * @return false if handler has errors or the command failed
   * @see org.aspectj.bridge.ICommand#repeatCommand(IMessageHandler)
   */
  @Override
  public boolean repeatCommand(@NotNull IMessageHandler handler) {
    if (null == buildManager) {
      MessageUtil.abort(handler, "repeatCommand called before runCommand");
      return false;
    }
    return doCommand(handler, true);
  }

  /**
   * Delegate of both runCommand and repeatCommand.
   * This invokes the argument parser each time
   * (even when repeating).
   * If the parser detects errors, this signals an
   * abort with the usage message and returns false.
   *
   * @param handler the IMessageHandler sink for any messages
   * @param repeat  if true, do incremental build, else do batch build
   * @return false if handler has any errors or command failed
   */
  protected boolean doCommand(@NotNull IMessageHandler handler, boolean repeat) {
    try {
      if (handler instanceof IMessageHolder) {
        Dump.saveMessageHolder((IMessageHolder) handler);
      }
      // buildManager.setMessageHandler(handler);
      final CountingMessageHandler counter = new CountingMessageHandler(handler);
      if (counter.hasErrors()) {
        return false;
      }
      // regenerate configuration b/c world might have changed (?)
      final AjBuildConfig config = genBuildConfig(savedArgs, counter);
      if (!config.shouldProceed()) {
        return true;
      }
      if (!config.hasSources()) {
        MessageUtil.error(counter, "no sources specified");
      }
      if (counter.hasErrors()) { // print usage for config errors
        final String usage = BuildArgParser.getUsage();
        MessageUtil.abort(handler, usage);
        return false;
      }
      //System.err.println("errs: " + counter.hasErrors());          
      final boolean result = ((repeat
          ? buildManager.incrementalBuild(config, handler)
          : buildManager.batchBuild(config, handler))
          && !counter.hasErrors());
      Dump.dumpOnExit();
      return result;
    } catch (AbortException ae) {
      if (ae.isSilent()) {
        throw ae;
      } else {
        MessageUtil.abort(handler, ABORT_MESSAGE, ae);
      }
    } catch (MissingSourceFileException t) {
      MessageUtil.error(handler, t.getMessage());
    } catch (Throwable t) {
      MessageUtil.abort(handler, ABORT_MESSAGE, t);
      Dump.dumpWithException(t);
    }
    return false;
  }

  /**
   * @return IMessage.WARNING unless message contains error or info
   */
  @NotNull
  protected static IMessage.Kind inferKind(@NotNull String message) { // XXX dubious
    if (-1 != message.indexOf("error")) {
      return IMessage.ERROR;
    } else if (-1 != message.indexOf("info")) {
      return IMessage.INFO;
    } else {
      return IMessage.WARNING;
    }
  }
}
