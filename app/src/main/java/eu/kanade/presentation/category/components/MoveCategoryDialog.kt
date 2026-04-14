package eu.kanade.presentation.category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.domain.category.model.Category

@Composable
fun MoveCategoryDialog(
    category: Category,
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onMove: (Category) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val otherCategories = categories.filter { it.id != category.id }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Move \"${category.name}\"",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Select destination category:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (otherCategories.isEmpty()) {
                    Text(
                        text = "No other categories available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory == null) Color(0xFF333333)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedCategory = null }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF3B82F6),
                                    ),
                                )
                                Text(
                                    text = "No parent (Root level)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                            }
                        }

                        items(otherCategories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory?.id == cat.id) Color(0xFF333333)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selectedCategory?.id == cat.id,
                                    onClick = { selectedCategory = cat },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF3B82F6),
                                    ),
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategory?.let { onMove(it) }
                    onDismissRequest()
                },
                enabled = true,
            ) {
                Text(
                    text = "Move",
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF888888),
                )
            }
        },
    )
}
