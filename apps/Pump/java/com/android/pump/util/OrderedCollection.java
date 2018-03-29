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

package com.android.pump.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

@AnyThread
public class OrderedCollection<T, U> extends AbstractCollection<T> {
    private final KeyRetriever<T, U> mKeyRetriever;
    private final KeyComparator<U> mKeyComparator;
    private final List<T> mItems = new ArrayList<>();

    @FunctionalInterface
    public interface KeyRetriever<T, U> {
        @NonNull U getKey(@NonNull T value);
    }

    @FunctionalInterface
    public interface KeyComparator<U> {
        int compare(@NonNull U lhs, @NonNull U rhs);
    }

    public OrderedCollection(@NonNull KeyRetriever<T, U> keyRetriever,
            @NonNull KeyComparator<U> keyComparator) {
        mKeyRetriever = keyRetriever;
        mKeyComparator = keyComparator;
    }

    public @NonNull T get(@NonNull U key) {
        int index = indexOfU(key);
        if (index >= 0) {
            return mItems.get(index);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return mItems.iterator();
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    public boolean contains(@NonNull Object o) {
        return indexOfO(o) >= 0;
    }

    @Override
    public boolean add(@NonNull T e) {
        int index = indexOfT(e);
        if (index >= 0) {
            return false;
        }
        mItems.add(~index, e);
        return true;
    }

    @Override
    public boolean remove(@NonNull Object o) {
        int index = indexOfO(o);
        if (index >= 0) {
            mItems.remove(index);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        mItems.clear();
    }

    @SuppressWarnings("unchecked")
    private int indexOfO(@NonNull Object o) {
        return indexOfT((T) o);
    }

    private int indexOfT(@NonNull T e) {
        return indexOfU(mKeyRetriever.getKey(e));
    }

    private int indexOfU(@NonNull U key) {
        int lo = 0;
        int hi = mItems.size() - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = mKeyComparator.compare(mKeyRetriever.getKey(mItems.get(mid)), key);

            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo;
    }
}
