/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.event.WebApplicationFailedEvent;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ServletParameterResolvers;
import cn.taketoday.web.servlet.initializer.DispatcherServletInitializer;
import cn.taketoday.web.servlet.initializer.WebFilterInitializer;
import cn.taketoday.web.servlet.initializer.WebListenerInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import cn.taketoday.web.view.template.DefaultTemplateViewResolver;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * Initialize Web application in a server like tomcat, jetty, undertow
 *
 * @author TODAY <br>
 * 2019-01-12 17:28
 */
public class WebServletApplicationLoader
        extends WebApplicationLoader implements ServletContainerInitializer {

  /** @since 3.0 */
  private String requestCharacterEncoding = Constant.DEFAULT_ENCODING;
  /** @since 3.0 */
  private String responseCharacterEncoding = Constant.DEFAULT_ENCODING;

  @Override
  protected ServletWebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
    return new ServletCompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
  }

  @Override
  public WebServletApplicationContext obtainApplicationContext() {
    return (WebServletApplicationContext) super.obtainApplicationContext();
  }

  @Override
  protected String getWebMvcConfigLocation() {
    String webMvcConfigLocation = super.getWebMvcConfigLocation();
    if (StringUtils.isEmpty(webMvcConfigLocation)) {
      webMvcConfigLocation = getServletContext().getInitParameter(Constant.WEB_MVC_CONFIG_LOCATION);
    }
    if (StringUtils.isEmpty(webMvcConfigLocation)) { // scan from '/'
      final String rootPath = getServletContext().getRealPath("/");
      final HashSet<String> paths = new HashSet<>();
      final File dir = new File(rootPath);
      if (dir.exists()) {
        log.trace("Finding Configuration File From Root Path: [{}]", rootPath);
        final class XmlFileFilter implements FileFilter {
          @Override
          public boolean accept(File path) {
            return path.isDirectory() || path.getName().endsWith(".xml");
          }
        }
        scanConfigLocation(dir, paths, new XmlFileFilter());
        return StringUtils.collectionToString(paths);
      }
      return null;
    }
    return webMvcConfigLocation;
  }

  /**
   * @return {@link ServletContext} or null if {@link ApplicationContext} not
   * initialize
   */
  protected ServletContext getServletContext() {
    return obtainApplicationContext().getServletContext();
  }

  /**
   * Find configuration file.
   *
   * @param dir
   *         directory
   */
  protected void scanConfigLocation(final File dir, final Set<String> files, FileFilter filter) {
    if (log.isTraceEnabled()) {
      log.trace("Enter [{}]", dir.getAbsolutePath());
    }
    final File[] listFiles = dir.listFiles(filter);
    if (listFiles == null) {
      log.error("File: [{}] Does not exist", dir);
      return;
    }
    for (final File file : listFiles) {
      if (file.isDirectory()) { // recursive
        scanConfigLocation(file, files, filter);
      }
      else {
        files.add(file.getAbsolutePath());
      }
    }
  }

  /**
   * Prepare {@link WebServletApplicationContext}
   *
   * @param servletContext
   *         {@link ServletContext}
   *
   * @return {@link WebServletApplicationContext}
   */
  protected WebServletApplicationContext prepareApplicationContext(ServletContext servletContext) {
    WebServletApplicationContext ret = getWebServletApplicationContext();
    if (ret == null) {
      final long startupDate = System.currentTimeMillis();
      log.info("Your application starts to be initialized at: [{}].",
               new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));
      final ConfigurableWebServletApplicationContext context = createContext();
      ret = context;
      context.setServletContext(servletContext);
      setApplicationContext(context);
      context.load();
    }
    else if (ret instanceof ConfigurableWebServletApplicationContext && ret.getServletContext() == null) {
      ((ConfigurableWebServletApplicationContext) ret).setServletContext(servletContext);
      log.info("ServletContext: [{}] Configure Success.", servletContext);
    }
    return ret;
  }

  /**
   * create a {@link ConfigurableWebServletApplicationContext},
   * subclasses can override this method to create user customize context
   */
  protected ConfigurableWebServletApplicationContext createContext() {
    return new StandardWebServletApplicationContext();
  }

  private WebServletApplicationContext getWebServletApplicationContext() {
    return (WebServletApplicationContext) getApplicationContext();
  }

  @Override
  public void onStartup(Set<Class<?>> classes, ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext can't be null");
    final WebApplicationContext context = prepareApplicationContext(servletContext);
    try {
      try {
        servletContext.setRequestCharacterEncoding(getRequestCharacterEncoding());
        servletContext.setResponseCharacterEncoding(getResponseCharacterEncoding());
      }
      catch (Throwable ignored) {}
      onStartup(context);
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      context.publishEvent(new WebApplicationFailedEvent(context, ex));
      throw new ConfigurationException("Your Application Initialized ERROR: [" + ex + "]", ex);
    }
  }

  @Override
  protected void configureParameterResolver(List<ParameterResolver> resolvers, WebMvcConfiguration mvcConfiguration) {
    // register servlet env resolvers
    ServletParameterResolvers.register(resolvers, getServletContext());
    super.configureParameterResolver(resolvers, mvcConfiguration);
  }

  @Override
  protected void checkFrameWorkComponents(WebApplicationContext context) {
    if (!context.containsBeanDefinition(TemplateViewResolver.class)) {
      // use default view resolver
      context.registerBean(DefaultTemplateViewResolver.class);
      log.info("Use default view resolver: [{}].", context.getBean(DefaultTemplateViewResolver.class));
    }
    super.checkFrameWorkComponents(context);
  }

  @Override
  protected DispatcherHandler createDispatcher(WebApplicationContext ctx) {
    Assert.isInstanceOf(WebServletApplicationContext.class, ctx, "context must be a WebServletApplicationContext");
    final WebServletApplicationContext context = (WebServletApplicationContext) ctx;
    final DispatcherServletInitializer initializer = context.getBean(DispatcherServletInitializer.class);
    if (initializer != null) {
      DispatcherServlet ret = initializer.getServlet();
      if (ret == null) {
        ret = doCreateDispatcherServlet(context);
        initializer.setServlet(ret);
      }
      return ret;
    }
    return doCreateDispatcherServlet(context);
  }

  protected DispatcherServlet doCreateDispatcherServlet(WebServletApplicationContext context) {
    return new DispatcherServlet(context);
  }

  @Override
  protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {
    final WebServletApplicationContext ctx = obtainApplicationContext();

    configureFilterInitializers(ctx, initializers);
    configureServletInitializers(ctx, initializers);
    configureListenerInitializers(ctx, initializers);

    // DispatcherServlet Initializer
    if (!ctx.containsBeanDefinition(DispatcherServletInitializer.class)) {
      initializers.add(new DispatcherServletInitializer(ctx, obtainDispatcher()));
    }

    super.configureInitializer(initializers, config);
  }

  @Override
  public DispatcherServlet obtainDispatcher() {
    return (DispatcherServlet) super.obtainDispatcher();
  }

  /**
   * Configure {@link Filter}
   *
   * @param applicationContext
   *         {@link ApplicationContext}
   * @param contextInitializers
   *         {@link WebApplicationInitializer}s
   */
  protected void configureFilterInitializers(
          final WebApplicationContext applicationContext, final List<WebApplicationInitializer> contextInitializers) {

    List<Filter> filters = applicationContext.getAnnotatedBeans(WebFilter.class);
    for (final Filter filter : filters) {
      final Class<?> beanClass = filter.getClass();
      WebFilterInitializer<Filter> webFilterInitializer = new WebFilterInitializer<>(filter);
      WebFilter webFilter = beanClass.getAnnotation(WebFilter.class);
      final Set<String> urlPatterns = new HashSet<>();
      Collections.addAll(urlPatterns, webFilter.value());
      Collections.addAll(urlPatterns, webFilter.urlPatterns());

      webFilterInitializer.addUrlMappings(StringUtils.toStringArray(urlPatterns));
      webFilterInitializer.addServletNames(webFilter.servletNames());
      webFilterInitializer.setAsyncSupported(webFilter.asyncSupported());

      for (WebInitParam initParam : webFilter.initParams()) {
        webFilterInitializer.addInitParameter(initParam.name(), initParam.value());
      }

      String name = webFilter.filterName();
      if (StringUtils.isEmpty(name)) {
        final String displayName = webFilter.displayName();
        if (StringUtils.isEmpty(displayName)) {
          name = applicationContext.getBeanName(beanClass);
        }
        else {
          name = displayName;
        }
      }

      webFilterInitializer.setName(name);
      webFilterInitializer.setDispatcherTypes(webFilter.dispatcherTypes());

      contextInitializers.add(webFilterInitializer);
    }
  }

  /**
   * Configure {@link Servlet}
   *
   * @param applicationContext
   *         {@link ApplicationContext}
   * @param contextInitializers
   *         {@link WebApplicationInitializer}s
   */
  protected void configureServletInitializers(
          final WebApplicationContext applicationContext, final List<WebApplicationInitializer> contextInitializers) {

    Collection<Servlet> servlets = applicationContext.getAnnotatedBeans(WebServlet.class);
    for (Servlet servlet : servlets) {
      final Class<?> beanClass = servlet.getClass();
      WebServletInitializer<Servlet> webServletInitializer = new WebServletInitializer<>(servlet);
      WebServlet webServlet = beanClass.getAnnotation(WebServlet.class);
      String[] urlPatterns = webServlet.urlPatterns();
      if (StringUtils.isArrayEmpty(urlPatterns)) {
        urlPatterns = new String[] { applicationContext.getBeanName(beanClass) };
      }
      webServletInitializer.addUrlMappings(urlPatterns);
      webServletInitializer.setLoadOnStartup(webServlet.loadOnStartup());
      webServletInitializer.setAsyncSupported(webServlet.asyncSupported());

      for (WebInitParam initParam : webServlet.initParams()) {
        webServletInitializer.addInitParameter(initParam.name(), initParam.value());
      }

      final MultipartConfig multipartConfig = beanClass.getAnnotation(MultipartConfig.class);
      if (multipartConfig != null) {
        webServletInitializer.setMultipartConfig(new MultipartConfigElement(multipartConfig));
      }
      final ServletSecurity servletSecurity = beanClass.getAnnotation(ServletSecurity.class);
      if (servletSecurity != null) {
        webServletInitializer.setServletSecurity(new ServletSecurityElement(servletSecurity));
      }

      String name = webServlet.name();
      if (StringUtils.isEmpty(name)) {
        final String displayName = webServlet.displayName();
        if (StringUtils.isEmpty(displayName)) {
          name = applicationContext.getBeanName(beanClass);
        }
        else {
          name = displayName;
        }
      }
      webServletInitializer.setName(name);

      contextInitializers.add(webServletInitializer);
    }
  }

  /**
   * Configure listeners
   *
   * @param applicationContext
   *         {@link ApplicationContext}
   * @param contextInitializers
   *         {@link WebApplicationInitializer}s
   */
  protected void configureListenerInitializers(
          final WebApplicationContext applicationContext, final List<WebApplicationInitializer> contextInitializers) {
    Collection<EventListener> eventListeners = applicationContext.getAnnotatedBeans(WebListener.class);
    for (EventListener eventListener : eventListeners) {
      contextInitializers.add(new WebListenerInitializer<>(eventListener));
    }
  }

  //

  /**
   * Sets the request character encoding for this ServletContext.
   *
   * @param encoding
   *         request character encoding
   *
   * @since 3.0
   */
  public void setRequestCharacterEncoding(String encoding) {
    this.requestCharacterEncoding = encoding;
  }

  /**
   * Sets the response character encoding for this ServletContext.
   *
   * @param encoding
   *         response character encoding
   *
   * @since 3.0
   */
  public void setResponseCharacterEncoding(String encoding) {
    this.responseCharacterEncoding = encoding;
  }

  /**
   * Gets the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>. This method returns null if no request
   * encoding character encoding has been specified in deployment descriptor
   * or container specific configuration (for all web applications in the
   * container).
   *
   * @return the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>
   */
  public String getRequestCharacterEncoding() {
    return requestCharacterEncoding;
  }

  /**
   * Gets the response character encoding that are supported by default for
   * this <tt>ServletContext</tt>. This method returns null if no response
   * encoding character encoding has been specified in deployment descriptor
   * or container specific configuration (for all web applications in the
   * container).
   *
   * @return the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>
   */
  public String getResponseCharacterEncoding() {
    return responseCharacterEncoding;
  }

}
