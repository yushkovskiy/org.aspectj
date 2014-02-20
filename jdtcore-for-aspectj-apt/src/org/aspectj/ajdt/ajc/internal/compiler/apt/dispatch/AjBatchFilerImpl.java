package org.aspectj.ajdt.ajc.internal.compiler.apt.dispatch;

import org.aspectj.org.eclipse.jdt.internal.compiler.apt.dispatch.BaseAnnotationProcessorManager;
import org.aspectj.org.eclipse.jdt.internal.compiler.apt.dispatch.BatchFilerImpl;
import org.aspectj.org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl;
import org.aspectj.org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import java.io.*;
import java.net.URI;

/**
 * @author p.yushkovskiy
 */
public final class AjBatchFilerImpl extends BatchFilerImpl {

  public AjBatchFilerImpl(@NotNull BaseAnnotationProcessorManager dispatchManager, @NotNull BatchProcessingEnvImpl env) {
    super(dispatchManager, env);
  }

  @Override
  @NotNull
  public FileObject createResource(@NotNull JavaFileManager.Location location, @NotNull CharSequence pkg, @NotNull CharSequence relativeName, @NotNull Element... originatingElements) throws IOException {
    final String name = String.valueOf(relativeName);
    final FileObject resource = super.createResource(location, pkg, relativeName, originatingElements);
    if (!name.endsWith(".aj")) {
      return resource;
    }
    return new HookedFileObject(resource);
  }

  private final class HookedFileObject implements FileObject {
    @NotNull
    private final FileObject fileObject;
    private boolean _closed = false;

    HookedFileObject(@NotNull FileObject fileObject) {
      this.fileObject = fileObject;
    }

    @NotNull
    @Override
    public URI toUri() {
      return fileObject.toUri();
    }

    @NotNull
    @Override
    public String getName() {
      return fileObject.getName();
    }

    @NotNull
    @Override
    public InputStream openInputStream() throws IOException {
      return fileObject.openInputStream();
    }

    @NotNull
    @Override
    public OutputStream openOutputStream() throws IOException {
      return new ForwardingOutputStream(fileObject.openOutputStream());
    }

    @NotNull
    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
      return fileObject.openReader(ignoreEncodingErrors);
    }

    @NotNull
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return fileObject.getCharContent(ignoreEncodingErrors);
    }

    @NotNull
    @Override
    public Writer openWriter() throws IOException {
      return new ForwardingWriter(fileObject.openWriter());
    }

    @Override
    public long getLastModified() {
      return fileObject.getLastModified();
    }

    @Override
    public boolean delete() {
      return fileObject.delete();
    }

    private void onClose() {
      if (_closed)
        return;
      _closed = true;
      final String name = fileObject.getName();
      final CompilationUnit unit = new CompilationUnit(null, name, null /* encoding */);
      addNewUnit(unit);
    }

    private final class ForwardingWriter extends Writer {
      @NotNull
      private final Writer writer;

      public ForwardingWriter(@NotNull Writer writer) {
        this.writer = writer;
      }

      @Override
      public void write(int c) throws IOException {
        writer.write(c);
      }

      @Override
      public void write(@NotNull char[] cbuf) throws IOException {
        writer.write(cbuf);
      }

      @Override
      public void write(@NotNull String str) throws IOException {
        writer.write(str);
      }

      @Override
      public void write(@NotNull String str, int off, int len) throws IOException {
        writer.write(str, off, len);
      }

      @Override
      public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
      }

      @Override
      public void flush() throws IOException {
        writer.flush();
      }

      @Override
      public void close() throws IOException {
        writer.close();
        onClose();
      }

      @NotNull
      @Override
      public Writer append(@NotNull CharSequence csq) throws IOException {
        return writer.append(csq);
      }

      @NotNull
      @Override
      public Writer append(@NotNull CharSequence csq, int start, int end) throws IOException {
        return writer.append(csq, start, end);
      }

      @NotNull
      @Override
      public Writer append(char c) throws IOException {
        return writer.append(c);
      }
    }

    private final class ForwardingOutputStream extends OutputStream {
      @NotNull
      private final OutputStream stream;

      public ForwardingOutputStream(@NotNull OutputStream stream) {
        this.stream = stream;
      }

      @Override
      public void write(@NotNull byte[] b) throws IOException {
        stream.write(b);
      }

      @Override
      public void write(@NotNull byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
      }

      @Override
      public void flush() throws IOException {
        stream.flush();
      }

      @Override
      public void close() throws IOException {
        stream.close();
        onClose();
      }

      @Override
      public void write(int b) throws IOException {
        stream.write(b);
      }
    }
  }
}
