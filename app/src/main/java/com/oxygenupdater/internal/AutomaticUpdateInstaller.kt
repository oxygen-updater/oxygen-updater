package com.oxygenupdater.internal

import android.content.Context
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.exceptions.UpdateInstallationException
import com.oxygenupdater.utils.Logger.logVerbose
import eu.chainfire.libsuperuser.Shell
import java.util.*

@Suppress("unused")
object AutomaticUpdateInstaller {

    private const val INSTALL_SCRIPT_NON_AB_DIRECTORY = "/cache/recovery"
    private const val INSTALL_SCRIPT_AB_DIRECTORY = "/data/cache/recovery"
    private const val INSTALL_SCRIPT_PATH_SUFFIX = "/openrecoveryscript"

    // Open recovery script (http://wiki.rootzwiki.com/OpenRecoveryScript) commands
    private const val BACKUP = "backup "
    private const val RESTORE = "restore "
    private const val INSTALL = "install "
    private const val WIPE = "wipe "
    private const val MOUNT = "mount "
    private const val UNMOUNT = "unmount "
    private const val SET_VARIABLE = "set "
    private const val MAKE_DIRECTORY = "mkdir "
    private const val EXECUTE_COMMAND = "cmd "

    // Backup partitions
    private const val SYSTEM_PARTITION = "S"
    private const val BOOT_PARTITION = "B"
    private const val DATA_PARTITION = "D"
    private const val CACHE_PARTITION = "C"
    private const val RECOVERY_PARTITION = "R"
    private const val SPECIAL_PARTITION_1 = "1"
    private const val SPECIAL_PARTITION_2 = "2"
    private const val SPECIAL_PARTITION_3 = "3"
    private const val ANDROID_SECURE_PARTITION = "A"
    private const val SD_EXT_PARTITION = "E"

    // Custom commands
    private const val ECHO = "echo "
    private const val REBOOT = "reboot"
    private const val REBOOT_RECOVERY = "$REBOOT recovery"
    private const val MUTE_OUTPUT = " > /dev/null"
    private const val AND = " && "
    private const val CACHE = "cache"
    private const val TEXT_SUCCESS = "success"

