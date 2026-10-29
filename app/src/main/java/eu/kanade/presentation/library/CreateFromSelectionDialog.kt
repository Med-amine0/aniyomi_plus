package eu.kanade.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks how the current selection should be grouped.
 *
 * Both options run the same category creation; the difference is purely how the resulting
 * category is drawn when it is opened.
 */
@Composable
fun CreateFromSelectionDialog(
    onDismissRequest: () -> Unit,
    onCreateCategory: () -> Unit,
    onCreateMerged: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.create_from_selection_title))
        },
        text = {
            Column {
                CreateFromSelectionOption(
                    title = stringResource(MR.strings.create_from_selection_category),
                    summary = stringResource(MR.strings.create_from_selection_category_summary),
                    onClick = {
                        onDismissRequest()
                        onCreateCategory()
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                CreateFromSelectionOption(
                    title = stringResource(MR.strings.create_from_selection_merge),
                    summary = stringResource(MR.strings.create_from_selection_merge_summary),
                    onClick = {
                        onDismissRequest()
                        onCreateMerged()
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun CreateFromSelectionOption(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
