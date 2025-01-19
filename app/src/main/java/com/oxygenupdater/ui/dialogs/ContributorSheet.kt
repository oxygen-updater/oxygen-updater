package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.ui.common.CheckboxText
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
            .testTag(BottomSheet_ContentTestTag)
    )

    /**
     * Show enrollment UI only if [confirm] was passed in, and if
     * device is rooted with the OS being >= Android 10/Q.
     */
    if (confirm != null && ContributorUtils.isAtLeastQAndPossiblyRooted) {
        ContributorSheetEnroll(hide = hide, confirm = confirm)
    }
}

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

    val context = LocalContext.current
    SheetButtons(
        dismissResId = android.R.string.cancel,
        onDismiss = hide,
        confirmIcon = Icons.Rounded.CheckCircleOutline,
        confirmResId = R.string.contribute_save,
        onConfirm = {
            if (contribute) hasRootAccess {
                if (it) {
                    confirm(true); hide()
                } else context.showToast(R.string.contribute_allow_storage)
            } else {
                confirm(false); hide()
            }
        },
    )
}

@PreviewThemes
@Composable
fun PreviewContributorSheet() = PreviewModalBottomSheet {
    ContributorSheet(hide = {}, confirm = {})
}
