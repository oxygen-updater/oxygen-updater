package com.arjanvlek.oxygenupdater.news;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.AlphaInAnimationAdapter;
import com.arjanvlek.oxygenupdater.views.MainActivity;
import com.arjanvlek.oxygenupdater.views.NewsAdapter;

import java.util.List;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;

public class NewsFragment extends AbstractFragment {

	private static final String TAG = "NewsFragment";
	private boolean hasBeenLoadedOnce = false;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
		RecyclerView newsContainer = view.findViewById(R.id.newsContainer);

		// animate items when they load
		AlphaInAnimationAdapter alphaInAnimationAdapter = new AlphaInAnimationAdapter(new NewsAdapter(getContext(), (MainActivity) getActivity(), newsItems));
		alphaInAnimationAdapter.setFirstOnly(false);
		newsContainer.setAdapter(alphaInAnimationAdapter);
		newsContainer.setLayoutManager(new LinearLayoutManager(getContext()));

		if (!isAdded()) {
			logDebug(TAG, "isAdded() returned false (displayNewsItems)");
			return;
		}

		if (getActivity() == null) {
			logError(TAG, new OxygenUpdaterException("getActivity() returned null (displayNewsItems)"));
			return;
		}

		if (callback != null) {
			callback.accept(null);
		}
	}

	private int getLoadDelayMilliseconds() {
		if (!hasBeenLoadedOnce) {
			hasBeenLoadedOnce = true;
			return 3000;
		}

		return 10;
	}
}
