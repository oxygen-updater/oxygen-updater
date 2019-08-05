package com.arjanvlek.oxygenupdater.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.download.DownloadService;
import com.arjanvlek.oxygenupdater.internal.Worker;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;

public class Dialogs {

	/**
	 * Shows an {@link MessageDialog} with the occurred download error.
	 *
	 * @param title   Title of the error message
	 * @param message Contents of the error message
	 */
	public static void showDownloadError(Fragment fragment, UpdateData updateData, boolean isResumable, @StringRes int title, @StringRes int message) {
		showDownloadError(fragment, updateData, isResumable, fragment.getString(title), fragment.getString(message));
	}

	/**
	 * Shows an {@link MessageDialog} with the occurred download error.
	 *
	 * @param title   Title of the error message
	 * @param message Contents of the error message
	 */
	public static void showDownloadError(Fragment fragment, UpdateData updateData, boolean isResumable, String title, String message) {
		checkPreconditions(fragment, () -> {
			MessageDialog errorDialog = new MessageDialog()
					.setTitle(title)
					.setMessage(message)
					.setPositiveButtonText(fragment.getString(R.string.download_error_close))
					.setNegativeButtonText(isResumable ? fragment.getString(R.string.download_error_resume) : fragment
							.getString(R.string.download_error_retry))
					.setClosable(true)
					.setDialogListener(new MessageDialog.DialogListener() {
						@Override
						public void onDialogPositiveButtonClick(DialogInterface dialogFragment) {
							LocalNotifications.hideDownloadCompleteNotification(fragment.getActivity());
						}

						@Override
						public void onDialogNegativeButtonClick(DialogInterface dialogFragment) {
							LocalNotifications.hideDownloadCompleteNotification(fragment.getActivity());

							DownloadService.performOperation(fragment.getActivity(), DownloadService.ACTION_CANCEL_DOWNLOAD, updateData);
							DownloadService.performOperation(fragment.getActivity(), isResumable ? DownloadService.ACTION_RESUME_DOWNLOAD : DownloadService.ACTION_DOWNLOAD_UPDATE, updateData);
						}
					});

			try {
				errorDialog.setTargetFragment(fragment, 0);
				FragmentTransaction transaction = fragment.getActivity()
						.getSupportFragmentManager()
						.beginTransaction();
				transaction.add(errorDialog, "DownloadError");
				transaction.commitAllowingStateLoss();
			} catch (IllegalStateException e) {
				if (e.getMessage() != null && e.getMessage().contains("onSaveInstanceState")) {
					logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e);
				} else {
					logError("MessageDialog", "Error when displaying dialog 'DownloadError'", e);
				}
			}
		});
	}

	public static void showNoNetworkConnectionError(Fragment fragment) {
		checkPreconditions(fragment, () -> {
			MessageDialog errorDialog = new MessageDialog()
					.setTitle(fragment.getString(R.string.error_app_requires_network_connection))
					.setMessage(fragment.getString(R.string.error_app_requires_network_connection_message))
					.setNegativeButtonText(fragment.getString(R.string.download_error_close))
					.setClosable(false);
			errorDialog.setTargetFragment(fragment, 0);
			errorDialog.show(fragment.getFragmentManager(), "NetworkError");
		});
	}

	public static void showServerMaintenanceError(Fragment fragment) {
		checkPreconditions(fragment, () -> {
			MessageDialog serverMaintenanceErrorFragment = new MessageDialog()
					.setTitle(fragment.getString(R.string.error_maintenance))
					.setMessage(fragment.getString(R.string.error_maintenance_message))
					.setNegativeButtonText(fragment.getString(R.string.download_error_close))
					.setClosable(false);
			serverMaintenanceErrorFragment.setTargetFragment(fragment, 0);
			serverMaintenanceErrorFragment.show(fragment.getFragmentManager(), "MaintenanceError");
		});
	}

	public static void showAppOutdatedError(Fragment fragment, Activity activity) {
		ActivityLauncher activityLauncher = new ActivityLauncher(activity);

		checkPreconditions(fragment, () -> {
			MessageDialog appOutdatedErrorFragment = new MessageDialog()
					.setTitle(fragment.getString(R.string.error_app_outdated))
					.setMessage(fragment.getString(R.string.error_app_outdated_message))
					.setPositiveButtonText(fragment.getString(R.string.error_google_play_button_text))
					.setNegativeButtonText(fragment.getString(R.string.download_error_close))
					.setClosable(false)
					.setDialogListener(new MessageDialog.DialogListener() {
						@Override
						public void onDialogPositiveButtonClick(DialogInterface dialogFragment) {
							activityLauncher.launchPlayStorePage(activity);
						}

						@Override
						public void onDialogNegativeButtonClick(DialogInterface dialogFragment) {

						}
					});
			appOutdatedErrorFragment.setTargetFragment(fragment, 0);
			appOutdatedErrorFragment.show(fragment.getFragmentManager(), "AppOutdatedError");
		});
	}

	public static void showUpdateAlreadyDownloadedMessage(UpdateData updateData, Fragment fragment, Consumer<Void> actionPerformedCallback) {
		checkPreconditions(fragment, () -> {
			MessageDialog dialog = new MessageDialog()
					.setTitle(fragment.getString(R.string.delete_message_title))
					.setMessage(fragment.getString(R.string.delete_message_contents))
					.setClosable(true)
					.setPositiveButtonText(fragment.getString(R.string.install))
					.setNegativeButtonText(fragment.getString(R.string.delete_message_delete_button))
					.setDialogListener(new MessageDialog.DialogListener() {
						@Override
						public void onDialogPositiveButtonClick(DialogInterface dialogInterface) {
							if (fragment.getActivity() == null || fragment.getActivity()
									.getApplication() == null) {
								return;
							}

							ActivityLauncher activityLauncher = new ActivityLauncher(fragment.getActivity());
							activityLauncher.UpdateInstallation(true, updateData);
						}

						@Override
						public void onDialogNegativeButtonClick(DialogInterface dialogFragment) {
							actionPerformedCallback.accept(null);
						}
					});

			try {
				dialog.setTargetFragment(fragment, 0);
				FragmentTransaction transaction = fragment.getActivity()
						.getSupportFragmentManager()
						.beginTransaction();
				transaction.add(dialog, "DeleteDownload");
				transaction.commitAllowingStateLoss();
			} catch (IllegalStateException e) {
				if (e.getMessage() != null && e.getMessage().contains("onSaveInstanceState")) {
					logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e);
				} else {
					logError("MessageDialog", "Error when displaying dialog 'DeleteDownload'", e);
				}
			}
		});
	}

	public static void showAdvancedModeExplanation(Context ctx, FragmentManager fm) {
		if (fm == null) {
			return;
		}

		MessageDialog dialog = new MessageDialog()
				.setTitle(ctx.getString(R.string.settings_advanced_mode))
				.setMessage(ctx.getString(R.string.settings_advanced_mode_explanation))
				.setClosable(true)
				.setPositiveButtonText(ctx.getString(R.string.update_information_close));

		dialog.show(fm, "OU_AdvancedModeExplanation");
	}

	private static void checkPreconditions(Fragment fragment, Worker callback) {
		if (fragment != null && fragment.getFragmentManager() != null && fragment.isAdded() && fragment
				.getActivity() != null && !fragment.getActivity().isFinishing()) {
			callback.start();
		}
	}

}
