package com.arjanvlek.oxygenupdater.news;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_NEWS_ITEM;

public class NewsFragment extends AbstractFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        refreshNews(view, null);

        SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.newsRefreshContainer);
        refreshLayout.setOnRefreshListener(() -> refreshNews(view, (__) -> refreshLayout.setRefreshing(false)));
    }


    private void refreshNews(View view, Consumer<Void> callback) {
        Long deviceId = getSettingsManager().getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);
        Long updateMethodId = getSettingsManager().getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);
        getServerConnector().getNews(getApplicationData(), deviceId, updateMethodId, (newsItems -> displayNewsItems(view, newsItems, callback)));
    }

    private static class NewsItemView {
        private CardView container;
        private ImageView image;
        private TextView title;
        private TextView subtitle;
    }

    private void displayNewsItems(View view, List<NewsItem> newsItems, Consumer<Void> callback) {
        ListView newsContainer = (ListView) view.findViewById(R.id.newsContainer);

        newsContainer.setAdapter(new ListAdapter() {

            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return newsItems.size();
            }

            @Override
            public Object getItem(int position) {
                return newsItems.get(position);
            }

            @Override
            public long getItemId(int position) {
                return newsItems.get(position).getId();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                NewsItemView newsItemView;

                // Shared logic for all news items to prevent unnecessary view creation.
                if (convertView == null) {

                    newsItemView = new NewsItemView();

                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    convertView = inflater.inflate(R.layout.news_item, parent, false);

                    newsItemView.container = (CardView) convertView.findViewById(R.id.newsItemContainer);
                    newsItemView.image = (ImageView) convertView.findViewById(R.id.newsItemImage);
                    newsItemView.title = (TextView) convertView.findViewById(R.id.newsItemTitle);
                    newsItemView.subtitle = (TextView) convertView.findViewById(R.id.newsItemSubTitle);

                    convertView.setTag(newsItemView);

                } else {
                    newsItemView = (NewsItemView) convertView.getTag();
                }

                // Logic to set the title, subtitle and image of each individual news item.
                Locale locale = Locale.getLocale();

                NewsItem newsItem = newsItems.get(position);
                newsItemView.title.setText(newsItem.getTitle(locale));
                newsItemView.subtitle.setText(newsItem.getSubtitle(locale));
                newsItemView.container.setOnClickListener(v -> openNewsItem(view, getApplicationData(), newsItem));

                if(newsItem.isRead()) {
                    newsItemView.title.setAlpha(0.5f);
                    newsItemView.subtitle.setAlpha(0.7f);
                }

                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    public Bitmap doInBackground(Void...params) {
                        Bitmap image;

                        try {
                            InputStream in = new URL(newsItem.getImageUrl()).openStream();
                            image = BitmapFactory.decodeStream(in);
                        } catch(Exception e) {
                            image = null;
                            Logger.logError("NewsFragment", "Error loading news image: ", e);
                        }

                        return image;
                    }

                    @Override
                    public void onPostExecute(Bitmap result) {
                        if(result == null) {
                            Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.mipmap.image_error, null);
                            newsItemView.image.setImageDrawable(errorImage);
                        } else {
                            newsItemView.image.setImageBitmap(result);
                        }
                    }
                }.execute();

                return convertView;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });

        if(callback != null) {
            callback.accept(null);
        }
    }

    private void openNewsItem(View view, Context context, NewsItem newsItem) {
        Intent intent = new Intent(getActivity(), NewsActivity.class);
        intent.putExtra(INTENT_NEWS_ITEM, newsItem);
        startActivity(intent);

        NewsDatabaseHelper helper = new NewsDatabaseHelper(context);
        helper.markNewsItemAsRead(newsItem);
        helper.close();

        SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.newsRefreshContainer);

        new Handler().postDelayed(() -> refreshNews(view, (__) -> refreshLayout.setRefreshing(false)), 2000);
    }
}
