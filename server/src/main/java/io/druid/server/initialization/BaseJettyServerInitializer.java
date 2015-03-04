/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.server.initialization;

import java.util.Set;

import com.google.common.base.Joiner;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.metamx.common.ISE;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.AsyncGzipFilter;
import org.eclipse.jetty.servlets.GzipFilter;

import javax.ws.rs.HttpMethod;

public abstract class BaseJettyServerInitializer implements JettyServerInitializer
{

  public static final String GZIP_METHODS = Joiner.on(",").join(HttpMethod.GET, HttpMethod.POST);

  public FilterHolder defaultGzipFilterHolder()
  {
    final FilterHolder gzipFilterHolder = new FilterHolder(GzipFilter.class);
    setDefaultGzipFilterHolderParameters(gzipFilterHolder);
    return gzipFilterHolder;
  }

  public FilterHolder defaultAsyncGzipFilterHolder()
  {
    final FilterHolder gzipFilterHolder = new FilterHolder(AsyncGzipFilter.class);
    setDefaultGzipFilterHolderParameters(gzipFilterHolder);
    return gzipFilterHolder;
  }

  private static void setDefaultGzipFilterHolderParameters(final FilterHolder filterHolder)
  {
    filterHolder.setInitParameter("minGzipSize", "0");
    filterHolder.setInitParameter("methods", GZIP_METHODS);

    // We don't actually have any precomputed .gz resources, and checking for them inside jars is expensive.
    filterHolder.setInitParameter("checkGzExists", String.valueOf(false));
  }

  public void addExtensionFilters(ServletContextHandler handler, Injector injector) {
    Set<ServletFilterHolder> extensionFilters = injector.getInstance(Key.get(new TypeLiteral<Set<ServletFilterHolder>>(){}));

    for (ServletFilterHolder servletFilterHolder : extensionFilters) {
      // Check the Filter first to guard against people who don't read the docs and return the Class even
      // when they have an instance.
      FilterHolder holder = null;
      if (servletFilterHolder.getFilter() != null) {
        holder = new FilterHolder(servletFilterHolder.getFilter());
      } else if (servletFilterHolder.getFilterClass() != null) {
        holder = new FilterHolder(servletFilterHolder.getFilterClass());
      } else {
        throw new ISE("Filter[%s] for path[%s] didn't have a Filter!?", servletFilterHolder, servletFilterHolder.getPath());
      }

      if(servletFilterHolder.getInitParameters() != null) {
        holder.setInitParameters(servletFilterHolder.getInitParameters());
      }

      handler.addFilter(holder, servletFilterHolder.getPath(), servletFilterHolder.getDispatcherType());
    }
  }
}
