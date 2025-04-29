package dev.cyberdeck.qs.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cyberdeck.qs.R
import dev.cyberdeck.qs.common.Settings
import dev.cyberdeck.qs.common.prepStorageDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(NavState.HOME) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.app_name),
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    label = { Text(text = stringResource(R.string.home)) },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    },
                    selected = current == NavState.HOME,
                    onClick = {
                        current = NavState.HOME
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    label = { Text(text = stringResource(R.string.stats)) },
                    icon = {
                        Icon(
                            Icons.Filled.InsertChart,
                            contentDescription = stringResource(R.string.home)
                        )
                    },
                    selected = current == NavState.STATS,
                    onClick = {
                        current = NavState.STATS
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                val context = LocalContext.current
                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    label = { Text(text = stringResource(R.string.implode)) },
                    icon = {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = stringResource(R.string.home)
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            val done = withContext(Dispatchers.IO) {
                                context.prepStorageDir().deleteRecursively()
                            }
                            drawerState.close()
                            Toast.makeText(
                                context,
                                context.getString(if (done) R.string.imploded else R.string.failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    label = { Text(text = stringResource(R.string.settings)) },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            current = NavState.SETTINGS
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    }
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = ""
                            )
                        }
                    },
                    title = { Text(text = stringResource(R.string.app_name)) }
                )
            },
            floatingActionButton = {

            }
        ) { insets ->
            Box(
                modifier = Modifier
                    .padding(insets)
                    .padding(16.dp)
            ) {
                when (current) {
                    NavState.HOME -> HomeView()
                    NavState.STATS -> StatsView()
                    NavState.SETTINGS -> SettingsView()
                }
            }
        }
    }
}

@Composable
fun SettingsView() {
    val context = LocalContext.current
    val settings = remember(context) { Settings.get(context) }
    val wideAngle by settings.wideAngle().collectAsState(false)
    val scope = rememberCoroutineScope()

    Column {
        Text("Wide Angle Lens")
        Switch(
            checked = wideAngle,
            onCheckedChange = {
                scope.launch { settings.toggleWideAngle() }
            }
        )
    }
}


enum class NavState {
    HOME,
    STATS,
    SETTINGS
}