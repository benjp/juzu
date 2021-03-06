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

package http.lifecycle;

import juzu.Action;
import juzu.Resource;
import juzu.Response;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {

  @Action
  public Response.View action() {
    return A_.done("d");
  }

  @juzu.View
  public Response.Render index() {
    return Response.ok(A_.action().toString());
  }

  @juzu.View
  public Response.Render done(String p) {
    if ("d".equals(p)) {
      return Response.ok(A_.resource().toString());
    }
    else {
      return Response.ok("<html><body>fail</body></html>");
    }
  }

  @Resource
  public Response.Render resource() {
    return Response.ok("<html><body>done</body></html>");
  }
}
