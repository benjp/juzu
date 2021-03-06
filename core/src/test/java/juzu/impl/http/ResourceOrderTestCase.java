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

package juzu.impl.http;

import junit.framework.AssertionFailedError;
import juzu.test.protocol.http.AbstractHttpTestCase;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a> */
public class ResourceOrderTestCase extends AbstractHttpTestCase {

  @Drone
  WebDriver driver;

  @Test
  public void testResourceOrder() throws Exception {
    assertDeploy("http.resource");
    driver.get(deploymentURL.toString());
    List<WebElement> elts = driver.findElements(By.xpath("//head/*"));
    List<String> previous = new ArrayList<String>();
    for (WebElement elt : elts) {
      assertOrder(previous, elt);
      previous.add(elt.getTagName());
    }
  }

  private void assertOrder(List<String> previous, WebElement current) {
    if ("link".equals(current.getTagName()) && previous.contains("script")) {
      throw new AssertionFailedError("js must be set before css resource");
    }
  }
}
