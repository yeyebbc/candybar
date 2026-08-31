package candybar.lib.activities;

import static candybar.lib.helpers.DrawableHelper.getDrawableId;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.BackEventCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.danimahardhika.android.helpers.core.ColorHelper;
import com.danimahardhika.android.helpers.core.DrawableHelper;
import com.danimahardhika.android.helpers.core.FileHelper;
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper;
import com.danimahardhika.android.helpers.core.utils.LogUtil;
import com.danimahardhika.android.helpers.license.LicenseHelper;
import com.danimahardhika.android.helpers.permission.PermissionCode;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import candybar.lib.R;
import candybar.lib.applications.CandyBarApplication;
import candybar.lib.databases.Database;
import candybar.lib.fragments.AboutFragment;
import candybar.lib.fragments.ApplyFragment;
import candybar.lib.fragments.FAQsFragment;
import candybar.lib.fragments.HomeFragment;
import candybar.lib.fragments.IconsBaseFragment;
import candybar.lib.fragments.PresetsFragment;
import candybar.lib.fragments.RequestFragment;
import candybar.lib.fragments.SettingsFragment;
import candybar.lib.fragments.WallpapersFragment;
import candybar.lib.fragments.dialog.ChangelogFragment;
import candybar.lib.fragments.dialog.InAppBillingFragment;
import candybar.lib.fragments.dialog.IntentChooserFragment;
import candybar.lib.helpers.ConfigurationHelper;
import candybar.lib.helpers.IntentHelper;
import candybar.lib.helpers.JsonHelper;
import candybar.lib.helpers.LicenseCallbackHelper;
import candybar.lib.helpers.LocaleHelper;
import candybar.lib.helpers.NavigationViewHelper;
import candybar.lib.helpers.PresetsHelper;
import candybar.lib.helpers.RequestHelper;
import candybar.lib.helpers.ThemeHelper;
import candybar.lib.helpers.TypefaceHelper;
import candybar.lib.helpers.WallpaperHelper;
import candybar.lib.items.Home;
import candybar.lib.items.Icon;
import candybar.lib.items.InAppBilling;
import candybar.lib.items.Request;
import candybar.lib.items.Wallpaper;
import candybar.lib.preferences.Preferences;
import candybar.lib.services.CandyBarService;
import candybar.lib.tasks.IconRequestTask;
import candybar.lib.tasks.IconsLoaderTask;
import candybar.lib.tasks.WallpaperThumbPreloaderTask;
import candybar.lib.utils.CandyBarGlideModule;
import candybar.lib.utils.Extras;
import candybar.lib.utils.InAppBillingClient;
import candybar.lib.utils.listeners.InAppBillingListener;
import candybar.lib.utils.listeners.RequestListener;
import candybar.lib.utils.listeners.SearchListener;
import candybar.lib.utils.listeners.WallpapersListener;
import candybar.lib.utils.views.HeaderView;

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
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

