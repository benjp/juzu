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

package juzu.test;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import javax.inject.Provider;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Abstract compiler.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public enum JavaCompilerProvider implements Provider<JavaCompiler> {

  JAVAC() {
    public JavaCompiler get() {
      return ToolProvider.getSystemJavaCompiler();
    }
  },

  ECJ() {
    public JavaCompiler get() {
      return new EclipseCompiler();
    }
  };

  public abstract JavaCompiler get();

  /** . */
  public static final JavaCompilerProvider DEFAULT;

  static {
    String compilerProperty = System.getProperty("juzu.test.compiler");
    JavaCompilerProvider compiler = JAVAC;
    try {
      if (compilerProperty != null) {
        compiler = JavaCompilerProvider.valueOf(compilerProperty.toUpperCase());
      }
    }
    catch (Throwable ignore) {
    }
    DEFAULT = compiler;
  }
}
