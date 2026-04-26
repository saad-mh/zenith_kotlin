package com.saadm.zenith.ui.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.ui.components.TxnRow
import kotlin.time.ExperimentalTime


@Composable
fun TxnInsightCard(
    viewModel: TxnListViewModel = hiltViewModel()
) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    TxnInsightCardContent(recentTransactions = recentTransactions)
}

@Composable
fun TxnInsightCardContent(
    recentTransactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (recentTransactions.isEmpty()) {
                Text(
                    text = "Add transactions son",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(16.dp)
                )
            }
            recentTransactions.forEach { txn ->
                TxnRow(
                    amount = txn.amount,
                    type = txn.type,
                    transactedAt = txn.transactedAt,
                    categoryId = txn.categoryId,
                    note = txn.note,
                    currency = "INR",
                    onClick = {},
                    timeFormatter = { txn.transactedAt.toString() },
                    typeLabelResolver = { it.name },
                    categoryEmojiResolver = { "💰" },
                    categoryLabelResolver = { "${txn.categoryId}" }
                )
            }
        }
    }
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun PreviewTxnInsightCard() {
    TxnInsightCard(

    )
}
