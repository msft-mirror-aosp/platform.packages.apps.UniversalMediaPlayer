/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.pump.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AnyThread
public class TaskExecutor extends ThreadPoolExecutor {
    public static @NonNull ExecutorService newFixedThreadPool(int nThreads) {
        return new TaskExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, new TaskQueue());
    }

    public static @NonNull ExecutorService newFixedThreadPool(int nThreads,
                @NonNull ThreadFactory threadFactory) {
        return new TaskExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, new TaskQueue(),
                threadFactory);
    }

    public static @NonNull ExecutorService newCachedThreadPool() {
        return new TaskExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new TaskQueue());
    }

    public static @NonNull ExecutorService newCachedThreadPool(
            @NonNull ThreadFactory threadFactory) {
        return new TaskExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new TaskQueue(),
                threadFactory);
    }

    public TaskExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull TaskQueue workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public TaskExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull TaskQueue workQueue,
            @NonNull ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public TaskExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull TaskQueue workQueue,
            @NonNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public TaskExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull TaskQueue workQueue,
            @NonNull ThreadFactory threadFactory, @NonNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                handler);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!(command instanceof Task)) {
            throw new IllegalArgumentException("The Runnable must be a Task");
        }
        super.execute(command);
    }

    @Override
    protected void beforeExecute(@NonNull Thread t, @NonNull Runnable r) {
        getQueue().prepare((Task) r);
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(@NonNull Runnable r, @Nullable Throwable t) {
        super.afterExecute(r, t);
        getQueue().finish((Task) r);
    }

    @Override
    public @NonNull TaskQueue getQueue() {
        return (TaskQueue) super.getQueue();
    }
}
