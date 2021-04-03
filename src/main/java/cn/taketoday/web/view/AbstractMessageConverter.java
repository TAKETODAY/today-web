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

import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ui.JsonSequence;

/**
 * @author TODAY 2021/3/10 12:37
 * @since 3.0
 */
public abstract class AbstractMessageConverter implements MessageConverter {

  @Override
  public void write(RequestContext context, Object message) throws IOException {
    if (message != null) {
      if (message instanceof CharSequence) {
        writeStringInternal(context, message.toString());
      }
      else {
        if (message instanceof JsonSequence) {
          message = ((JsonSequence) message).getJSON();
        }
        context.setContentType(Constant.CONTENT_TYPE_JSON);
        writeInternal(context, message);
      }
    }
    else {
      writeNullInternal(context);
    }
  }

  protected void writeStringInternal(RequestContext context, String message) throws IOException {
    context.getWriter().println(message);
  }

  protected void writeNullInternal(RequestContext context) throws IOException { }

  /**
   * Write none null message
   */
  abstract void writeInternal(RequestContext context, Object noneNullMessage) throws IOException;
}
