@file:OptIn(ExperimentalMaterial3Api::class)

package com.ansar.rotitrack.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ansar.rotitrack.ui.screens.CalendarScreen
import com.ansar.rotitrack.ui.screens.DueScreen
import com.ansar.rotitrack.ui.screens.OrderScreen
import com.ansar.rotitrack.ui.screens.RatesScreen
import kotlinx.coroutines.launch

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    ORDER("order", "Order Roti", Icons.Rounded.RestaurantMenu),
    CALENDAR("calendar", "Track By Calendar", Icons.Rounded.CalendarMonth),
    RATES("rates", "Edit Rates", Icons.Rounded.CurrencyRupee),
    DUE("due", "Total Due", Icons.Rounded.AccountBalanceWallet)
}

@Composable
fun RotiAppShell(viewModel: RotiViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.ORDER.route

    val today by viewModel.today.collectAsStateWithLifecycle()
    val homeDay by viewModel.homeDay.collectAsStateWithLifecycle()
    val monthState by viewModel.monthState.collectAsStateWithLifecycle()
    val ratesState by viewModel.ratesState.collectAsStateWithLifecycle()
    val dueState by viewModel.dueState.collectAsStateWithLifecycle()
    val rateBook by viewModel.rateBook.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Navigation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider()
                Destination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = destination.route == currentRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (destination.route != currentRoute) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            // The home screen shows the date itself, as in the design.
                            text = if (currentRoute == Destination.ORDER.route) {
                                homeDay.date.format(DateFormats.dayWithName)
                            } else {
                                Destination.entries.firstOrNull { it.route == currentRoute }?.label ?: "Roti Track"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open navigation")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.ORDER.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    slideInHorizontally(tween(320)) { width -> width / 4 } + fadeIn(tween(320))
                },
                exitTransition = {
                    slideOutHorizontally(tween(320)) { width -> -width / 4 } + fadeOut(tween(220))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(320)) { width -> -width / 4 } + fadeIn(tween(320))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(320)) { width -> width / 4 } + fadeOut(tween(220))
                }
            ) {
                composable(Destination.ORDER.route) {
                    OrderScreen(
                        day = homeDay,
                        onAddEntry = { item, count ->
                            viewModel.addEntry(homeDay.date, item, count)
                        },
                        onDeleteEntry = viewModel::deleteEntry
                    )
                }
                composable(Destination.CALENDAR.route) {
                    CalendarScreen(
                        state = monthState,
                        today = today,
                        rateBook = rateBook,
                        onShiftMonth = viewModel::shiftMonth,
                        onAddEntry = viewModel::addEntry,
                        onDeleteEntry = viewModel::deleteEntry
                    )
                }
                composable(Destination.RATES.route) {
                    RatesScreen(
                        state = ratesState,
                        onSetRate = viewModel::setRate,
                        onDeleteRate = viewModel::deleteRate
                    )
                }
                composable(Destination.DUE.route) {
                    DueScreen(
                        state = dueState,
                        today = today,
                        onAddPayment = viewModel::addPayment,
                        onDeletePayment = viewModel::deletePayment
                    )
                }
            }
        }
    }
}
