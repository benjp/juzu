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

package inject;

import juzu.Scope;
import juzu.impl.common.Filter;
import juzu.impl.inject.ScopeController;
import juzu.impl.inject.Scoped;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.disk.DiskFileSystem;
import juzu.impl.inject.spi.Injector;
import juzu.impl.inject.spi.InjectorProvider;
import juzu.impl.inject.spi.InjectionContext;

import java.io.File;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class AbstractInjectTestCase<B, I> extends juzu.test.AbstractInjectTestCase {

  /** . */
  protected Injector bootstrap;

  /** . */
  protected InjectionContext<B, I> mgr;

  /** . */
  protected ReadFileSystem<?> fs;

  /** . */
  protected ScopingContextImpl scopingContext;

  public AbstractInjectTestCase(InjectorProvider di) {
    super(di);
  }

  protected final void init() throws Exception {
    init(getClass().getPackage().getName());
  }

  protected final void init(String pkg) throws Exception {
    File root = new File(AbstractInjectTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    assertTrue(root.exists());
    assertTrue(root.isDirectory());
    init(new DiskFileSystem(root, pkg), Thread.currentThread().getContextClassLoader());
  }

  protected final void init(ReadFileSystem<?> fs, ClassLoader classLoader) throws Exception {
    Injector bootstrap = getManager();
    bootstrap.addFileSystem(fs);
    bootstrap.setClassLoader(classLoader);

    //
    this.bootstrap = bootstrap;
    this.fs = fs;
  }

  protected final void boot(Scope... scopes) throws Exception {
    boot(null, scopes);
  }

  protected final void boot(Filter<Class<?>> filter, Scope... scopes) throws Exception {
    for (Scope scope : scopes) {
      bootstrap.addScope(scope);
    }
    if (filter == null) {
      mgr = (InjectionContext<B, I>)bootstrap.create();
    } else {
      mgr = (InjectionContext<B, I>)bootstrap.create(filter);
    }
  }

  protected final <T> T getBean(Class<T> beanType) throws Exception {
    B bean = mgr.resolveBean(beanType);
    assertNotNull("Could not resolve bean of type " + beanType, bean);
    I beanInstance = mgr.create(bean);
    assertNotNull("Could not create bean instance of type " + beanType + " from bean " + bean, beanInstance);
    Object o = mgr.get(bean, beanInstance);
    assertNotNull("Could not obtain bean object from bean instance " + beanInstance + " of type " + beanType, o);
    return beanType.cast(o);
  }

  protected final Object getBean(String beanName) throws Exception {
    B bean = mgr.resolveBean(beanName);
    assertNotNull("Could not find bean " + beanName, bean);
    I beanInstance = mgr.create(bean);
    assertNotNull(beanInstance);
    return mgr.get(bean, beanInstance);
  }

  protected final void beginScoping() throws Exception {
    if (scopingContext != null) {
      throw failure("Already scoping");
    }
    ScopeController.begin(scopingContext = new ScopingContextImpl());
  }

  protected final void endScoping() throws Exception {
    if (scopingContext == null) {
      throw failure("Not scoping");
    }
    ScopeController.end();
    for (Scoped scoped : scopingContext.getEntries().values()) {
      scoped.destroy();
    }
    scopingContext = null;
  }

  protected final Injector getManager() throws Exception {
    return getDI().get();
  }
}
