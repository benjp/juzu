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

package juzu.impl.bridge.spi.web;

import juzu.Response;
import juzu.impl.plugin.application.Application;
import juzu.impl.bridge.spi.ResourceBridge;
import juzu.impl.common.Tools;
import juzu.impl.request.Method;
import juzu.io.AppendableStream;
import juzu.io.BinaryOutputStream;
import juzu.io.Stream;
import juzu.request.ClientContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class WebResourceBridge extends WebMimeBridge implements ResourceBridge {

  /** . */
  private Response.Content response;

  WebResourceBridge(
      Application application,
      Handler handler,
      WebBridge bridge,
      Method<?> target,
      Map<String, String[]> parameters) {
    super(application, handler, bridge, target, parameters);
  }

  public ClientContext getClientContext() {
    return http.getClientContext();
  }

  public void setResponse(Response response) throws IllegalStateException, IOException {
    super.setResponse(response);
    if (response instanceof Response.Content) {
      this.response = (Response.Content)response;
    } else {
      throw new IllegalArgumentException();
    }
  }

  void send() throws IOException {
    if (response != null) {
      //
      int status = response.getStatus();
      if (status != 200) {
        http.setStatus(status);
      }

      // Set mime type
      String mimeType = response.getMimeType();
      if (mimeType != null) {
        http.setContentType(mimeType);
      }

      // Set headers
      for (Map.Entry<String, String[]> entry : responseHeaders.entrySet()) {
        http.setHeader(entry.getKey(), entry.getValue()[0]);
      }

      // Send response
      if (response.getKind() == Stream.Char.class) {
        Writer writer = http.getWriter();
        try {
          response.send(new AppendableStream(writer));
        }
        finally {
          Tools.safeClose(writer);
        }
      }
      else {
        OutputStream out = http.getOutputStream();
        try {
          response.send(new BinaryOutputStream(out));
        }
        finally {
          Tools.safeClose(out);
        }
      }
    }
  }
}
