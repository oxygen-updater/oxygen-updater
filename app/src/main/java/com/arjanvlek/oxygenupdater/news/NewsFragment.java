package com.arjanvlek.oxygenupdater.news;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.server.NetworkException;
import com.arjanvlek.oxygenupdater.internal.server.RedirectingResourceStream;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.MainActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.joda.time.LocalDateTime;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_NEWS_ITEM_ID;

public class NewsFragment extends AbstractFragment {

	private static final SparseArray<Bitmap> imageCache = new SparseArray<>();
	private static final String TAG = "NewsFragment";
	private boolean hasBeenLoadedOnce = false;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_news, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		// Load the news after up to 3 seconds to allow the update info screen to load first
		// This way, the app feels a lot faster. Also, it doesn't affect users that much, as they will always see the update info screen first.
		new Handler().postDelayed(() -> refreshNews(view, null), getLoadDelayMilliseconds());

		SwipeRefreshLayout refreshLayout = view.findViewById(R.id.newsRefreshContainer);
		refreshLayout.setOnRefreshListener(() -> refreshNews(view, __ -> refreshLayout.setRefreshing(false)));
	}

	private void refreshNews(View view, Consumer<Void> callback) {
		// If the view was suspended during the 3-second delay, stop performing any further actions.
		if (!isAdded() || getActivity() == null) {
			return;
		}
		Long deviceId = getSettingsManager().getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);
		Long updateMethodId = getSettingsManager().getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);
		getServerConnector().getNews(getApplicationData(), deviceId, updateMethodId, (newsItems -> displayNewsItems(view, newsItems, callback)));
	}

	private void displayNewsItems(View view, List<NewsItem> newsItems, Consumer<Void> callback) {
		ListView newsContainer = view.findViewById(R.id.newsContainer);

		if (!isAdded()) {
			Logger.logError(TAG, new OxygenUpdaterException("isAdded() returned false (displayNewsItems)"));
			return;
		}

		if (getActivity() == null) {
			Logger.logError(TAG, new OxygenUpdaterException("getActivity() returned null (displayNewsItems)"));
			return;
		}

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
					if (inflater == null) {
						Logger.logError(TAG, new OxygenUpdaterException("layoutInflater not available (displayNewsItem)"));
						return new View(getContext());
					}

					convertView = inflater.inflate(R.layout.news_item, parent, false);

					newsItemView.container = convertView.findViewById(R.id.newsItemContainer);
					newsItemView.image = convertView.findViewById(R.id.newsItemImage);
					newsItemView.imagePlaceholder = convertView.findViewById(R.id.newsItemImagePlaceholder);
					newsItemView.title = convertView.findViewById(R.id.newsItemTitle);
					newsItemView.subtitle = convertView.findViewById(R.id.newsItemSubTitle);

					convertView.setTag(newsItemView);

				} else {
					newsItemView = (NewsItemView) convertView.getTag();
				}

				// Logic to set the title, subtitle and image of each individual news item.
				Locale locale = Locale.getLocale();

				NewsItem newsItem = newsItems.get(position);
				if (newsItem == null) {
					return convertView;
				}

				newsItemView.title.setText(newsItem.getTitle(locale));
				newsItemView.subtitle.setText(newsItem.getSubtitle(locale));
				newsItemView.container.setOnClickListener(v -> openNewsItem(view, getApplicationData(), newsItem.getId()));

				if (newsItem.isRead()) {
					newsItemView.title.setAlpha(0.5f);
					newsItemView.subtitle.setAlpha(0.7f);
				}

				// Obtain the thumbnail image from the server.
				new FunctionalAsyncTask<Void, Void, Bitmap>(() -> {
					newsItemView.image.setVisibility(View.INVISIBLE);
					newsItemView.imagePlaceholder.setVisibility(View.VISIBLE);
				}, __ -> {
					if (newsItem.getId() == null) {
						return null;
					}

					Bitmap image = imageCache.get(newsItem.getId().intValue());

					if (image != null) {
						return image;
					}

					image = doGetImage(newsItem.getImageUrl());
					imageCache.put(newsItem.getId().intValue(), image);

					return image;
				}, image -> {
					if (!isAdded() || getActivity() == null) {
						return;
					}

					// If a fragment is not attached, do not crash the entire application but return an empty view.
					try {
						getResources();
					} catch (Exception e) {
						return;
					}

					if (image == null) {
						Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.mipmap.image_error, null);
						newsItemView.image.setImageDrawable(errorImage);
					} else {
						newsItemView.image.setImageBitmap(image);
					}
					newsItemView.image.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
					newsItemView.image.setVisibility(View.VISIBLE);

					newsItemView.imagePlaceholder.setVisibility(View.INVISIBLE);
				}).execute();

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

		if (callback != null) {
			callback.accept(null);
		}
	}

	private void openNewsItem(View view, Context context, Long newsItemId) {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			if (activity.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
				try {
					InterstitialAd ad = activity.getNewsAd();
					ad.setAdListener(new AdListener() {
						@Override
						public void onAdClosed() {
							super.onAdClosed();
							doOpenNewsItem(view, newsItemId);
							ad.loadAd(getApplicationData().buildAdRequest());
						}
					});
					ad.show();

					// Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
					getSettingsManager().savePreference(SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN, LocalDateTime.now().toString());
				} catch (NullPointerException e) {
					// Ad is not loaded, because the user bought the ad-free upgrade. Nothing to do here...
				}
			} else {
				// If offline, too many ads are shown or the user has bought the ad-free upgrade, open the news item directly.
				doOpenNewsItem(view, newsItemId);
			}
		} else {
			// If not attached to main activity or coming from other activity, open the news item.
			doOpenNewsItem(view, newsItemId);
		}
	}

	private Bitmap doGetImage(String imageUrl) {
		return doGetImage(imageUrl, 0);
	}

	private Bitmap doGetImage(String imageUrl, int retryCount) {
		try {
			InputStream in = RedirectingResourceStream.getInputStream(imageUrl);
			return BitmapFactory.decodeStream(in);
		} catch (MalformedURLException e) {
			// No retry, because malformed url will never work.
			Logger.logError(TAG, new NetworkException(String.format("Error displaying news image: Invalid image URL <%s>", imageUrl)));
			return null;
		} catch (Exception e) {
			if (retryCount < 5) {
				return doGetImage(imageUrl, retryCount + 1);
			} else {
				if (ExceptionUtils.isNetworkError(e)) {
					Logger.logWarning(TAG, new NetworkException(String.format("Error obtaining news image from <%s>.", imageUrl)));
				} else {
					Logger.logError(TAG, String.format("Error obtaining news image from <%s>", imageUrl), e);
				}
				return null;
			}
		}
	}

	private void doOpenNewsItem(View view, Long newsItemId) {
		Intent intent = new Intent(getActivity(), NewsActivity.class);
		intent.putExtra(INTENT_NEWS_ITEM_ID, newsItemId);
		startActivity(intent);

		SwipeRefreshLayout refreshLayout = view.findViewById(R.id.newsRefreshContainer);

		new Handler().postDelayed(() -> refreshNews(view, (__) -> refreshLayout.setRefreshing(false)), 2000);
	}

	private int getLoadDelayMilliseconds() {
		if (!hasBeenLoadedOnce) {
			hasBeenLoadedOnce = true;
			return 3000;
		}

		return 10;
	}

	private static class NewsItemView {
		private ConstraintLayout container;
		private ImageView image;
		private ImageView imagePlaceholder;
		private TextView title;
		private TextView subtitle;
	}
}
