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

package org.aspectj.bridge;

import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.util.LangUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

/**
 * Convenience API's for constructing, printing, and sending messages.
 */
public class MessageUtil {

  // ------ some constant, content-less messages
  // no variants for "info" or "debug", which should always have content
  @NotNull
  public static final IMessage ABORT_NOTHING_TO_RUN = new Message("aborting - nothing to run", IMessage.ABORT, null, null);
  @NotNull
  public static final IMessage FAIL_INCOMPLETE = new Message("run not completed", IMessage.FAIL, null, null);
  @NotNull
  public static final IMessage ABORT_NOMESSAGE = new Message("", IMessage.ABORT, null, null);
  @NotNull
  public static final IMessage FAIL_NOMESSAGE = new Message("", IMessage.FAIL, null, null);
  @NotNull
  public static final IMessage ERROR_NOMESSAGE = new Message("", IMessage.ERROR, null, null);
  @NotNull
  public static final IMessage WARNING_NOMESSAGE = new Message("", IMessage.WARNING, null, null);

  // ------------------ visitors to select messages
  @NotNull
  public static final IMessageHandler PICK_ALL = new KindSelector((IMessage.Kind) null);
  @NotNull
  public static final IMessageHandler PICK_ABORT = new KindSelector(IMessage.ABORT);
  @NotNull
  public static final IMessageHandler PICK_DEBUG = new KindSelector(IMessage.DEBUG);
  @NotNull
  public static final IMessageHandler PICK_ERROR = new KindSelector(IMessage.ERROR);
  @NotNull
  public static final IMessageHandler PICK_FAIL = new KindSelector(IMessage.FAIL);
  @NotNull
  public static final IMessageHandler PICK_INFO = new KindSelector(IMessage.INFO);
  @NotNull
  public static final IMessageHandler PICK_WARNING = new KindSelector(IMessage.WARNING);
  @NotNull
  public static final IMessageHandler PICK_ABORT_PLUS = new KindSelector(IMessage.ABORT, true);
  @NotNull
  public static final IMessageHandler PICK_DEBUG_PLUS = new KindSelector(IMessage.DEBUG, true);
  @NotNull
  public static final IMessageHandler PICK_ERROR_PLUS = new KindSelector(IMessage.ERROR, true);
  @NotNull
  public static final IMessageHandler PICK_FAIL_PLUS = new KindSelector(IMessage.FAIL, true);
  @NotNull
  public static final IMessageHandler PICK_INFO_PLUS = new KindSelector(IMessage.INFO, true);
  @NotNull
  public static final IMessageHandler PICK_WARNING_PLUS = new KindSelector(IMessage.WARNING, true);

  /**
   * render message more verbosely if it is worse
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_SCALED = new IMessageRenderer() {
    @NotNull
    public String toString() {
      return "MESSAGE_SCALED";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      final IMessage.Kind kind = message.getKind();
      int level = 3;
      if ((kind == IMessage.ABORT) || (kind == IMessage.FAIL)) {
        level = 1;
      } else if ((kind == IMessage.ERROR) || (kind == IMessage.WARNING)) {
        level = 2;
      } else {
        level = 3;
      }
      String result = null;
      switch (level) {
        case (1):
          result = MESSAGE_TOSTRING.renderToString(message);
          break;
        case (2):
          result = MESSAGE_LINE.renderToString(message);
          break;
        case (3):
          result = MESSAGE_SHORT.renderToString(message);
          break;
      }
      final Throwable thrown = message.getThrown();
      if (null != thrown) {
        if (level == 3) {
          result += "Thrown: \n" + LangUtil.renderExceptionShort(thrown);
        } else {
          result += "Thrown: \n" + LangUtil.renderException(thrown);
        }
      }

      return result;
    }
  };

  /**
   * render message as label, i.e., less than 33 char
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_LABEL = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_LABEL";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 5, 5, 32);
    }
  };

  /**
   * render message as label, i.e., less than 33 char, with no source location
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_LABEL_NOLOC = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_LABEL_NOLOC";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 10, 0, 32);
    }
  };

  /**
   * render message as line, i.e., less than 75 char, no internal line sep
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_LINE = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_LINE";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 8, 2, 74);
    }
  };

  /**
   * render message as line, i.e., less than 75 char, no internal line sep, trying to trim text as needed to end with a full
   * source location
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_LINE_FORCE_LOC = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_LINE_FORCE_LOC";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 2, 40, 74);
    }
  };

  /**
   * render message without restriction, up to 10K, including throwable
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_ALL = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_ALL";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      return renderMessage(message);
    }
  };

  // /** render message without restriction, up to 10K, including (but eliding) throwable */
  // public static final IMessageRenderer MESSAGE_ALL_ELIDED= new IMessageRenderer() {
  // public String toString() { return "MESSAGE_ALL_ELIDED"; }
  // public String renderToString(IMessage message) {
  // return renderMessage(message, true);
  // }
  // };

