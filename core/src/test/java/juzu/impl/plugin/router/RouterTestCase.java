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

package juzu.impl.plugin.router;

import juzu.impl.inject.spi.InjectorProvider;
import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.test.AbstractInjectTestCase;
import juzu.test.protocol.mock.MockApplication;
import org.junit.Test;

import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RouterTestCase extends AbstractInjectTestCase {

  public RouterTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void testDeclaration() throws Exception {
    MockApplication<?> application = application("plugin.router.declaration").init();

    //
    ApplicationDescriptor descriptor = application.getContext().getDescriptor();
    RouteDescriptor desc = (RouteDescriptor)descriptor.getPluginDescriptor("router");
    List<RouteDescriptor> routes = desc.getChildren();
    assertEquals(3, routes.size());
  }
}
