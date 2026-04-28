package com.saadm.zenith.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.people.PeopleMiniCard
import com.saadm.zenith.ui.theme.ZenithTheme
//import com.saadm.zenith.ui.transactions.TxnInsightCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Insights") },
                expandedHeight = 75.dp,
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
            )
        }
    ) { innerPadding ->
         LazyColumn(
             modifier = Modifier.padding(innerPadding),
             contentPadding = PaddingValues(16.dp),
             verticalArrangement = Arrangement.spacedBy(16.dp),
             horizontalAlignment = Alignment.CenterHorizontally
         ) {
             item {
                 InsightCard(
                     title = "Spending Overview"
//                     modifier = Modifier.fillMaxWidth(0.92f)
                 )
             }
             item {
                 InsightCard(
                     title = "Top Categories",
                 )
             }
             item {
                 InsightCard(
                     title = "Dues",
                 ) {
                     PeopleMiniCard()
                 }
             }
             item {
                 InsightCard(
                     title = "Recent Transactions"
                 ) {
                     // TODO: Change back when done
//                     TxnInsightCard()
                     Text(
                         text = "yuh"
                     )
                 }
             }
             item {
                 InsightCard(
                     title = "Savings Goals",
                 ) {
                     Text(text = "No active savings goals", style = MaterialTheme.typography.bodyMedium)
                     Text(text = "Set up your first goal", style = MaterialTheme.typography.bodySmall)
                 }
             }
         }
     }
}

@Composable
fun InsightCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Card(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInsightCard() {
    ZenithTheme {
        InsightCard(title = "Deez") {
            Text("This boutta be lit")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInsightsScreen() {
    ZenithTheme {
        InsightsScreen()
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun PreviewInsightsScreenDark() {
    ZenithTheme(darkTheme = true) {
        InsightsScreen()
    }
}
