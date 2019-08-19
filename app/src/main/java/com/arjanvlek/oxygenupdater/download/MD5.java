/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.arjanvlek.oxygenupdater.download;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;

class MD5 {
	private static final String TAG = "MD5";

	static boolean checkMD5(String md5, File updateFile) {
		if (TextUtils.isEmpty(md5) || updateFile == null) {
			logError(TAG, new UpdateVerificationException("MD5 string empty or updateFile null"));
			return false;
		}

		String calculatedDigest = calculateMD5(updateFile, 0);
		if (calculatedDigest == null) {
			logError(TAG, new UpdateVerificationException("calculatedDigest null"));
			return false;
		}

		logVerbose(TAG, "Calculated digest: " + calculatedDigest);
		logVerbose(TAG, "Provided digest: " + md5);

		return calculatedDigest.equalsIgnoreCase(md5);
	}

	private static String calculateMD5(File updateFile, int retryCount) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			logError(TAG, "Exception while getting digest", e);
			return null;
		}

		InputStream is;
		try {
			is = new FileInputStream(updateFile);
		} catch (FileNotFoundException e) {
			logError(TAG, "Exception while getting FileInputStream", e);

			// If the downloaded file may not yet be accessed (because it's still being flushed or previously-existing files are being rotated, wait a bit and try verifying it again.
			if (retryCount < 5) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException i) {
					logError(TAG, "Error while trying to re-verify file after 2 seconds", i);
				}
				return calculateMD5(updateFile, ++retryCount);
			} else {
				return null;
			}
		}

		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			return output;
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logError(TAG, "Exception on closing MD5 input stream", e);
			}
		}
	}
}
