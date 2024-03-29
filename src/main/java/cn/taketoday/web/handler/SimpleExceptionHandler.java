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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.imageio.ImageIO;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.http.HttpStatusCapable;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.TemplateResultHandler;

/**
 * Simple {@link HandlerExceptionHandler}
 *
 * @author TODAY 2020-03-29 21:01
 */
public class SimpleExceptionHandler
        extends OrderedSupport implements HandlerExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(SimpleExceptionHandler.class);

  @Override
  public Object handleException(final RequestContext context,
                                final Throwable target, final Object handler) throws Throwable {
    logCatchThrowable(target);
    try {
      if (handler instanceof HandlerMethod) {
        return handleHandlerMethodInternal(target, context, (HandlerMethod) handler);
      }
      if (handler instanceof ViewController) {
        return handleViewControllerInternal(target, context, (ViewController) handler);
      }
      if (handler instanceof ResourceRequestHandler) {
        return handleResourceMappingInternal(target, context, (ResourceRequestHandler) handler);
      }
      return handleExceptionInternal(target, context);
    }
    catch (Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      throw handlerEx;
    }
  }

  /**
   * record exception log occurred in target request handler
   *
   * @param target
   *         Throwable occurred in target request handler
   */
  protected void logCatchThrowable(final Throwable target) {
    if (log.isDebugEnabled()) {
      log.debug("Catch Throwable: [{}]", target.toString(), target);
    }
  }

  /**
   * record log when a exception occurred in this exception handler
   *
   * @param target
   *         Throwable that occurred in request handler
   * @param handlerException
   *         Throwable occurred in this exception handler
   */
  protected void logResultedInException(Throwable target, Throwable handlerException) {
    log.error("Handling of [{}] resulted in Exception: [{}]",
              target.getClass().getName(),
              handlerException.getClass().getName(), handlerException);
  }

  /**
   * Resolve {@link ResourceRequestHandler} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param handler
   *         {@link ResourceRequestHandler}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleResourceMappingInternal(final Throwable ex,
                                                 final RequestContext context,
                                                 final ResourceRequestHandler handler) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link ViewController} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param viewController
   *         {@link ViewController}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleViewControllerInternal(final Throwable ex,
                                                final RequestContext context,
                                                final ViewController viewController) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link HandlerMethod} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param handlerMethod
   *         {@link HandlerMethod}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleHandlerMethodInternal(final Throwable ex,
                                               final RequestContext context,
                                               final HandlerMethod handlerMethod) throws Throwable//
  {
    context.setStatus(getErrorStatusValue(ex));

    if (handlerMethod.isAssignableTo(RenderedImage.class)) {
      return resolveImageException(ex, context);
    }
    if (!handlerMethod.is(void.class)
            && !handlerMethod.is(Object.class)
            && !handlerMethod.is(ModelAndView.class)
            && TemplateResultHandler.supportsHandlerMethod(handlerMethod)) {

      return handleExceptionInternal(ex, context);
    }

    writeErrorMessage(ex, context);
    return NONE_RETURN_VALUE;
  }

  /**
   * Write error message to request context, default is write json
   *
   * @param ex
   *         Throwable that occurred in request handler
   * @param context
   *         current request context
   */
  protected void writeErrorMessage(Throwable ex, RequestContext context) throws IOException {
    context.setContentType(Constant.CONTENT_TYPE_JSON);
    final PrintWriter writer = context.getWriter();
    writer.write(buildDefaultErrorMessage(ex));
    writer.flush();
  }

  protected String buildDefaultErrorMessage(final Throwable ex) {
    return new StringBuilder()
            .append("{\"message\":\"")
            .append(ex.getMessage())
            .append("\"}")
            .toString();
  }

  /**
   * Get error http status value, if target throwable is {@link HttpStatusCapable}
   * its return from {@link HttpStatusCapable#getHttpStatus()}
   *
   * @param ex
   *         Throwable that occurred in request handler
   *
   * @return Http status code
   */
  public int getErrorStatusValue(Throwable ex) {
    if (ex instanceof HttpStatusCapable) { // @since 3.0.1
      final HttpStatus httpStatus = ((HttpStatusCapable) ex).getHttpStatus();
      return httpStatus.value();
    }
    return WebUtils.getStatusValue(ex);
  }

  /**
   * resolve view exception
   *
   * @param ex
   *         Target {@link Exception}
   * @param context
   *         Current request context
   */
  public Object handleExceptionInternal(
          final Throwable ex, final RequestContext context) throws IOException {
    context.sendError(getErrorStatusValue(ex), ex.getMessage());
    return NONE_RETURN_VALUE;
  }

  /**
   * resolve image
   */
  public BufferedImage resolveImageException(
          final Throwable ex, final RequestContext context) throws IOException {
    final URL resource = ClassUtils.getClassLoader()
            .getResource(new StringBuilder()
                                 .append("/error/")
                                 .append(getErrorStatusValue(ex))
                                 .append(".png").toString());

    Assert.state(resource != null, "System Error");

    context.setContentType(Constant.CONTENT_TYPE_IMAGE);
    return ImageIO.read(resource);
  }

}
