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

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.pump.R;
import com.android.pump.fragment.AlbumFragment;
import com.android.pump.fragment.ArtistFragment;
import com.android.pump.fragment.AudioFragment;
import com.android.pump.fragment.GenreFragment;
import com.android.pump.fragment.HomeFragment;
import com.android.pump.fragment.MovieFragment;
import com.android.pump.fragment.OtherFragment;
import com.android.pump.fragment.PermissionFragment;
import com.android.pump.fragment.PlaylistFragment;
import com.android.pump.fragment.SeriesFragment;
import com.android.pump.util.Globals;
import com.android.pump.util.Permissions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.tabs.TabLayout;

@UiThread
public class PumpActivity extends AppCompatActivity implements OnNavigationItemSelectedListener {
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 42;

    // TODO Remove ugly PERMISSION_PAGES hack
    private static final Pages PERMISSION_PAGES =
        new Pages(R.id.menu_home, new Page[] {
            new Page(PermissionFragment::newInstance, "Permission")
        });
    private static final Pages[] PAGES_LIST = {
        new Pages(R.id.menu_home, new Page[] {
            new Page(HomeFragment::newInstance, "Home")
        }),
        new Pages(R.id.menu_video, new Page[] {
            new Page(MovieFragment::newInstance, "Movies"),
            new Page(SeriesFragment::newInstance, "TV Shows"),
            new Page(OtherFragment::newInstance, "Personal"),
            new Page(HomeFragment::newInstance, "All videos")
        }),
        new Pages(R.id.menu_audio, new Page[] {
            new Page(AudioFragment::newInstance, "All audios"),
            new Page(PlaylistFragment::newInstance, "Playlists"),
            new Page(AlbumFragment::newInstance, "Albums"),
            new Page(GenreFragment::newInstance, "Genres"),
            new Page(ArtistFragment::newInstance, "Artists")
        }),
        new Pages(R.id.menu_favorite, new Page[] {
            new Page(HomeFragment::newInstance, "Videos"),
            new Page(HomeFragment::newInstance, "Audios")
        })
    };

    private ActivityPagerAdapter mActivityPagerAdapter;

    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private BottomNavigationView mBottomNavigationView;

    public static void requestPermissions(@NonNull Activity activity) {
        Permissions.requestMissingPermissions(activity, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // NOTE: If you are facing StrictMode violation by setContentView please disable instant run
        setContentView(R.layout.activity_pump);

        setSupportActionBar(findViewById(R.id.activity_pump_toolbar));

        mActivityPagerAdapter = new ActivityPagerAdapter(getSupportFragmentManager());

        mDrawerLayout = findViewById(R.id.activity_pump_drawer_layout);
        mViewPager = findViewById(R.id.activity_pump_view_pager);
        mTabLayout = findViewById(R.id.activity_pump_tab_layout);
        mBottomNavigationView = findViewById(R.id.activity_pump_bottom_navigation_view);

        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
        mBottomNavigationView.setSelectedItemId(R.id.menu_home);
        mViewPager.setAdapter(mActivityPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        if (!Permissions.isMissingPermissions(this)) {
            initialize();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pump, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // TODO Remove ugly hack
        if (item.getItemId() == R.id.menu_home && Permissions.isMissingPermissions(this)) {
            selectPages(item.getTitle(), PERMISSION_PAGES);
            return true;
        }

        for (Pages pages : PAGES_LIST) {
            if (pages.id == item.getItemId()) {
                selectPages(item.getTitle(), pages);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            if (Permissions.isGranted(permissions, grantResults)) {
                initialize();
                // TODO Remove ugly hack
                mBottomNavigationView.setSelectedItemId(R.id.menu_home);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initialize() {
        Globals.getMediaDb(this).load();
    }

    private void selectPages(@NonNull CharSequence title, @NonNull Pages pages) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }

        Pages current = mActivityPagerAdapter.getPages();
        if (current != null) {
            current.setCurrent(mViewPager.getCurrentItem());
        }

        mActivityPagerAdapter.setPages(pages);
        int count = mActivityPagerAdapter.getCount();
        mTabLayout.setVisibility(count <= 1 ? View.GONE : View.VISIBLE);
        mTabLayout.setTabMode(count <= 4 ? TabLayout.MODE_FIXED : TabLayout.MODE_SCROLLABLE);
        mViewPager.setCurrentItem(pages.getCurrent());
    }

    private static class ActivityPagerAdapter extends FragmentPagerAdapter {
        private Pages mPages;

        private ActivityPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        private void setPages(@NonNull Pages pages) {
            mPages = pages;
            notifyDataSetChanged();
        }

        private @Nullable Pages getPages() {
            return mPages;
        }

        @Override
        public int getCount() {
            return mPages.pages.length;
        }

        @Override
        public @NonNull Fragment getItem(int position) {
            return mPages.pages[position].pageCreator.newInstance();
        }

        @Override
        public long getItemId(int position) {
            return mPages.pages[position].id;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public @NonNull CharSequence getPageTitle(int position) {
            return mPages.pages[position].title;
        }
    }

    private static class Page {
        private static int sid;
        private Page(@NonNull PageCreator pageCreator, @NonNull String title) {
            this.id = sid++;
            this.pageCreator = pageCreator;
            this.title = title;
        }

        private final int id;
        private final PageCreator pageCreator;
        private final String title;
    }

    private static class Pages {
        private Pages(@IdRes int id, @NonNull Page[] pages) {
            this.id = id;
            this.pages = pages;
        }

        private final int id;
        private final Page[] pages;

        private int current;

        private void setCurrent(int current) {
            this.current = current;
        }

        private int getCurrent() {
            return current;
        }
    }

    @FunctionalInterface
    private interface PageCreator {
        @NonNull Fragment newInstance();
    }
}
