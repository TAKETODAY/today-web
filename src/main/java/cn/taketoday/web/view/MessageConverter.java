/*
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
package cn.taketoday.web.view;

import java.io.IOException;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.ui.JsonSequence;

/**
 * @author TODAY <br>
 * 2019-07-17 13:31
 * @see JsonSequence
 */
public interface MessageConverter {

  /**
   * Write message to client
   *
   * @param context
   *         Current request context
   * @param message
   *         The message write to client
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  void write(RequestContext context, Object message) throws IOException;

  /**
   * Read The request body and convert it to Target object
   *
   * @param context
   *         Current request context
   * @param parameter
   *         Handler method parameter
   *
   * @return The handler method parameter object
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  Object read(RequestContext context, MethodParameter parameter) throws IOException;

}