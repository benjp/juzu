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

package juzu.impl.inject.spi.spring;

import juzu.Scope;
import juzu.impl.common.Tools;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ScopeMetadata;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class DeclaredProviderBean extends AbstractBean {

  /** . */
  private final Class<? extends Provider> providerType;

  /** . */
  private final Scope scope;

  DeclaredProviderBean(
    Class<?> type,
    Scope scope,
    Iterable<Annotation> qualifiers,
    Class<? extends Provider> providerType) {
    super(type, qualifiers);

    //
    this.scope = scope;
    this.providerType = providerType;
  }

  @Override
  void configure(String name, SpringBuilder builder, DefaultListableBeanFactory factory) {
    String id = Tools.nextUUID();
    AnnotatedGenericBeanDefinition def = new AnnotatedGenericBeanDefinition(providerType);
    def.setScope("singleton");
    factory.registerBeanDefinition(id, def);

    //
    AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(type);

    //
    if (scope != null) {
      definition.setScope(scope.name().toLowerCase());
    }
    else {
      ScopeMetadata scopeMD = builder.scopeResolver.resolveScopeMetadata(definition);
      if (scopeMD != null) {
        definition.setScope(scopeMD.getScopeName());
      }
    }

    //
    if (qualifiers != null) {
      for (AutowireCandidateQualifier qualifier : qualifiers) {
        definition.addQualifier(qualifier);
      }
    }

    //
    if (qualifiers != null) {
      for (AutowireCandidateQualifier qualifier : qualifiers) {
        definition.addQualifier(qualifier);
      }
    }

    //
    definition.setFactoryBeanName(id);
    definition.setFactoryMethodName("get");

    //
    factory.registerBeanDefinition(name, definition);
  }
}
