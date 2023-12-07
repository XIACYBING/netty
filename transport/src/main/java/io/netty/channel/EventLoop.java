/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.util.concurrent.OrderedEventExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 用于处理{@link Channel}IO事件的工作线程池
 * <p>
 * 子类{@link SingleThreadEventLoop}则代表单线程的线程池，很多具体使用的实现类都继承该子类，比如
 * {@link io.netty.channel.nio.NioEventLoop}，可以理解为这些继承了{@link SingleThreadEventLoop}都是单线程的线程池，甚至可以直接视为单个工作线程，类似
 * {@link ThreadPoolExecutor.Worker JUC线程池的工作线程}
 * <p>
 * 当然，也有没继承{@link SingleThreadEventLoop}的实现类，比如{@link io.netty.channel.embedded.EmbeddedEventLoop}和
 * {@link AbstractEventLoop}，但是较少被使用
 * <p>
 * Will handle all the I/O operations for a {@link Channel} once registered.
 * <p>
 * One {@link EventLoop} instance will usually handle more than one {@link Channel} but this may depend on
 * implementation details and internals.
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();
}
