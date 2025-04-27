package dev.cyberdeck.qs.ui

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.cyberdeck.qs.common.prepStorageDir
import java.io.File

@Composable
fun StatsView() {
    val context = LocalContext.current
    val dir = remember(context) { context.prepStorageDir() }
    var lastRefresh by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var size by remember { mutableLongStateOf(0L) }
    var count by remember { mutableIntStateOf(0) }
    val formatted = remember(size) { formatFileSize(context, size) }

    LaunchedEffect(lastRefresh) {
        val files = (dir.listFiles() ?: emptyArray()).filter { it.isFile }
        size = files.sumOf(File::length)
        count = files.size
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Text("Stats: $count files using $formatted")
        IconButton(
            onClick = {
                lastRefresh = System.currentTimeMillis()
            },
            enabled = true,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
            )
        }
    }




}