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

package bridge.servlet.route.action.redirecttoview;

import juzu.Action;
import juzu.PropertyType;
import juzu.Response;
import juzu.Route;
import juzu.request.RenderContext;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {

  @juzu.View
  public Response.Render index() {
    return Response.ok(
        "<form id='form' action='" + A_.foo() + "' method='post'>" +
            "<input id='trigger' type='submit' name='click'/>" +
            "</form>");
  }

  @Action
  @Route("/foo")
  public Response.View foo() {
    return A_.bar("juu");
  }

  @juzu.View
  @Route("/bar")
  public Response.Render bar(String juu, RenderContext renderContext) {
    String path = renderContext.getProperty(PropertyType.PATH);
    return Response.ok("/juzu/bar".equals(path) && "juu".equals(juu) ? "pass" : "fail");
  }
}
