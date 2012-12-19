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

package juzu.impl.bridge;

import juzu.Response;
import juzu.impl.bridge.spi.EventBridge;
import juzu.impl.plugin.application.ApplicationException;
import juzu.impl.plugin.application.ApplicationLifeCycle;
import juzu.impl.asset.AssetServer;
import juzu.impl.bridge.spi.ActionBridge;
import juzu.impl.bridge.spi.RenderBridge;
import juzu.impl.bridge.spi.RequestBridge;
import juzu.impl.bridge.spi.ResourceBridge;
import juzu.impl.compiler.CompilationError;
import juzu.impl.compiler.CompilationException;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.common.Logger;
import juzu.impl.common.Tools;
import juzu.impl.common.TrimmingException;
import juzu.impl.plugin.module.ModuleLifeCycle;
import juzu.impl.resource.ResourceResolver;
import juzu.bridge.portlet.JuzuPortlet;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Bridge implements Closeable {

  /** . */
  private final Logger log;

  /** . */
  private final AssetServer server;

  /** . */
  private final BridgeConfig config;

  /** . */
  private final ReadFileSystem<?> resources;

  /** . */
  private final ResourceResolver resolver;

  /** . */
  private final ModuleLifeCycle module;

  /** . */
  public ClassLoader classLoader;

  /** . */
  public ApplicationLifeCycle application;

  public Bridge(Logger log, ModuleLifeCycle module, BridgeConfig config, ReadFileSystem<?> resources, AssetServer server, ResourceResolver resolver) {
    this.log = log;
    this.module = module;
    this.config = config;
    this.resources = resources;
    this.server = server;
    this.resolver = resolver;
  }

  public BridgeConfig getConfig() {
    return config;
  }

  public void refresh() throws Exception {

    if (application == null) {
      application = new ApplicationLifeCycle(
          log,
          module,
          config.injectImpl,
          config.name,
          resources,
          server,
          resolver);
    }

    //
    application.refresh();
  }

  public void invoke(RequestBridge requestBridge) throws Throwable {
    if (requestBridge instanceof  ActionBridge) {
      processAction((ActionBridge)requestBridge);
    } else if (requestBridge instanceof RenderBridge) {
      render((RenderBridge)requestBridge);
    } else if (requestBridge instanceof ResourceBridge) {
      serveResource((ResourceBridge)requestBridge);
    } else {
      throw new AssertionError();
    }
  }

  public void processAction(final ActionBridge requestBridge) throws Throwable {
    try {
      TrimmingException.invoke(new TrimmingException.Callback() {
        public void call() throws Throwable {
          try {
            application.getApplication().invoke(requestBridge);
          }
          catch (ApplicationException e) {
            // For now we do that until we find something better specially for the dev mode
            throw e.getCause();
          }
        }
      });
    }
    catch (TrimmingException e) {
      throw e.getSource();
    }
    finally {
      requestBridge.close();
    }
  }

  public void processEvent(final EventBridge requestBridge) throws Throwable {
    try {
      TrimmingException.invoke(new TrimmingException.Callback() {
        public void call() throws Throwable {
          try {
            application.getApplication().invoke(requestBridge);
          }
          catch (ApplicationException e) {
            // For now we do that until we find something better specially for the dev mode
            throw e.getCause();
          }
        }
      });
    }
    catch (TrimmingException e) {
      throw e.getSource();
    }
    finally {
      requestBridge.close();
    }
  }

  public void render(final RenderBridge requestBridge) throws Throwable {

    //
    Collection<CompilationError> errors = null;
    try {
      refresh();
    }
    catch (CompilationException e) {
      errors = e.getErrors();
    }

    //
    if (errors == null || errors.isEmpty()) {

      //
      if (errors != null) {
        requestBridge.purgeSession();
      }

      //
      try {
        TrimmingException.invoke(new TrimmingException.Callback() {
          public void call() throws Throwable {
            try {
              application.getApplication().invoke(requestBridge);
            }
            catch (ApplicationException e) {
              throw e.getCause();
            }
          }
        });
      }
      catch (TrimmingException e) {
        if (config.isProd()) {
          throw e.getSource();
        }
        else {
          StringWriter writer = new StringWriter();
          PrintWriter printer = new PrintWriter(writer);
          renderThrowable(printer, e);
          requestBridge.setResponse(Response.ok(writer.getBuffer()));
        }
      } finally {
        requestBridge.close();
      }
    }
    else {
      try {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        renderErrors(printer, errors);
        requestBridge.setResponse(Response.ok(writer.getBuffer()));
      }
      finally {
        requestBridge.close();
      }
    }
  }

  public void serveResource(final ResourceBridge requestBridge) throws Throwable{
    try {
      TrimmingException.invoke(new TrimmingException.Callback() {
        public void call() throws Throwable {
          try {
            application.getApplication().invoke(requestBridge);
          }
          catch (ApplicationException e) {
            throw e.getCause();
          }
        }
      });
    }
    catch (TrimmingException e) {

      //
      logThrowable(e);

      // Internal server error
      Response response;
      if (!config.isProd()) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        printer.print("<html>\n");
        printer.print("<head>\n");
        printer.print("</head>\n");
        printer.print("<body>\n");
        renderThrowable(printer, e);
        printer.print("</body>\n");
        response = Response.content(500, writer.getBuffer());
      } else {
        response = Response.content(500, "todo");
      }

      // Set response
      requestBridge.setResponse(response);
    } finally {
      requestBridge.close();
    }
  }

  private void logThrowable(Throwable t) {
    log.log(t.getMessage(), t);
  }

  private void renderThrowable(PrintWriter writer, Throwable t) throws IOException {
    // Trim the stack trace to remove stuff we don't want to see
    int size = 0;
    StackTraceElement[] trace = t.getStackTrace();
    for (StackTraceElement element : trace) {
      if (element.getClassName().equals(JuzuPortlet.class.getName())) {
        break;
      }
      else {
        size++;
      }
    }
    StackTraceElement[] ourTrace = new StackTraceElement[size];
    System.arraycopy(trace, 0, ourTrace, 0, ourTrace.length);
    t.setStackTrace(ourTrace);

    //
    sendJuzuCSS(writer);

    // We hack a bit
    final AtomicBoolean open = new AtomicBoolean(false);
    PrintWriter formatter = new PrintWriter(writer) {
      @Override
      public void println(Object x) {
        if (open.get()) {
          super.append("</ul></pre>");
        }
        super.append("<div class=\"juzu-message\">");
        super.append(String.valueOf(x));
        super.append("</div>");
        open.set(false);
      }

      @Override
      public void println(String x) {
        if (!open.get()) {
          super.append("<pre><ul>");
          open.set(true);
        }
        super.append("<li><span>");
        super.append(x);
        super.append("</span></li>");
      }

      @Override
      public void println() {
        // Do nothing
      }
    };

    //
    writer.append("<div class=\"juzu\">");
    writer.append("<div class=\"juzu-box\">");

    // We hack a bit with our formatter
    t.printStackTrace(formatter);

    //
    if (open.get()) {
      writer.append("</ul></pre>");
    }

    //
    writer.append("</div>");
    writer.append("</div>");
  }

  private void sendJuzuCSS(PrintWriter writer) throws IOException {
    // Get CSS
    URL cssURL = JuzuPortlet.class.getResource("juzu.css");
    String css = Tools.read(cssURL);
    css = css.replace("\"", "\\\"");
    css = css.replace("'", "\\'");
    css = css.replace("\n", "\\n");

    //
    writer.append("<script type='text/javascript'>\n");
    writer.append("var styleElement = document.createElement('style');\n");
    writer.append("var css = '");
    writer.append(css);
    writer.append("';\n");
    writer.append("styleElement.type = 'text/css';\n");
    writer.append("if (styleElement.styleSheet) {;\n");
    writer.append("styleElement.styleSheet.cssText = css;\n");
    writer.append("} else {\n");
    writer.append("styleElement.appendChild(document.createTextNode(css));\n");
    writer.append("}\n");
    writer.append("document.getElementsByTagName(\"head\")[0].appendChild(styleElement);\n");
    writer.append("</script>\n");
  }

  private void renderErrors(PrintWriter writer, Collection<CompilationError> errors) throws IOException {
    sendJuzuCSS(writer);

    //
    writer.append("<div class=\"juzu\">");
    for (CompilationError error : errors) {
      writer.append("<div class=\"juzu-box\">");
      writer.append("<div class=\"juzu-message\">").append(error.getMessage()).append("</div>");

      // Display the source code
      File source = error.getSourceFile();
      if (source != null) {
        int line = error.getLocation().getLine();
        int from = line - 2;
        int to = line + 3;
        BufferedReader reader = new BufferedReader(new FileReader(source));
        int count = 1;
        writer.append("<pre><ol start=\"").append(String.valueOf(from)).append("\">");
        for (String s = reader.readLine();s != null;s = reader.readLine()) {
          if (count >= from && count < to) {
            if (count == line) {
              writer.append("<li><span class=\"error\">").append(s).append("</span></li>");
            }
            else {
              writer.append("<li><span>").append(s).append("</span></li>");
            }
          }
          count++;
        }
        writer.append("</ol></pre>");
      }
      writer.append("</div>");
    }
    writer.append("</div>");
  }

  public void close() {
    Tools.safeClose(application);
  }
}
