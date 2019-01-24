package com.arjanvlek.oxygenupdater.news;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.google.android.gms.ads.InterstitialAd;

import java.util.HashMap;
import java.util.Map;

public class NewsActivity extends AppCompatActivity {

    public static final String INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID";
    public static final String INTENT_START_WITH_AD = "START_WITH_AD";

    @SuppressLint("SetJavaScriptEnabled")
    // JS is required to load videos and other dynamic content.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getExtras() == null) {
            finish();
            return;
        }

        setTitle(getString(R.string.app_name));
        setContentView(R.layout.loading);

        loadNewsItem();
    }

    private void loadNewsItem() {
        loadNewsItem(0);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadNewsItem(int retryCount) {
        ApplicationData applicationData = (ApplicationData) getApplication();
        // Obtain the contents of the news item (to save data when loading the entire list of news items, only title + subtitle are returned there).
        applicationData.getServerConnector().getNewsItem(getApplication(), getIntent().getLongExtra(INTENT_NEWS_ITEM_ID, -1L), (newsItem -> {

            if (retryCount == 0) {
                setContentView(R.layout.activity_news);
            }

            if (newsItem == null || !newsItem.isFullyLoaded()) {
                if (Utils.checkNetworkConnection(getApplication()) && retryCount < 5) {
                    loadNewsItem(retryCount + 1);
                } else {
                    String newsContents = getString(R.string.news_load_error);

                    WebView contentView = findViewById(R.id.newsContent);
                    contentView.loadDataWithBaseURL("", newsContents, "text/html", "UTF-8", "");

                    // Hide the title, author name and last updated views.
                    findViewById(R.id.newsTitle).setVisibility(View.GONE);
                    findViewById(R.id.newsDatePublished).setVisibility(View.GONE);
                    findViewById(R.id.newsAuthor).setVisibility(View.GONE);

                    Button retryButton = findViewById(R.id.newsRetryButton);
                    retryButton.setVisibility(View.VISIBLE);
                    retryButton.setOnClickListener((v) -> loadNewsItem(1));
                }
                return;
            }

            findViewById(R.id.newsRetryButton).setVisibility(View.GONE);

            Locale locale = Locale.getLocale();

            // Mark the item as read on the device.
            NewsDatabaseHelper helper = new NewsDatabaseHelper(getApplication());
            helper.markNewsItemAsRead(newsItem);
            helper.close();

            // Display the title of the article.
            TextView titleView = findViewById(R.id.newsTitle);
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(newsItem.getTitle(locale));

            // Display the contents of the article.
            WebView contentView = findViewById(R.id.newsContent);
            contentView.setVisibility(View.VISIBLE);
            contentView.getSettings().setJavaScriptEnabled(true);

            String newsContents = newsItem.getText(locale);

            if (newsContents.isEmpty()) {
                newsContents = getString(R.string.news_empty);
                contentView.loadDataWithBaseURL("", newsContents, "text/html", "UTF-8", "");
            } else {
                String newsLanguage = locale == Locale.NL ? "NL" : "EN";
                String newsContentUrl = BuildConfig.SERVER_BASE_URL + "news-content/" + newsItem.getId() + "/" + newsLanguage;
                contentView.getSettings().setUserAgentString(ApplicationData.APP_USER_AGENT);
                contentView.loadUrl(newsContentUrl);
            }

            // Display the name of the author of the article
            TextView authorView = findViewById(R.id.newsAuthor);
            if (newsItem.getAuthorName() != null && !newsItem.getAuthorName().isEmpty()) {
                authorView.setVisibility(View.VISIBLE);
                authorView.setText(getString(R.string.news_author, newsItem.getAuthorName()));
            } else {
                authorView.setVisibility(View.GONE);
            }

            // Display the last update time of the article.
            TextView datePublishedView = findViewById(R.id.newsDatePublished);

            if (newsItem.getDateLastEdited() == null && newsItem.getDatePublished() != null) {
                datePublishedView.setVisibility(View.VISIBLE);
                datePublishedView.setText(getString(R.string.news_date_published, Utils.formatDateTime(getApplication(), newsItem.getDatePublished())));
            } else if (newsItem.getDateLastEdited() != null) {
                datePublishedView.setVisibility(View.VISIBLE);
                datePublishedView.setText(getString(R.string.news_date_published, Utils.formatDateTime(getApplication(), newsItem.getDateLastEdited())));
            } else {
                datePublishedView.setVisibility(View.GONE);
                titleView.setVisibility(View.GONE);
            }

            // Mark the item as read on the server (to increase times read counter)
            if (getApplication() != null && getApplication() instanceof ApplicationData && Utils.checkNetworkConnection(getApplication())) {
                ((ApplicationData) getApplication()).getServerConnector().markNewsItemAsRead(newsItem.getId(), (result) -> {
                    if (result != null && !result.isSuccess()) {
                        Logger.logError("NewsActivity", "Error marking news item as read on the server:" + result.getErrorMessage());
                    }

                    // Delayed display of ad if coming from a notification. Otherwise ad is displayed when transitioning from NewsFragment.
                    if (getIntent().getBooleanExtra(INTENT_START_WITH_AD, false) && !new SettingsManager(getApplication()).getPreference(SettingsManager.PROPERTY_AD_FREE, false)) {
                        InterstitialAd interstitialAd = new InterstitialAd(getApplication());
                        interstitialAd.setAdUnitId(getString(R.string.news_ad_unit_id));
                        interstitialAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());

                        // The ad will be shown after 10 seconds.
                        new Handler().postDelayed(interstitialAd::show, 10000);
                    }
                });
            }
        }));
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
