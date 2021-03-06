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

package juzu.impl.router.parser;

import juzu.impl.router.regex.SyntaxException;
import juzu.test.AbstractTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RouteParserTestCase extends AbstractTestCase {

  
  static class Collector implements RouteParserHandler {

    /** . */
    private final List<String> chunks = new ArrayList<String>();

    /** . */
    private final StringBuilder buffer = new StringBuilder();

    public void segmentOpen() {
      buffer.append("/");
    }

    public void segmentChunk(CharSequence s, int from, int to) {
      buffer.append(s, from, to);
    }

    public void segmentClose() {
      chunks.add(buffer.toString());
      buffer.setLength(0);
    }

    public void pathClose(boolean slash) {
      if (slash) {
        chunks.add("/$");
      } else {
        chunks.add("$");
      }
    }

    public void query() {
      chunks.add("?");
    }

    public void exprOpen() {
      buffer.append('{');
    }

    public void exprIdent(CharSequence s, int from, int to) {
      buffer.append(s, from, to);
    }

    public void exprClose() {
      buffer.append('}');
    }
  }
  
  private List<String> parse(String route) {
    try {
      Collector collector = new Collector();
      RouteParser.parse(route, collector);
      return collector.chunks;
    }
    catch (SyntaxException e) {
      throw failure(e);
    }
  }

  private void fail(String route, int expectedCode, int index) {
    try {
      RouteParser.parse(route, new Collector());
      throw failure("Was expecting route to fail at " + index);
    }
    catch (SyntaxException e) {
      assertEquals(expectedCode, e.getCode());
      assertNotNull(e.getLocation());
      assertEquals(index, e.getLocation().getCol());
      assertEquals(1, e.getLocation().getLine());
    }
  }

  @Test
  public void testSimple() {
    assertEquals(Arrays.asList("$"), parse(""));
    assertEquals(Arrays.asList("/a", "$"), parse("a"));
    assertEquals(Arrays.asList("/a", "$"), parse("/a"));

    assertEquals(Arrays.asList("/a", "$"), parse("//a"));
    assertEquals(Arrays.asList("/a", "/b", "$"), parse("a/b"));
  }
  
  @Test
  public void testPathParam() {
    assertEquals(Arrays.asList("/{a}", "$"), parse("{a}"));
    assertEquals(Arrays.asList("/a{b}c", "$"), parse("a{b}c"));
  }

  @Test
  public void testEndWithSeparator() {
    assertEquals(Arrays.asList("/$"), parse("/"));
    assertEquals(Arrays.asList("/a", "/$"), parse("a/"));
    assertEquals(Arrays.asList("/a", "/$"), parse("a//"));
    assertEquals(Arrays.asList("/a", "/$"), parse("/a/"));
    assertEquals(Arrays.asList("/a", "/$"), parse("/a//"));
  }

  @Test
  public void testInvalid() {
    fail("{", RouteParser.CODE_UNCLOSED_EXPR, 2);
    fail("{a", RouteParser.CODE_UNCLOSED_EXPR, 3);
    fail("{}", RouteParser.CODE_MISSING_EXPR_IDENT, 2);
  }
}
