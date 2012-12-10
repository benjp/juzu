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

import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class RenderTestCase extends AbstractControllerTestCase {

  @Test
  public void testRoot1() throws Exception {
    Router router = new Router();
    Route r = router.append("/");

    //
    assertEquals("/", r.matches(Collections.<String, String>emptyMap()).render());
  }

  @Test
  public void testRoot2() throws Exception {
    Router router = new Router();
    Route r = router.append("");

    //
    assertEquals("/", r.matches(Collections.<String, String>emptyMap()).render());
  }

  @Test
  public void testA() throws Exception {
    Router router = new Router();
    Route r = router.append("/a");

    //
    assertEquals("/a", r.matches(Collections.<String, String>emptyMap()).render());
  }

  @Test
  public void testAB() throws Exception {
    Router router = new Router();
    Route r = router.append("/a/b");

    //
    assertEquals("/a/b", r.matches(Collections.<String, String>emptyMap()).render());
  }

  @Test
  public void testPathParam() throws Exception {
    Router router = new Router();
    Route r = router.append("/{p}");

    //
    assertEquals("/a", r.matches(Collections.singletonMap(Names.P, "a")).render());
    assertNull(r.matches(Collections.<String, String>emptyMap()));
  }

  @Test
  public void testSimplePatternPathParam() throws Exception {
    Router router = new Router();
    Route r = router.append("/{p}", Collections.singletonMap(Names.P, PathParam.matching("a")));

    //
    assertEquals("/a", r.matches(Collections.singletonMap(Names.P, "a")).render());
    assertNull(r.matches(Collections.singletonMap(Names.P, "ab")));
  }

  @Test
  public void testPrecedence() throws Exception {
    Router router = new Router();
    Route a = router.append("/a");
    Route b = router.append("/{p}/b", Collections.singletonMap(Names.P, PathParam.matching("a")));

    //
    assertEquals("/a", a.matches(Collections.<String, String>emptyMap()).render());
    assertEquals("/a/b", b.matches(Collections.singletonMap(Names.P, "a")).render());
  }

  @Test
  public void testLang() throws Exception {
    Router router = new Router();
    Route r = router.append("/{a}b", Collections.singletonMap(Names.A, PathParam.matching("(([A-Za-z]{2})/)?").preservePath(true)));

    //
    assertEquals("/fr/b", r.matches(Collections.singletonMap(Names.A, "fr/")).render());
    assertEquals("/b", r.matches(Collections.singletonMap(Names.A, "")).render());
  }

  @Test
  public void testDisjunction() throws Exception {
    Router router = new Router();
    Route r = router.append("/{a}", Collections.singletonMap(Names.A, PathParam.matching("a|b")));

    //
    assertEquals("/b", r.matches(Collections.singletonMap(Names.A, "b")).render());
  }

  @Test
  public void testCaptureGroup() throws Exception {
    Router router = new Router();
    Route r = router.append("/{a}", Collections.singletonMap(Names.A, PathParam.matching("a(.)c").captureGroup(true)));

    //
    assertEquals("/abc", r.matches(Collections.singletonMap(Names.A, "b")).render());
  }
}
