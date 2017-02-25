package com.arjanvlek.oxygenupdater.notifications;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Download.UpdateDownloader;
import com.arjanvlek.oxygenupdater.Model.UpdateData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.views.MessageDialog;

import java8.util.function.Consumer;

public class Dialogs {

    /**
     * Shows an {@link MessageDialog} with the occured download error.
     * @param title Title of the error message
     * @param message Contents of the error message
     */
    public static void showDownloadError(final Fragment fragment, final UpdateDownloader updateDownloader, final UpdateData updateData, @StringRes int title, @StringRes int message) {
        showDownloadError(fragment, updateDownloader, updateData, fragment.getString(title), fragment.getString(message));
    }

    /**
     * Shows an {@link MessageDialog} with the occured download error.
     * @param title Title of the error message
     * @param message Contents of the error message
     */
    public static void showDownloadError(final Fragment fragment, final UpdateDownloader updateDownloader, final UpdateData updateData, String title, String message) {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setPositiveButtonText(fragment.getString(R.string.download_error_close))
                .setNegativeButtonText(fragment.getString(R.string.download_error_retry))
                .setClosable(true)
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        LocalNotifications.hideDownloadCompleteNotification(fragment.getActivity());
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                        LocalNotifications.hideDownloadCompleteNotification(fragment.getActivity());
                        updateDownloader.cancelDownload(updateData);
                        updateDownloader.downloadUpdate(updateData);
                    }
                });
        errorDialog.setTargetFragment(fragment, 0);
        FragmentTransaction transaction = fragment.getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(errorDialog, "DownloadError");
        transaction.commitAllowingStateLoss();
    }

    public static void showNoNetworkConnectionError(Fragment fragment) {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(fragment.getString(R.string.error_app_requires_network_connection))
                .setMessage(fragment.getString(R.string.error_app_requires_network_connection_message))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false);
        errorDialog.setTargetFragment(fragment, 0);
        errorDialog.show(fragment.getFragmentManager(), "NetworkError");
    }

    public static void showServerMaintenanceError(Fragment fragment) {
        MessageDialog serverMaintenanceErrorFragment = new MessageDialog()
                .setTitle(fragment.getString(R.string.error_maintenance))
                .setMessage(fragment.getString(R.string.error_maintenance_message))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false);
        serverMaintenanceErrorFragment.setTargetFragment(fragment, 0);
        serverMaintenanceErrorFragment.show(fragment.getFragmentManager(), "MaintenanceError");
    }

    public static void showAppOutdatedError(Fragment fragment) {
        MessageDialog appOutdatedErrorFragment = new MessageDialog()
                .setTitle(fragment.getString(R.string.error_app_outdated))
                .setMessage(fragment.getString(R.string.error_app_outdated_message))
                .setPositiveButtonText(fragment.getString(R.string.error_google_play_button_text))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false)
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        try {
                            final String appPackageName = BuildConfig.APPLICATION_ID;
                            try {
                                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (ActivityNotFoundException e) {
                                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        } catch (Exception ignored) {

                        }
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {

                    }
                });
        appOutdatedErrorFragment.setTargetFragment(fragment, 0);
        appOutdatedErrorFragment.show(fragment.getFragmentManager(), "AppOutdatedError");
    }

    public static void showUpdateAlreadyDownloadedMessage(final Fragment fragment, final Consumer<Void> actionPerformedCallback) {
        MessageDialog dialog = new MessageDialog()
                .setTitle(fragment.getString(R.string.delete_message_title))
                .setMessage(fragment.getString(R.string.delete_message_contents))
                .setClosable(true)
                .setPositiveButtonText(fragment.getString(R.string.install_guide))
                .setNegativeButtonText(fragment.getString(R.string.delete_message_delete_button))
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        ActivityLauncher activityLauncher = new ActivityLauncher(fragment.getActivity());
                        activityLauncher.UpdateInstructions(true);
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                        actionPerformedCallback.accept(null);
                    }
                });
        dialog.setTargetFragment(fragment, 0);
        FragmentTransaction transaction = fragment.getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(dialog, "DeleteDownload");
        transaction.commitAllowingStateLoss();
    }

}
