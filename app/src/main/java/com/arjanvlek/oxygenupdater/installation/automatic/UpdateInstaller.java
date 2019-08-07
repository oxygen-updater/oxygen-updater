package com.arjanvlek.oxygenupdater.installation.automatic;

import android.content.Context;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;

public class UpdateInstaller {

	private static final String INSTALL_SCRIPT_NON_AB_DIRECTORY = "/cache/recovery";
	private static final String INSTALL_SCRIPT_AB_DIRECTORY = "/data/cache/recovery";
	private static final String INSTALL_SCRIPT_PATH_SUFFIX = "/openrecoveryscript";

	// Open recovery script (http://wiki.rootzwiki.com/OpenRecoveryScript) commands
	private static final String BACKUP = "backup ";
	private static final String RESTORE = "restore ";
	private static final String INSTALL = "install ";
	private static final String WIPE = "wipe ";
	private static final String MOUNT = "mount ";
	private static final String UNMOUNT = "unmount ";
	private static final String SET_VARIABLE = "set ";
	private static final String MAKE_DIRECTORY = "mkdir ";
	private static final String EXECUTE_COMMAND = "cmd ";

	// Backup partitions
	private static final String SYSTEM_PARTITION = "S";
	private static final String BOOT_PARTITION = "B";
	private static final String DATA_PARTITION = "D";
	private static final String CACHE_PARTITION = "C";
	private static final String RECOVERY_PARTITION = "R";
	private static final String SPECIAL_PARTITION_1 = "1";
	private static final String SPECIAL_PARTITION_2 = "2";
	private static final String SPECIAL_PARTITION_3 = "3";
	private static final String ANDROID_SECURE_PARTITION = "A";
	private static final String SD_EXT_PARTITION = "E";

	// Custom commands
	private static final String ECHO = "echo ";
	private static final String REBOOT = "reboot";
	private static final String REBOOT_RECOVERY = REBOOT + " recovery";
	private static final String MUTE_OUTPUT = " > /dev/null";
	private static final String AND = " && ";

	private static final String CACHE = "cache";
	private static final String TEXT_SUCCESS = "success";

