package com.saadm.zenith.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithTheme

@Composable
fun PeopleScreen(onNavigateToPayee: (Long) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use the full management content which lists payees; clicking a payee will invoke the
        // provided navigation callback with the payee id so the caller can navigate by id.
        PeopleManagementContent(onPayeeClick = onNavigateToPayee)
    }
}

@Composable
fun PeopleMiniCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Most interacted with")
        Row() {
            PeopleMiniCardIcon("Khushi")
            PeopleMiniCardIcon("Priyan")
            PeopleMiniCardIcon("Aryan")
        }
    }
}

@Composable
fun PeopleMiniCardIcon(name: String = "") {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .drawBehind {
                    drawCircle(color = androidx.compose.ui.graphics.Color.LightGray)
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewPeopleMiniCard() {
    ZenithTheme {
        PeopleMiniCard()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPeopleScreen() {
    ZenithTheme {
        PeopleScreen()
    }
}
