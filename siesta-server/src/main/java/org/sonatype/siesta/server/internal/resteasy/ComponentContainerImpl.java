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
package org.sonatype.siesta.server.internal.resteasy;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.ext.RuntimeDelegate;

import org.sonatype.siesta.Resource;
import org.sonatype.siesta.server.ComponentContainer;
import org.sonatype.siesta.server.SiestaServlet;
import org.eclipse.sisu.BeanEntry;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTEasy {@link ComponentContainer}.
 *
 * @since 2.0
 */
public class ComponentContainerImpl
  extends HttpServletDispatcher
  implements ComponentContainer
{
  private static final Logger log = LoggerFactory.getLogger(ComponentContainerImpl.class);

  private transient final ResteasyDeployment deployment = new SisuResteasyDeployment();

  public ComponentContainerImpl() {
    // Configure runtime delegate as early as possible
    RuntimeDelegate.setInstance(deployment.getProviderFactory());
  }

  @Override
  public void init(final ServletConfig servletConfig) throws ServletException {
    if (isPresetTccl(servletConfig)) {
      doInit(servletConfig);
      return;
    }

    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(ResteasyProviderFactory.class.getClassLoader());
      doInit(servletConfig);
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  private boolean isPresetTccl(final ServletConfig servletConfig) {
    Object presetTccl = servletConfig.getServletContext().getAttribute(SiestaServlet.ATTR_PRESET_TCCL);
    return presetTccl != null && (presetTccl == Boolean.TRUE || Boolean.parseBoolean(presetTccl.toString()));
  }

  private void doInit(final ServletConfig servletConfig) throws ServletException {
    deployment.start();

    servletConfig.getServletContext().setAttribute(ResteasyDeployment.class.getName(), deployment);

    super.init(servletConfig);

    if (log.isDebugEnabled()) {
      ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
      log.debug("Provider factory: {}", providerFactory);
      log.debug("Configuration: {}", providerFactory.getConfiguration());
      log.debug("Runtime type: {}", providerFactory.getRuntimeType());
      log.debug("Built-ins registered: {}", providerFactory.isBuiltinsRegistered());
      log.debug("Properties: {}", providerFactory.getProperties());
      log.debug("Dynamic features: {}", providerFactory.getServerDynamicFeatures());
      log.debug("Enabled features: {}", providerFactory.getEnabledFeatures());
      log.debug("Class contracts: {}", providerFactory.getClassContracts());
      log.debug("Reader interceptor registry: {}", providerFactory.getServerReaderInterceptorRegistry());
      log.debug("Writer interceptor registry: {}", providerFactory.getServerWriterInterceptorRegistry());
      log.debug("Injector factory: {}", providerFactory.getInjectorFactory());
      log.debug("Instances: {}", providerFactory.getInstances());
      log.debug("Exception mappers: {}", providerFactory.getExceptionMappers());
    }
  }

  @Override
  public void destroy() {
    super.destroy();

    deployment.stop();
  }

  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    super.service(request, response);
  }

  private static boolean isResource(final Class<?> type) {
    return Resource.class.isAssignableFrom(type);
  }

  @Nullable
  private static String resourcePath(final Class<?> type) {
    Path path = type.getAnnotation(Path.class);
    if (path != null) {
      return path.value();
    }
    return null;
  }

  @Override
  public void addComponent(final BeanEntry<?, ?> entry) throws Exception {
    Class<?> type = entry.getImplementationClass();
    if (isResource(type)) {
      getDispatcher().getRegistry().addResourceFactory(new SisuResourceFactory(entry));
      String path = resourcePath(type);
      if (path == null) {
        log.warn("Found resource implementation missing @Path: {}", type.getName());
      }
      else {
        log.debug("Added resource: {} with path: {}", type.getName(), path);
      }
    }
    else {
      // TODO: Doesn't seem to be a late-biding/factory here so we create the object early
      getDispatcher().getProviderFactory().register(entry.getValue());
      log.debug("Added component: {}", type.getName());
    }
  }

  @Override
  public void removeComponent(final BeanEntry<?, ?> entry) throws Exception {
    Class<?> type = entry.getImplementationClass();
    if (isResource(type)) {
      getDispatcher().getRegistry().removeRegistrations(type);
      String path = resourcePath(type);
      log.debug("Removed resource: {} with path: {}", type.getName(), path);
    }
    else {
      ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
      if (providerFactory instanceof SisuResteasyProviderFactory) {
        ((SisuResteasyProviderFactory) providerFactory).removeRegistrations(type);
        log.debug("Removed component: {}", type.getName());
      }
      else {
        log.warn("Component removal not supported; Unable to remove component: {}", type.getName());
      }
    }
  }
}
