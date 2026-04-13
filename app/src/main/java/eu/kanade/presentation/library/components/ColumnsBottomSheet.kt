package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnsBottomSheet(
    currentColumns: Int,
    onColumnsChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var columns by remember { mutableIntStateOf(currentColumns) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 32.dp,
            ),
        ) {
            SliderItem(
                value = columns,
                valueRange = 1..10,
                label = stringResource(AYMR.strings.pref_entry_columns),
                valueText = if (columns == 0) "Auto" else columns.toString(),
                onChange = {
                    columns = it
                    onColumnsChange(it)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
