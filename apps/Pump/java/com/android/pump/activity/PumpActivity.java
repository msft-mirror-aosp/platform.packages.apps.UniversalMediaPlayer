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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.pump.R;
import com.android.pump.fragment.AlbumFragment;
import com.android.pump.fragment.ArtistFragment;
import com.android.pump.fragment.AudioFragment;
import com.android.pump.fragment.GenreFragment;
import com.android.pump.fragment.HomeFragment;
import com.android.pump.fragment.MovieFragment;
import com.android.pump.fragment.OtherFragment;
import com.android.pump.fragment.PlaylistFragment;
import com.android.pump.fragment.SeriesFragment;
import com.android.pump.util.Globals;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

@UiThread
public class PumpActivity extends AppCompatActivity implements OnNavigationItemSelectedListener {
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 42;
    private static final String[] REQUIRED_PERMISSIONS = {
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final Page HOME_PAGES[] = {
        new Page(HomeFragment::newInstance, "Home")
    };
    private static final Page VIDEO_PAGES[] = {
        new Page(MovieFragment::newInstance, "Movies"),
        new Page(SeriesFragment::newInstance, "TV Shows"),
        new Page(OtherFragment::newInstance, "Personal"),
        new Page(HomeFragment::newInstance, "All videos")
    };
    private static final Page AUDIO_PAGES[] = {
        new Page(AudioFragment::newInstance, "All audios"),
        new Page(PlaylistFragment::newInstance, "Playlists"),
        new Page(AlbumFragment::newInstance, "Albums"),
        new Page(GenreFragment::newInstance, "Genres"),
        new Page(ArtistFragment::newInstance, "Artists")
    };
    private static final Page FAVORITE_PAGES[] = {
        new Page(HomeFragment::newInstance, "Videos"),
        new Page(HomeFragment::newInstance, "Audios")
    };

    private ActivityPagerAdapter mActivityPagerAdapter;

    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private BottomNavigationView mBottomNavigationView;

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

        if (!requestMissingPermissions()) {
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
        switch (item.getItemId()) {
            case R.id.menu_home:
                selectPages(item.getTitle(), HOME_PAGES);
                return true;
            case R.id.menu_video:
                selectPages(item.getTitle(), VIDEO_PAGES);
                return true;
            case R.id.menu_audio:
                selectPages(item.getTitle(), AUDIO_PAGES);
                return true;
            case R.id.menu_favorite:
                selectPages(item.getTitle(), FAVORITE_PAGES);
                return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
            @NonNull int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            boolean granted = true;
            if (grantResults.length == 0) {
                granted = false;
            } else {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                    }
                }
            }
            if (!granted) {
                finish();
            } else {
                initialize();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initialize() {
        Globals.getMediaDb(this).load();
    }

    private boolean requestMissingPermissions() {
        if (isMissingPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    REQUIRED_PERMISSIONS_REQUEST_CODE);
            return true;
        }
        return false;
    }

    private boolean isMissingPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void selectPages(@NonNull CharSequence title, @NonNull Page pages[]) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }

        mTabLayout.setVisibility(pages.length <= 1 ? View.GONE : View.VISIBLE);
        mTabLayout.setTabMode(pages.length <= 4 ? TabLayout.MODE_FIXED : TabLayout.MODE_SCROLLABLE);
        mActivityPagerAdapter.setPages(pages);
    }

    private static class ActivityPagerAdapter extends FragmentPagerAdapter {
        private Page mPages[];

        private ActivityPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        private void setPages(@NonNull Page pages[]) {
            mPages = pages;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mPages.length;
        }

        @Override
        public @NonNull Fragment getItem(int position) {
            return mPages[position].pageCreator.newInstance();
        }

        @Override
        public long getItemId(int position) {
            return mPages[position].id;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public @NonNull CharSequence getPageTitle(int position) {
            return mPages[position].title;
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

    @FunctionalInterface
    private interface PageCreator {
        @NonNull Fragment newInstance();
    }
}
