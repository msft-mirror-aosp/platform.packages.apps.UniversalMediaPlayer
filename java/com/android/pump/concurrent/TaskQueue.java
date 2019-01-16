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

import com.android.pump.util.Clog;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AnyThread
public class TaskQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {
    private static final String TAG = Clog.tag(TaskQueue.class);

    private static final int NANOS_PER_MILLI = 1000000;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final List mQueue = new List();
    @GuardedBy("mLock")
    private final List mRunning = new List();

    @Override
    public @NonNull Iterator<Runnable> iterator() {
        Clog.i(TAG, "iterator()");
        return new Iterator<Runnable>() {
            private final Iterator<Runnable> mIterator = mQueue.iterator();

            @Override
            public boolean hasNext() {
                synchronized (mLock) {
                    return mIterator.hasNext();
                }
            }

            @Override
            public @Nullable Runnable next() {
                synchronized (mLock) {
                    return mIterator.next();
                }
            }

            @Override
            public void remove() {
                synchronized (mLock) {
                    mIterator.remove();
                }
            }
        };
    }

    @Override
    public int size() {
        Clog.i(TAG, "size()");
        return mQueue.size();
    }

    @Override
    public void put(@NonNull Runnable runnable) throws InterruptedException {
        Clog.i(TAG, "put(" + runnable + ")");
        offer(runnable);
    }

    @Override
    public boolean offer(@NonNull Runnable runnable, long timeout, @NonNull TimeUnit unit)
            throws InterruptedException {
        Clog.i(TAG, "offer(" + runnable + ", " + timeout + ", " + unit + ")");
        return offer(runnable);
    }

    @Override
    public @NonNull Runnable take() throws InterruptedException {
        Clog.i(TAG, "take()");
        synchronized (mLock) {
            while (mQueue.size() == 0) {
                mLock.wait();
            }
            Task task = dequeue();
            if (mQueue.size() > 0) {
                mLock.notifyAll();
            }
            return task;
        }
    }

    @Override
    public @Nullable Runnable poll(long timeout, @NonNull TimeUnit unit)
            throws InterruptedException {
        Clog.i(TAG, "poll(" + timeout + ", " + unit + ")");
        synchronized (mLock) {
            if (mQueue.size() == 0) {
                wait(timeout, unit);
                if (mQueue.size() == 0) {
                    return null;
                }
            }
            Task task = dequeue();
            if (mQueue.size() > 0) {
                mLock.notifyAll();
            }
            return task;
        }
    }

