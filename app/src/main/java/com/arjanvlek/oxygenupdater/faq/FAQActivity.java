package com.arjanvlek.oxygenupdater.faq;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.ThemeUtils;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity;

import static com.arjanvlek.oxygenupdater.ApplicationData.APP_USER_AGENT;

public class FAQActivity extends SupportActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_faq);
	}

	@Override
	protected void onStart() {
		super.onStart();

		SwipeRefreshLayout refreshLayout = findViewById(R.id.faq_webpage_layout);
		refreshLayout.setColorSchemeResources(R.color.colorPrimary);
		refreshLayout.setOnRefreshListener(this::loadFaqPage);

		loadFaqPage();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handles action bar item clicks
		int id = item.getItemId();

		// Respond to the action bar's Up/Home button, exit the activity gracefully to prevent downloads getting stuck
		if (id == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Gracefully quit the activity if the Back button is pressed to speed the app up
	 */
	@Override
	public void onBackPressed() {
		finish();
	}

	/**
	 * Loads the FAQ page, or displays a No Network connection screen if there is no network connection
	 */
	@SuppressLint("SetJavaScriptEnabled") // JavaScript is required to toggle the FAQ Item boxes.
	private void loadFaqPage() {
		if (Utils.checkNetworkConnection(getApplicationContext())) {
			SwipeRefreshLayout refreshLayout = findViewById(R.id.faq_webpage_layout);

			refreshLayout.setRefreshing(true);

			WebView faqPageView = findViewById(R.id.faqWebView);

			switchViews(true);

			faqPageView.getSettings().setJavaScriptEnabled(true);
			faqPageView.getSettings().setUserAgentString(APP_USER_AGENT);
			faqPageView.clearCache(true);

			String faqServerUrl = BuildConfig.FAQ_SERVER_URL + "/";

			// since we can't edit CSS in WebViews,
			// append 'Light' or 'Dark' to faqServerUrl to get the corresponding themed version
			// backend handles CSS according to material spec
			faqServerUrl += ThemeUtils.isNightModeActive(this)
					? "Dark"
					: "Light";

			faqPageView.loadUrl(faqServerUrl);

			refreshLayout.setRefreshing(false);
		} else {
			switchViews(false);
		}
	}

	/**
	 * Handler for the Retry button on the "No network connection" page.
	 *
	 * @param v View
	 */
	public void onRetryButtonClick(View v) {
		loadFaqPage();
	}

	/**
	 * Switches between the Web Browser view and the No Network connection screen based on the
	 * hasNetwork parameter.
	 *
	 * @param hasNetwork Whether the device has a network connection or not.
	 */
	private void switchViews(boolean hasNetwork) {
		LinearLayout noNetworkLayout = findViewById(R.id.faq_no_network_view);
		noNetworkLayout.setVisibility(hasNetwork ? View.GONE : View.VISIBLE);

		SwipeRefreshLayout webPageLayout = findViewById(R.id.faq_webpage_layout);
		webPageLayout.setVisibility(hasNetwork ? View.VISIBLE : View.GONE);
	}
}
