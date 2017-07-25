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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.MainActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.List;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_NEWS_ITEM;

public class NewsFragment extends AbstractFragment {

    private static final SparseArray<Bitmap> imageCache = new SparseArray<>();

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
        private ImageView imagePlaceholder;
        private TextView title;
        private TextView subtitle;
    }

    private void displayNewsItems(View view, List<NewsItem> newsItems, Consumer<Void> callback) {
        ListView newsContainer = (ListView) view.findViewById(R.id.newsContainer);

        if(!isAdded() || getActivity() == null) return;

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
                    newsItemView.imagePlaceholder = (ImageView) convertView.findViewById(R.id.newsItemImagePlaceholder);
                    newsItemView.title = (TextView) convertView.findViewById(R.id.newsItemTitle);
                    newsItemView.subtitle = (TextView) convertView.findViewById(R.id.newsItemSubTitle);

                    convertView.setTag(newsItemView);

                } else {
                    newsItemView = (NewsItemView) convertView.getTag();
                }

                // Logic to set the title, subtitle and image of each individual news item.
                Locale locale = Locale.getLocale();

                NewsItem newsItem = newsItems.get(position);
                if(newsItem == null) return convertView;

                newsItemView.title.setText(newsItem.getTitle(locale));
                newsItemView.subtitle.setText(newsItem.getSubtitle(locale));
                newsItemView.container.setOnClickListener(v -> openNewsItem(view, getApplicationData(), newsItem));

                if(newsItem.isRead()) {
                    newsItemView.title.setAlpha(0.5f);
                    newsItemView.subtitle.setAlpha(0.7f);
                }

                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    public void onPreExecute() {
                        newsItemView.image.setVisibility(View.INVISIBLE);
                        newsItemView.imagePlaceholder.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public Bitmap doInBackground(Void...params) {
                        if(newsItem.getId() == null) return null;

                        Bitmap image = imageCache.get(newsItem.getId().intValue());

                        if(image != null) return image;

                        try {
                            InputStream in = new URL(newsItem.getImageUrl()).openStream();
                            image = BitmapFactory.decodeStream(in);
                            imageCache.put(newsItem.getId().intValue(), image);
                        } catch(Exception e) {
                            image = null;
                            imageCache.put(newsItem.getId().intValue(), null);
                            Logger.logError("NewsFragment", "Error loading news image: ", e);
                        }

                        return image;
                    }

                    @Override
                    public void onPostExecute(Bitmap result) {
                        if(!isAdded() || getActivity() == null) return;

                        // If a fragment is not attached, do not crash the entire application but return an empty view.
                        try {
                            getResources();
                        } catch (Exception e) {
                            return;
                        }

                        if(result == null) {
                            Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.mipmap.image_error, null);
                            newsItemView.image.setImageDrawable(errorImage);
                        } else {
                            newsItemView.image.setImageBitmap(result);
                        }
                        newsItemView.image.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        newsItemView.image.setVisibility(View.VISIBLE);

                        newsItemView.imagePlaceholder.setVisibility(View.INVISIBLE);
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
                return newsItems.isEmpty();
            }
        });

        if(callback != null) {
            callback.accept(null);
        }
    }

    private void openNewsItem(View view, Context context, NewsItem newsItem) {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
                try {
                    InterstitialAd ad = activity.getNewsAd();
                    ad.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            doOpenNewsItem(view, context, newsItem);
                            ad.loadAd(getApplicationData().buildAdRequest());
                        }
                    });
                    ad.show();

                    // Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
                    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(new File(context.getFilesDir(), ApplicationData.NEWS_ADS_SHOWN_DATE_FILENAME)));
                    stream.writeObject(LocalDateTime.now());

                } catch (IOException e) {
                    Logger.logError("NewsFragment", "Failed to store last shown date for news ads: ", e);
                    // Ad is already shown and can be closed. Nothing to do anymore...
                }
            } else {
                // If offline or when too many ads are shown, open the news item.
                doOpenNewsItem(view, context, newsItem);
            }
        } else {
            // If not attached to main activity or coming from other activity, open the news item.
            doOpenNewsItem(view, context, newsItem);
        }
    }

    private void doOpenNewsItem(View view, Context context, NewsItem newsItem) {
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
