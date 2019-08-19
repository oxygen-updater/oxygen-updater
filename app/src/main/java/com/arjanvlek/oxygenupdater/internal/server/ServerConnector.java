package com.arjanvlek.oxygenupdater.internal.server;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.installation.automatic.RootInstall;
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.root.RootAccessChecker;
import com.arjanvlek.oxygenupdater.news.NewsDatabaseHelper;
import com.arjanvlek.oxygenupdater.news.NewsItem;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase;
import com.arjanvlek.oxygenupdater.updateinformation.Banner;
import com.arjanvlek.oxygenupdater.updateinformation.ServerMessage;
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.LocalDateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationData.APP_OUTDATED_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationData.APP_USER_AGENT;
import static com.arjanvlek.oxygenupdater.ApplicationData.NETWORK_CONNECTION_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationData.SERVER_MAINTENANCE_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationData.UNABLE_TO_FIND_A_MORE_RECENT_BUILD;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_FILE_NAME;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static com.arjanvlek.oxygenupdater.updateinformation.ServerStatus.Status.NORMAL;
import static com.arjanvlek.oxygenupdater.updateinformation.ServerStatus.Status.UNREACHABLE;

public class ServerConnector implements Cloneable {

	private final static String USER_AGENT_TAG = "User-Agent";
	private static final String TAG = "ServerConnector";

	private final ObjectMapper objectMapper;
	private final SettingsManager settingsManager;

	private final List<Device> devices;
	private LocalDateTime deviceFetchDate;
	private ServerStatus serverStatus;

	public ServerConnector(SettingsManager settingsManager) {
		this.settingsManager = settingsManager;
		objectMapper = new ObjectMapper();
		devices = new ArrayList<>();
	}

	public void getDevices(Consumer<List<Device>> callback) {
		getDevices(false, callback);
	}

	public void getDevices(boolean alwaysFetch, Consumer<List<Device>> callback) {
		if (deviceFetchDate != null && deviceFetchDate.plusMinutes(5).isAfter(LocalDateTime.now()) && !alwaysFetch) {
			logVerbose(TAG, "Used local cache to fetch devices...");
			callback.accept(devices);
		} else {
			logVerbose(TAG, "Used remote server to fetch devices...");
			new CollectionResponseExecutor<Device>(ServerRequest.DEVICES, devices -> {
				this.devices.clear();
				this.devices.addAll(devices);
				deviceFetchDate = LocalDateTime.now();
				callback.accept(devices);
			}).execute();
		}
	}

	public void getUpdateMethods(@NonNull Long deviceId, Consumer<List<UpdateMethod>> callback) {
		new CollectionResponseExecutor<UpdateMethod>(ServerRequest.UPDATE_METHODS, (updateMethods -> RootAccessChecker
				.checkRootAccess(hasRootAccess -> {
					if (hasRootAccess) {
						callback.accept(StreamSupport.stream(updateMethods)
								.filter(UpdateMethod::isForRootedDevice)
								.map(um -> um.setRecommended(um.isRecommendedWithRoot() ? "1" : "0"))
								.collect(Collectors.toList()));
					} else {
						callback.accept(StreamSupport.stream(updateMethods)
								.map(um -> um.setRecommended(um.isRecommendedWithoutRoot() ? "1" : "0"))
								.collect(Collectors.toList()));
					}
				})), deviceId).execute();
	}

	public void getAllUpdateMethods(Consumer<List<UpdateMethod>> callback) {
		new CollectionResponseExecutor<>(ServerRequest.ALL_UPDATE_METHODS, callback).execute();
	}

