/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.compiler;

import juzu.impl.common.Tools;
import juzu.impl.common.FileKey;
import juzu.impl.compiler.file.JavaFileObjectImpl;
import juzu.impl.compiler.file.SimpleFileManager;
import juzu.impl.fs.Filter;
import juzu.impl.fs.Visitor;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.ReadWriteFileSystem;
import juzu.impl.fs.spi.ram.RAMFileSystem;
import juzu.impl.common.Location;
import juzu.impl.common.Spliterator;

import javax.annotation.processing.Processor;
import javax.inject.Provider;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Compiler {

  public static Builder builder() {
    return new Builder(null, null, null, new ArrayList<ReadFileSystem<?>>());
  }

  public static class Builder {

    /** . */
    private ReadFileSystem<?> sourcePath;

    /** . */
    private ReadWriteFileSystem<?> classOutput;

    /** . */
    private ReadWriteFileSystem<?> sourceOutput;

    /** . */
    private List<ReadFileSystem<?>> classPaths;

    /** . */
    private Provider<? extends Processor> processor;

    /** . */
    private JavaCompiler javaCompiler;

    /** . */
    private CompilerConfig config;

    private Builder(
      ReadFileSystem<?> sourcePath,
      ReadWriteFileSystem<?> sourceOutput,
      ReadWriteFileSystem<?> classOutput,
      List<ReadFileSystem<?>> classPaths) {
      this.processor = null;
      this.javaCompiler = ToolProvider.getSystemJavaCompiler();
      this.sourcePath = sourcePath;
      this.sourceOutput = sourceOutput;
      this.classOutput = classOutput;
      this.classPaths = classPaths;
      this.config = new CompilerConfig();
    }

    public Builder javaCompiler(JavaCompiler javaCompiler) {
      this.javaCompiler = javaCompiler;
      return this;
    }

    public Builder processor(Provider<? extends Processor> processor) {
      this.processor = processor;
      return this;
    }

    public Builder classOutput(ReadWriteFileSystem<?> classOutput) {
      this.classOutput = classOutput;
      return this;
    }

    public Builder output(ReadWriteFileSystem<?> output) {
      this.classOutput = this.sourceOutput = output;
      return this;
    }

    public Builder sourcePath(ReadFileSystem<?> sourcePath) {
      this.sourcePath = sourcePath;
      return this;
    }

    public Builder sourceOutput(ReadWriteFileSystem<?> sourceOutput) {
      this.sourceOutput = sourceOutput;
      return this;
    }

    public Builder addClassPath(ReadFileSystem<?> classPath) {
      classPaths.add(classPath);
      return this;
    }

    public Builder addClassPath(Iterable<ReadFileSystem<?>> classPaths) {
      for (ReadFileSystem<?> classPath : classPaths) {
        addClassPath(classPath);
      }
      return this;
    }

    public Builder config(CompilerConfig config) {
      this.config = config;
      return this;
    }

    public Compiler build() {
      if (sourcePath == null) {
        throw new IllegalStateException("No null source path");
      }
      if (classOutput == null) {
        throw new IllegalStateException("No null class output");
      }
      if (sourceOutput == null) {
        throw new IllegalStateException("No null source output");
      }
      Compiler compiler = new Compiler(
        javaCompiler,
        sourcePath,
        classPaths,
        sourceOutput,
        classOutput,
        config
      );
      if (processor != null) {
        compiler.addAnnotationProcessor(processor.get());
      }
      return compiler;
    }
  }

  /** . */
  static final Pattern PATTERN = Pattern.compile("\\[" + "([^\\]]+)" + "\\]\\(" + "([^\\)]*)" + "\\)");

  /** . */
  private JavaCompiler javaCompiler;

  /** . */
  private Set<Processor> processors;

  /** . */
  private ReadFileSystem<?> sourcePath;

  /** . */
  private ReadWriteFileSystem<?> classOutput;

  /** . */
  private ReadWriteFileSystem<?> sourceOutput;

  /** . */
  private Collection<ReadFileSystem<?>> classPaths;

  /** . */
  private CompilerConfig config;

  public Compiler(
    ReadFileSystem<?> sourcePath,
    ReadWriteFileSystem<?> output,
    CompilerConfig config) {
    this(sourcePath, output, output, config);
  }

  public Compiler(
    ReadFileSystem<?> sourcePath,
    ReadWriteFileSystem<?> sourceOutput,
    ReadWriteFileSystem<?> classOutput,
    CompilerConfig config) {
    this(sourcePath, Collections.<ReadFileSystem<?>>emptyList(), sourceOutput, classOutput, config);
  }

  public Compiler(
    ReadFileSystem<?> sourcePath,
    ReadFileSystem<?> classPath,
    ReadWriteFileSystem<?> sourceOutput,
    ReadWriteFileSystem<?> classOutput,
    CompilerConfig config) {
    this(sourcePath, Collections.<ReadFileSystem<?>>singletonList(classPath), sourceOutput, classOutput, config);
  }

  public Compiler(
      ReadFileSystem<?> sourcePath,
      Collection<ReadFileSystem<?>> classPaths,
      ReadWriteFileSystem<?> sourceOutput,
      ReadWriteFileSystem<?> classOutput,
      CompilerConfig config) {
    this(ToolProvider.getSystemJavaCompiler(), sourcePath, classPaths, sourceOutput, classOutput, config);
  }

  public Compiler(
    JavaCompiler javaCompiler,
    ReadFileSystem<?> sourcePath,
    Collection<ReadFileSystem<?>> classPaths,
    ReadWriteFileSystem<?> sourceOutput,
    ReadWriteFileSystem<?> classOutput,
    CompilerConfig config) {
    this.sourcePath = sourcePath;
    this.classPaths = classPaths;
    this.sourceOutput = sourceOutput;
    this.classOutput = classOutput;
    this.javaCompiler = javaCompiler;
    this.processors = new HashSet<Processor>();
    this.config = config;
  }

  public void addAnnotationProcessor(Processor annotationProcessorType) {
    if (annotationProcessorType == null) {
      throw new NullPointerException("No null processor allowed");
    }
    processors.add(annotationProcessorType);
  }

  public void compile(String... compilationUnits) throws IOException, CompilationException {
    // Copy anything that is not a java file
    RAMFileSystem sourcePath1 = new RAMFileSystem();
    sourcePath.copy(new Filter() {
      public boolean acceptDir(Object dir, String name) throws IOException {
        return true;
      }

      public boolean acceptFile(Object file, String name) throws IOException {
        return !name.endsWith(".java");
      }
    }, sourcePath1);

    //
    VirtualFileManager fileManager = new VirtualFileManager(
      javaCompiler.getStandardFileManager(null, null, null),
      sourcePath1,
      classPaths,
      sourceOutput,
      classOutput
    );

    //
    Collection<JavaFileObject> files = getFromSourcePath(sourcePath, compilationUnits);

    //
    compile(fileManager, files);
  }

  private <P> Collection<JavaFileObject> getFromSourcePath(ReadFileSystem<P> fs, String... compilationUnits) throws IOException {
    SimpleFileManager<P> manager = new SimpleFileManager<P>(StandardLocation.SOURCE_PATH, fs);
    ArrayList<String> tmp = new ArrayList<String>();
    final ArrayList<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
    for (String compilationUnit : compilationUnits) {
      tmp.clear();
      ArrayList<String> names = Spliterator.split(compilationUnit.substring(1), '/', tmp);
      String name = tmp.get(tmp.size() - 1);
      if (!name.endsWith(".java")) {
        throw new IllegalArgumentException("Illegal compilation unit: " + compilationUnit);
      }
      P file = manager.getFileSystem().getPath(names);
      if (file == null) {
        throw new IllegalArgumentException("Could not find compilation unit: " + compilationUnit);
      }
      names.remove(names.size() - 1);
      String pkg = Tools.join('.', names);
      FileKey key = FileKey.newJavaName(pkg, name);
      javaFiles.add(manager.getReadable(key));
    }
    return javaFiles;
  }

  public void compile() throws IOException, CompilationException {
    VirtualFileManager fileManager = new VirtualFileManager(
      javaCompiler.getStandardFileManager(null, null, null),
      sourcePath,
      classPaths,
      sourceOutput,
      classOutput
    );
    compile(fileManager, getFromSourcePath(fileManager.sourcePath));
  }

  private <P> Collection<JavaFileObject> getFromSourcePath(final SimpleFileManager<P> fileManager) throws IOException {
    final ArrayList<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
    final StringBuilder buffer = new StringBuilder();
    ((ReadFileSystem<P>)fileManager.getFileSystem()).traverse(new Visitor.Default<P>() {
      @Override
      public void enterDir(Object dir, String name) throws IOException {
        if (name.length() > 0) {
          buffer.append(name).append('.');
        }
      }

      public void file(P file, String name) throws IOException {
        if (name.endsWith(".java")) {
          FileKey key = FileKey.newJavaName(buffer.substring(0, buffer.length() - 1), name);
          JavaFileObject fileObject = fileManager.getReadable(key);
          javaFiles.add(fileObject);
        }
      }

      @Override
      public void leaveDir(Object dir, String name) throws IOException {
        if (name.length() > 0) {
          buffer.setLength(buffer.length() - name.length() - 1);
        }
      }
    });
    return javaFiles;
  }

  private void compile(
      VirtualFileManager fileManager,
      Collection<JavaFileObject> compilationUnits) throws IOException, CompilationException {
    if (compilationUnits.isEmpty()) {
      if (!config.getForce()) {
        return;
      }
      else {
        URI uri = URI.create("/Dumb.java");
        compilationUnits = Collections.<JavaFileObject>singleton(new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return "public class Dumb {}";
          }
        });
      }
    }

    //
    final List<CompilationError> errors = new ArrayList<CompilationError>();
    DiagnosticListener<JavaFileObject> listener = new DiagnosticListener<JavaFileObject>() {
      public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
          int columnNumber = (int)diagnostic.getColumnNumber();
          int lineNumber = (int)diagnostic.getLineNumber();
          Location location = (columnNumber > 0 && lineNumber > 0) ? new Location(columnNumber, lineNumber) : null;

          // We pass the default locale instead of null as ECJ has issues with null
          String message = diagnostic.getMessage(Locale.getDefault());

          //
          MessageCode code = null;
          List<String> arguments = Collections.emptyList();
          Matcher matcher = PATTERN.matcher(message);
          if (matcher.find()) {
            String codeKey = matcher.group(1);
            code = MessageCode.decode(codeKey);

            //
            if (matcher.group(2).length() > 0) {
              arguments = new ArrayList<String>();
              for (String argument : Spliterator.split(matcher.group(2), ',')) {
                arguments.add(argument.trim());
              }
            }
          }

          // Best effort to get a java.io.File
          JavaFileObject obj = diagnostic.getSource();
          String source = null;
          File resolvedFile = null;
          if (obj != null) {
            URI uri = obj.toUri();
            source = uri.getPath();
            if (obj instanceof JavaFileObjectImpl) {
              JavaFileObjectImpl foo = (JavaFileObjectImpl)obj;
              try {
                resolvedFile = foo.getFile();
              }
              catch (Exception ignore) {
              }
            } else {
              // We are likely in eclipse (with its JavaFileObject wrapper) and we should get the file from the URI
              if (uri.getScheme().equals("file")) {
                resolvedFile = new File(uri);
              }
            }
          }

          //
          errors.add(new CompilationError(code, arguments, source, resolvedFile, location, message));
        }
      }
    };

    //
    List<String> options = null;
    for (String optionName : config.getProcessorOptionNames()) {
      if (options == null) {
        options = new ArrayList<String>();
      }
      options.add("-A" + optionName + "=" + config.getProcessorOptionValue(optionName));
    }

    //
    JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, listener, options, null, compilationUnits);
    task.setProcessors(processors);

    // We don't use the return value because sometime it says it is failed although
    // it is not, need to investigate this at some piont
    boolean ok = task.call();

    // Clear caches
    fileManager.sourceOutput.clearCache();
    fileManager.classOutput.clearCache();
    fileManager.sourceOutput.clearCache();
    if (fileManager.classPath != null) {
      fileManager.classPath.clearCache();
    }

    // Clear processors as we should not reuse them
    processors.clear();

    //
    if (!ok) {
      throw new CompilationException(errors);
    }
  }
}
