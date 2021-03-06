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

package inject.supertype.qualifier;

import inject.AbstractInjectTestCase;
import juzu.impl.inject.spi.InjectorProvider;
import org.junit.Test;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class SuperTypeTestCase<B, I> extends AbstractInjectTestCase<B, I> {

  public SuperTypeTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void testSuperType() throws Exception {
    init();
    bootstrap.declareBean(Fruit.class, null, null, Apple.class);
    bootstrap.declareBean(InjectedWithSuperType.class, null, null, null);
    bootstrap.declareBean(InjectedWithActualType.class, null, null, null);
    boot();

    //
    InjectedWithSuperType withSuperType = getBean(InjectedWithSuperType.class);
    assertNotNull(withSuperType);
    assertNotNull(withSuperType.fruit);

    //
    InjectedWithActualType withActualType = getBean(InjectedWithActualType.class);
    assertNotNull(withActualType);
    assertNotNull(withActualType.apple);
  }
}