	public static void installUpdate(Context context, boolean isAbPartitionLayout, String downloadPath, String additionalZipFilePath, boolean backup, boolean wipeCachePartition, boolean rebootDevice) throws UpdateInstallationException, InterruptedException {
		// Build the installation script.
		StringBuilder recoveryCommands = new StringBuilder();

		// Print the banner. Note: Current TWRP (2019-04-03) does not support ECHO command :(
		addRecoveryText(recoveryCommands, " -------------------------");
		addRecoveryText(recoveryCommands, "| (↑) OXYGEN UPDATER      |");
		addRecoveryText(recoveryCommands, "|     " + context.getString(R.string.install_recovery_installer_title) + "  |");
		addRecoveryText(recoveryCommands, " -------------------------");

		// Print the "Thank you" message.
		addRecoveryNewLine(recoveryCommands);
		addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_thank_you));

		// Backup the device if requested.
		if (backup) {
			addRecoveryNewLine(recoveryCommands);
			addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_backing_up));
			addRecoveryCommand(recoveryCommands, BACKUP + SYSTEM_PARTITION + BOOT_PARTITION + DATA_PARTITION + CACHE_PARTITION + RECOVERY_PARTITION + ANDROID_SECURE_PARTITION);
		}

		// Install the update.
		addRecoveryNewLine(recoveryCommands);
		addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_installing_update));
		addRecoveryCommand(recoveryCommands, INSTALL + downloadPath);

		// Install the additional zip if it is selected.
		if (additionalZipFilePath != null && !additionalZipFilePath.isEmpty()) {
			addRecoveryNewLine(recoveryCommands);
			addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_installing_additional_zip));
			addRecoveryCommand(recoveryCommands, INSTALL + additionalZipFilePath);
		}

		// Clear the cache partition if requested.
		if (wipeCachePartition) {
			addRecoveryNewLine(recoveryCommands);
			addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_clearing_cache));
			addRecoveryCommand(recoveryCommands, WIPE + CACHE);
		}

		// Reboot the device if requested.
		if (rebootDevice) {
			addRecoveryNewLine(recoveryCommands);
			addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_done));
			addRecoveryCommand(recoveryCommands, EXECUTE_COMMAND + REBOOT);
		}

		Thread.sleep(1000);

		List<String> suCommands = new ArrayList<>();

		String INSTALL_SCRIPT_DIRECTORY = (isAbPartitionLayout ? INSTALL_SCRIPT_AB_DIRECTORY : INSTALL_SCRIPT_NON_AB_DIRECTORY);
		String INSTALL_SCRIPT_PATH = INSTALL_SCRIPT_DIRECTORY + INSTALL_SCRIPT_PATH_SUFFIX;

		// Create the install script directory (if not exists) by calling "su mkdir -p (/data)/cache/recovery".
		addSUCommand(suCommands, "mkdir -p " + INSTALL_SCRIPT_DIRECTORY);

		// Write the install script by calling "su echo '<commands>' > (/data)/cache/recovery/openrecoveryscript"
		addSUCommand(suCommands, ECHO + singleQuoted(recoveryCommands.toString()) + " > " + INSTALL_SCRIPT_PATH, false);

		// Reboot the device by calling "su reboot recovery".
		addSUCommand(suCommands, REBOOT_RECOVERY);

		// Execute all commands at once to prevent unnecessary SU popups.
		executeSUCommands(suCommands, context);

		// The device will now restart to install the update.
	}

	private static StringBuilder addRecoveryCommand(StringBuilder builder, String content) {
		return builder.append(content).append(System.getProperty("line.separator"));
	}

	private static StringBuilder addRecoveryNewLine(StringBuilder commands) {
		return addRecoveryCommand(commands, EXECUTE_COMMAND + ECHO);
	}

	private static StringBuilder addRecoveryText(StringBuilder commands, String text) {
		return addRecoveryCommand(commands, EXECUTE_COMMAND + ECHO + doubleQuoted(text));
	}

	private static void addSUCommand(List<String> commands, String command) {
		addSUCommand(commands, command, true);
	}

	private static void addSUCommand(List<String> commands, String command, boolean ignoreOutput) {
		commands.add(command + (ignoreOutput ? MUTE_OUTPUT : ""));
	}

	private static void executeSUCommands(List<String> commands, Context context) throws UpdateInstallationException {
		// Append "echo success" to the last command to check if the user actually granted root permissions.
		String lastCommand = commands.get(commands.size() - 1);
		lastCommand = lastCommand + AND + ECHO + singleQuoted(TEXT_SUCCESS);

		commands.set(commands.size() - 1, lastCommand);

		if (BuildConfig.DEBUG) {
			for (String command : commands) {
				logVerbose("UpdateInstaller", "Running SU command: " + command);
			}
		}

		List<String> commandsOutput = Shell.SU.run(commands);

		if (commandsOutput == null || commandsOutput.isEmpty() || !commandsOutput.get(0)
				.equals(TEXT_SUCCESS)) {
			if (context != null) {
				throw new UpdateInstallationException(context.getString(R.string.install_error_write_script_failed));
			} else {
				throw new UpdateInstallationException("Internal error");
			}
		}

		if (BuildConfig.DEBUG) {
			StringBuilder outputString = new StringBuilder();
			for (String output : commandsOutput) {
				outputString.append(output);
				outputString.append(System.getProperty("line.separator"));
			}

			logVerbose("UpdateInstaller", "Output of commands: " + outputString.toString());
		}
	}

	private static String singleQuoted(String in) {
		return "'" + in + "'";
	}

	private static String doubleQuoted(String in) {
		return "\"" + in + "\"";
	}

}
