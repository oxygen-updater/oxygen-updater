package com.arjanvlek.oxygenupdater.news;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;

public class NewsActivity extends AppCompatActivity {

    public static final String INTENT_NEWS_ITEM = "NEWS_ITEM";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale locale = Locale.getLocale();
        if(getIntent() == null || getIntent().getExtras() == null) {
            finish();
            return;
        }

        NewsItem newsItem = (NewsItem) getIntent().getExtras().getSerializable(INTENT_NEWS_ITEM);

        if(newsItem == null) {
            finish();
            return;
        }

        setTitle(getString(R.string.app_name));
        setContentView(R.layout.activity_news);

        TextView titleView = (TextView) findViewById(R.id.newsTitle);
        titleView.setText(newsItem.getTitle(locale));

        WebView contentView = (WebView) findViewById(R.id.newsContent);
        contentView.getSettings().setJavaScriptEnabled(true);
        contentView.loadDataWithBaseURL("", newsItem.getText(locale), "text/html", "UTF-8", "");


        TextView authorView = (TextView) findViewById(R.id.newsAuthor);
        authorView.setText(getString(R.string.news_author, newsItem.getAuthorName()));

        TextView datePublishedView = (TextView) findViewById(R.id.newsDatePublished);

        if(newsItem.getDateLastEdited() == null) {
            datePublishedView.setText(getString(R.string.news_date_published, Utils.formatDateTime(getApplication(), newsItem.getDatePublished())));
        } else {
            datePublishedView.setText(getString(R.string.news_date_published, Utils.formatDateTime(getApplication(), newsItem.getDateLastEdited())));
        }

        if(getApplication() != null && getApplication() instanceof ApplicationData) {
            ((ApplicationData)getApplication()).getServerConnector().markNewsItemAsRead(newsItem.getId(), (result) -> {
                if (result != null && !result.isSuccess()) {
                    Logger.logError("NewsActivity", "Error marking news item as read on the server:" + result.getErrorMessage());
                }
            });
        }
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