  /**
   * render message without restriction, except any Throwable thrown
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_MOST = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_MOST";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 1, 1, 10000);
    }
  };

  /**
   * render message as wide line, i.e., less than 256 char, no internal line sep, except any Throwable thrown
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_WIDELINE = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_WIDELINE";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return renderMessageLine(message, 8, 2, 255); // XXX revert to 256
    }
  };
  /**
   * render message using its toString() or "((IMessage) null)"
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_TOSTRING = new IMessageRenderer() {
    public String toString() {
      return "MESSAGE_TOSTRING";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      if (null == message) {
        return "((IMessage) null)";
      }
      return message.toString();
    }
  };

  /**
   * render message using toShortString(IMessage)"
   */
  @NotNull
  public static final IMessageRenderer MESSAGE_SHORT = new IMessageRenderer() {
    @NotNull
    public String toString() {
      return "MESSAGE_SHORT";
    }

    @NotNull
    @Override
    public String renderToString(@NotNull IMessage message) {
      return toShortString(message);
    }
  };

  /**
   * handle abort message (ignored if handler is null)
   */
  public static boolean abort(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(abort(message)));
  }

  /**
   * create and handle exception message (ignored if handler is null)
   */
  public static boolean abort(@Nullable IMessageHandler handler, @NotNull String message, @NotNull Throwable t) {
    if (handler != null) {
      return handler.handleMessage(abort(message, t));
    }
    return false;
  }

  /**
   * create and handle fail message (ignored if handler is null)
   */
  public static boolean fail(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(fail(message)));
  }

  // /** create and handle fail message from reader (ignored if handler is null) */
  // public static boolean fail(IMessageHandler handler, String message, LineReader reader) {
  // return ((null != handler)
  // && handler.handleMessage(fail(message, reader)));
  // }

  /**
   * create and handle fail message (ignored if handler is null)
   */
  public static boolean fail(@Nullable IMessageHandler handler, @NotNull String message, @NotNull Throwable thrown) {
    return ((null != handler) && handler.handleMessage(fail(message, thrown)));
  }

  /**
   * create and handle error message (ignored if handler is null)
   */
  public static boolean error(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(error(message)));
  }

  /**
   * create and handle warn message (ignored if handler is null)
   */
  public static boolean warn(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(warn(message)));
  }

  /**
   * create and handle debug message (ignored if handler is null)
   */
  public static boolean debug(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(debug(message)));
  }

  /**
   * create and handle info message (ignored if handler is null)
   */
  public static boolean info(@Nullable IMessageHandler handler, @NotNull String message) {
    return ((null != handler) && handler.handleMessage(info(message)));
  }

  /**
   * @return ABORT_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage abort(@NotNull String message) {
    if (LangUtil.isEmpty(message)) {
      return ABORT_NOMESSAGE;
    } else {
      return new Message(message, IMessage.ABORT, null, null);
    }
  }

  /**
   * @return abort IMessage with thrown and message o ABORT_NOMESSAGE if both are empty/null
   */
  @NotNull
  public static IMessage abort(@NotNull String message, @Nullable Throwable thrown) {
    if (!LangUtil.isEmpty(message)) {
      return new Message(message, IMessage.ABORT, thrown, null);
    } else if (null == thrown) {
      return ABORT_NOMESSAGE;
    } else {
      return new Message(thrown.getMessage(), IMessage.ABORT, thrown, null);
    }
  }

  /**
   * @return FAIL_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage fail(@NotNull String message) {
    if (LangUtil.isEmpty(message)) {
      return FAIL_NOMESSAGE;
    } else {
      return new Message(message, IMessage.FAIL, null, ISourceLocation.EMPTY);
      // return fail(message, (LineReader) null);
    }
  }

  /**
   * Create fail message. If message is empty but thrown is not, use thrown.getMessage() as the message. If message is empty and
   * thrown is null, return FAIL_NOMESSAGE.
   *
   * @return FAIL_NOMESSAGE if thrown is null and message is empty or IMessage FAIL with message and thrown otherwise
   */
  @NotNull
  public static IMessage fail(@NotNull String message, @Nullable Throwable thrown) {
    if (LangUtil.isEmpty(message)) {
      if (null == thrown) {
        return FAIL_NOMESSAGE;
      } else {
        return new Message(thrown.getMessage(), IMessage.FAIL, thrown, null);
      }
    } else {
      return new Message(message, IMessage.FAIL, thrown, null);
    }
  }

  /**
   * @return IMessage with IMessage.Kind FAIL and message as text and soure location from reader
   */
  // public static IMessage fail(String message) {//, LineReader reader) {
  // ISourceLocation loc = null;
  // if (null == reader) {
  // loc = ISourceLocation.EMPTY;
  // } else {
  // int line = reader.getLineNumber();
  // if (0 < line) {
  // line = 0;
  // }
  // loc = new SourceLocation(reader.getFile(), line, line, 0);
  // }
  // return new Message(message, IMessage.FAIL, null, ISourceLocation.EMPTY);
  // }

  /**
   * @return ERROR_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage error(@NotNull String message, @NotNull ISourceLocation location) {
    if (LangUtil.isEmpty(message)) {
      return ERROR_NOMESSAGE;
    } else {
      return new Message(message, IMessage.ERROR, null, location);
    }
  }

  /**
   * @return WARNING_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage warn(@Nullable String message, @NotNull ISourceLocation location) {
    if (LangUtil.isEmpty(message)) {
      return WARNING_NOMESSAGE;
    } else {
      return new Message(message, IMessage.WARNING, null, location);
    }
  }

  /**
   * @return ERROR_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage error(@Nullable String message) {
    if (LangUtil.isEmpty(message)) {
      return ERROR_NOMESSAGE;
    } else {
      return new Message(message, IMessage.ERROR, null, null);
    }
  }

  /**
   * @return WARNING_NOMESSAGE if message is empty or IMessage otherwise
   */
  @NotNull
  public static IMessage warn(@Nullable String message) {
    if (LangUtil.isEmpty(message)) {
      return WARNING_NOMESSAGE;
    } else {
      return new Message(message, IMessage.WARNING, null, null);
    }
  }

  /**
   * @return IMessage.DEBUG message with message content
   */
  @NotNull
  public static IMessage debug(@Nullable String message) {
    return new Message(message, IMessage.DEBUG, null, null);
  }

  /**
   * @return IMessage.INFO message with message content
   */
  @NotNull
  public static IMessage info(@Nullable String message) {
    return new Message(message, IMessage.INFO, null, null);
  }

  // /** @return ISourceLocation with the current File/line of the reader */
  // public static ISourceLocation makeSourceLocation(LineReader reader) {
  // LangUtil.throwIaxIfNull(reader, "reader");
  //
  // int line = reader.getLineNumber();
  // if (0 < line) {
  // line = 0;
  // }
  // return new SourceLocation(reader.getFile(), line, line, 0);
  // }

  // ------------------------ printing messages

  /**
   * Print total counts message to the print stream, starting each on a new line
   *
   * @param messageHolder
   * @param out
   */
  public static void printMessageCounts(@Nullable PrintStream out, @Nullable IMessageHolder messageHolder) {
    if ((null == out) || (null == messageHolder)) {
      return;
    }
    printMessageCounts(out, messageHolder, "");
  }

  public static void printMessageCounts(@NotNull PrintStream out, @NotNull IMessageHolder holder, @Nullable String prefix) {
    out.println(prefix + "MessageHolder: " + MessageUtil.renderCounts(holder));
  }

  /**
   * Print all message to the print stream, starting each on a new line
   *
   * @param messageHolder
   * @param out
   * @see #print(PrintStream, IMessageHolder, String, IMessageRenderer, IMessageHandler)
   */
  public static void print(@Nullable PrintStream out, @Nullable IMessageHolder messageHolder) {
    print(out, messageHolder, (String) null, (IMessageRenderer) null, (IMessageHandler) null);
  }

  /**
   * Print all message to the print stream, starting each on a new line, with a prefix.
   *
   * @param messageHolder
   * @param out
   * @see #print(PrintStream, IMessageHolder, String, IMessageRenderer, IMessageHandler)
   */
  public static void print(@Nullable PrintStream out, @Nullable IMessageHolder holder, @Nullable String prefix) {
    print(out, holder, prefix, (IMessageRenderer) null, (IMessageHandler) null);
  }

  /**
   * Print all message to the print stream, starting each on a new line, with a prefix and using a renderer.
   *
   * @param messageHolder
   * @param out
   * @param renderer      IMessageRender to render result - use MESSAGE_LINE if null
   * @see #print(PrintStream, IMessageHolder, String, IMessageRenderer, IMessageHandler)
   */
  public static void print(@Nullable PrintStream out, @Nullable IMessageHolder holder, @Nullable String prefix, @Nullable IMessageRenderer renderer) {
    print(out, holder, prefix, renderer, (IMessageHandler) null);
  }

  /**
   * Print all message to the print stream, starting each on a new line, with a prefix and using a renderer. The first line
   * renders a summary: {prefix}MessageHolder: {summary} Each message line has the following form:
   * <p/>
   * <pre>
   * {prefix}[{kind} {index}]: {rendering}
   * </pre>
   * <p/>
   * (where "{index}" (length 3) is the position within the set of like-kinded messages, ignoring selector omissions. Renderers
   * are free to render multi-line output.
   *
   * @param out           the PrintStream sink - return silently if null
   * @param messageHolder the IMessageHolder with the messages to print
   * @param renderer      IMessageRender to render result - use MESSAGE_ALL if null
   * @param selector      IMessageHandler to select messages to render - if null, do all non-null
   */
  public static void print(@Nullable PrintStream out, @Nullable IMessageHolder holder, @Nullable String prefix,
                           @Nullable IMessageRenderer renderer, @Nullable IMessageHandler selector) {
    print(out, holder, prefix, renderer, selector, true);
  }

  public static void print(@Nullable PrintStream out, @Nullable IMessageHolder holder, @Nullable String prefix,
                           @Nullable IMessageRenderer renderer, @Nullable IMessageHandler selector, boolean printSummary) {
    if ((null == out) || (null == holder)) {
      return;
    }
    if (null == renderer) {
      renderer = MESSAGE_ALL;
    }
    if (null == selector) {
      selector = PICK_ALL;
    }
    if (printSummary) {
      out.println(prefix + "MessageHolder: " + MessageUtil.renderCounts(holder));
    }
    for (IMessage.Kind kind : IMessage.KINDS) {
      if (!selector.isIgnoring(kind)) {
        final IMessage[] messages = holder.getMessages(kind, IMessageHolder.EQUAL);
        for (int i = 0; i < messages.length; i++) {
          if (selector.handleMessage(messages[i])) {
            final String label = (null == prefix ? "" : prefix + "[" + kind + " " + LangUtil.toSizedString(i, 3) + "]: ");
            out.println(label + renderer.renderToString(messages[i]));
          }
        }
      }
    }
  }

  @NotNull
  public static String toShortString(@Nullable IMessage message) {
    if (null == message) {
      return "null";
    }
    final String m = message.getMessage();
    final Throwable t = message.getThrown();

    return (message.getKind() + (null == m ? "" : ": " + m) + (null == t ? "" : ": " + LangUtil.unqualifiedClassName(t)));
  }

  /**
   * @return int number of message of this kind (optionally or greater) in list
   */
  public static int numMessages(@Nullable List<IMessage> messages, @Nullable Kind kind, boolean orGreater) {
    if (LangUtil.isEmpty(messages)) {
      return 0;
    }
    final IMessageHandler selector = makeSelector(kind, orGreater, null);
    final IMessage[] result = visitMessages(messages, selector, true, false);
    return result.length;
  }

  /**
   * Select all messages in holder except those of the same kind (optionally or greater). If kind is null, then all messages are
   * rejected, so an empty list is returned.
   *
   * @return unmodifiable list of specified IMessage
   */
  @NotNull
  public static IMessage[] getMessagesExcept(@Nullable IMessageHolder holder, @Nullable final IMessage.Kind kind, final boolean orGreater) {
    if ((null == holder) || (null == kind)) {
      return new IMessage[0];
    }

    final IMessageHandler selector = new IMessageHandler() {
      @Override
      public boolean handleMessage(@NotNull IMessage message) {
        final IMessage.Kind test = message.getKind();
        return (!(orGreater ? kind.isSameOrLessThan(test) : kind == test));
      }

      @Override
      public boolean isIgnoring(@Nullable Kind kind) {
        return false;
      }

      @Override
      public void dontIgnore(IMessage.Kind kind) {

      }

      @Override
      public void ignore(@Nullable Kind kind) {
      }
    };
    return visitMessages(holder, selector, true, false);
  }

  /**
   * @return unmodifiable list of IMessage complying with parameters
   */
  @NotNull
  public static List<IMessage> getMessages(@Nullable IMessageHolder holder, @Nullable Kind kind, boolean orGreater, @Nullable String infix) {
    if (null == holder) {
      return Collections.emptyList();
    }
    if ((null == kind) && LangUtil.isEmpty(infix)) {
      return holder.getUnmodifiableListView();
    }
    final IMessageHandler selector = makeSelector(kind, orGreater, infix);
    final IMessage[] messages = visitMessages(holder, selector, true, false);
    if (LangUtil.isEmpty(messages)) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(Arrays.asList(messages));
  }

  /**
   * Extract messages of type kind from the input list.
   *
   * @param messages if null, return EMPTY_LIST
   * @param kind     if null, return messages
   * @see MessageHandler#getMessages(org.aspectj.bridge.IMessage.Kind, boolean)
   */
  @NotNull
  public static List<IMessage> getMessages(@Nullable List<IMessage> messages, @Nullable Kind kind) {
    if (null == messages) {
      return Collections.emptyList();
    }
    if (null == kind) {
      return messages;
    }
    final ArrayList<IMessage> result = new ArrayList<IMessage>();
    for (IMessage message : messages) {
      if (kind == message.getKind()) {
        result.add(message);
      }
    }
    if (0 == result.size()) {
      return Collections.emptyList();
    }
    return result;
  }

  /**
   * Map to the kind of messages associated with this string key.
   *
   * @param kind the String representing the kind of message (IMessage.Kind.toString())
   * @return Kind the associated IMessage.Kind, or null if not found
   */
  @Nullable
  public static IMessage.Kind getKind(@Nullable String kind) {
    if (null != kind) {
      kind = kind.toLowerCase();
      for (IMessage.Kind k : IMessage.KINDS) {
        if (kind.equals(k.toString())) {
          return k;
        }
      }
    }
    return null;
  }

  /**
   * Run visitor over the set of messages in holder, optionally accumulating those accepted by the visitor
   */
  @NotNull
  public static IMessage[] visitMessages(@Nullable IMessageHolder holder, @NotNull IMessageHandler visitor,
                                         boolean accumulate, boolean abortOnFail) {
    if (null == holder) {
      return IMessage.RA_IMessage;
    } else {
      return visitMessages(holder.getUnmodifiableListView(), visitor, accumulate, abortOnFail);
    }
  }

  /**
   * Run visitor over the set of messages in holder, optionally accumulating those accepted by the visitor
   */
  @NotNull
  public static IMessage[] visitMessages(@Nullable IMessage[] messages, @NotNull IMessageHandler visitor, boolean accumulate, boolean abortOnFail) {
    if (LangUtil.isEmpty(messages)) {
      return IMessage.RA_IMessage;
    } else {
      return visitMessages(Arrays.asList(messages), visitor, accumulate, abortOnFail);
    }
  }

  /**
   * Run visitor over a collection of messages, optionally accumulating those accepted by the visitor
   *
   * @param messages    if null or empty, return IMessage.RA_IMessage
   * @param visitor     run visitor.handleMessage(message) on each message - if null and messages not empty, IllegalArgumentException
   * @param accumulate  if true, then return accepted IMessage[]
   * @param abortOnFail if true and visitor returns false, stop visiting
   * @return IMessage.RA_IMessage if collection is empty, if not accumulate, or if visitor accepted no IMessage, or IMessage[] of
   * accepted messages otherwise
   * @throws IllegalArgumentException if any in collection are not instanceof IMessage
   */
  @NotNull
  public static IMessage[] visitMessages(@Nullable Collection<IMessage> messages, @NotNull IMessageHandler visitor,
                                         final boolean accumulate, final boolean abortOnFail) {
    if (LangUtil.isEmpty(messages)) {
      return IMessage.RA_IMessage;
    }
    LangUtil.throwIaxIfNull(visitor, "visitor");
    final ArrayList<IMessage> result = (accumulate ? new ArrayList<IMessage>() : null);
    for (IMessage m : messages) {
      if (visitor.handleMessage(m)) {
        if (accumulate) {
          result.add(m);
        }
      } else if (abortOnFail) {
        break;
      }
    }
    if (!accumulate || (0 == result.size())) {
      return IMessage.RA_IMessage;
    } else {
      return result.toArray(IMessage.RA_IMessage);
    }
  }

  /**
   * Make an IMessageHandler that handles IMessage if they have the right kind (or greater) and contain some infix String.
   *
   * @param kind      the IMessage.Kind required of the message
   * @param orGreater if true, also accept messages with greater kinds, as defined by IMessage.Kind.COMPARATOR
   * @param infix     the String text to require in the message - may be null or empty to accept any message with the specified kind.
   * @return IMessageHandler selector that works to param specs
   */
  @NotNull
  public static IMessageHandler makeSelector(@Nullable Kind kind, boolean orGreater, @Nullable String infix) {
    if (!orGreater && LangUtil.isEmpty(infix)) {
      if (kind == IMessage.ABORT) {
        return PICK_ABORT;
      } else if (kind == IMessage.DEBUG) {
        return PICK_DEBUG;
      } else if (kind == IMessage.DEBUG) {
        return PICK_DEBUG;
      } else if (kind == IMessage.ERROR) {
        return PICK_ERROR;
      } else if (kind == IMessage.FAIL) {
        return PICK_FAIL;
      } else if (kind == IMessage.INFO) {
        return PICK_INFO;
      } else if (kind == IMessage.WARNING) {
        return PICK_WARNING;
      }
    }
    return new KindSelector(kind, orGreater, infix);
  }

  /**
   * This renders IMessage as String, ignoring empty elements and eliding any thrown stack traces.
   *
   * @return "((IMessage) null)" if null or String rendering otherwise, including everything (esp. throwable stack trace)
   * @see renderSourceLocation(ISourceLocation loc)
   */
  @NotNull
  public static String renderMessage(@Nullable IMessage message) {
    return renderMessage(message, true);
  }

  /**
   * This renders IMessage as String, ignoring empty elements and eliding any thrown.
   *
   * @return "((IMessage) null)" if null or String rendering otherwise, including everything (esp. throwable stack trace)
   * @see renderSourceLocation(ISourceLocation loc)
   */
  @NotNull
  public static String renderMessage(@Nullable IMessage message, boolean elide) {
    if (null == message) {
      return "((IMessage) null)";
    }

    final ISourceLocation loc = message.getSourceLocation();
    final String locString = (null == loc ? "" : " at " + loc);

    String result = message.getKind() + locString + " " + message.getMessage();

    final Throwable thrown = message.getThrown();
    if (thrown != null) {
      result += " -- " + LangUtil.renderExceptionShort(thrown);
      result += "\n" + LangUtil.renderException(thrown, elide);
    }

    if (message.getExtraSourceLocations().isEmpty()) {
      return result;
    } else {
      return addExtraSourceLocations(message, result);
    }
  }

  @NotNull
  public static String addExtraSourceLocations(@NotNull IMessage message, @NotNull String baseMessage) {
    final StringWriter buf = new StringWriter();
    final PrintWriter writer = new PrintWriter(buf);
    writer.println(baseMessage);
    for (final Iterator<ISourceLocation> iter = message.getExtraSourceLocations().iterator(); iter.hasNext(); ) {
      final ISourceLocation element = iter.next();
      if (element != null) {
        writer.print("\tsee also: " + element.toString());
        if (iter.hasNext()) {
          writer.println();
        }
      }
    }
    try {
      buf.close();
    } catch (IOException ioe) {
    }
    return buf.getBuffer().toString();
  }

  /**
   * Render ISourceLocation to String, ignoring empty elements (null or ISourceLocation.NO_FILE or ISourceLocation.NO_COLUMN
   * (though implementations may return 0 from getColumn() when passed NO_COLUMN as input)).
   *
   * @return "((ISourceLocation) null)" if null or String rendering
   * <p/>
   * <pre>
   * {file:}line{:column}
   * </pre>
   */
  @NotNull
  public static String renderSourceLocation(@Nullable ISourceLocation loc) {
    if (null == loc) {
      return "((ISourceLocation) null)";
    }
    final StringBuffer sb = new StringBuffer();

    final File sourceFile = loc.getSourceFile();
    if (sourceFile != ISourceLocation.NO_FILE) {
      sb.append(sourceFile.getPath());
      sb.append(":");
    }
    final int line = loc.getLine();
    sb.append("" + line);

    final int column = loc.getColumn();
    if (column != ISourceLocation.NO_COLUMN) {
      sb.append(":" + column);
    }

    return sb.toString();
  }

  /**
   * Render message in a line. IMessage.Kind is always printed, then any unqualified exception class, then the remainder of text
   * and location according to their relative scale, all to fit in max characters or less. This does not render thrown except for
   * the unqualified class name
   *
   * @param max       the number of characters - forced to 32..10000
   * @param textScale relative proportion to spend on message and/or exception message, relative to source location - if 0,
   *                  message is suppressed
   * @param locScale  relative proportion to spend on source location suppressed if 0
   * @return "((IMessage) null)" or message per spec
   */
  @NotNull
  public static String renderMessageLine(@Nullable IMessage message, int textScale, int locScale, int max) {

    if (null == message) {
      return "((IMessage) null)";
    }
    if (max < 32) {
      max = 32;
    } else if (max > 10000) {
      max = 10000;
    }
    if (0 > textScale) {
      textScale = -textScale;
    }
    if (0 > locScale) {
      locScale = -locScale;
    }

    String text = message.getMessage();
    final Throwable thrown = message.getThrown();
    final ISourceLocation sl = message.getSourceLocation();
    final IMessage.Kind kind = message.getKind();
    final StringBuilder result = new StringBuilder();
    result.append(kind.toString());
    result.append(": ");
    if (null != thrown) {
      result.append(LangUtil.unqualifiedClassName(thrown) + " ");
      if ((null == text) || ("".equals(text))) {
        text = thrown.getMessage();
      }
    }

    if (0 == textScale) {
      text = "";
    } else if ((null != text) && (null != thrown)) {
      // decide between message and exception text?
      final String s = thrown.getMessage();
      if ((null != s) && (0 < s.length())) {
        text += " - " + s;
      }
    }
    String loc = "";
    if ((0 != locScale) && (null != sl)) {
      File f = sl.getSourceFile();
      if (f == ISourceLocation.NO_FILE) {
        f = null;
      }
      if (null != f) {
        loc = f.getName();
      }
      final int line = sl.getLine();
      final int col = sl.getColumn();
      final int end = sl.getEndLine();
      if ((0 == line) && (0 == col) && (0 == end)) {
        // ignore numbers if default
      } else {
        loc += ":" + line + (col == 0 ? "" : ":" + col);
        if (line != end) { // XXX consider suppressing nonstandard...
          loc += ":" + end;
        }
      }
      if (!LangUtil.isEmpty(loc)) {
        loc = "@[" + loc; // matching "]" added below after clipping
      }
    }

    // now budget between text and loc
    final float totalScale = locScale + textScale;
    final float remainder = max - result.length() - 4;
    if ((remainder > 0) && (0 < totalScale)) {
      int textSize = (int) (remainder * textScale / totalScale);
      int locSize = (int) (remainder * locScale / totalScale);
      // adjust for underutilization
      int extra = locSize - loc.length();
      if (0 < extra) {
        locSize = loc.length();
        textSize += extra;
      }
      extra = textSize - text.length();
      if (0 < extra) {
        textSize = text.length();
        if (locSize < loc.length()) {
          locSize += extra;
        }
      }
      if (locSize > loc.length()) {
        locSize = loc.length();
      }
      if (textSize > text.length()) {
        textSize = text.length();
      }
      if (0 < textSize) {
        result.append(text.substring(0, textSize));
      }
      if (0 < locSize) {
        if (0 < textSize) {
          result.append(" ");
        }
        result.append(loc.substring(0, locSize) + "]");
      }
    }
    return result.toString();
  }

  /**
   * @return String of the form "{(# {type}) }.." for message kinds, skipping 0
   */
  @NotNull
  public static String renderCounts(@NotNull IMessageHolder holder) {
    if (0 == holder.numMessages(null, false)) {
      return "(0 messages)";
    }
    final StringBuffer sb = new StringBuffer();
    for (IMessage.Kind kind : IMessage.KINDS) {
      final int num = holder.numMessages(kind, false);
      if (0 < num) {
        sb.append(" (" + num + " " + kind + ") ");
      }
    }
    return sb.toString();
  }

  /**
   * Factory for handler adapted to PrintStream XXX weak - only handles println(String)
   *
   * @param handler the IMessageHandler sink for the messages generated
   * @param kind    the IMessage.Kind of message to create
   * @param overage the OuputStream for text not captured by the handler (if null, System.out used)
   * @throws IllegalArgumentException if kind or handler is null
   */
  @NotNull
  public static PrintStream handlerPrintStream(@NotNull final IMessageHandler handler, @NotNull final IMessage.Kind kind,
                                               @Nullable final OutputStream overage, @Nullable final String prefix) {
    LangUtil.throwIaxIfNull(handler, "handler");
    LangUtil.throwIaxIfNull(kind, "kind");
    class HandlerPrintStream extends PrintStream {
      HandlerPrintStream() {
        super(null == overage ? System.out : overage);
      }

      @Override
      public void println() {
        println("");
      }

      @Override
      public void println(@Nullable Object o) {
        println(null == o ? "null" : o.toString());
      }

      @Override
      public void println(@Nullable String input) {
        final String textMessage = (null == prefix ? input : prefix + input);
        final IMessage m = new Message(textMessage, kind, null, null);
        handler.handleMessage(m);
      }
    }
    return new HandlerPrintStream();
  }

  /**
   * Handle all messages in the second handler using the first
   *
   * @param handler  the IMessageHandler sink for all messages in source
   * @param holder   the IMessageHolder source for all messages to handle
   * @param fastFail if true, stop on first failure
   * @return false if any sink.handleMessage(..) failed
   */
  public static boolean handleAll(@NotNull IMessageHandler sink, @NotNull IMessageHolder source, boolean fastFail) {
    return handleAll(sink, source, null, true, fastFail);
  }

  /**
   * Handle messages in the second handler using the first
   *
   * @param handler   the IMessageHandler sink for all messages in source
   * @param holder    the IMessageHolder source for all messages to handle
   * @param kind      the IMessage.Kind to select, if not null
   * @param orGreater if true, also accept greater kinds
   * @param fastFail  if true, stop on first failure
   * @return false if any sink.handleMessage(..) failed
   */
  public static boolean handleAll(@NotNull IMessageHandler sink, @NotNull IMessageHolder source, @Nullable IMessage.Kind kind, boolean orGreater,
                                  boolean fastFail) {
    LangUtil.throwIaxIfNull(sink, "sink");
    LangUtil.throwIaxIfNull(source, "source");
    return handleAll(sink, source.getMessages(kind, orGreater), fastFail);
  }

  /**
   * Handle messages in the second handler using the first if they are NOT of this kind (optionally, or greater). If you pass null
   * as the kind, then all messages are ignored and this returns true.
   *
   * @param handler   the IMessageHandler sink for all messages in source
   * @param holder    the IMessageHolder source for all messages to handle
   * @param kind      the IMessage.Kind to reject, if not null
   * @param orGreater if true, also reject greater kinds
   * @param fastFail  if true, stop on first failure
   * @return false if any sink.handleMessage(..) failed
   */
  public static boolean handleAllExcept(@NotNull IMessageHandler sink, @NotNull IMessageHolder source, @Nullable Kind kind, boolean orGreater,
                                        boolean fastFail) {
    LangUtil.throwIaxIfNull(sink, "sink");
    LangUtil.throwIaxIfNull(source, "source");
    if (null == kind) {
      return true;
    }
    final IMessage[] messages = getMessagesExcept(source, kind, orGreater);
    return handleAll(sink, messages, fastFail);
  }

  /**
   * Handle messages in the sink.
   *
   * @param handler  the IMessageHandler sink for all messages in source
   * @param sources  the IMessage[] messages to handle
   * @param fastFail if true, stop on first failure
   * @return false if any sink.handleMessage(..) failed
   * @throws IllegalArgumentException if sink is null
   */
  public static boolean handleAll(@NotNull IMessageHandler sink, @Nullable IMessage[] sources, boolean fastFail) {
    LangUtil.throwIaxIfNull(sink, "sink");
    if (LangUtil.isEmpty(sources)) {
      return true;
    }
    boolean result = true;
    for (int i = 0; i < sources.length; i++) {
      if (!sink.handleMessage(sources[i])) {
        if (fastFail) {
          return false;
        }
        if (result) {
          result = false;
        }
      }
    }
    return result;
  }

  /**
   * utility class
   */
  private MessageUtil() {
  }

  /**
   * implementation for PICK_... constants
   */
  private static class KindSelector implements IMessageHandler {
    @Nullable
    final IMessage.Kind sought;
    final boolean floor;
    @Nullable
    final String infix;

    KindSelector(@Nullable Kind sought) {
      this(sought, false);
    }

    KindSelector(@Nullable Kind sought, boolean floor) {
      this(sought, floor, null);
    }

    KindSelector(@Nullable Kind sought, boolean floor, @Nullable String infix) {
      this.sought = sought;
      this.floor = floor;
      this.infix = (LangUtil.isEmpty(infix) ? null : infix);
    }

    /**
     * @return false if this message is null, of true if we seek any kind (null) or if this has the exact kind we seek and this
     * has any text sought
     */
    @Override
    public boolean handleMessage(@NotNull IMessage message) {
      return ((null != message) && !isIgnoring(message.getKind()) && textIn(message));
    }

    /**
     * @return true if handleMessage would return false for a message of this kind
     */
    @Override
    public boolean isIgnoring(@Nullable Kind kind) {
      if (!floor)
        return ((null != sought) && (sought != kind));

      if (null == sought)
        return false;
      return (0 < IMessage.Kind.COMPARATOR.compare(sought, kind));
    }

    @Override
    public void dontIgnore(IMessage.Kind kind) {

    }

    @Override
    public void ignore(@Nullable Kind kind) {
    }

    private boolean textIn(@NotNull IMessage message) {
      if (null == infix) {
        return true;
      }
      final String text = message.getMessage();
      return (text.indexOf(infix) != -1);
    }
  }

  // ------------------ components to render messages

  /**
   * parameterize rendering behavior for messages
   */
  public static interface IMessageRenderer {
    @NotNull
    String renderToString(@NotNull IMessage message);
  }
}
