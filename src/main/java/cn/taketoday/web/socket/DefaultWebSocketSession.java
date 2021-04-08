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

package cn.taketoday.web.socket;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/4/5 14:25
 * @since 3.0
 */
public class DefaultWebSocketSession extends AbstractWebSocketSession {

  private Session session;
  private final RequestContext context;

  public DefaultWebSocketSession(RequestContext context, WebSocketHandler handler) {
    this.context = context;
    super.socketHandler = handler;
  }

  public void initializeNativeSession(Session session) {
    this.session = session;
  }

  @Override
  protected void sendText(String text) throws IOException {
    obtainNativeSession().getBasicRemote().sendText(text);
  }

  @Override
  protected void sendText(String partialMessage, boolean isLast) throws IOException {
    obtainNativeSession().getBasicRemote().sendText(partialMessage, isLast);
  }

  @Override
  protected void sendBinary(BinaryMessage data) throws IOException {

  }

  @Override
  protected void sendBinary(BinaryMessage partialByte, boolean isLast) throws IOException {

  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return obtainNativeSession().getMaxBinaryMessageBufferSize();
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return obtainNativeSession().getMaxTextMessageBufferSize();
  }

  @Override
  public long getMaxIdleTimeout() {
    return obtainNativeSession().getMaxIdleTimeout();
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    obtainNativeSession().setMaxBinaryMessageBufferSize(max);
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    obtainNativeSession().setMaxIdleTimeout(timeout);
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    obtainNativeSession().setMaxTextMessageBufferSize(max);
  }

  @Override
  public boolean isSecure() {
    return obtainNativeSession().isSecure();
  }

  @Override
  public boolean isOpen() {
    return obtainNativeSession().isOpen();
  }

  @Override
  public void close() throws IOException {
    obtainNativeSession().close();
  }

  @Override
  public void close(final CloseStatus status) throws IOException {
    final CloseReason closeReason = new CloseReason(
            CloseReason.CloseCodes.getCloseCode(status.getCode()), status.getReason());
    obtainNativeSession().close(closeReason);
  }

  public Session obtainNativeSession() {
    final Session session = this.session;
    Assert.state(session != null, "No javax.websocket.Session");
    return session;
  }
}