	public void getUpdateData(boolean online, @NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull String incrementalSystemVersion, Consumer<UpdateData> callback, Consumer<String> errorFunction) {

		new ObjectResponseExecutor<>(ServerRequest.UPDATE_DATA, (Consumer<UpdateData>) updateData -> {
			if (updateData != null
					&& updateData.getInformation() != null
					&& updateData.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD)
					&& updateData.isUpdateInformationAvailable()
					&& updateData.isSystemIsUpToDate()) {
				getMostRecentOxygenOTAUpdate(deviceId, updateMethodId, callback);
			} else if (!online) {
				if (settingsManager.checkIfOfflineUpdateDataIsAvailable()) {
					updateData = new UpdateData();
					updateData.setId(settingsManager.getPreference(PROPERTY_OFFLINE_ID, null));
					updateData.setVersionNumber(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME, null));
					updateData.setDownloadSize(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, 0L));
					updateData.setDescription(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, null));
					updateData.setDownloadUrl(settingsManager.getPreference(PROPERTY_OFFLINE_DOWNLOAD_URL, null));
					updateData.setUpdateInformationAvailable(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, false));
					updateData.setFilename(settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME, null));
					updateData.setSystemIsUpToDate(settingsManager.getPreference(PROPERTY_OFFLINE_IS_UP_TO_DATE, false));
					callback.accept(updateData);
				} else {
					errorFunction.accept(NETWORK_CONNECTION_ERROR);
				}
			} else {
				callback.accept(updateData);
			}
		}, deviceId, updateMethodId, incrementalSystemVersion).execute();
	}

	public void getInAppMessages(boolean online, Consumer<List<Banner>> callback, Consumer<String> errorCallback) {
		List<Banner> inAppBars = new ArrayList<>();

		getServerStatus(online, serverStatus -> getServerMessages(settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L), settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L), serverMessages -> {
			// Add the "No connection" bar depending on the network status of the device.
			if (!online) {
				inAppBars.add(new Banner() {
					@Override
					public String getBannerText(Context context) {
						return context.getString(R.string.error_no_internet_connection);
					}

					@Override
					public int getColor(Context context) {
						// since the primary color is red, use that to match the app bar
						return ContextCompat.getColor(context, R.color.colorPrimary);
					}
				});
			}

			if (serverMessages != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
				inAppBars.addAll(serverMessages);
			}

			ServerStatus.Status status = serverStatus.getStatus();

			if (status.isUserRecoverableError()) {
				inAppBars.add(serverStatus);
			}

			if (status.isNonRecoverableError()) {
				switch (status) {
					case MAINTENANCE:
						errorCallback.accept(SERVER_MAINTENANCE_ERROR);
						break;
					case OUTDATED:
						errorCallback.accept(APP_OUTDATED_ERROR);
						break;
				}
			}

			if (settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverStatus.checkIfAppIsUpToDate()) {
				inAppBars.add(new Banner() {

					@Override
					public CharSequence getBannerText(Context context) {
						return Html.fromHtml(String.format(context.getString(R.string.new_app_version), serverStatus.getLatestAppVersion()));
					}

					@Override
					public int getColor(Context context) {
						return ContextCompat.getColor(context, R.color.colorPositive);
					}
				});
			}
			callback.accept(inAppBars);
		}));
	}

	public void getInstallGuidePage(@NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull Integer pageNumber, Consumer<InstallGuidePage> callback) {
		new ObjectResponseExecutor<>(ServerRequest.INSTALL_GUIDE_PAGE, callback, deviceId, updateMethodId, pageNumber).execute();
	}

	public void submitUpdateFile(@NonNull String filename, Consumer<ServerPostResult> callback) {
		JSONObject postBody = new JSONObject();
		try {
			postBody.put("filename", filename);
		} catch (JSONException e) {
			ServerPostResult errorResult = new ServerPostResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("IN-APP ERROR (ServerConnector): Json parse error on input data " + filename);
			callback.accept(errorResult);
		}

		new ObjectResponseExecutor<>(ServerRequest.SUBMIT_UPDATE_FILE, postBody, callback).execute();
	}

	public void logRootInstall(RootInstall rootInstall, Consumer<ServerPostResult> callback) {

		try {
			JSONObject installationData = new JSONObject(objectMapper.writeValueAsString(rootInstall));

			new ObjectResponseExecutor<>(ServerRequest.LOG_UPDATE_INSTALLATION, installationData, callback).execute();
		} catch (JSONException | JsonProcessingException e) {
			ServerPostResult errorResult = new ServerPostResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("IN-APP ERROR (ServerConnector): Json parse error on input data " + rootInstall
					.toString());
			callback.accept(errorResult);
		}
	}

	public void verifyPurchase(@NonNull Purchase purchase, String amount, PurchaseType purchaseType, Consumer<ServerPostResult> callback) {
		JSONObject purchaseData;

		try {
			purchaseData = new JSONObject(purchase.getOriginalJson());
			purchaseData.put("purchaseType", purchaseType.toString());
			purchaseData.put("itemType", purchase.getItemType());
			purchaseData.put("signature", purchase.getSignature());
			purchaseData.put("amount", amount);
		} catch (JSONException ignored) {
			ServerPostResult result = new ServerPostResult();
			result.setSuccess(false);
			result.setErrorMessage("IN-APP ERROR (ServerConnector): JSON parse error on input data " + purchase.getOriginalJson());
			callback.accept(result);
			return;
		}

		new ObjectResponseExecutor<>(ServerRequest.VERIFY_PURCHASE, purchaseData, callback).execute();
	}

	public void markNewsItemAsRead(@NonNull Long newsItemId, Consumer<ServerPostResult> callback) {
		JSONObject body = new JSONObject();

		try {
			body.put("news_item_id", newsItemId);
		} catch (JSONException ignored) {

		}

		new ObjectResponseExecutor<>(ServerRequest.NEWS_READ, body, callback).execute();
	}

	private void getMostRecentOxygenOTAUpdate(@NonNull Long deviceId, @NonNull Long updateMethodId, Consumer<UpdateData> callback) {
		new ObjectResponseExecutor<>(ServerRequest.MOST_RECENT_UPDATE_DATA, callback, deviceId, updateMethodId).execute();
	}

	public void getServerStatus(boolean online, Consumer<ServerStatus> callback) {
		if (serverStatus == null) {
			new ObjectResponseExecutor<ServerStatus>(ServerRequest.SERVER_STATUS, serverStatus -> {
				boolean automaticInstallationEnabled = false;
				int pushNotificationsDelaySeconds = 1800;

				if (settingsManager != null) {
					automaticInstallationEnabled = settingsManager.getPreference(PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false);
					pushNotificationsDelaySeconds = settingsManager.getPreference(PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800);
				}

				if (serverStatus == null && online) {
					this.serverStatus = new ServerStatus(UNREACHABLE, BuildConfig.VERSION_NAME, automaticInstallationEnabled, pushNotificationsDelaySeconds);
				} else {
					this.serverStatus = serverStatus != null
							? serverStatus
							: new ServerStatus(NORMAL, BuildConfig.VERSION_NAME, automaticInstallationEnabled, pushNotificationsDelaySeconds);
				}

				if (settingsManager != null) {
					settingsManager.savePreference(PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, this.serverStatus.isAutomaticInstallationEnabled());
					settingsManager.savePreference(PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, this.serverStatus.getPushNotificationDelaySeconds());
				}

				callback.accept(this.serverStatus);
			}).execute();
		} else {
			callback.accept(serverStatus);
		}
	}

	private void getServerMessages(@NonNull Long deviceId, @NonNull Long updateMethodId, Consumer<List<ServerMessage>> callback) {
		new CollectionResponseExecutor<>(ServerRequest.SERVER_MESSAGES, callback, deviceId, updateMethodId)
				.execute();
	}

	public void getNews(Context context, @NonNull Long deviceId, @NonNull Long updateMethodId, Consumer<List<NewsItem>> callback) {
		new CollectionResponseExecutor<NewsItem>(ServerRequest.NEWS, (newsItems -> {
			NewsDatabaseHelper databaseHelper = new NewsDatabaseHelper(context);

			if (newsItems != null && !newsItems.isEmpty() && Utils.checkNetworkConnection(context)) {
				databaseHelper.saveNewsItems(newsItems);
			}

			callback.accept(databaseHelper.getAllNewsItems());

			databaseHelper.close();
		}), deviceId, updateMethodId).execute();
	}

	public void getNewsItem(Context context, @NonNull Long newsItemId, Consumer<NewsItem> callback) {
		new ObjectResponseExecutor<NewsItem>(ServerRequest.NEWS_ITEM, (newsItem -> {
			NewsDatabaseHelper databaseHelper = new NewsDatabaseHelper(context);

			if (newsItem != null && Utils.checkNetworkConnection(context)) {
				databaseHelper.saveNewsItem(newsItem);
			}

			callback.accept(databaseHelper.getNewsItem(newsItemId));

			databaseHelper.close();
		}), newsItemId).execute();
	}

	private <T> List<T> findMultipleFromServerResponse(ServerRequest serverRequest, JSONObject body, Object... params) {
		try {
			String response = performServerRequest(serverRequest, body, params);
			if (response == null || response.isEmpty()) {
				return new ArrayList<>();
			}

			return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, serverRequest.getReturnClass()));
		} catch (Exception e) {
			logError(TAG, "JSON parse error", e);
			return new ArrayList<>();
		}
	}

	private <T> T findOneFromServerResponse(ServerRequest serverRequest, JSONObject body, Object... params) {
		try {
			String response = performServerRequest(serverRequest, body, params);
			if (response == null || response.isEmpty()) {
				return null;
			}

			return objectMapper.readValue(response, objectMapper.getTypeFactory().constructType(serverRequest.getReturnClass()));
		} catch (Exception e) {
			logError(TAG, "JSON parse error", e);
			return null;
		}
	}

	private String performServerRequest(ServerRequest request, JSONObject body, Object... params) {
		return performServerRequest(request, body, 0, params);
	}

	private String performServerRequest(ServerRequest request, JSONObject body, int retryCount, Object... params) {

		try {
			URL requestUrl = request.getUrl(params);

			if (requestUrl == null) {
				return null;
			}

			logVerbose(TAG, "");
			logVerbose(TAG, "Performing " + request.getRequestMethod()
					.toString() + " request to URL " + requestUrl.toString());
			logVerbose(TAG, "Timeout is set to " + request.getTimeOutInSeconds() + " seconds.");

			HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();

			int timeOutInMilliseconds = request.getTimeOutInSeconds() * 1000;

			//setup request
			urlConnection.setRequestProperty(USER_AGENT_TAG, APP_USER_AGENT);
			urlConnection.setRequestMethod(request.getRequestMethod().toString());
			urlConnection.setConnectTimeout(timeOutInMilliseconds);
			urlConnection.setReadTimeout(timeOutInMilliseconds);

			if (body != null) {
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Accept", "application/json");

				OutputStream out = urlConnection.getOutputStream();
				byte[] outputBytes = body.toString().getBytes();
				out.write(outputBytes);
				out.close();
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();
			String rawResponse = response.toString();
			logVerbose(TAG, "Response: " + rawResponse);
			return rawResponse;
		} catch (Exception e) {
			if (retryCount < 5) {
				return performServerRequest(request, body, retryCount + 1, params);
			} else {
				if (ExceptionUtils.isNetworkError(e)) {
					logWarning(TAG, new NetworkException("Error performing request <" + request.toString(params) + ">."));
				} else {
					logError(TAG, "Error performing request <" + request.toString(params) + ">", e);
				}
				return null;
			}
		}
	}

	@Override
	public ServerConnector clone() {
		try {
			return (ServerConnector) super.clone();
		} catch (CloneNotSupportedException e) {
			logError(TAG, "Internal error cloning ServerConnector", e);
			return new ServerConnector(settingsManager);
		}
	}

	private class CollectionResponseExecutor<T> extends AsyncTask<Void, Void, List<T>> {

		private final ServerRequest serverRequest;
		private final JSONObject body;
		private final Consumer<List<T>> callback;
		private final Object[] params;

		CollectionResponseExecutor(ServerRequest serverRequest, Consumer<List<T>> callback, Object... params) {
			this(serverRequest, null, callback, params);
		}

		CollectionResponseExecutor(ServerRequest serverRequest, JSONObject body, Consumer<List<T>> callback, Object... params) {
			this.serverRequest = serverRequest;
			this.body = body;
			this.params = params;
			this.callback = callback;
		}

		@Override
		protected List<T> doInBackground(Void... voids) {
			return findMultipleFromServerResponse(serverRequest, body, params);
		}

		@Override
		protected void onPostExecute(List<T> results) {
			if (callback != null) {
				callback.accept(results);
			}
		}
	}

	private class ObjectResponseExecutor<E> extends AsyncTask<Void, Void, E> {

		private final ServerRequest serverRequest;
		private final JSONObject body;
		private final Consumer<E> callback;
		private final Object[] params;

		ObjectResponseExecutor(@NonNull ServerRequest serverRequest, @Nullable Consumer<E> callback, Object... params) {
			this(serverRequest, null, callback, params);
		}

		ObjectResponseExecutor(@NonNull ServerRequest serverRequest, JSONObject body, @Nullable Consumer<E> callback, Object... params) {
			this.serverRequest = serverRequest;
			this.body = body;
			this.params = params;
			this.callback = callback;
		}

		@Override
		protected E doInBackground(Void... voids) {
			return findOneFromServerResponse(serverRequest, body, params);
		}

		@Override
		protected void onPostExecute(E result) {
			if (callback != null) {
				callback.accept(result);
			}
		}

	}
}

