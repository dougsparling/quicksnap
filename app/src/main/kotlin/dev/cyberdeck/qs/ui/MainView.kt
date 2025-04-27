package dev.cyberdeck.qs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cyberdeck.qs.R
import dev.cyberdeck.qs.common.prepStorageDir
import kotlinx.coroutines.launch

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
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(text = stringResource(R.string.home)) },
                    selected = current == NavState.HOME,
                    onClick = {
                        current = NavState.HOME
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                NavigationDrawerItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(text = stringResource(R.string.stats)) },
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
                    label = { Text(text = stringResource(R.string.implode)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            context.prepStorageDir().deleteRecursively()
                        }
                    }
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.app_name)) }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Show drawer") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "") },
                    onClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                )
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
                }
            }
        }
    }
}


enum class NavState {
    HOME,
    STATS,
}