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
import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.android.pump.db.DataProvider;
import com.android.pump.db.Episode;
import com.android.pump.db.Movie;
import com.android.pump.db.Series;
import com.android.pump.util.Clog;
import com.android.pump.util.Http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        Pair<String, String> metadata = getMetadata(movie.getTitle(), "Movie");
        if (metadata.first != null) {
            updated |= movie.setPosterUri(Uri.parse(metadata.first));
        }
        if (metadata.second != null) {
            updated |= movie.setDescription(metadata.second);
        }
        return updated;
    }

    @Override
    public boolean populateSeries(@NonNull Series series) throws IOException {
        boolean updated = false;
        Pair<String, String> metadata = getMetadata(series.getTitle(), "TVSeries");
        if (metadata.first != null) {
            updated |= series.setPosterUri(Uri.parse(metadata.first));
        }
        if (metadata.second != null) {
            updated |= series.setDescription(metadata.second);
        }
        return updated;
    }

    @Override
    public boolean populateEpisode(@NonNull Episode episode) throws IOException {
        boolean updated = false;
        Pair<String, String> metadata = getMetadata(episode.getTitle(), "TVEpisode");
        if (metadata.first != null) {
            updated |= episode.setPosterUri(Uri.parse(metadata.first));
        }
        if (metadata.second != null) {
            updated |= episode.setDescription(metadata.second);
        }
        return updated;
    }

    private Pair<String, String> getMetadata(String title, String type) throws IOException {
        String imageUrl = null;
        String description = null;
        try {
            JSONObject root = (JSONObject) getContent(getContentUri(title, type));
            JSONArray items = root.getJSONArray("itemListElement");
            JSONObject item = (JSONObject) items.get(0);
            JSONObject result = item.getJSONObject("result");
            JSONObject image = result.optJSONObject("image");
            if (image != null) {
                String url = image.getString("contentUrl");
                if (url != null) {
                    // TODO (b/125143807): Remove once HTTPS scheme urls are retrieved.
                    imageUrl = url.replaceFirst("^http://", "https://");
                }
            }
            JSONObject detailedDescription = result.optJSONObject("detailedDescription");
            if (detailedDescription != null) {
                description = detailedDescription.getString("articleBody");
            }
        } catch (JSONException e) {
            Clog.w(TAG, "Failed to parse search result", e);
            throw new IOException(e);
        }
        return new Pair<>(imageUrl, description);
    }

    private static @NonNull Uri getContentUri(@NonNull String title, @NonNull String type) {
        Uri.Builder ub = new Uri.Builder();
        ub.scheme("https");
        ub.authority("kgsearch.googleapis.com");
        ub.appendPath("v1");
        ub.appendEncodedPath("entities:search");
        ub.appendQueryParameter("key", ApiKeys.KG_API);
        ub.appendQueryParameter("limit", "1");
        ub.appendQueryParameter("query", title);
        ub.appendQueryParameter("types", type);
        return ub.build();
    }

    private static @NonNull Object getContent(@NonNull Uri uri) throws IOException, JSONException {
        return new JSONTokener(new String(Http.get(uri.toString()), StandardCharsets.UTF_8))
                .nextValue();
    }
}
