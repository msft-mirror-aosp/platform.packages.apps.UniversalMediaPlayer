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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AnyThread
public abstract class Task implements Runnable {
    private boolean mCancelled;

    public abstract void execute();

    public abstract void merge(@NonNull Task task);

    public abstract void finish();

    @Override
    public final void run() {
        if (!mCancelled) {
            execute();
        }
    }

    @Override
    public abstract boolean equals(@Nullable Object obj);

    @Override
    public abstract int hashCode();

    void cancel() {
        mCancelled = true;
    }
}
