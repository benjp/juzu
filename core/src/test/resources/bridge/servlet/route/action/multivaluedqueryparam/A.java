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

package bridge.servlet.route.action.multivaluedqueryparam;

import juzu.Action;
import juzu.Response;
import juzu.Route;

import java.util.Arrays;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {

  @juzu.View
  public Response.Render index() {
    return Response.ok("<a id='trigger' href='" + A_.foo(new String[]{"bar1","bar2"}) + "'>click</div>");
  }

  @Action
  @Route("/foo")
  public Response.View foo(String[] juu) {
    return A_.bar(juu);
  }

  @juzu.View
  @Route("/bar")
  public Response.Content<?> bar(String[] juu) {
    return Response.ok("" + Arrays.asList(juu));
  }
}
