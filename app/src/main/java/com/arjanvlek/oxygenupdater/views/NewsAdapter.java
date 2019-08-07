package com.arjanvlek.oxygenupdater.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.server.NetworkException;
import com.arjanvlek.oxygenupdater.internal.server.RedirectingResourceStream;
import com.arjanvlek.oxygenupdater.news.NewsActivity;
import com.arjanvlek.oxygenupdater.news.NewsItem;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.NewsAdapter.NewsViewHolder;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.joda.time.LocalDateTime;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;
import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_NEWS_ITEM_ID;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public class NewsAdapter extends Adapter<NewsViewHolder> {

	private static final String TAG = "NewsAdapter";
	private static final SparseArray<Bitmap> imageCache = new SparseArray<>();

	private final Context context;
	private final AppCompatActivity activity;
	private final SettingsManager settingsManager;

	private final List<NewsItem> newsItemList;

	public NewsAdapter(Context context, AppCompatActivity activity, List<NewsItem> newsItemList) {
		this.context = context;
		this.activity = activity;

		this.newsItemList = newsItemList;

		settingsManager = new SettingsManager(context);
	}

	@NonNull
	@Override
	public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(context);

		return new NewsViewHolder(inflater.inflate(R.layout.news_item, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
		// Logic to set the title, subtitle and image of each individual news item.
		Locale locale = Locale.getLocale();

		NewsItem newsItem = newsItemList.get(position);

		holder.title.setText(newsItem.getTitle(locale));
		holder.subtitle.setText(newsItem.getSubtitle(locale));
		holder.container.setOnClickListener(v -> openNewsItem(newsItem));

		if (newsItem.isRead()) {
			holder.title.setAlpha(0.5f);
			holder.subtitle.setAlpha(0.7f);
		}

		// Obtain the thumbnail image from the server.
		new FunctionalAsyncTask<Void, Void, Bitmap>(() -> {
			holder.image.setVisibility(View.INVISIBLE);
			holder.imagePlaceholder.setVisibility(View.VISIBLE);
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
			if (context == null || activity == null) {
				return;
			}

			// If a fragment is not attached, do not crash the entire application but return an empty view.
			try {
				context.getResources();
			} catch (Exception e) {
				return;
			}

			if (image == null) {
				Drawable errorImage = ResourcesCompat.getDrawable(context.getResources(), R.drawable.image, null);
				holder.image.setImageDrawable(errorImage);
			} else {
				holder.image.setImageBitmap(image);
			}
			holder.image.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in));
			holder.image.setVisibility(View.VISIBLE);

			holder.imagePlaceholder.setVisibility(View.INVISIBLE);
		}).execute();
	}

	@Override
	public int getItemCount() {
		return newsItemList.size();
	}

	private void openNewsItem(NewsItem newsItem) {
		if (activity instanceof MainActivity) {
			MainActivity mainActivity = (MainActivity) activity;
			if (mainActivity.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
				try {
					InterstitialAd ad = mainActivity.getNewsAd();
					ad.setAdListener(new AdListener() {
						@Override
						public void onAdClosed() {
							super.onAdClosed();
							doOpenNewsItem(newsItem);
							ad.loadAd(ApplicationData.buildAdRequest());
						}
					});
					ad.show();

					// Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
					settingsManager.savePreference(SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN, LocalDateTime.now().toString());
				} catch (NullPointerException e) {
					// Ad is not loaded, because the user bought the ad-free upgrade. Nothing to do here...
				}
			} else {
				// If offline, too many ads are shown or the user has bought the ad-free upgrade, open the news item directly.
				doOpenNewsItem(newsItem);
			}
		} else {
			// If not attached to main activity or coming from other activity, open the news item.
			doOpenNewsItem(newsItem);
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
			logError(TAG, new NetworkException(String.format("Error displaying news image: Invalid image URL <%s>", imageUrl)));
			return null;
		} catch (Exception e) {
			if (retryCount < 5) {
				return doGetImage(imageUrl, retryCount + 1);
			} else {
				if (ExceptionUtils.isNetworkError(e)) {
					logWarning(TAG, new NetworkException(String.format("Error obtaining news image from <%s>.", imageUrl)));
				} else {
					logError(TAG, String.format("Error obtaining news image from <%s>", imageUrl), e);
				}
				return null;
			}
		}
	}

	private void doOpenNewsItem(NewsItem newsItem) {
		Intent intent = new Intent(context, NewsActivity.class);
		intent.putExtra(INTENT_NEWS_ITEM_ID, newsItem.getId());
		context.startActivity(intent);

		new Handler().postDelayed(() -> newsItem.setRead(true), 2000);
	}

	class NewsViewHolder extends ViewHolder {
		private RelativeLayout container;
		private ImageView image;
		private ImageView imagePlaceholder;
		private TextView title;
		private TextView subtitle;

		NewsViewHolder(@NonNull View itemView) {
			super(itemView);

			container = itemView.findViewById(R.id.newsItemContainer);
			image = itemView.findViewById(R.id.newsItemImage);
			imagePlaceholder = itemView.findViewById(R.id.newsItemImagePlaceholder);
			title = itemView.findViewById(R.id.newsItemTitle);
			subtitle = itemView.findViewById(R.id.newsItemSubTitle);
		}
	}
}
