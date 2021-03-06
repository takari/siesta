/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.siesta.server.resteasy;

import javax.inject.Singleton;

import org.sonatype.siesta.server.ComponentContainer;
import org.sonatype.siesta.server.internal.resteasy.ComponentContainerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.jboss.resteasy.core.Dispatcher;

/**
 * RESTEasy module.
 *
 * @since 2.0
 */
public class ResteasyModule
  extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ComponentContainer.class).to(ComponentContainerImpl.class).in(Singleton.class);
  }

  /**
   * Expose RESTEasy {@link Dispatcher} binding.
   */
  @Provides
  public Dispatcher dispatcher(final ComponentContainer container) {
    return ((ComponentContainerImpl)container).getDispatcher();
  }
}
