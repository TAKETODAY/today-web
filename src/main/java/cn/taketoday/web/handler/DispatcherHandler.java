/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.AbstractRequestContext;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlerCapable;
import cn.taketoday.web.view.RuntimeResultHandler;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.context.utils.ExceptionUtils.unwrapThrowable;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY <br>
 * 2019-11-16 19:05
 * @since 3.0
 */
public class DispatcherHandler extends WebApplicationContextSupport {

  /** Action mapping registry */
  private HandlerRegistry handlerRegistry;
  private HandlerAdapter[] handlerAdapters;
  private RuntimeResultHandler[] resultHandlers;
  /** exception handler */
  private HandlerExceptionHandler exceptionHandler;

  public DispatcherHandler() {}

  public DispatcherHandler(WebApplicationContext context) {
    setApplicationContext(context);
  }

  // Handler
  // ----------------------------------

  /**
   * Find a suitable handler to handle this HTTP request
   *
   * @param context
   *         Current HTTP request context
   *
   * @return Target handler, if returns {@code null} indicates that there isn't a
   * handler to handle this request
   */
  public Object lookupHandler(final RequestContext context) {
    return handlerRegistry.lookup(context);
  }

  /**
   * Find a {@link HandlerAdapter} for input handler
   *
   * @param handler
   *         HTTP handler
   *
   * @return A {@link HandlerAdapter}
   *
   * @throws IllegalStateException
   *         If there isn't a {@link HandlerAdapter} for target handler
   */
  public HandlerAdapter lookupHandlerAdapter(final Object handler) {
    if (handler instanceof HandlerAdapter) {
      return (HandlerAdapter) handler;
    }
    if (handler instanceof HandlerAdapterCapable) {
      return ((HandlerAdapterCapable) handler).getHandlerAdapter();
    }
    for (final HandlerAdapter requestHandler : handlerAdapters) {
      if (requestHandler.supports(handler)) {
        return requestHandler;
      }
    }
    throw new IllegalStateException("No HandlerAdapter for handler: [" + handler + ']');
  }

  /**
   * Find {@link ResultHandler} for handler and handler execution result
   *
   * @param handler
   *         HTTP handler
   * @param result
   *         Handler execution result
   *
   * @return {@link ResultHandler}
   *
   * @throws IllegalStateException
   *         If there isn't a {@link ResultHandler} for target handler and
   *         handler execution result
   */
  public ResultHandler lookupResultHandler(final Object handler, final Object result) {
    if (handler instanceof ResultHandler) {
      return (ResultHandler) handler;
    }
    if (handler instanceof ResultHandlerCapable) {
      return ((ResultHandlerCapable) handler).getResultHandler();
    }
    for (final RuntimeResultHandler resultHandler : resultHandlers) {
      if (resultHandler.supportsResult(result) || resultHandler.supportsHandler(handler)) {
        return resultHandler;
      }
    }
    throw new IllegalStateException("No RuntimeResultHandler for result: [" + result + ']');
  }

  /**
   * Check if this request is not modified
   *
   * @param handler
   *         HTTP handler
   * @param context
   *         Current HTTP request context
   * @param adapter
   *         Handler's {@link HandlerAdapter Adapter}
   *
   * @return If not modified
   */
  public boolean notModified(final Object handler,
                             final RequestContext context,
                             final HandlerAdapter adapter) {
    final String method = context.method();
    // Process last-modified header, if supported by the handler.
    final boolean isGet = "GET".equals(method);
    if (isGet || "HEAD".equals(method)) {
      final long lastModified = adapter.getLastModified(context, handler);
      return isGet && WebUtils.checkNotModified(null, lastModified, context);
    }
    return false;
  }

  /**
   * Handle HTTP request
   *
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} occurred in handler
   */
  public void handle(final RequestContext context) throws Throwable {
    handle(lookupHandler(context), context);
  }

  /**
   * Handle HTTP request
   *
   * @param handler
   *         HTTP handler
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} occurred in handler
   */
  public void handle(final Object handler, final RequestContext context) throws Throwable {
    handle(handler, context, lookupHandlerAdapter(handler));
  }

