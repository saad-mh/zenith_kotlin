package com.saadm.zenith.ui.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.saadm.zenith.ui.theme.ZenithTheme

@Composable
fun PeopleScreen(onNavigateToPayee: (Long) -> Unit = {}, createRequests: Int = 0) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use the full management content which lists payees; clicking a payee will invoke the
        // provided navigation callback with the payee id so the caller can navigate by id.
        PeopleManagementContent(onPayeeClick = onNavigateToPayee, createRequests = createRequests)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPeopleScreen() {
    ZenithTheme {
        PeopleScreen()
    }
}
