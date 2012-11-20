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

package juzu.request;

import juzu.Dispatch;
import juzu.PropertyType;
import juzu.impl.common.ParameterHashMap;
import juzu.impl.common.ParameterMap;
import juzu.impl.plugin.application.ApplicationContext;
import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.plugin.controller.descriptor.MethodDescriptor;
import juzu.impl.request.Request;
import juzu.impl.bridge.spi.RequestBridge;

import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class RequestContext {

  /** . */
  private static final Object[] EMPTY = new Object[0];

  /** . */
  protected final ApplicationContext application;

  /** . */
  protected final MethodDescriptor method;

  /** . */
  protected final Request request;

  public RequestContext(Request request, ApplicationContext application, MethodDescriptor method) {
    this.request = request;
    this.application = application;
    this.method = method;
  }

  public ApplicationContext getApplication() {
    return application;
  }

  public MethodDescriptor getMethod() {
    return method;
  }

  public Map<String, String[]> getParameters() {
    return request.getParameters();
  }

  public HttpContext getHttpContext() {
    return getBridge().getHttpContext();
  }

  public SecurityContext getSecurityContext() {
    return getBridge().getSecurityContext();
  }

  public <T> T getProperty(PropertyType<T> propertyType) {
    return getBridge().getProperty(propertyType);
  }

  public abstract Phase getPhase();

  protected abstract RequestBridge getBridge();

  public Dispatch createDispatch(MethodDescriptor method) {
    return createDispatch(method, EMPTY, ParameterMap.EMPTY);
  }

  public Dispatch createDispatch(MethodDescriptor method, Object arg) {
    return createDispatch(method, new Object[]{arg}, new ParameterHashMap());
  }

  public Dispatch createDispatch(MethodDescriptor method, Object[] args) {
    return createDispatch(method, args, new ParameterHashMap());
  }

  private Dispatch createDispatch(MethodDescriptor method, Object[] args, ParameterMap parameters) {
    method.setArgs(args, parameters);
    Dispatch builder = getBridge().createDispatch(method.getPhase(), method.getHandle(), parameters);

    // Bridge escape XML value
    ApplicationDescriptor desc = application.getDescriptor();
    builder.escapeXML(desc.getControllers().getEscapeXML());

    //
    return builder;
  }
}
