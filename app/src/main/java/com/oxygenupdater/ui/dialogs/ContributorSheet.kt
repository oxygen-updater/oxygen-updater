package com.oxygenupdater.ui.dialogs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.hasRootAccess

@Composable
fun ColumnScope.ContributorSheet(
    hide: () -> Unit,
    confirm: ((Boolean) -> Unit)? = null,
) {
    SheetHeader(R.string.contribute_title)

    Text(
        text = stringResource(R.string.contribute_explanation),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
    )

    /**
     * Show enrollment UI only if [confirm] was passed in, and if
     * device is rooted with the OS being >= Android 10/Q.
     */
    if (confirm != null && ContributorUtils.isAtLeastQAndPossiblyRooted) {
        ContributorSheetEnroll(hide = hide, confirm = confirm)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun ContributorSheetEnroll(
    hide: () -> Unit,
    confirm: (Boolean) -> Unit,
) {
    var contribute by rememberSaveableState("contribute", true)
    CheckboxText(
        checked = contribute,
        onCheckedChange = { contribute = it },
        textResId = R.string.contribute_agree,
        modifier = Modifier.padding(end = 16.dp)
    )

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifierMaxWidth.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        TextButton(
            onClick = hide,
            colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(stringResource(android.R.string.cancel))
        }

        val context = LocalContext.current
        OutlinedIconButton(
            onClick = {
                if (contribute) hasRootAccess {
                    if (it) {
                        confirm(true)
                        hide()
                    } else context.showToast(R.string.contribute_allow_storage)
                } else {
                    confirm(false)
                    hide()
                }
            },
            icon = Icons.Rounded.CheckCircleOutline,
            textResId = R.string.contribute_save,
        )
    }
}

@PreviewThemes
@Composable
fun PreviewContributorSheet() = PreviewModalBottomSheet {
    ContributorSheet(hide = {}, confirm = {})
}
