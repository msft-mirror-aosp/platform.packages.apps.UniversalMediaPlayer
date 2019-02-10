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

package com.android.pump.provider;

import android.net.Uri;

import com.android.pump.db.DataProvider;
import com.android.pump.db.Episode;
import com.android.pump.db.Movie;
import com.android.pump.db.Series;
import com.android.pump.util.Clog;
import com.android.pump.util.Http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@WorkerThread
public final class KnowledgeGraph implements DataProvider {
    private static final String TAG = Clog.tag(KnowledgeGraph.class);

    private static final DataProvider INSTANCE = new KnowledgeGraph();

    private KnowledgeGraph() { }

    @AnyThread
    public static @NonNull DataProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean populateMovie(@NonNull Movie movie) throws IOException {
        boolean updated = false;
        try {
            JSONObject root = (JSONObject) getContent(getContentUri(movie));
            JSONArray items = root.getJSONArray("itemListElement");
            JSONObject item = (JSONObject) items.get(0);
            JSONObject result = item.getJSONObject("result");
            JSONObject image = result.optJSONObject("image");
            if (image != null) {
                String imageUrl = image.getString("contentUrl");
                if (imageUrl != null) {
                    // TODO (b/125143807): Remove once HTTPS scheme urls are retrieved.
                    imageUrl = imageUrl.replaceFirst("^http://", "https://");
                    updated |= movie.setPosterUri(Uri.parse(imageUrl));
                }
            }
            JSONObject description = result.optJSONObject("detailedDescription");
            if (description != null) {
                String descriptionText = description.getString("articleBody");
                if (descriptionText != null) {
                    updated |= movie.setSynopsis(descriptionText);
                }
            }
        } catch (JSONException e) {
            Clog.w(TAG, "Failed to parse search result", e);
            throw new IOException(e);
        }
        return updated;
    }

    @Override
    public boolean populateSeries(@NonNull Series series) throws IOException {
        return false;
    }

    @Override
    public boolean populateEpisode(@NonNull Episode episode) throws IOException {
        return false;
    }

    private static @NonNull Uri getContentUri(@NonNull Movie movie) {
        // TODO: add logic to consider the year
        Uri.Builder ub = getContentUri(movie.getTitle());
        ub.appendQueryParameter("types", "Movie");
        return ub.build();
    }

    private static @NonNull Uri.Builder getContentUri(@NonNull String title) {
        Uri.Builder ub = new Uri.Builder();
        ub.scheme("https");
        ub.authority("kgsearch.googleapis.com");
        ub.appendPath("v1");
        ub.appendEncodedPath("entities:search");
        ub.appendQueryParameter("key", ApiKeys.KG_API);
        ub.appendQueryParameter("limit", "1");
        ub.appendQueryParameter("query", title);
        return ub;
    }

    private static @NonNull Object getContent(@NonNull Uri uri) throws IOException, JSONException {
        return new JSONTokener(new String(Http.get(uri.toString()), StandardCharsets.UTF_8))
                .nextValue();
    }
}
