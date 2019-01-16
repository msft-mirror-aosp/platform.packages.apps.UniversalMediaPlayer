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

package com.android.pump.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.RecyclerView;

@UiThread
public class HeaderRecyclerView extends RecyclerView {
    public HeaderRecyclerView(@NonNull Context context) {
        super(context);
    }

    public HeaderRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void swapAdapter(@Nullable Adapter adapter, boolean removeAndRecycleExistingViews) {
        if (adapter != null && !(adapter instanceof HeaderRecyclerAdapter)) {
            adapter = new HeaderRecyclerAdapter<>(cast(adapter));
        }
        super.swapAdapter(adapter, removeAndRecycleExistingViews);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        if (adapter != null && !(adapter instanceof HeaderRecyclerAdapter)) {
            adapter = new HeaderRecyclerAdapter<>(cast(adapter));
        }
        super.setAdapter(adapter);
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            // TODO override GridLayoutManager.setSpanCount() & setSpanSizeLookup()
            SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();
            if (!(spanSizeLookup instanceof HeaderSpanSizeLookup)) {
                gridLayoutManager.setSpanSizeLookup(new HeaderSpanSizeLookup(spanSizeLookup,
                        gridLayoutManager.getSpanCount()));
            }
        }
        super.setLayoutManager(layoutManager);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(@Nullable Object obj) {
        return (T) obj;
    }

    private static class HeaderRecyclerAdapter<VH extends ViewHolder> extends Adapter<ViewHolder> {
        private final static int HEADER = Integer.MIN_VALUE;
        //private final static int FOOTER = Integer.MAX_VALUE; TODO add footer

        private final Adapter<VH> mDelegate;

        private HeaderRecyclerAdapter(@NonNull Adapter<VH> delegate) {
            mDelegate = delegate;
            setHasStableIds(mDelegate.hasStableIds());
            mDelegate.registerAdapterDataObserver(new AdapterDataObserver() {
                @Override
                public void onChanged() {
                    notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    notifyItemRangeChanged(toPosition(positionStart), itemCount);
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount,
                                               @Nullable Object payload) {
                    notifyItemRangeChanged(toPosition(positionStart), itemCount, payload);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    notifyItemRangeInserted(toPosition(positionStart), itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    notifyItemRangeRemoved(toPosition(positionStart), itemCount);
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    for (int i = 0; i < itemCount; ++i) {
                        notifyItemMoved(toPosition(fromPosition + i), toPosition(toPosition + i));
                    }
                }
            });
        }

        @Override
        public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == HEADER) {
                // TODO Handle this differently?
                FrameLayout frameLayout = new FrameLayout(parent.getContext());
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                frameLayout.setLayoutParams(params);

                return new HeaderViewHolder(frameLayout);
            } else {
                return mDelegate.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (isHeader(position)) {
                // TODO
            } else {
                mDelegate.onBindViewHolder(cast(holder), fromPosition(position));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                     @NonNull List<Object> payloads) {
            if (isHeader(position)) {
                onBindViewHolder(holder, position);
            } else {
                mDelegate.onBindViewHolder(cast(holder), fromPosition(position), payloads);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isHeader(position)) {
                return HEADER;
            }
            return mDelegate.getItemViewType(fromPosition(position));
        }

        @Override
        public long getItemId(int position) {
            if (isHeader(position)) {
                return Long.MIN_VALUE;
            }
            return mDelegate.getItemId(fromPosition(position));
        }

        @Override
        public int getItemCount() {
            return getCount(mDelegate.getItemCount());
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            if (!(holder instanceof HeaderViewHolder)) {
                mDelegate.onViewRecycled(cast(holder));
            }
        }

        @Override
        public boolean onFailedToRecycleView(@NonNull ViewHolder holder) {
            if (!(holder instanceof HeaderViewHolder)) {
                mDelegate.onFailedToRecycleView(cast(holder));
            }
            return false;
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            if (!(holder instanceof HeaderViewHolder)) {
                mDelegate.onViewAttachedToWindow(cast(holder));
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            if (!(holder instanceof HeaderViewHolder)) {
                mDelegate.onViewDetachedFromWindow(cast(holder));
            }
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            mDelegate.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            mDelegate.onDetachedFromRecyclerView(recyclerView);
        }
    }

    private static class HeaderViewHolder extends ViewHolder {
        private HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class HeaderSpanSizeLookup extends SpanSizeLookup {
        private final SpanSizeLookup mDelegate;
        private final int mSpanCount;

        private HeaderSpanSizeLookup(@NonNull SpanSizeLookup delegate, int spanCount) {
            mDelegate = delegate;
            mSpanCount = spanCount;
            setSpanIndexCacheEnabled(mDelegate.isSpanIndexCacheEnabled());
        }

        @Override
        public int getSpanSize(int position) {
            if (isHeader(position)) {
                return mSpanCount;
            }
            return mDelegate.getSpanSize(fromPosition(position));
        }

        @Override
        public int getSpanIndex(int position, int spanCount) {
            if (isHeader(position)) {
                return 0;
            }
            return mDelegate.getSpanIndex(fromPosition(position), spanCount);
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            if (isHeader(adapterPosition)) {
                return 0;
            }
            return mDelegate.getSpanIndex(fromPosition(adapterPosition), spanCount)
                    + (hasHeader() ? 1 : 0);
        }
    }

    private static boolean hasHeader() {
        return true;
    }

    private static boolean isHeader(int position) {
        return hasHeader() && position == 0;
    }

    private static int getCount(int count) {
        if (hasHeader()) {
            return count + 1;
        }
        return count;
    }

    private static int toPosition(int position) {
        if (hasHeader()) {
            return position + 1;
        }
        return position;
    }

    private static int fromPosition(int position) {
        if (hasHeader()) {
            return position - 1;
        }
        return position;
    }
}
