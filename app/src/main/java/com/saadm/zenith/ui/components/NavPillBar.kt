package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController


sealed class NavRoute(val route: String) {
    object Home : NavRoute("home")
    object Transactions : NavRoute("insights")
    object Settings : NavRoute("settings")
    object People: NavRoute("people")
}

data class NavItem(
    val route: NavRoute,
    val label: String,
    val icon: ImageVector,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector
)

data class PrimaryActionSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

val navItems = listOf(
    NavItem(
        NavRoute.Home, "Home",
        Icons.Rounded.Home,
        iconUnselected = Icons.Outlined.Home,
        iconSelected = Icons.Rounded.Home
    ),
    NavItem(
        NavRoute.Transactions, "Insights",
        Icons.Rounded.Analytics,
        iconUnselected = Icons.Outlined.PieChart,
        iconSelected = Icons.Rounded.PieChart
    ),
    NavItem(
        NavRoute.Settings, "Settings",
        Icons.Rounded.Settings,
        iconUnselected = Icons.Outlined.Settings,
        iconSelected = Icons.Rounded.Settings
    )
)

@Composable
fun NavPillBar(
    navController: NavHostController,
    currentRoute: String,
    primaryAction: PrimaryActionSpec,
    modifier: Modifier = Modifier) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    NavPill(
                        label = item.label,
                        icon = if (currentRoute == item.route.route) item.iconSelected else item.iconUnselected,
                        isSelected = currentRoute == item.route.route,
                        onClick = {
                            navController.navigate(item.route.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        PrimaryActionButton(
            icon = primaryAction.icon,
            contentDescription = primaryAction.contentDescription,
            onClick = primaryAction.onClick,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun NavPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(50.dp)
            )
            .clip(RoundedCornerShape(50.dp))
            .clickable(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNavPillBar() {
    MaterialTheme {
        NavPillBar(
            navController = rememberNavController(),
            currentRoute = NavRoute.Home.route,
            primaryAction = PrimaryActionSpec(
                icon = Icons.Rounded.Add,
                contentDescription = "Add",
                onClick = {}
            )
        )
    }
}
