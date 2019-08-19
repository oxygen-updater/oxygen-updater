package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.about.AboutActivity;
import com.arjanvlek.oxygenupdater.contribution.ContributorActivity;
import com.arjanvlek.oxygenupdater.faq.FAQActivity;
import com.arjanvlek.oxygenupdater.help.HelpActivity;
import com.arjanvlek.oxygenupdater.installation.InstallActivity;
import com.arjanvlek.oxygenupdater.settings.SettingsActivity;
import com.arjanvlek.oxygenupdater.setupwizard.SetupActivity;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

import java.lang.ref.WeakReference;

import static android.widget.Toast.LENGTH_LONG;
import static com.arjanvlek.oxygenupdater.contribution.ContributorActivity.INTENT_HIDE_ENROLLMENT;
import static com.arjanvlek.oxygenupdater.installation.InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE;
import static com.arjanvlek.oxygenupdater.installation.InstallActivity.INTENT_UPDATE_DATA;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;

public class ActivityLauncher {

	private final WeakReference<Activity> baseActivity;

	public ActivityLauncher(Activity baseActivity) {
		this.baseActivity = new WeakReference<>(baseActivity);
	}

	public void launchPlayStorePage(Context context) {
		String appPackageName = context.getPackageName();

		try {
			// try opening Play Store
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
		} catch (ActivityNotFoundException e) {
			try {
				// try opening browser
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
			} catch (ActivityNotFoundException e1) {
				// give up and cry
				Toast.makeText(context, context.getString(R.string.error_unable_to_rate_app), LENGTH_LONG).show();
				logWarning("AboutActivity", "App rating without google play store support", e1);
			}
		}
	}


	/**
	 * Opens the settings page.
	 */
	public void Settings() {
		startActivity(SettingsActivity.class);
	}

	/**
	 * Opens the welcome tutorial.
	 */
	public void Tutorial() {
		startActivity(SetupActivity.class);
	}

	/**
	 * Opens the about page.
	 */
	public void About() {
		startActivity(AboutActivity.class);
	}

	/**
	 * Opens the contribution popup.
	 */
	public void Contribute() {
		startActivity(ContributorActivity.class);
	}

	/**
	 * Opens the contribution popup without option to enroll.
	 */
	public void Contribute_noenroll() {
		Intent i = new Intent(baseActivity.get(), ContributorActivity.class);
		i.putExtra(INTENT_HIDE_ENROLLMENT, true);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		baseActivity.get().startActivity(i);
	}

	/**
	 * Opens the help page.
	 */
	public void Help() {
		startActivity(HelpActivity.class);
	}

	/**
	 * Opens the faq page.
	 */
	public void FAQ() {
		startActivity(FAQActivity.class);
	}


	/**
	 * Opens the update installation page.
	 */
	public void UpdateInstallation(boolean isDownloaded, UpdateData updateData) {
		Intent i = new Intent(baseActivity.get(), InstallActivity.class);
		i.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, (!isDownloaded));
		i.putExtra(INTENT_UPDATE_DATA, updateData);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		baseActivity.get().startActivity(i);
	}

	private <T> void startActivity(Class<T> activityClass) {
		Intent i = new Intent(baseActivity.get(), activityClass);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		baseActivity.get().startActivity(i);
	}

	public void dispose() {
		baseActivity.clear();
	}


}
