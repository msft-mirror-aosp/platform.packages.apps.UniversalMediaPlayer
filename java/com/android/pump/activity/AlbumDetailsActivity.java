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

package com.android.pump.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.android.pump.R;
import com.android.pump.db.Album;
import com.android.pump.db.MediaDb;
import com.android.pump.util.Globals;

@UiThread
public class AlbumDetailsActivity extends AppCompatActivity {
    private Album mAlbum;

    public static void start(@NonNull Context context, @NonNull Album album) {
        Intent intent = new Intent(context, AlbumDetailsActivity.class);
        // TODO(b/123704452) Pass URI instead
        intent.putExtra("id", album.getId()); // TODO Add constant key
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        handleIntent();
    }

    @Override
    protected void onNewIntent(@Nullable Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) {
            long id = extras.getLong("id");

            MediaDb mediaDb = Globals.getMediaDb(this);
            mAlbum = mediaDb.getAlbumById(id);
        } else {
            mAlbum = null;
        }
    }
}