    @Override
    public int remainingCapacity() {
        Clog.i(TAG, "remainingCapacity()");
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(@NonNull Collection<? super Runnable> c) {
        Clog.i(TAG, "drainTo(" + c + ")");
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(@NonNull Collection<? super Runnable> c, int maxElements) {
        Clog.i(TAG, "drainTo(" + c + ", " + maxElements + ")");
        if (c == this) {
            throw new IllegalArgumentException("Queue can't drain itself");
        }
        if (maxElements <= 0) {
            return 0;
        }
        synchronized (mLock) {
            /*
            int n = Math.min(mCount, maxElements);
            Node node = mHead;
            for (int i = 0; i < n; ++i) {
                c.add(node.task);
                node = node.next;
            }
            mHead = node;
            if (mHead == null) {
                mTail = null;
            }
            mCount -= n;
            mModificationId++;
            return n;
            */
            throw new IllegalStateException("Not yet implemented");
        }
    }

    @Override
    public boolean offer(@NonNull Runnable runnable) {
        Clog.i(TAG, "offer(" + runnable + ")");
        if (!(runnable instanceof Task)) {
            throw new IllegalArgumentException("The Runnable must be a Task");
        }
        synchronized (mLock) {
            boolean notify = mQueue.size() == 0;
            enqueue((Task) runnable);
            if (notify) {
                mLock.notifyAll();
            }
            return true;
        }
    }

    @Override
    public @Nullable Runnable poll() {
        Clog.i(TAG, "poll()");
        synchronized (mLock) {
            if (mQueue.size() == 0) {
                return null;
            }
            Task task = dequeue();
            if (mQueue.size() > 0) {
                mLock.notifyAll();
            }
            return task;
        }
    }

    @Override
    public @Nullable Runnable peek() {
        Clog.i(TAG, "peek()");
        synchronized (mLock) {
            return mQueue.peek();
        }
    }

    @Override
    public boolean remove(@Nullable Object o) {
        Clog.i(TAG, "remove(" + o + ")");
        if (!(o instanceof Task)) {
            return false;
        }
        synchronized (mLock) {
            return mQueue.remove((Task) o) != null;
        }
    }

    @Override
    public boolean contains(@Nullable Object o) {
        Clog.i(TAG, "contains(" + o + ")");
        if (!(o instanceof Task)) {
            return false;
        }
        synchronized (mLock) {
            return mQueue.find((Task) o) != null;
        }
    }

    @Override
    public @NonNull Object[] toArray() {
        Clog.i(TAG, "toArray()");
        synchronized (mLock) {
            /*
            Object[] a = new Object[mCount];
            int i = 0;
            for (Node node = mHead; node != null; node = node.next) {
                a[i++] = node.task;
            }
            return a;
            */
            throw new IllegalStateException("Not yet implemented");
        }
    }

    @Override
    public @NonNull <T> T[] toArray(@NonNull T[] a) {
        Clog.i(TAG, "toArray(" + a + ")");
        synchronized (mLock) {
            /*
            if (a.length < mCount) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), mCount);
            }
            int i = 0;
            for (Node node = mHead; node != null; node = node.next) {
                a[i++] = (T) node.task;
            }
            if (a.length > i) {
                a[i] = null;
            }
            return a;
            */
            throw new IllegalStateException("Not yet implemented");
        }
    }

    @Override
    public void clear() {
        Clog.i(TAG, "clear()");
        synchronized (mLock) {
            mQueue.clear();
        }
    }

    void prepare(@NonNull Task task) {
        Clog.i(TAG, "prepare(" + task + ")");
        synchronized (mLock) {
            Task runningTask = mRunning.find(task);
            if (runningTask != null) {
                if (runningTask != task) {
                    runningTask.merge(task); // TODO find another solution
                    task.cancel();
                }
            } else {
                Task queuedTask = mQueue.remove(task);
                if (queuedTask != null) {
                    task.merge(queuedTask); // TODO find another solution
                }
                mRunning.put(task);
            }
        }
    }

    void finish(@NonNull Task task) {
        Clog.i(TAG, "finished(" + task + ")");
        synchronized (mLock) {
            Task removed = mRunning.remove(task);
            if (removed != task) {
                throw new IllegalStateException("Failed to find running task " + task +
                        " found " + removed);
            }
        }
        task.finish();
    }

    @GuardedBy("mLock")
    private void enqueue(@NonNull Task task) {
        Clog.i(TAG, "enqueue(" + task + ")");
        Task runningTask = mRunning.find(task);
        if (runningTask != null) {
            runningTask.merge(task); // TODO find another solution
            return;
        }
        Task queuedTask = mQueue.find(task);
        if (queuedTask != null) {
            queuedTask.merge(task); // TODO find another solution
            return;
        }
        mQueue.put(task);
    }

    @GuardedBy("mLock")
    private @NonNull Task dequeue() {
        Clog.i(TAG, "dequeue()");
        // TODO Reuse the node just dequeued
        Task task = mQueue.get();
        mRunning.put(task);
        return task;
    }

    @GuardedBy("mLock")
    private void wait(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        long duration = unit.toNanos(timeout);
        long start = System.nanoTime();
        for (;;) {
            mLock.wait(duration / NANOS_PER_MILLI, (int) (duration % NANOS_PER_MILLI));
            long now = System.nanoTime();
            long elapsed = now - start;
            if (elapsed >= duration) {
                break;
            }
            duration -= elapsed;
            start = now;
        }
    }

    private static class List {
        private int mModificationId;

        private int mSize;
        private Node mHead;

        private int size() {
            return mSize;
        }

        private void clear() {
            mModificationId++;
            mSize = 0;
            mHead = null;
        }

        private @Nullable Task peek() {
            return mHead == null ? null : mHead.task;
        }

        private void put(@NonNull Task task) {
            mModificationId++;
            mSize++;
            mHead = new Node(task, mHead);
        }

        private @NonNull Task get() {
            mModificationId++;
            mSize--;
            Node node = mHead;
            mHead = node.next;
            return node.task;
        }

        private @Nullable Task find(@NonNull Task task) {
            for (Node node = mHead; node != null; node = node.next) {
                if (task.equals(node.task)) {
                    return node.task;
                }
            }
            return null;
        }

        private @Nullable Task remove(@NonNull Task task) {
            for (Node node = mHead, prev = null; node != null; node = (prev = node).next) {
                if (task.equals(node.task)) {
                    mModificationId++;
                    mSize--;
                    if (prev == null) {
                        mHead = node.next;
                    } else {
                        prev.next = node.next;
                    }
                    return node.task;
                }
            }
            return null;
        }

        private @NonNull Iterator<Runnable> iterator() {
            return new Iterator<Runnable>() {
                private int mExpectedModificationId;

                private Node mPrev;
                private Node mCurrent;
                private Node mNext = mHead;

                @Override
                public boolean hasNext() {
                    return mNext != null;
                }

                @Override
                public @NonNull Runnable next() {
                    if (mModificationId != mExpectedModificationId) {
                        throw new ConcurrentModificationException();
                    }
                    if (mNext == null) {
                        throw new NoSuchElementException();
                    }
                    mPrev = mCurrent;
                    mCurrent = mNext;
                    mNext = mNext.next;
                    return mCurrent.task;
                }

                @Override
                public void remove() {
                    if (mModificationId != mExpectedModificationId) {
                        throw new ConcurrentModificationException();
                    }
                    if (mCurrent == mPrev) {
                        throw new IllegalStateException();
                    }
                    mModificationId++;
                    mSize--;
                    if (mPrev == null) {
                        mHead = mNext;
                    } else {
                        mPrev.next = mNext;
                    }
                    mCurrent = mPrev;
                    mExpectedModificationId = mModificationId;
                }
            };
        }

        private static class Node {
            private final Task task;
            private Node next;

            private Node(@NonNull Task task, @Nullable Node next) {
                this.task = task;
                this.next = next;
            }
        }
    }
}