    @Throws(UpdateInstallationException::class, InterruptedException::class)
    fun installUpdate(
        context: Context,
        isAbPartitionLayout: Boolean,
        downloadPath: String,
        additionalZipFilePath: String?,
        backup: Boolean,
        wipeCachePartition: Boolean,
        rebootDevice: Boolean
    ) = StringBuilder().let { recoveryCommands ->
        // Build the installation script.
        // Print the banner. Note: Current TWRP (2019-04-03) does not support ECHO command :(
        addRecoveryText(recoveryCommands, " -------------------------")
        addRecoveryText(recoveryCommands, "| (↑) OXYGEN UPDATER      |")
        addRecoveryText(
            recoveryCommands,
            "|     ${context.getString(R.string.install_recovery_installer_title)}  |"
        )
        addRecoveryText(recoveryCommands, " -------------------------")

        // Print the "Thank you" message.
        addRecoveryNewLine(recoveryCommands)
        addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_thank_you))

        // Backup the device if requested.
        if (backup) {
            addRecoveryNewLine(recoveryCommands)
            addRecoveryText(
                recoveryCommands,
                context.getString(R.string.install_recovery_backing_up)
            )
            addRecoveryCommand(
                recoveryCommands,
                BACKUP + SYSTEM_PARTITION + BOOT_PARTITION + DATA_PARTITION + CACHE_PARTITION + RECOVERY_PARTITION + ANDROID_SECURE_PARTITION
            )
        }

        // Install the update.
        addRecoveryNewLine(recoveryCommands)
        addRecoveryText(
            recoveryCommands,
            context.getString(R.string.install_recovery_installing_update)
        )
        addRecoveryCommand(recoveryCommands, INSTALL + downloadPath)

        // Install the additional zip if it is selected.
        if (!additionalZipFilePath.isNullOrEmpty()) {
            addRecoveryNewLine(recoveryCommands)
            addRecoveryText(
                recoveryCommands,
                context.getString(R.string.install_recovery_installing_additional_zip)
            )
            addRecoveryCommand(recoveryCommands, INSTALL + additionalZipFilePath)
        }

        // Clear the cache partition if requested.
        if (wipeCachePartition) {
            addRecoveryNewLine(recoveryCommands)
            addRecoveryText(
                recoveryCommands,
                context.getString(R.string.install_recovery_clearing_cache)
            )
            addRecoveryCommand(recoveryCommands, WIPE + CACHE)
        }

        // Reboot the device if requested.
        if (rebootDevice) {
            addRecoveryNewLine(recoveryCommands)
            addRecoveryText(recoveryCommands, context.getString(R.string.install_recovery_done))
            addRecoveryCommand(recoveryCommands, EXECUTE_COMMAND + REBOOT)
        }

        Thread.sleep(1000)

        val suCommands: MutableList<String> = ArrayList()
        val installScriptDirectory = if (isAbPartitionLayout) INSTALL_SCRIPT_AB_DIRECTORY else INSTALL_SCRIPT_NON_AB_DIRECTORY
        val installScriptPath = installScriptDirectory + INSTALL_SCRIPT_PATH_SUFFIX

        // Create the install script directory (if not exists) by calling "su mkdir -p (/data)/cache/recovery".
        addSUCommand(suCommands, "mkdir -p $installScriptDirectory")

        // Write the install script by calling "su echo '<commands>' > (/data)/cache/recovery/openrecoveryscript"
        addSUCommand(
            suCommands,
            ECHO + singleQuoted(recoveryCommands.toString()) + " > " + installScriptPath,
            false
        )

        // Reboot the device by calling "su reboot recovery".
        addSUCommand(suCommands, REBOOT_RECOVERY)

        // Execute all commands at once to prevent unnecessary SU popups.
        executeSUCommands(suCommands, context)
        // The device will now restart to install the update.
    }

    private fun addRecoveryCommand(builder: StringBuilder, content: String) = builder
        .append(content)
        .append(System.getProperty("line.separator"))

    private fun addRecoveryNewLine(commands: StringBuilder) = addRecoveryCommand(
        commands,
        EXECUTE_COMMAND + ECHO
    )

    private fun addRecoveryText(commands: StringBuilder, text: String) = addRecoveryCommand(
        commands,
        EXECUTE_COMMAND + ECHO + doubleQuoted(text)
    )

    private fun addSUCommand(commands: MutableList<String>, command: String, ignoreOutput: Boolean = true) = commands.add(
        command + if (ignoreOutput) MUTE_OUTPUT else ""
    )

    @Throws(UpdateInstallationException::class)
    private fun executeSUCommands(commands: MutableList<String>, context: Context?) {
        // Append "echo success" to the last command to check if the user actually granted root permissions.
        val lastCommand = commands[commands.size - 1] + AND + ECHO + singleQuoted(TEXT_SUCCESS)

        commands[commands.size - 1] = lastCommand

        if (BuildConfig.DEBUG) {
            commands.forEach { logVerbose("UpdateInstaller", "Running SU command: $it") }
        }

        @Suppress("DEPRECATION") val commandsOutput = Shell.SU.run(commands)

        if (commandsOutput.isNullOrEmpty() || commandsOutput[0] != TEXT_SUCCESS) {
            throw UpdateInstallationException(
                context?.getString(R.string.install_error_write_script_failed) ?: "Internal error"
            )
        }

        if (BuildConfig.DEBUG) {
            val outputString = StringBuilder()

            commandsOutput.forEach { outputString.append(it).append(System.getProperty("line.separator")) }

            logVerbose("UpdateInstaller", "Output of commands: $outputString")
        }
    }

    private fun singleQuoted(string: String) = "'$string'"

    private fun doubleQuoted(string: String) = "\"$string\""
}
