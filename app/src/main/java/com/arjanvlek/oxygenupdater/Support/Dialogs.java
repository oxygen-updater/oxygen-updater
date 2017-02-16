package com.arjanvlek.oxygenupdater.Support;

import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.views.MessageDialog;

public class Dialogs {

    /**
     * Shows an {@link MessageDialog} with the occured download error.
     * @param title Title of the error message
     * @param message Contents of the error message
     */
    public static void showDownloadError(final Fragment fragment, final UpdateDownloader updateDownloader, final OxygenOTAUpdate oxygenOTAUpdate, @StringRes int title, @StringRes int message) {
        showDownloadError(fragment, updateDownloader, oxygenOTAUpdate, fragment.getString(title), fragment.getString(message));
    }

    /**
     * Shows an {@link MessageDialog} with the occured download error.
     * @param title Title of the error message
     * @param message Contents of the error message
     */
    public static void showDownloadError(final Fragment fragment, final UpdateDownloader updateDownloader, final OxygenOTAUpdate oxygenOTAUpdate, String title, String message) {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setPositiveButtonText(fragment.getString(R.string.download_error_close))
                .setNegativeButtonText(fragment.getString(R.string.download_error_retry))
                .setClosable(true)
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        Notifications.hideDownloadCompleteNotification(fragment.getActivity());
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                        Notifications.hideDownloadCompleteNotification(fragment.getActivity());
                        updateDownloader.cancelDownload();
                        updateDownloader.downloadUpdate(oxygenOTAUpdate);
                    }
                });
        errorDialog.setTargetFragment(fragment, 0);
        FragmentTransaction transaction = fragment.getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(errorDialog, "DownloadError");
        transaction.commitAllowingStateLoss();

    }
}
