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

package juzu.impl.plugin.module;

import juzu.impl.common.Logger;
import juzu.impl.compiler.*;
import juzu.impl.compiler.Compiler;
import juzu.impl.fs.Change;
import juzu.impl.fs.FileSystemScanner;
import juzu.impl.fs.Filter;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.classloader.ClassLoaderFileSystem;
import juzu.impl.fs.spi.ram.RAMFileSystem;
import juzu.processor.MainProcessor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * The module life cycle.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public abstract class ModuleLifeCycle<R, C> {

  /** . */
  protected final Logger logger;

  /** . */
  protected ReadFileSystem<R> resources;

  protected ModuleLifeCycle(Logger logger) {
    this.logger = logger;
  }

  public ReadFileSystem<R> getResources() {
    return resources;
  }

  public void setResources(ReadFileSystem<R> resources) {
    this.resources = resources;
  }

  public abstract boolean refresh() throws Exception, CompilationException ;

  /**
   * Returns the module classloader.
   *
   * @return the module classloader
   */
  public abstract ClassLoader getClassLoader();

  /**
   * Returns the module classes.
   *
   * @return the module classes
   */
  public abstract ReadFileSystem<C> getClasses();

  public static class Dynamic<R, S> extends ModuleLifeCycle<R, String[]> {

    /** . */
    private final ReadFileSystem<S> source;

    /** . */
    private final ClassLoader baseClassLoader;

    /** . */
    private ClassLoaderFileSystem classPath;

    /** . */
    private FileSystemScanner<S> scanner;

    /** . */
    private ClassLoader classLoader;

    /** . */
    private ReadFileSystem<String[]> classes;

    /** . */
    private boolean initialized;

    public Dynamic(Logger logger, ClassLoader baseClassLoader, ReadFileSystem<S> source) {
      super(logger);
      this.classLoader = null;
      this.baseClassLoader = baseClassLoader;
      this.scanner = FileSystemScanner.createTimestamped(source);
      this.source = source;
      this.initialized = false;
    }

    @Override
    public boolean refresh() throws Exception, CompilationException {

      // Lazy initialize
      if (!initialized) {
        classPath = new ClassLoaderFileSystem(baseClassLoader);
        scanner = FileSystemScanner.createHashing(source);
        initialized = true;
      }

      //
      Map<String, Change> changes = scanner.scan();

      //
      if (changes.size() > 0) {
        logger.log("Detected changes : " + changes);

        logger.log("Building application");

        //
        ReadFileSystem<S> sourcePath = scanner.getFileSystem();

        // Copy everything that is not a java source
        RAMFileSystem classOutput = new RAMFileSystem();
        sourcePath.copy(new Filter.Default() {
          @Override
          public boolean acceptFile(Object file, String name) throws IOException {
            return !name.endsWith(".java");
          }
        }, classOutput);

        //
        Compiler compiler = Compiler.
            builder().
            sourcePath(sourcePath).
            sourceOutput(classOutput).
            classOutput(classOutput).
            addClassPath(classPath).build();
        compiler.addAnnotationProcessor(new MainProcessor());
        compiler.compile();

        //
        this.classLoader = new URLClassLoader(new URL[]{classOutput.getURL()}, classPath.getClassLoader());
        this.classes = classOutput;

        //
        return true;
      }
      else {
        logger.log("No changes detected");
        return false;
      }
    }

    @Override
    public ClassLoader getClassLoader() {
      return classLoader;
    }

    @Override
    public ReadFileSystem<String[]> getClasses() {
      return classes;
    }
  }

  public static class Static<R, P> extends ModuleLifeCycle<R, P> {

    /** . */
    private final ClassLoader classLoader;

    /** . */
    private final ReadFileSystem<P> classes;

    public Static(Logger logger, ClassLoader classLoader, ReadFileSystem<P> classes) {
      super(logger);

      //
      this.classLoader = classLoader;
      this.classes = classes;
    }

    @Override
    public boolean refresh() {
      return false;
    }

    @Override
    public ClassLoader getClassLoader() {
      return classLoader;
    }

    @Override
    public ReadFileSystem<P> getClasses() {
      return classes;
    }
  }
}
