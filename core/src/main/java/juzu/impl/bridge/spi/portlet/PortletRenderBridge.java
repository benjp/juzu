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

package juzu.impl.bridge.spi.portlet;

import juzu.PropertyMap;
import juzu.PropertyType;
import juzu.Response;
import juzu.asset.Asset;
import juzu.impl.plugin.application.Application;
import juzu.impl.bridge.Bridge;
import juzu.impl.inject.ScopedContext;
import juzu.impl.bridge.spi.RenderBridge;
import juzu.request.Phase;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;

import javax.portlet.MimeResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceURL;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class PortletRenderBridge extends PortletMimeBridge<RenderRequest, RenderResponse> implements RenderBridge {

  /** . */
  private final Bridge bridge;

  /** . */
  private String title;

  /** . */
  private LinkedList<Element> headers = new LinkedList<Element>();

  public PortletRenderBridge(Application application, Bridge bridge, RenderRequest request, RenderResponse response, PortletConfig config, boolean prod) {
    super(application, request, response, config, prod);

    //
    this.bridge = bridge;
  }

  @Override
  protected Phase getPhase() {
    return Phase.VIEW;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public void setResponse(Response response) throws IllegalStateException, IOException {
    super.setResponse(response);
    if (response instanceof Response.Content) {
      Response.Content content = (Response.Content)response;
      PropertyMap properties = content.getProperties();

      //
      String title = properties.getValue(PropertyType.TITLE);
      if (title != null) {
        this.title = title;
      }

      //
      Iterable<Asset> scriptsProp = properties.getValues(PropertyType.SCRIPT);
      Iterable<Asset> stylesheetsProp = properties.getValues(PropertyType.STYLESHEET);
      Iterable<Map.Entry<String, String>> metas = properties.getValues(PropertyType.META_TAG);

      //
      if (metas != null) {
        for (Map.Entry<String, String> entry : metas) {
          Element elt = this.resp.createElement("meta");
          elt.setAttribute("name", entry.getKey());
          elt.setAttribute("content", entry.getValue());
          headers.add(elt);
        }
      }

      //
      if (stylesheetsProp != null) {
        Iterable<Asset.Value> stylesheets = bridge.application.getStylesheetManager().resolveAssets(stylesheetsProp);
        for (Asset.Value stylesheet : stylesheets) {
          int pos = stylesheet.getURI().lastIndexOf('.');
          String ext = pos == -1 ? "css" : stylesheet.getURI().substring(pos + 1);
          Element elt = this.resp.createElement("link");
          elt.setAttribute("media", "screen");
          elt.setAttribute("rel", "stylesheet");
          elt.setAttribute("type", "text/" + ext);
          elt.setAttribute("href", getAssetURL(stylesheet));
          headers.add(elt);
        }
      }

      //
      if (scriptsProp != null) {
        Iterable<Asset.Value> scripts = bridge.application.getScriptManager().resolveAssets(scriptsProp);
        for (Asset.Value script : scripts) {
          String url = getAssetURL(script);
          Element elt = this.resp.createElement("script");
          elt.setAttribute("type", "text/javascript");
          elt.setAttribute("src", url);
          // This comment is needed for liferay to make the script pass the minifier
          // it forces to have a <script></script> tag
          Comment comment = elt.getOwnerDocument().createComment(request.getApplication().getName() + " script ");
          elt.appendChild(comment);
          headers.add(elt);
        }
      }

      //
      super.setResponse(response);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private String getAssetURL(Asset.Value asset) {
    StringBuilder sb;
    String url;
    String uri = asset.getURI();
    switch (asset.getLocation()) {
      case SERVER:
        sb = new StringBuilder();
        if (!uri.startsWith("/")) {
          sb.append(req.getContextPath());
          sb.append('/');
        }
        sb.append(uri);
        url = sb.toString();
        break;
      case CLASSPATH:
        if (prod) {
          sb = new StringBuilder();
          sb.append(req.getContextPath()).append("/assets");
          if (!uri.startsWith("/")) {
            sb.append('/');
          }
          sb.append(uri);
          url = sb.toString();
        }
        else {
          ResourceURL r = resp.createResourceURL();
          r.setParameter("juzu.request", "assets");
          r.setResourceID(uri);
          url = r.toString();
        }
        break;
      case URL:
        url = uri;
        break;
      default:
        throw new AssertionError();
    }
    return url;
  }

  @Override
  public void end() {
    super.end();

    //
    ScopedContext context = getFlashContext(false);
    if (context != null) {
      context.close();
    }
  }

  @Override
  public void send() throws IOException {

    // Set title
    if (title != null) {
      resp.setTitle(title);
    }

    // Add elements
    for (Element elt : headers) {
      resp.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, elt);
    }

    //
    super.send();
  }
}
