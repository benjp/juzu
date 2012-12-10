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
import juzu.impl.common.MimeType;
import juzu.impl.plugin.application.ApplicationContext;
import juzu.impl.common.MethodHandle;
import juzu.impl.plugin.controller.ControllerResolver;
import juzu.impl.plugin.controller.descriptor.MethodDescriptor;
import juzu.impl.inject.Scoped;
import juzu.impl.inject.ScopedContext;
import juzu.impl.request.Request;
import juzu.impl.bridge.spi.RequestBridge;
import juzu.bridge.portlet.JuzuPortlet;
import juzu.impl.bridge.spi.DispatchSPI;
import juzu.request.HttpContext;
import juzu.request.Phase;
import juzu.request.SecurityContext;
import juzu.request.WindowContext;

import javax.portlet.BaseURL;
import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
abstract class PortletRequestBridge<Rq extends PortletRequest, Rs extends PortletResponse> implements RequestBridge {

  /** . */
  protected Map<String, String[]> responseHeaders;

  /** . */
  protected final ApplicationContext application;

  /** . */
  protected final Rq req;

  /** . */
  protected final Rs resp;

  /** . */
  protected final MethodHandle target;

  /** . */
  protected final Map<String, String[]> parameters;

  /** . */
  protected final PortletHttpContext httpContext;

  /** . */
  protected final PortletSecurityContext securityContext;

  /** . */
  protected final PortletWindowContext windowContext;

  /** . */
  protected Request request;

  /** . */
  protected final boolean prod;

  /** . */
  protected Response response;

  PortletRequestBridge(ApplicationContext application, Rq req, Rs resp, boolean prod) {
    String methodId = null;
    Map<String, String[]> parameters = new HashMap<String, String[]>(req.getParameterMap());
    for (Iterator<Map.Entry<String, String[]>> i = parameters.entrySet().iterator();i.hasNext();) {
      Map.Entry<String, String[]> parameter = i.next();
      String key = parameter.getKey();
      if (key.startsWith("juzu.")) {
        if (parameter.getKey().equals("juzu.op")) {
          methodId = parameter.getValue()[0];
        }
        i.remove();
      }
    }

    //
    Phase phase = getPhase();
    ControllerResolver<MethodDescriptor> resolver = application.getDescriptor().getControllers().getResolver();
    MethodDescriptor target;
    if (methodId != null) {
      target = resolver.resolve(phase, methodId, parameters.keySet());
    } else {
      target = resolver.resolve(parameters.keySet());
    }

    //
    this.application = application;
    this.req = req;
    this.resp = resp;
    this.target = target.getHandle();
    this.parameters = parameters;
    this.httpContext = new PortletHttpContext(req);
    this.securityContext = new PortletSecurityContext(req);
    this.windowContext = new PortletWindowContext(this);
    this.prod = prod;
  }

  protected abstract Phase getPhase();

  public <T> T getProperty(PropertyType<T> propertyType) {
    Object propertyValue = null;
    if (JuzuPortlet.PORTLET_MODE.equals(propertyType)) {
      propertyValue = req.getPortletMode();
    }
    else if (JuzuPortlet.WINDOW_STATE.equals(propertyType)) {
      propertyValue = req.getWindowState();
    }
    return propertyValue == null ? null : propertyType.cast(propertyValue);
  }

  public MethodHandle getTarget() {
    return target;
  }

  public final Map<String, String[]> getParameters() {
    return parameters;
  }

  public final HttpContext getHttpContext() {
    return httpContext;
  }

  public final SecurityContext getSecurityContext() {
    return securityContext;
  }

  public final WindowContext getWindowContext() {
    return windowContext;
  }

  public final Scoped getRequestValue(Object key) {
    ScopedContext context = getRequestContext(false);
    return context != null ? context.get(key) : null;
  }

  public final void setRequestValue(Object key, Scoped value) {
    if (value != null) {
      ScopedContext context = getRequestContext(false);
      if (context != null) {
        context.set(key, null);
      }
    }
    else {
      getRequestContext(true).set(key, value);
    }
  }

  public final Scoped getFlashValue(Object key) {
    ScopedContext context = getFlashContext(false);
    return context != null ? context.get(key) : null;
  }

  public final void setFlashValue(Object key, Scoped value) {
    if (value == null) {
      ScopedContext context = getFlashContext(false);
      if (context != null) {
        context.set(key, null);
      }
    }
    else {
      getFlashContext(true).set(key, value);
    }
  }

  public final Scoped getSessionValue(Object key) {
    ScopedContext context = getSessionContext(false);
    return context != null ? context.get(key) : null;
  }

  public final void setSessionValue(Object key, Scoped value) {
    if (value == null) {
      ScopedContext context = getSessionContext(false);
      if (context != null) {
        context.set(key, null);
      }
    }
    else {
      getSessionContext(true).set(key, value);
    }
  }

  public final Scoped getIdentityValue(Object key) {
    return null;
  }

  public final void setIdentityValue(Object key, Scoped value) {
  }

  public void purgeSession() {
    PortletSession session = req.getPortletSession(false);
    if (session != null) {
      for (String key : new HashSet<String>(session.getAttributeMap().keySet())) {
        session.removeAttribute(key);
      }
    }
  }

  public void close() {
  }

  protected final ScopedContext getRequestContext(boolean create) {
    ScopedContext context = (ScopedContext)req.getAttribute("juzu.request_scope");
    if (context == null && create) {
      req.setAttribute("juzu.request_scope", context = new ScopedContext());
    }
    return context;
  }

