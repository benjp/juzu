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

package juzu.impl.router;

import juzu.UndeclaredIOException;
import juzu.impl.common.MimeType;
import juzu.impl.common.URIWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RouteMatch {

  /** The matched route. */
  final Route route;

  /** The matched parameters. */
  final Map<PathParam, String> matched;

  /** The un matched parameters. */
  final Map<String, String> unmatched;

  RouteMatch(Route route, Map<PathParam, String> matched) {
    this.route = route;
    this.matched = Collections.unmodifiableMap(matched);
    this.unmatched = Collections.emptyMap();
  }

  RouteMatch(Route route, Map<String, String> unmatched, Map<PathParam, String> matched) {
    this.route = route;
    this.matched = Collections.unmodifiableMap(matched);
    this.unmatched = Collections.unmodifiableMap(unmatched);
  }

  public Route getRoute() {
    return route;
  }

  public Map<PathParam, String> getMatched() {
    return Collections.unmodifiableMap(matched);
  }

  public Map<String, String> getUnmatched() {
    return unmatched;
  }

  public void render(URIWriter writer) throws IOException {
    route.renderPath(this, writer, false);
  }

  public String render() {
    try {
      StringBuilder sb = new StringBuilder();
      URIWriter writer = new URIWriter(sb, MimeType.PLAIN);
      render(writer);
      return sb.toString();
    }
    catch (IOException e) {
      throw new UndeclaredIOException(e);
    }
  }
}
