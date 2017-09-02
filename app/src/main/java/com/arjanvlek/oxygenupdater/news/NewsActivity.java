package com.arjanvlek.oxygenupdater.news;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.google.android.gms.ads.InterstitialAd;

public class NewsActivity extends AppCompatActivity {

    public static final String INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID";
    public static final String INTENT_START_WITH_AD = "START_WITH_AD";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent() == null || getIntent().getExtras() == null) {
            finish();
            return;
        }

        setTitle(getString(R.string.app_name));
        setContentView(R.layout.loading);

        // Obtain the contents of the news item (to save data when loading the entire list of news items, only title + subtitle are returned there).
        ((ApplicationData) getApplication()).getServerConnector().getNewsItem(getApplication(), getIntent().getLongExtra(INTENT_NEWS_ITEM_ID, -1L), (newsItem -> {
            setContentView(R.layout.activity_news);

            Locale locale = Locale.getLocale();

            // Mark the item as read on the device.
            NewsDatabaseHelper helper = new NewsDatabaseHelper(getApplication());
            helper.markNewsItemAsRead(newsItem);
            helper.close();

            // Display the title of the article.
            TextView titleView = (TextView) findViewById(R.id.newsTitle);
            titleView.setText(newsItem.getTitle(locale));

            // Display the contents of the article.
            WebView contentView = (WebView) findViewById(R.id.newsContent);
            contentView.getSettings().setJavaScriptEnabled(true);

            String newsContents = newsItem.getText(locale);

            if (newsContents == null) {
                newsContents = getString(R.string.news_load_error);
            }

            if (newsContents.isEmpty()) {
                newsContents = getString(R.string.news_empty);
            }

            contentView.loadDataWithBaseURL("", newsContents, "text/html", "UTF-8", "");

            // Display the name of the author of the article
            TextView authorView = (TextView) findViewById(R.id.newsAuthor);
            if (newsItem.getAuthorName() != null && !newsItem.getAuthorName().isEmpty()) {
                authorView.setText(getString(R.string.news_author, newsItem.getAuthorName()));
            } else {
                authorView.setVisibility(View.GONE);
            }

            // Display the last update time of the article.
            TextView datePublishedView = (TextView) findViewById(R.id.newsDatePublished);

            if (newsItem.getDateLastEdited() == null && newsItem.getDatePublished() != null) {
                datePublishedView.setText(getString(R.string.news_date_published, Utils.formatDateTime(getApplication(), newsItem.getDatePublished())));
            } else if (newsItem.getDateLastEdited() != null) {
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
