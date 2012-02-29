/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.juzu.impl.spi.inject;

import org.juzu.impl.spi.inject.cdi.CDIBootstrap;
import org.juzu.impl.spi.inject.guice.GuiceBootstrap;
import org.juzu.impl.spi.inject.spring.SpringBootstrap;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public enum InjectImplementation
{

   CDI_WELD("cdi/weld")
   {
      @Override
      public InjectBuilder bootstrap()
      {
         return new CDIBootstrap();
      }
   },

   INJECT_GUICE("inject/guice")
   {
      @Override
      public InjectBuilder bootstrap()
      {
         return new GuiceBootstrap();
      }
   },

   INJECT_SPRING("inject/spring")
   {
      @Override
      public InjectBuilder bootstrap()
      {
         return new SpringBootstrap();
      }
   };

   /** . */
   final String value;

   private InjectImplementation(String value)
   {
      this.value = value;
   }

   public String getValue()
   {
      return value;
   }

   public abstract InjectBuilder bootstrap();

}