  /**
   * Handle HTTP request not modify
   *
   * @param handler
   *         HTTP handler
   * @param context
   *         Current HTTP request context
   * @param adapter
   *         {@link HandlerAdapter}
   *
   * @throws Throwable
   *         If {@link Throwable} occurred in handler
   */
  public void handleNotModify(final Object handler,
                              final RequestContext context,
                              final HandlerAdapter adapter) throws Throwable {
    if (!notModified(handler, context, adapter)) {
      handle(handler, context, adapter);
    }
  }

  /**
   * Handle HTTP request
   *
   * @param handler
   *         HTTP handler
   * @param context
   *         Current HTTP request context
   * @param adapter
   *         {@link HandlerAdapter}
   */
  public void handle(final Object handler,
                     final RequestContext context,
                     final HandlerAdapter adapter) throws Throwable {
    try {
      final Object view = adapter.handle(context, handler);
      if (view != HandlerAdapter.NONE_RETURN_VALUE) {
        lookupResultHandler(handler, view)
                .handleResult(context, handler, view);
      }
      // @since 3.0 flush headers
      applyHeaders(context);
    }
    catch (Throwable e) {
      handleException(handler, e, context);
    }
  }

  private void applyHeaders(final RequestContext context) {
    if(context instanceof AbstractRequestContext) {
      ((AbstractRequestContext) context).applyHeaders();
    }
  }

  /**
   * Handle {@link Exception} occurred in target handler
   *
   * @param handler
   *         HTTP handler
   * @param exception
   *         {@link Throwable} occurred in target handler
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} occurred in {@link HandlerExceptionHandler}
   */
  public void handleException(final Object handler,
                              final Throwable exception,
                              final RequestContext context) throws Throwable {
    final Object view = getExceptionHandler()
            .handleException(context, unwrapThrowable(exception), handler);
    if (view != HandlerAdapter.NONE_RETURN_VALUE) {
      for (final RuntimeResultHandler resultHandler : resultHandlers) {
        if (resultHandler.supportsResult(view)) {
          resultHandler.handleResult(context, handler, view);
          break;
        }
      }
    }
    // @since 3.0 flush headers
    applyHeaders(context);
  }

  /**
   * Destroy Application
   */
  public void destroy() {

    final WebApplicationContext context = obtainApplicationContext();
    if (context != null) {
      final State state = context.getState();
      if (state != State.CLOSING && state != State.CLOSED) {
        context.close();

        final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);
        final String msg = new StringBuilder("Your application destroyed at: [")
                .append(dateFormat.format(System.currentTimeMillis()))
                .append("] on startup date: [")
                .append(dateFormat.format(context.getStartupDate()))
                .append(']')
                .toString();

        log(msg);
      }
    }
  }

  /**
   * Log internal
   *
   * @param msg
   *         Log message
   */
  protected void log(final String msg) {
    log.info(msg);
  }

  public final HandlerAdapter[] getHandlerAdapters() {
    return handlerAdapters;
  }

  public final RuntimeResultHandler[] getResultHandlers() {
    return resultHandlers;
  }

  public final HandlerRegistry getHandlerRegistry() {
    return handlerRegistry;
  }

  public HandlerExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setHandlerRegistry(HandlerRegistry handlerRegistry) {
    Assert.notNull(resultHandlers, "HandlerRegistry must not be null");
    this.handlerRegistry = nonNull(handlerRegistry, "handler registry must not be null");
  }

  public void setHandlerAdapters(HandlerAdapter... handlerAdapters) {
    Assert.notNull(handlerAdapters, "handlerAdapters must not be null");
    this.handlerAdapters = handlerAdapters;
  }

  public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler must not be null");
    this.exceptionHandler = exceptionHandler;
  }

  public void setResultHandlers(RuntimeResultHandler... resultHandlers) {
    Assert.notNull(resultHandlers, "resultHandlers must not be null");
    this.resultHandlers = resultHandlers;
  }
}