public abstract class CandyBarMainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, RequestListener, InAppBillingListener,
        SearchListener, WallpapersListener {

    private TextView mToolbarTitle;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private BottomNavigationView mBottomNavigation;
    private FrameLayout mBottomNavigationContainer;

    private Extras.Tag mFragmentTag;
    private int mPosition, mLastPosition;
    private ActionBarDrawerToggle mDrawerToggle;
    private FragmentManager mFragManager;
    private LicenseHelper mLicenseHelper;

    private boolean mIsMenuVisible = true;
    private boolean mUseBottomNavigation;
    private OnBackPressedCallback mChildBackCallback;
    private View mChildPageScrim;
    // Note 1: Predictive-back state for Home child pages.
    // mChildBackCallback is disabled by default (see the constructor
    // argument "false") and only enabled while a child page is shown.
    // mChildPageScrim is the dim overlay laid between Home and the
    // child; it is created lazily when the child page is opened and
    // removed again when the page is left.
    private boolean prevIsDarkTheme;

    public static List<Request> sMissedApps;
    public static List<Icon> sSections;
    public static Home sHomeIcon;
    public static int sInstalledAppsCount;
    public static int sIconsCount;

    private ActivityConfiguration mConfig;

    private Handler mTimesVisitedHandler;
    private Runnable mTimesVisitedRunnable;

    private static final int NOTIFICATION_PERMISSION_CODE = 10;

    @NonNull
    public abstract ActivityConfiguration onInit();

    // Note 2: The FIRST back handler registered for this activity. The
    // dispatcher consults callbacks in the reverse order in which they
    // were added, so the LAST enabled callback wins; this one is the
    // fallback for everything that is not a Home child page. It is
    // disabled while a child page is shown so the child-specific
    // handler receives the gesture instead (see Note 3).
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // Note 4: Any other in-fragment back stack (e.g. the icon
            // search) is popped wholesale. This also fires after the
            // predictive back pop of a child page, but by then the
            // back stack is empty, so this branch is skipped.
            if (mFragManager.getBackStackEntryCount() > 0) {
                clearBackStack();
                return;
            }

            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers();
                return;
            }

            // Note 5: Non-Home pages are simply replaced by Home. This
            // covers top-level tabs (Apply, Icons, ...) and drawer
            // pages; the four Home child pages never reach this branch
            // because their callback is the one that consumes Back.
            if (mFragmentTag != Extras.Tag.HOME) {
                mPosition = mLastPosition = 0;
                setFragment(getFragment(mPosition));
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final boolean isMaterialYou = Preferences.get(this).isMaterialYou();
        final int nightMode = switch (Preferences.get(this).getTheme()) {
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
        AppCompatDelegate.setDefaultNightMode(nightMode);

        LocaleHelper.setLocale(this);
        super.onCreate(savedInstanceState);
        super.setTheme(isMaterialYou ? R.style.CandyBar_Theme_App_MaterialYou : R.style.CandyBar_Theme_App_DayNight);
        setContentView(R.layout.activity_main);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationView = findViewById(R.id.navigation_view);
        mBottomNavigationContainer = findViewById(R.id.bottom_navigation_container);
        mToolbar = findViewById(R.id.toolbar);
        mToolbarTitle = findViewById(R.id.toolbar_title);
        mUseBottomNavigation = getResources().getBoolean(R.bool.use_bottom_navigation);
        mToolbar.setPopupTheme(isMaterialYou
                ? R.style.CandyBar_Theme_App_MaterialYou
                : R.style.CandyBar_Theme_App_DayNight);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        mFragManager = getSupportFragmentManager();

        if (mUseBottomNavigation) {
            initBottomNavigation(isMaterialYou);
        } else {
            initNavigationView(mToolbar);
            initNavigationViewHeader();
        }
        // Note 3: The child-page back handler. A custom OnBackPressedCallback
        // is used instead of a Fragment back stack on purpose: a Fragment
        // stack would play the system animation only at the commit moment,
        // whereas AndroidX forwards the live drag progress to these
        // callbacks, letting us move the view 1:1 with the finger (the
        // "predictive back" experience). The four state methods are:
        // started -> progressed (many times) -> cancelled OR pressed.
        registerBackPressHandler();
        mChildBackCallback = new OnBackPressedCallback(false) {
            // Note 6: Starts disabled; updateNavigationChrome() enables it
            // exactly while a bottom-nav child page is shown. An enabled
            // callback that consumes Back prevents the system from playing
            // its own end-screen animation, so we must stay in charge here.
            @Override
            public void handleOnBackStarted(BackEventCompat event) {
                View view = getChildPageView();
                if (view != null) {
                    // Note 7: ViewCompat.setZ fixes the DRAWING order
                    // between the child page and the scrim. The scrim is a
                    // plain view added to the same FrameLayout; FragmentManager
                    // may place the fragment view either side of it. Z is by
                    // definition above any default-z sibling, so whatever the
                    // view-pair order is, the child always draws on top of
                    // the scrim, and only Home is dimmed.
                    ViewCompat.setZ(view, 1f);
                    view.setTranslationX(0f);
                }
                setChildPageScrimAlpha(1f);
            }

            @Override
            public void handleOnBackProgressed(BackEventCompat event) {
                // Note 8: Runs on EVERY animation frame while the finger
                // moves, so it must stay cheap: one map lookup plus two
                // property writes. BackEventCompat.getProgress() is the
                // normalized drag position in [0, 1]; the multiplier is
                // the child width, making it slide exactly as far as the
                // finger does. The scrim fades the OPPOSITE way: fully
                // dimmed at progress 0, fully clear at progress 1.
                View view = getChildPageView();
                if (view != null) {
                    view.setTranslationX(event.getProgress() * view.getWidth());
                }
                setChildPageScrimAlpha(1f - event.getProgress());
            }

            @Override
            public void handleOnBackCancelled() {
                // Note 9: The gesture was released before it committed, so
                // both the page and the scrim must SPRING BACK to their
                // resting state. Property animators are started instead of
                // setting values directly, so the restoration is smooth
                // rather than an instant jump.
                View view = getChildPageView();
                if (view != null) {
                    view.animate().translationX(0f).setDuration(200).start();
                }
                if (mChildPageScrim != null) {
                    mChildPageScrim.animate().alpha(0f).setDuration(200).start();
                }
            }

            // Note 10: Past the commit threshold. The callback performs the
            // navigation itself (remove child, restore chrome via
            // leaveChildPage()) instead of delegating to the dispatcher -
            // delegating would fall through to the system default and
            // finish the whole activity.
            @Override
            public void handleOnBackPressed() {
                leaveChildPage();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, mChildBackCallback);

        ViewCompat.setOnApplyWindowInsetsListener(mDrawerLayout, (v, insets) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
            params.topMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            findViewById(R.id.inset_padding).getLayoutParams().height = params.topMargin;
            if (mBottomNavigation != null) {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                mBottomNavigation.setPadding(
                        mBottomNavigation.getPaddingLeft(),
                        mBottomNavigation.getPaddingTop(),
                        mBottomNavigation.getPaddingRight(),
                        bottomInset);
            }
            return insets;
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.navigationBar));
        //getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        //mDrawerLayout.setStatusBarBackground(R.color.colorPrimaryDark);
        int visibilityFlags = 0;
        if (ColorHelper.isLightColor(ColorHelper.getAttributeColor(this, R.attr.cb_colorPrimaryDark)) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            visibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (ColorHelper.isLightColor(ColorHelper.getAttributeColor(this, R.attr.cb_navigationBar)) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            visibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(visibilityFlags);

        try {
            startService(new Intent(this, CandyBarService.class));
        } catch (IllegalStateException e) {
            LogUtil.e("Unable to start CandyBarService. App is probably running in background.");
        }

        //Todo: wait until google fix the issue, then enable wallpaper crop again on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Preferences.get(this).setCropWallpaper(false);
        }

        mConfig = onInit();
        InAppBillingClient.get(this).init();

        mPosition = mLastPosition = 0;
        if (savedInstanceState != null) {
            mPosition = mLastPosition = savedInstanceState.getInt(Extras.EXTRA_POSITION, 0);
            onSearchExpanded(false);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            int position = bundle.getInt(Extras.EXTRA_POSITION, -1);
            if (position >= 0 && position < 6) {
                mPosition = mLastPosition = position;
            }
        }

        IntentHelper.sAction = IntentHelper.getAction(getIntent());
        if (IntentHelper.sAction == IntentHelper.ACTION_DEFAULT) {
            setFragment(getFragment(mPosition));
        } else {
            setFragment(getActionFragment(IntentHelper.sAction));
        }

        checkWallpapers();
        new WallpaperThumbPreloaderTask(this).execute();
        new IconRequestTask(this).executeOnThreadPool();
        new IconsLoaderTask(this).execute();

        /*
        The below code does this
        #1. If new version - set `firstRun` to `true`
        #2. If `firstRun` equals `true`, run the following steps
            #X. License check
                - Enabled: Run check, when completed run #Y
                - Disabled: Run #Y
            #Y. Reset icon request limit, clear cache and show changelog
        */

        if (Preferences.get(this).isNewVersion()) {
            // Check licenses on new version
            Preferences.get(this).setFirstRun(true);
        }

        final Runnable askNotificationPermission = () -> {
            final Runnable showToast = () -> {
                Toast.makeText(this, getResources().getString(R.string.permission_notification_denied_1), Toast.LENGTH_LONG).show();
                Toast.makeText(this, getResources().getString(R.string.permission_notification_denied_2), Toast.LENGTH_LONG).show();
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && CandyBarApplication.getConfiguration().isNotificationEnabled()) {
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    CandyBarApplication.getConfiguration().getNotificationHandler().setMode(Preferences.get(this).isNotificationsEnabled());
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        int permissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);
                        if (permissionState != PackageManager.PERMISSION_GRANTED) {
                            if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                                showToast.run();
                            } else {
                                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                            }
                        }
                    } else {
                        showToast.run();
                    }
                }
            }
        };

        final Runnable onNewVersion = () -> {
            ChangelogFragment.showChangelog(mFragManager, askNotificationPermission);
            File cache = getCacheDir();
            FileHelper.clearDirectory(cache);
        };

        if (Preferences.get(this).isFirstRun()) {
            final Runnable checkLicenseIfEnabled = () -> {
                final Runnable onAllChecksCompleted = () -> {
                    Preferences.get(this).setFirstRun(false);
                    onNewVersion.run();
                };

                if (mConfig.isLicenseCheckerEnabled()) {
                    mLicenseHelper = new LicenseHelper(this);
                    mLicenseHelper.run(mConfig.getLicenseKey(), mConfig.getRandomString(),
                            new LicenseCallbackHelper(this, onAllChecksCompleted));
                } else {
                    onAllChecksCompleted.run();
                }
            };

            checkLicenseIfEnabled.run();

            return;
        }

        if (mConfig.isLicenseCheckerEnabled() && !Preferences.get(this).isLicensed()) {
            finish();
        }

        if (getResources().getBoolean(R.bool.enable_in_app_review)) {
            int timesVisited = Preferences.get(this).getTimesVisited();
            int afterVisits = getResources().getInteger(R.integer.in_app_review_after_visits);
            int nextReviewVisitIdx = Preferences.get(this).getNextReviewVisit();

            if (timesVisited == afterVisits || (timesVisited > afterVisits && timesVisited == nextReviewVisitIdx)) {
                ReviewManager manager = ReviewManagerFactory.create(this);
                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ReviewInfo reviewInfo = task.getResult();
                        manager.launchReviewFlow(this, reviewInfo);

                        Preferences.get(this).setNextReviewVisit(timesVisited + 3);
                        // We are scheduling next review to be on 3rd visit from the current visit
                    } else {
                        LogUtil.e(Log.getStackTraceString(task.getException()));
                    }
                });
            }

            mTimesVisitedHandler = new Handler(Looper.getMainLooper());
            mTimesVisitedRunnable = () -> Preferences.get(this).setTimesVisited(timesVisited + 1);
            mTimesVisitedHandler.postDelayed(mTimesVisitedRunnable, getResources().getInteger(R.integer.in_app_review_visit_time) * 1000L);
        }

        askNotificationPermission.run();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (prevIsDarkTheme != ThemeHelper.isDarkTheme(this)) {
            recreate();
            return;
        }
        LocaleHelper.setLocale(this);
        if (mIsMenuVisible && mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        LocaleHelper.setLocale(newBase);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        int action = IntentHelper.getAction(intent);
        if (action != IntentHelper.ACTION_DEFAULT)
            setFragment(getActionFragment(action));
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        RequestHelper.checkPiracyApp(this);
        IntentHelper.sAction = IntentHelper.getAction(getIntent());
        super.onResume();
        InAppBillingClient.get(this).checkForUnprocessedPurchases();
    }

    @Override
    protected void onDestroy() {
        InAppBillingClient.get(this).destroy();

        if (mLicenseHelper != null) {
            mLicenseHelper.destroy();
        }

        CandyBarMainActivity.sMissedApps = null;
        CandyBarMainActivity.sHomeIcon = null;
        stopService(new Intent(this, CandyBarService.class));
        Database.get(this.getApplicationContext()).closeDatabase();
        if (mTimesVisitedHandler != null) {
            mTimesVisitedHandler.removeCallbacks(mTimesVisitedRunnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Extras.EXTRA_POSITION, mPosition);
        Database.get(this.getApplicationContext()).closeDatabase();
        super.onSaveInstanceState(outState);
    }

    private void registerBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionCode.STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
                return;
            }
            Toast.makeText(this, R.string.permission_storage_denied, Toast.LENGTH_LONG).show();
        }
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CandyBarApplication.getConfiguration().getNotificationHandler().setMode(Preferences.get(this).isNotificationsEnabled());
            } else {
                Toast.makeText(this, getResources().getString(R.string.permission_notification_denied_1), Toast.LENGTH_LONG).show();
                Toast.makeText(this, getResources().getString(R.string.permission_notification_denied_2), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPiracyAppChecked(boolean isPiracyAppInstalled) {
        boolean visible = getResources().getBoolean(
                R.bool.enable_icon_request) || !isPiracyAppInstalled;
        setNavigationItemVisible(R.id.navigation_view_request, visible);
    }

    @Override
    public void onRequestSelected(int count) {
        if (mFragmentTag == Extras.Tag.REQUEST) {
            String title = getResources().getString(R.string.navigation_view_request);
            if (count > 0) title += " (" + count + ")";
            mToolbarTitle.setText(title);
        }
    }

    @Override
    public void onBuyPremiumRequest() {
        if (Preferences.get(this).isPremiumRequest()) {
            RequestHelper.showPremiumRequestStillAvailable(this);
            return;
        }

        if (this.getResources().getBoolean(R.bool.enable_restore_purchases)) {
            CountDownLatch doneSignal = new CountDownLatch(1);
            AtomicBoolean doesProductIdExist = new AtomicBoolean(false);
            InAppBillingClient.get(this.getApplicationContext()).getClient()
                    .queryPurchasesAsync(InAppBillingClient.INAPP_PARAMS, (billingResult, purchases) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : purchases) {
                                for (String premiumRequestProductId : mConfig.getPremiumRequestProductsId()) {
                                    if (purchase.getProducts().contains(premiumRequestProductId)) {
                                        doesProductIdExist.set(true);
                                        break;
                                    }
                                }
                            }
                        } else {
                            LogUtil.e("Failed to query purchases. Response Code: " + billingResult.getResponseCode());
                        }

                        doneSignal.countDown();
                    });

            try {
                doneSignal.await();
            } catch (InterruptedException e) {
                LogUtil.e(Log.getStackTraceString(e));
            }

            if (doesProductIdExist.get()) {
                RequestHelper.showPremiumRequestExist(this);
                return;
            }
        }

        InAppBillingFragment.showInAppBillingDialog(getSupportFragmentManager(),
                InAppBilling.PREMIUM_REQUEST,
                mConfig.getLicenseKey(),
                mConfig.getPremiumRequestProductsId(),
                mConfig.getPremiumRequestProductsCount());
    }

    @Override
    public void onRequestBuilt(Intent intent, int type) {
        if (type == IntentChooserFragment.ICON_REQUEST) {
            if (RequestFragment.sSelectedRequests == null)
                return;

            if (Preferences.get(this).isPremiumRequest()) {
                int count = Preferences.get(this).getPremiumRequestCount() - RequestFragment.sSelectedRequests.size();
                Preferences.get(this).setPremiumRequestCount(count);
                if (count == 0) {
                    AtomicReference<List<Purchase>> purchases = new AtomicReference<>();
                    CountDownLatch queryDoneSignal = new CountDownLatch(1);

                    InAppBillingClient.get(this).getClient()
                            .queryPurchasesAsync(InAppBillingClient.INAPP_PARAMS, (billingResult, aPurchases) -> {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    purchases.set(aPurchases);
                                } else {
                                    LogUtil.e("Failed to load purchase data. Response Code: " + billingResult.getResponseCode());
                                }
                                queryDoneSignal.countDown();
                            });

                    try {
                        queryDoneSignal.await();
                    } catch (InterruptedException e) {
                        LogUtil.e(Log.getStackTraceString(e));
                    }

                    AtomicBoolean isConsumeSuccess = new AtomicBoolean(false);
                    if (purchases.get() != null) {
                        String premiumRequestProductId = Preferences.get(this).getPremiumRequestProductId();
                        for (Purchase purchase : purchases.get()) {
                            if (purchase.getProducts().contains(premiumRequestProductId)) {
                                CountDownLatch consumeDoneSignal = new CountDownLatch(1);
                                InAppBillingClient.get(this).getClient().consumeAsync(
                                        ConsumeParams.newBuilder()
                                                .setPurchaseToken(purchase.getPurchaseToken())
                                                .build(),
                                        (billingResult, s) -> {
                                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                isConsumeSuccess.set(true);
                                            } else {
                                                LogUtil.e("Failed to consume premium request product. Response Code: " + billingResult.getResponseCode());
                                            }
                                            consumeDoneSignal.countDown();
                                        }
                                );
                                try {
                                    consumeDoneSignal.await();
                                } catch (InterruptedException e) {
                                    LogUtil.e(Log.getStackTraceString(e));
                                }
                                break;
                            }
                        }
                    }

                    if (isConsumeSuccess.get()) {
                        Preferences.get(this).setPremiumRequest(false);
                        Preferences.get(this).setPremiumRequestProductId("");
                    } else {
                        RequestHelper.showPremiumRequestConsumeFailed(this);
                        return;
                    }
                }
            } else {
                if (getResources().getBoolean(R.bool.enable_icon_request_limit)) {
                    int used = Preferences.get(this).getRegularRequestUsed();
                    Preferences.get(this).setRegularRequestUsed((used + RequestFragment.sSelectedRequests.size()));
                }
            }

            if (mFragmentTag == Extras.Tag.REQUEST) {
                RequestFragment fragment = (RequestFragment) mFragManager.findFragmentByTag(Extras.Tag.REQUEST.value);
                if (fragment != null) fragment.refreshIconRequest();
            }
        }

        if (intent != null) {
            try {
                startActivity(intent);
            } catch (IllegalArgumentException e) {
                startActivity(Intent.createChooser(intent,
                        getResources().getString(R.string.app_client)));
            }
        }

        CandyBarApplication.sRequestProperty = null;
        CandyBarApplication.sZipPath = null;
    }

    @Override
    public void onRestorePurchases() {
        InAppBillingClient.get(this).getClient()
                .queryPurchasesAsync(InAppBillingClient.INAPP_PARAMS, (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        List<String> productIds = new ArrayList<>();
                        for (Purchase purchase : purchases) {
                            productIds.add(purchase.getProducts().get(0));
                        }
                        this.runOnUiThread(() -> {
                            SettingsFragment fragment = (SettingsFragment) mFragManager.findFragmentByTag(Extras.Tag.SETTINGS.value);
                            if (fragment != null) fragment.restorePurchases(productIds,
                                    mConfig.getPremiumRequestProductsId(), mConfig.getPremiumRequestProductsCount());
                        });
                    } else {
                        LogUtil.e("Failed to load purchase data. Response Code: " + billingResult.getResponseCode());
                    }
                });
    }

    @Override
    public void onProcessPurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            return;
        }

        if (Preferences.get(this).getInAppBillingType() == InAppBilling.DONATE) {
            InAppBillingClient.get(this).getClient().consumeAsync(
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build(),
                    (billingResult, s) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            Preferences.get(this).setInAppBillingType(-1);
                            runOnUiThread(() -> new MaterialDialog.Builder(this)
                                    .typeface(TypefaceHelper.getMedium(this), TypefaceHelper.getRegular(this))
                                    .title(R.string.navigation_view_donate)
                                    .content(R.string.donation_success)
                                    .positiveText(R.string.close)
                                    .show());
                        } else {
                            LogUtil.e("Failed to consume donation product. Response Code: " + billingResult.getResponseCode());
                        }
                    }
            );
        } else if (Preferences.get(this).getInAppBillingType() == InAppBilling.PREMIUM_REQUEST) {
            if (!purchase.isAcknowledged()) {
                InAppBillingClient.get(this).getClient().acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build(),
                        (billingResult) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Preferences.get(this).setPremiumRequest(true);
                                Preferences.get(this).setPremiumRequestProductId(purchase.getProducts().get(0));
                                Preferences.get(this).setInAppBillingType(-1);

                                // Delete old premium purchase history
                                Database.get(this).deletePremiumRequests();

                                this.runOnUiThread(() -> {
                                    if (mFragmentTag == Extras.Tag.REQUEST) {
                                        RequestFragment fragment = (RequestFragment) mFragManager.findFragmentByTag(Extras.Tag.REQUEST.value);
                                        if (fragment != null) fragment.refreshIconRequest();
                                    }
                                });
                            } else {
                                LogUtil.e("Failed to acknowledge purchase. Response Code: " + billingResult.getResponseCode());
                            }
                        }
                );
            }
        }
    }

    @Override
    public void onInAppBillingSelected(int type, InAppBilling product) {
        Preferences.get(this).setInAppBillingType(type);
        if (type == InAppBilling.PREMIUM_REQUEST) {
            Preferences.get(this).setPremiumRequestCount(product.getProductCount());
            Preferences.get(this).setPremiumRequestTotal(product.getProductCount());
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product.getProductDetails())
                .build());

        InAppBillingClient.get(this).getClient().launchBillingFlow(this,
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build());
    }

    @Override
    public void onInAppBillingRequest() {
        if (mFragmentTag == Extras.Tag.REQUEST) {
            RequestFragment fragment = (RequestFragment) mFragManager.findFragmentByTag(Extras.Tag.REQUEST.value);
            if (fragment != null) fragment.prepareRequest();
        }
    }

    @Override
    public void onWallpapersChecked(int wallpaperCount) {
        Preferences.get(this).setAvailableWallpapersCount(wallpaperCount);

        if (mFragmentTag == Extras.Tag.HOME) {
            HomeFragment fragment = (HomeFragment) mFragManager.findFragmentByTag(Extras.Tag.HOME.value);
            if (fragment != null) fragment.resetWallpapersCount();
        }
    }

    @Override
    public void onSearchExpanded(boolean expand) {
        mIsMenuVisible = !expand;

        if (!expand) {
            SoftKeyboardHelper.closeKeyboard(this);
            ColorHelper.setStatusBarColor(this, Color.TRANSPARENT, true);
        }

        updateNavigationChrome();
        supportInvalidateOptionsMenu();
    }

    public void showSupportDevelopmentDialog() {
        InAppBillingFragment.showInAppBillingDialog(mFragManager,
                InAppBilling.DONATE,
                mConfig.getLicenseKey(),
                mConfig.getDonationProductsId(),
                null);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mUseBottomNavigation && mFragmentTag == Extras.Tag.HOME && mIsMenuVisible) {
            getMenuInflater().inflate(R.menu.menu_home_navigation_actions, menu);
            menu.findItem(R.id.menu_home_presets).setVisible(
                    PresetsHelper.getPresetsCount(this) > 0);
            int iconColor = ColorHelper.getAttributeColor(this, R.attr.cb_toolbarIcon);
            for (int i = 0; i < menu.size(); i++) {
                Drawable icon = menu.getItem(i).getIcon();
                if (icon != null) icon.setTint(iconColor);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int position;
        int id = item.getItemId();
        if (id == R.id.menu_home_presets) {
            position = Extras.Tag.PRESETS.idx;
        } else if (id == R.id.menu_home_settings) {
            position = Extras.Tag.SETTINGS.idx;
        } else if (id == R.id.menu_home_faqs) {
            position = Extras.Tag.FAQS.idx;
        } else if (id == R.id.menu_home_about) {
            position = Extras.Tag.ABOUT.idx;
        } else {
            return super.onOptionsItemSelected(item);
        }
        selectPosition(position);
        return true;
    }

    private void initBottomNavigation(boolean isMaterialYou) {
        mNavigationView.setVisibility(View.GONE);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mToolbar.setNavigationIcon(null);
        mToolbar.setNavigationOnClickListener(null);

        boolean useMd3Controls = isMaterialYou
                || getResources().getBoolean(R.bool.use_md3_controls);
        int layout = useMd3Controls
                ? R.layout.bottom_navigation_md3
                : R.layout.bottom_navigation_md2;
        mBottomNavigation = (BottomNavigationView) getLayoutInflater().inflate(
                layout, mBottomNavigationContainer, false);
        mBottomNavigationContainer.addView(mBottomNavigation);
        mBottomNavigationContainer.setVisibility(View.VISIBLE);
        mBottomNavigation.setLabelVisibilityMode(
                getResources().getBoolean(R.bool.show_bottom_navigation_labels)
                        ? NavigationBarView.LABEL_VISIBILITY_LABELED
                        : NavigationBarView.LABEL_VISIBILITY_UNLABELED);

        initNavigationItems();
        int[] itemIds = {
                R.id.navigation_view_home,
                R.id.navigation_view_apply,
                R.id.navigation_view_icons,
                R.id.navigation_view_request,
                R.id.navigation_view_wallpapers
        };
        for (int itemId : itemIds) {
            MenuItem drawerItem = mNavigationView.getMenu().findItem(itemId);
            mBottomNavigation.getMenu().findItem(itemId).setVisible(drawerItem.isVisible());
        }
        mBottomNavigation.setOnItemSelectedListener(item -> {
            int position = getPositionForNavigationItem(item.getItemId());
            return position >= 0 && selectPositionInternal(position);
        });
    }

    private void initNavigationItems() {
        if (WallpaperHelper.getWallpaperType(this) == WallpaperHelper.EXTERNAL_APP) {
            mNavigationView.getMenu().findItem(R.id.navigation_view_wallpapers)
                    .setTitle(R.string.navigation_view_wallpaper_app);
        }
        NavigationViewHelper.initApply(mNavigationView);
        NavigationViewHelper.initIconRequest(mNavigationView);
        NavigationViewHelper.initWallpapers(mNavigationView);
        NavigationViewHelper.initPresets(mNavigationView);
    }

    private void initNavigationView(Toolbar toolbar) {
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.txt_open, R.string.txt_close) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                SoftKeyboardHelper.closeKeyboard(CandyBarMainActivity.this);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                selectPosition(mPosition);
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(ConfigurationHelper.getNavigationIcon(this,
                CandyBarApplication.getConfiguration().getNavigationIcon()));
        toolbar.setNavigationOnClickListener(view ->
                mDrawerLayout.openDrawer(GravityCompat.START));

        if (CandyBarApplication.getConfiguration().getNavigationIcon() == CandyBarApplication.NavigationIcon.DEFAULT) {
            DrawerArrowDrawable drawerArrowDrawable = new DrawerArrowDrawable(this);
            drawerArrowDrawable.setColor(ColorHelper.getAttributeColor(this, R.attr.cb_toolbarIcon));
            drawerArrowDrawable.setSpinEnabled(true);
            mDrawerToggle.setDrawerArrowDrawable(drawerArrowDrawable);
            mDrawerToggle.setDrawerIndicatorEnabled(true);
        }

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        initNavigationItems();

        ColorStateList itemStateList = ContextCompat.getColorStateList(this,
                R.color.navigation_view_item_highlight);
        mNavigationView.setItemTextColor(itemStateList);
        mNavigationView.setItemIconTintList(itemStateList);
//        Drawable background = ContextCompat.getDrawable(this,
//                ThemeHelper.isDarkTheme(this) ?
//                        R.drawable.navigation_view_item_background_dark :
//                        R.drawable.navigation_view_item_background);
//        mNavigationView.setItemBackground(background);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int position = getPositionForNavigationItem(item.getItemId());
            if (position < 0) {
                return false;
            }
            mPosition = position;
            item.setChecked(true);
            mDrawerLayout.closeDrawers();
            return true;
        });
    }

    private void initNavigationViewHeader() {
        if (CandyBarApplication.getConfiguration().getNavigationViewHeader() == CandyBarApplication.NavigationViewHeader.NONE) {
            mNavigationView.removeHeaderView(mNavigationView.getHeaderView(0));
            return;
        }

        String imageUrl = getResources().getString(R.string.navigation_view_header);
        String titleText = getResources().getString(R.string.navigation_view_header_title);
        View header = mNavigationView.getHeaderView(0);
        HeaderView image = header.findViewById(R.id.header_image);
        LinearLayout container = header.findViewById(R.id.header_title_container);
        TextView title = header.findViewById(R.id.header_title);
        TextView version = header.findViewById(R.id.header_version);

        if (CandyBarApplication.getConfiguration().getNavigationViewHeader() == CandyBarApplication.NavigationViewHeader.MINI) {
            image.setRatio(16, 9);
        }

        if (titleText.isEmpty()) {
            container.setVisibility(View.GONE);
        } else {
            title.setText(titleText);
            try {
                String versionText = "v" + getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
                version.setText(versionText);
            } catch (Exception ignored) {
            }
        }

        if (ColorHelper.isValidColor(imageUrl)) {
            image.setBackgroundColor(Color.parseColor(imageUrl));
            return;
        }

        if (!URLUtil.isValidUrl(imageUrl)) {
            imageUrl = "drawable://" + getDrawableId(imageUrl);
        }

        final Context context = this;
        if (CandyBarGlideModule.isValidContextForGlide(context)) {
            Glide.with(context)
                    .load(imageUrl)
                    .override(720)
                    .optionalCenterInside()
                    .diskCacheStrategy(imageUrl.contains("drawable://")
                            ? DiskCacheStrategy.NONE
                            : DiskCacheStrategy.RESOURCE)
                    .into(image);
        }
    }

    private void checkWallpapers() {
        if (Preferences.get(this).isConnectedToNetwork()) {
            new Thread(() -> {
                try {
                    if (WallpaperHelper.getWallpaperType(this) != WallpaperHelper.CLOUD_WALLPAPERS)
                        return;

                    InputStream stream = WallpaperHelper.getJSONStream(this);

                    if (stream != null) {
                        List<?> list = JsonHelper.parseList(stream);
                        if (list == null) return;

                        List<Wallpaper> wallpapers = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Wallpaper wallpaper = JsonHelper.getWallpaper(list.get(i));
                            if (wallpaper != null) {
                                if (!wallpapers.contains(wallpaper)) {
                                    wallpapers.add(wallpaper);
                                } else {
                                    LogUtil.e("Duplicate wallpaper found: " + wallpaper.getURL());
                                }
                            }
                        }

                        this.runOnUiThread(() -> onWallpapersChecked(wallpapers.size()));
                    }
                } catch (IOException e) {
                    LogUtil.e(Log.getStackTraceString(e));
                }
            }).start();
        }

        int size = Preferences.get(this).getAvailableWallpapersCount();
        if (size > 0) {
            onWallpapersChecked(size);
        }
    }

    private void clearBackStack() {
        if (mFragManager.getBackStackEntryCount() > 0) {
            mFragManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            onSearchExpanded(false);
        }
    }

    public void selectPosition(int position) {
        selectPositionInternal(position);
    }

    private boolean selectPositionInternal(int position) {
        if (position == Extras.Tag.REQUEST.idx) {
            if (!getResources().getBoolean(R.bool.enable_icon_request) &&
                    getResources().getBoolean(R.bool.enable_premium_request)) {
                if (!Preferences.get(this).isPremiumRequestEnabled()) {
                    updateNavigationSelection();
                    return false;
                }

                if (!Preferences.get(this).isPremiumRequest()) {
                    mPosition = mLastPosition;
                    updateNavigationSelection();
                    onBuyPremiumRequest();
                    return false;
                }
            }
        }

        if (position == Extras.Tag.WALLPAPERS.idx &&
                WallpaperHelper.getWallpaperType(this) == WallpaperHelper.EXTERNAL_APP) {
            mPosition = mLastPosition;
            updateNavigationSelection();
            WallpaperHelper.launchExternalApp(CandyBarMainActivity.this);
            return false;
        }

        if (position != mLastPosition) {
            mLastPosition = mPosition = position;
            setFragment(getFragment(position));
        }
        return true;
    }

    // Note 11: The single navigation entry point. All destination switches
    // go through here, which is what keeps the rest of the chrome code
    // simple: pick a transaction strategy, commit it, then refresh every
    // dependent UI element.
    private void setFragment(Fragment fragment) {
        FragmentTransaction ft;
        if (isBottomNavChildPage()) {
            // Note 12: Two distinctly different paths for a child page:
            // (a) the fragment was RESTORED by FragmentManager after a
            // configuration change - re-show it with replace();
            // (b) a fresh open - ADD it on top of Home. It is important
            // that Home is NOT replace()d away here: the predictive back
            // preview reveals the previous screen, so Home must still be
            // alive in the view hierarchy under the child.
            if (mFragManager.findFragmentByTag(mFragmentTag.value) != null) {
                // State restore path: the child already exists, just re-show it.
                // The scrim was a plain view and was not restored; drop it.
                mChildPageScrim = null;
                ft = mFragManager.beginTransaction()
                        .replace(R.id.container, fragment, mFragmentTag.value);
            } else {
                // Add the child on top of Home and keep Home in the view
                // hierarchy behind it. The custom child-back callback slides
                // the child away with the finger, revealing Home underneath.
                // Invisible until the predictive back drag starts, so the child
                // page is not dimmed while it is simply shown.
                // 0x66 alpha = 40% black scrim over Home.
                mChildPageScrim = new View(this);
                mChildPageScrim.setBackgroundColor(0x66000000);
                mChildPageScrim.setAlpha(0f);
                ViewGroup container = findViewById(R.id.container);
                container.addView(mChildPageScrim, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                ft = mFragManager.beginTransaction()
                        .add(R.id.container, fragment, mFragmentTag.value)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            }
        } else {
            clearBackStack();
            ft = mFragManager.beginTransaction()
                    .replace(R.id.container, fragment, mFragmentTag.value);
        }
        // Note 13: commit() posts the transaction to the main thread (it
        // only succeeds while the activity is NOT saving state). Some
        // code paths reach here from onSaveInstanceState, e.g. a finish
        // happening mid-transition; commitAllowingStateLoss() tolerates
        // that, accepting that a fragment could theoretically be lost if
        // the process is killed right after - an acceptable edge case.
        try {
            ft.commit();
        } catch (Exception e) {
            ft.commitAllowingStateLoss();
        }

        updateNavigationSelection();
        MenuItem drawerItem = mNavigationView.getMenu().findItem(
                getNavigationItemForPosition(mPosition));
        if (drawerItem != null) {
            mToolbarTitle.setText(drawerItem.getTitle());
        }
        updateNavigationChrome();

        boolean isChildPage = isBottomNavChildPage();
        backPressedCallback.setEnabled(!isChildPage && mFragmentTag != Extras.Tag.HOME);
        supportInvalidateOptionsMenu();
    }

    // Note 14: Everything that reacts to "which page is shown" in a single
    // method: toolbar icon, bottom-bar visibility, drawer lock state and
    // the child-back callback. Keeping it centralized means a new page
    // type only needs a new branch here instead of scattered updates.
    private void updateNavigationChrome() {
        boolean isHomeChildPage = isHomeChildPage();
        boolean showBackButton = !mIsMenuVisible ||
                (mUseBottomNavigation && isHomeChildPage);

        mBottomNavigationContainer.setVisibility(
                mUseBottomNavigation && !isHomeChildPage ? View.VISIBLE : View.GONE);

        if (showBackButton) {
            int color = ColorHelper.getAttributeColor(this, R.attr.cb_toolbarIcon);
            mToolbar.setNavigationIcon(DrawableHelper.getTintedDrawable(
                    this, R.drawable.ic_toolbar_back, color));
            mToolbar.setNavigationOnClickListener(view ->
                    getOnBackPressedDispatcher().onBackPressed());
        } else if (mUseBottomNavigation) {
            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationOnClickListener(null);
        } else {
            if (CandyBarApplication.getConfiguration().getNavigationIcon() ==
                    CandyBarApplication.NavigationIcon.DEFAULT) {
                mDrawerToggle.setDrawerArrowDrawable(new DrawerArrowDrawable(this));
            } else {
                mToolbar.setNavigationIcon(ConfigurationHelper.getNavigationIcon(this,
                        CandyBarApplication.getConfiguration().getNavigationIcon()));
            }
            mToolbar.setNavigationOnClickListener(view ->
                    mDrawerLayout.openDrawer(GravityCompat.START));
        }

        // Note 15: The drawer is locked whenever either the search bar is
        // expanded OR bottom navigation is used (no drawer UI in that
        // mode). The drawer lock is a SINGLE source of truth: if it says
        // LOCKED, opening with the hamburger must also be blocked, which
        // is why the lock (not the button) is what prevents a child page
        // from exposing two parallel back paths at once.
        mDrawerLayout.setDrawerLockMode(
                !mIsMenuVisible || mUseBottomNavigation
                        ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                        : DrawerLayout.LOCK_MODE_UNLOCKED);

        if (mChildBackCallback != null) {
            mChildBackCallback.setEnabled(isBottomNavChildPage());
        }
    }

    // Note 16: Two predicate methods keep a single definition of "child
    // page". The enum-based one matches content, the navigation-mode one
    // adds the UI-mode condition. Using them instead of inlining the tag
    // comparisons means adding a new child destination happens in exactly
    // one place.
    private boolean isHomeChildPage() {
        return mFragmentTag == Extras.Tag.PRESETS ||
                mFragmentTag == Extras.Tag.SETTINGS ||
                mFragmentTag == Extras.Tag.FAQS ||
                mFragmentTag == Extras.Tag.ABOUT;
    }

    private boolean isBottomNavChildPage() {
        return mUseBottomNavigation && isHomeChildPage();
    }

    // Note 17: Runs when the predictive back gesture COMMITS (or the back
    // arrow is tapped): undo the child page and bring the chrome back to
    // the Home state. Fragment-based navigation is a transaction - find
    // the fragment by tag, remove it with the built-in FADE transition,
    // then restore every piece of state that setFragment() set while the
    // child page was entered.
    private void leaveChildPage() {
        Fragment child = mFragManager.findFragmentByTag(mFragmentTag.value);
        if (child != null) {
            // Note 18: TRANSIT_FRAGMENT_FADE is one of the platform-provided
            // transition constants - no custom animation XML needed when the
            // page has none of its own. During a predictive back the system
            // already drove the drag; this fade only handles the final swap.
            FragmentTransaction ft = mFragManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(child);
            try {
                ft.commit();
            } catch (Exception e) {
                ft.commitAllowingStateLoss();
            }
        }
        if (mChildPageScrim != null) {
            ViewGroup container = findViewById(R.id.container);
            container.removeView(mChildPageScrim);
            mChildPageScrim = null;
        }

        // Note 19: Home might NOT be behind the child - e.g. after a
        // configuration change the child was restored alone. In that case
        // the remove above leaves an empty container, so go through the
        // normal navigation entry point to rebuild Home. The position
        // MUST be set first: setFragment reads it to keep the bottom bar
        // and drawer selection in sync.
        if (mFragManager.findFragmentByTag(Extras.Tag.HOME.value) == null) {
            // Home was not kept behind (state restored without it): rebuild it.
            mPosition = mLastPosition = Extras.Tag.HOME.idx;
            setFragment(getFragment(Extras.Tag.HOME.idx));
            return;
        }

        // Note 20: Fast path: Home is still under the child. Restore the
        // activity state directly instead of re-entering setFragment, so
        // the already-revealed Home view is left untouched.
        mPosition = mLastPosition = Extras.Tag.HOME.idx;
        mFragmentTag = Extras.Tag.HOME;
        updateNavigationSelection();
        MenuItem drawerItem = mNavigationView.getMenu().findItem(
                getNavigationItemForPosition(mPosition));
        if (drawerItem != null) {
            mToolbarTitle.setText(drawerItem.getTitle());
        }
        updateNavigationChrome();
        supportInvalidateOptionsMenu();
    }

    // Note 21: Tag-based lookup keeps the gesture code independent of which
    // child page is open: the current "child tag" is the one stored by
    // getFragment() when the destination was selected. getView() may be
    // null briefly while the transaction commits, hence the null guard on
    // every use - the gesture then simply skips the frame.
    private View getChildPageView() {
        Fragment fragment = mFragManager.findFragmentByTag(mFragmentTag.value);
        return fragment != null ? fragment.getView() : null;
    }

    // Note 22: Central scrim alpha setter: the null guard makes the four
    // gesture callbacks safe even if the scrim was not created for this
    // page instance (e.g. when a restored page never went through the
    // fresh-open path).
    private void setChildPageScrimAlpha(float alpha) {
        if (mChildPageScrim != null) {
            mChildPageScrim.setAlpha(alpha);
        }
    }

    private void updateNavigationSelection() {
        int itemId = getNavigationItemForPosition(mPosition);
        MenuItem drawerItem = mNavigationView.getMenu().findItem(itemId);
        if (drawerItem != null) {
            drawerItem.setChecked(true);
        }
        if (mBottomNavigation != null) {
            MenuItem bottomItem = mBottomNavigation.getMenu().findItem(itemId);
            if (bottomItem != null && bottomItem.isVisible()) {
                bottomItem.setChecked(true);
            }
        }
    }

    private void setNavigationItemVisible(int itemId, boolean visible) {
        MenuItem drawerItem = mNavigationView.getMenu().findItem(itemId);
        if (drawerItem != null) {
            drawerItem.setVisible(visible);
        }
        if (mBottomNavigation != null) {
            MenuItem bottomItem = mBottomNavigation.getMenu().findItem(itemId);
            if (bottomItem != null) {
                bottomItem.setVisible(visible);
            }
        }
    }

    private int getPositionForNavigationItem(int itemId) {
        if (itemId == R.id.navigation_view_home) return Extras.Tag.HOME.idx;
        if (itemId == R.id.navigation_view_apply) return Extras.Tag.APPLY.idx;
        if (itemId == R.id.navigation_view_icons) return Extras.Tag.ICONS.idx;
        if (itemId == R.id.navigation_view_request) return Extras.Tag.REQUEST.idx;
        if (itemId == R.id.navigation_view_wallpapers) return Extras.Tag.WALLPAPERS.idx;
        if (itemId == R.id.navigation_view_presets) return Extras.Tag.PRESETS.idx;
        if (itemId == R.id.navigation_view_settings) return Extras.Tag.SETTINGS.idx;
        if (itemId == R.id.navigation_view_faqs) return Extras.Tag.FAQS.idx;
        if (itemId == R.id.navigation_view_about) return Extras.Tag.ABOUT.idx;
        return -1;
    }

    private int getNavigationItemForPosition(int position) {
        if (position == Extras.Tag.HOME.idx) return R.id.navigation_view_home;
        if (position == Extras.Tag.APPLY.idx) return R.id.navigation_view_apply;
        if (position == Extras.Tag.ICONS.idx) return R.id.navigation_view_icons;
        if (position == Extras.Tag.REQUEST.idx) return R.id.navigation_view_request;
        if (position == Extras.Tag.WALLPAPERS.idx) return R.id.navigation_view_wallpapers;
        if (position == Extras.Tag.PRESETS.idx) return R.id.navigation_view_presets;
        if (position == Extras.Tag.SETTINGS.idx) return R.id.navigation_view_settings;
        if (position == Extras.Tag.FAQS.idx) return R.id.navigation_view_faqs;
        if (position == Extras.Tag.ABOUT.idx) return R.id.navigation_view_about;
        return R.id.navigation_view_home;
    }

    private Fragment getFragment(int position) {
        if (position == Extras.Tag.HOME.idx) {
            mFragmentTag = Extras.Tag.HOME;
            return new HomeFragment();
        } else if (position == Extras.Tag.APPLY.idx) {
            mFragmentTag = Extras.Tag.APPLY;
            return new ApplyFragment();
        } else if (position == Extras.Tag.ICONS.idx) {
            mFragmentTag = Extras.Tag.ICONS;
            return new IconsBaseFragment();
        } else if (position == Extras.Tag.REQUEST.idx) {
            mFragmentTag = Extras.Tag.REQUEST;
            return new RequestFragment();
        } else if (position == Extras.Tag.WALLPAPERS.idx) {
            mFragmentTag = Extras.Tag.WALLPAPERS;
            return new WallpapersFragment();
        } else if (position == Extras.Tag.PRESETS.idx) {
            mFragmentTag = Extras.Tag.PRESETS;
            return new PresetsFragment();
        } else if (position == Extras.Tag.SETTINGS.idx) {
            mFragmentTag = Extras.Tag.SETTINGS;
            return new SettingsFragment();
        } else if (position == Extras.Tag.FAQS.idx) {
            mFragmentTag = Extras.Tag.FAQS;
            return new FAQsFragment();
        } else if (position == Extras.Tag.ABOUT.idx) {
            mFragmentTag = Extras.Tag.ABOUT;
            return new AboutFragment();
        }

        mFragmentTag = Extras.Tag.HOME;
        return new HomeFragment();
    }

    private Fragment getActionFragment(int action) {
        switch (action) {
            case IntentHelper.ICON_PICKER:
            case IntentHelper.IMAGE_PICKER:
                mPosition = mLastPosition = (mFragmentTag = Extras.Tag.ICONS).idx;
                return new IconsBaseFragment();
            case IntentHelper.WALLPAPER_PICKER:
                if (WallpaperHelper.getWallpaperType(this) == WallpaperHelper.CLOUD_WALLPAPERS) {
                    mPosition = mLastPosition = (mFragmentTag = Extras.Tag.WALLPAPERS).idx;
                    return new WallpapersFragment();
                }
            default:
                mPosition = mLastPosition = (mFragmentTag = Extras.Tag.HOME).idx;
                return new HomeFragment();
        }
    }

    public static class ActivityConfiguration {

        private boolean mIsLicenseCheckerEnabled;
        private byte[] mRandomString;
        private String mLicenseKey;
        private String[] mDonationProductsId;
        private String[] mPremiumRequestProductsId;
        private int[] mPremiumRequestProductsCount;

        public ActivityConfiguration setLicenseCheckerEnabled(boolean enabled) {
            mIsLicenseCheckerEnabled = enabled;
            return this;
        }

        public ActivityConfiguration setRandomString(@NonNull byte[] randomString) {
            mRandomString = randomString;
            return this;
        }

        public ActivityConfiguration setLicenseKey(@NonNull String licenseKey) {
            mLicenseKey = licenseKey;
            return this;
        }

        public ActivityConfiguration setDonationProductsId(@NonNull String[] productsId) {
            mDonationProductsId = productsId;
            return this;
        }

        public ActivityConfiguration setPremiumRequestProducts(@NonNull String[] ids, @NonNull int[] counts) {
            mPremiumRequestProductsId = ids;
            mPremiumRequestProductsCount = counts;
            return this;
        }

        public boolean isLicenseCheckerEnabled() {
            return mIsLicenseCheckerEnabled;
        }

        public byte[] getRandomString() {
            return mRandomString;
        }

        public String getLicenseKey() {
            return mLicenseKey;
        }

        public String[] getDonationProductsId() {
            return mDonationProductsId;
        }

        public String[] getPremiumRequestProductsId() {
            return mPremiumRequestProductsId;
        }

        public int[] getPremiumRequestProductsCount() {
            return mPremiumRequestProductsCount;
        }
    }
}