  protected final ScopedContext getFlashContext(boolean create) {
    ScopedContext context = null;
    PortletSession session = req.getPortletSession(create);
    if (session != null) {
      context = (ScopedContext)session.getAttribute("juzu.flash_scope");
      if (context == null && create) {
        session.setAttribute("juzu.flash_scope", context = new ScopedContext());
      }
    }
    return context;
  }

  protected final ScopedContext getSessionContext(boolean create) {
    ScopedContext context = null;
    PortletSession session = req.getPortletSession(create);
    if (session != null) {
      context = (ScopedContext)session.getAttribute("juzu.session_scope");
      if (context == null && create) {
        session.setAttribute("juzu.session_scope", context = new ScopedContext());
      }
    }
    return context;
  }

  public void setResponse(Response response) throws IllegalArgumentException, IOException {
    responseHeaders = Collections.emptyMap();
    Iterable<Map.Entry<String, String[]>> headers = response.getProperties().getValues(PropertyType.HEADER);
    if (headers != null) {
      for (Map.Entry<String, String[]> entry : headers) {
        if (responseHeaders.isEmpty()) {
          responseHeaders = new HashMap<String, String[]>();
        }
        responseHeaders.put(entry.getKey(), entry.getValue());
      }
    }
  }

  public void begin(Request request) {
    this.request = request;
  }

  public void end() {
    this.request = null;

    //
    ScopedContext context = getRequestContext(false);
    if (context != null) {
      context.close();
    }
  }

  public void send() throws IOException {
    if (responseHeaders != null) {
      for (Map.Entry<String, String[]> entry : responseHeaders.entrySet()) {
        resp.addProperty(entry.getKey(), entry.getValue()[0]);
      }
    }
  }

  private <T> String _checkPropertyValidity(Phase phase, PropertyType<T> propertyType, T propertyValue) {
    if (propertyType == JuzuPortlet.PORTLET_MODE) {
      if (phase == Phase.RESOURCE) {
        return "Resource URL don't have portlet modes";
      }
      PortletMode portletMode = (PortletMode)propertyValue;
      for (Enumeration<PortletMode> e = req.getPortalContext().getSupportedPortletModes();e.hasMoreElements();) {
        PortletMode next = e.nextElement();
        if (next.equals(portletMode)) {
          return null;
        }
      }
      return "Unsupported portlet mode " + portletMode;
    }
    else if (propertyType == JuzuPortlet.WINDOW_STATE) {
      if (phase == Phase.RESOURCE) {
        return "Resource URL don't have windwo state";
      }
      WindowState windowState = (WindowState)propertyValue;
      for (Enumeration<WindowState> e = req.getPortalContext().getSupportedWindowStates();e.hasMoreElements();) {
        WindowState next = e.nextElement();
        if (next.equals(windowState)) {
          return null;
        }
      }
      return "Unsupported window state " + windowState;
    }
    else {
      // For now we ignore other properties
      return null;
    }
  }

  public DispatchSPI createDispatch(final Phase phase, final MethodHandle target, final Map<String, String[]> parameters) throws NullPointerException, IllegalArgumentException {
    return new DispatchSPI() {

      public MethodHandle getTarget() {
        return target;
      }

      public Map<String, String[]> getParameters() {
        return parameters;
      }

      public <T> String checkPropertyValidity(PropertyType<T> propertyType, T propertyValue) {
        return _checkPropertyValidity(phase, propertyType, propertyValue);
      }

      public void renderURL(PropertyMap properties, MimeType mimeType, Appendable appendable) throws IOException {

        if (resp instanceof MimeResponse) {
          MimeResponse mimeResp = (MimeResponse)resp;

          //
          MethodDescriptor method = application.getDescriptor().getControllers().getMethodByHandle(target);

          //
          BaseURL url;
          if (method.getPhase() == Phase.ACTION) {
            url = mimeResp.createActionURL();
          } else if (method.getPhase() == Phase.VIEW) {
            url = mimeResp.createRenderURL();
          } else if (method.getPhase() == Phase.RESOURCE) {
            url = mimeResp.createResourceURL();
          } else {
            throw new AssertionError();
          }

          // Set generic parameters
          url.setParameters(parameters);

          //
          boolean escapeXML = false;
          if (properties != null) {
            Boolean escapeXMLProperty = properties.getValue(PropertyType.ESCAPE_XML);
            if (escapeXMLProperty != null && Boolean.TRUE.equals(escapeXMLProperty)) {
              escapeXML = true;
            }

            // Handle portlet mode
            PortletMode portletModeProperty = properties.getValue(JuzuPortlet.PORTLET_MODE);
            if (portletModeProperty != null) {
              if (url instanceof PortletURL) {
                try {
                  ((PortletURL)url).setPortletMode(portletModeProperty);
                }
                catch (PortletModeException e) {
                  throw new IllegalArgumentException(e);
                }
              }
              else {
                throw new IllegalArgumentException();
              }
            }

            // Handle window state
            WindowState windowStateProperty = properties.getValue(JuzuPortlet.WINDOW_STATE);
            if (windowStateProperty != null) {
              if (url instanceof PortletURL) {
                try {
                  ((PortletURL)url).setWindowState(windowStateProperty);
                }
                catch (WindowStateException e) {
                  throw new IllegalArgumentException(e);
                }
              }
              else {
                throw new IllegalArgumentException();
              }
            }

            // Set method id
            url.setParameter("juzu.op", method.getId());
          }

          //
          if (escapeXML) {
            StringWriter writer = new StringWriter();
            url.write(writer, true);
            appendable.append(writer.toString());
          }
          else {
            appendable.append(url.toString());
          }
        } else {
          throw new IllegalStateException("Cannot render an URL during phase " + phase);
        }
      }
    };
  }
}
