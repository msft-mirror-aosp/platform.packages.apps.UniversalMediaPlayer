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

import com.android.pump.util.Clog;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

@WorkerThread
public final class KnowledgeGraph {
    private static final String TAG = Clog.tag(KnowledgeGraph.class);

    private KnowledgeGraph() { }

    public static void search(@NonNull Query query) throws IOException {
        search(query, 1);
    }

    public static void search(@NonNull Query query, int maxResults) throws IOException {
        Clog.i(TAG, "search(" + query + ", " + maxResults + ")");
    }
}

/*
https://kgsearch.googleapis.com/v1/entities:search?key=AIzaSyCV2--pLOigY36buwE7bnmyPLj7-z8DOd0&indent=true&ids=/m/0524b41
https://kgsearch.googleapis.com/v1/entities:search?key=AIzaSyCV2--pLOigY36buwE7bnmyPLj7-z8DOd0&limit=1&indent=true&query=game+of+thrones&types=Movie&types=MovieSeries&types=TVSeries&types=TVEpisode
https://kgsearch.googleapis.com/v1/entities:search?key=AIzaSyCV2--pLOigY36buwE7bnmyPLj7-z8DOd0&limit=1&query=game+of+thrones&types=Movie&types=MovieSeries&types=TVSeries&types=TVEpisode&alt=json&pp=false
https://www.googleapis.com/kgraph/v1/search?key=AIzaSyDD7SzYetjesv1XXLvDHOrab2B_97FVUnI&limit=1&lang=en&output=(name)&query=/m/0524b41
https://www.googleapis.com/kgraph/v1/search?key=AIzaSyDD7SzYetjesv1XXLvDHOrab2B_97FVUnI&limit=1&lang=en&query=/m/0524b41&output=/common/topic/description+/common/topic/image+/type/object/name
https://www.googleapis.com/kgraph/v1kpanels/search?query=babar&indent=true&key=AIzaSyDD7SzYetjesv1XXLvDHOrab2B_97FVUnI

package com.google.tv.annotation.util;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.LruCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

public class Freebase extends LruCache<String, Freebase.FreebaseResult> {
    private static final boolean USE_KNOWLEDGE_GRAPH = true;
    private static final Pattern MID_REGEX = Pattern.compile("/m/[\\d\\w_]+");
    private static final int CACHE_SIZE = 256;
    private static final Freebase sFreebase = new Freebase();

    public interface FreebaseListener {
        void onDataLoaded(String mid, FreebaseResult result);
    }

    public static class FreebaseResult {
        protected FreebaseResult(String title, String description, String imageUri) {
            this.title = title;
            this.description = description;
            this.imageUri = imageUri;
        }

        public String title;
        public String description;
        public String imageUri;
    }

    private Freebase() {
        super(CACHE_SIZE);
    }

    public static void loadData(String mid, FreebaseListener listener) {
        sFreebase.internalLoadData(mid, listener);
    }

    private void internalLoadData(String mid, FreebaseListener listener) {
        FreebaseResult result = get(mid);
        if (result != null) {
            listener.onDataLoaded(mid, result);
        } else {
            try {
                new FreebaseLoader(mid, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {
            }
        }
    }

    private String getTopicUri(String mid) {
        Uri.Builder ub = new Uri.Builder();
        ub.scheme("https");
        ub.authority("www.googleapis.com");
        if (USE_KNOWLEDGE_GRAPH) {
            ub.appendPath("kgraph");
        } else {
            ub.appendPath("freebase");
        }
        ub.appendPath("v1");
        ub.appendPath("topic");
        ub.appendPath("m");
        ub.appendPath(getMidId(mid));
        ub.appendQueryParameter("filter", "/common/topic/description");
        ub.appendQueryParameter("filter", "/common/topic/image");
        ub.appendQueryParameter("filter", "/type/object/name");
        ub.appendQueryParameter("limit", "1");
        ub.appendQueryParameter("lang", Locale.getDefault().getLanguage());
        if (USE_KNOWLEDGE_GRAPH) {
            ub.appendQueryParameter("key", "AIzaSyDD7SzYetjesv1XXLvDHOrab2B_97FVUnI");
        } else {
            ub.appendQueryParameter("key", "AIzaSyCfYZxsM9VR99tFLIGyrxpMhJvyrqdCFnw");
        }
        return ub.build().toString();
    }

    private String getImageUri(String mid) {
        Uri.Builder ub = new Uri.Builder();
        ub.scheme("https");
        ub.authority("usercontent.googleapis.com");
        ub.appendPath("freebase");
        ub.appendPath("v1");
        ub.appendPath("image");
        ub.appendPath("m");
        ub.appendPath(getMidId(mid));
        return ub.build().toString();
    }

    private String getMidId(String mid) {
        if (!isValidMid(mid)) {
            throw new IllegalArgumentException("Invalid Freebase MID");
        }
        return mid.substring(3);
    }

    private boolean isValidMid(String mid) {
        return MID_REGEX.matcher(mid).matches();
    }

    class FreebaseLoader extends AsyncTask<Void, Void, FreebaseResult> {
        private String mMid;
        private FreebaseListener mListener;
        private String mTitle;
        private String mDescription;
        private String mImageMid;

        public FreebaseLoader(String mid, FreebaseListener listener) {
            mMid = mid;
            mListener = listener;
        }

        @Override
        protected FreebaseResult doInBackground(Void... params) {
            InputStream inputStream;
            try {
                inputStream = new URL(getTopicUri(mMid)).openStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                parseFreebaseResult(new JsonReader(bufferedReader));
                String imageUri = mImageMid == null ? null : getImageUri(mImageMid);
                return new FreebaseResult(mTitle, mDescription, imageUri);
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            } catch (IllegalArgumentException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(FreebaseResult result) {
            if (result != null) {
                put(mMid, result);
                mListener.onDataLoaded(mMid, result);
            }
        }

        void parseFreebaseResult(JsonReader jsonReader) throws IOException {
            String name;
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                name = jsonReader.nextName();
                if (name.equals("property")) {
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        name = jsonReader.nextName();
                        if (name.equals("/common/topic/description")) {
                            jsonReader.beginObject();
                            while (jsonReader.hasNext()) {
                                name = jsonReader.nextName();
                                if (name.equals("values")) {
                                    jsonReader.beginArray();
                                    while (jsonReader.hasNext()) {
                                        jsonReader.beginObject();
                                        while (jsonReader.hasNext()) {
                                            name = jsonReader.nextName();
                                            if (name.equals("text")) {
                                                mDescription = jsonReader.nextString();
                                            } else {
                                                jsonReader.skipValue();
                                            }
                                        }
                                        jsonReader.endObject();
                                    }
                                    jsonReader.endArray();
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        } else if (name.equals("/common/topic/image")) {
                            jsonReader.beginObject();
                            while (jsonReader.hasNext()) {
                                name = jsonReader.nextName();
                                if (name.equals("values")) {
                                    jsonReader.beginArray();
                                    while (jsonReader.hasNext()) {
                                        jsonReader.beginObject();
                                        while (jsonReader.hasNext()) {
                                            name = jsonReader.nextName();
                                            if (name.equals("id")) {
                                                mImageMid = jsonReader.nextString();
                                            } else {
                                                jsonReader.skipValue();
                                            }
                                        }
                                        jsonReader.endObject();
                                    }
                                    jsonReader.endArray();
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        } else if (name.equals("/type/object/name")) {
                            jsonReader.beginObject();
                            while (jsonReader.hasNext()) {
                                name = jsonReader.nextName();
                                if (name.equals("values")) {
                                    jsonReader.beginArray();
                                    while (jsonReader.hasNext()) {
                                        jsonReader.beginObject();
                                        while (jsonReader.hasNext()) {
                                            name = jsonReader.nextName();
                                            if (name.equals("value")) {
                                                mTitle = jsonReader.nextString();
                                            } else {
                                                jsonReader.skipValue();
                                            }
                                        }
                                        jsonReader.endObject();
                                    }
                                    jsonReader.endArray();
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
        }
    }
}
*/