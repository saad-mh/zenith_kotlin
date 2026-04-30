package com.saadm.zenith.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.ui.components.TxnRow

@Composable
fun PayeeDetailsScreen(
    payeeId: Long,
    peopleViewModel: PeopleViewModel
) {
    val payee by peopleViewModel.observePayeeById(payeeId).collectAsState(initial = null)

    payee?.let {
        PayeeDetailsScreen(payee = it)
    } ?: Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No payee found")
    }
}

/**
 * Route wrapper used by NavHost that shows an explicit top app bar with a back action.
 * The composable displayed below is the same details UI which will fetch its own data
 * from the provided ViewModel using the payeeId.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayeeDetailsRoute(
    payeeId: Long,
    peopleViewModel: PeopleViewModel,
    onBack: () -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Payee") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PayeeDetailsScreen(payeeId = payeeId, peopleViewModel = peopleViewModel)
        }
    }
}

@Composable
fun PayeeDetailsScreen(
    payee: Payee
) {
    val txnList = payee.getAllTransactions()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        PayeeHeader(payee)

        BalanceCard(payee)

        ActionRow()

        TransactionSection(txnList)
    }
}

@Composable
fun PayeeHeader(payee: Payee) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (fallback to initials)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = payee.name.take(1),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = payee.name,
                style = MaterialTheme.typography.titleLarge
            )

            payee.phone?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            payee.upiId?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                text = "Last: ${
                    payee.getLastInteraction()?.let {
                        payee.formatRelativeTime(it)
                    } ?: "N/A"
                }",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun BalanceCard(payee: Payee) {
    val isPositive = payee.netBalance > 0
    val isNegative = payee.netBalance < 0

    val color = when {
        isPositive -> Color(0xFF2E7D32) // green
        isNegative -> Color(0xFFC62828) // red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = when {
                    isPositive -> "You will get"
                    isNegative -> "You owe"
                    else -> "All settled"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = payee.balanceText(),
                style = MaterialTheme.typography.headlineLarge,
                color = color
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Text(
                    text = "You lent: ₹${payee.formatCompact(payee.dueFromAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "You borrowed: ₹${payee.formatCompact(payee.dueToAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ActionRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        Button(onClick = { /* Add txn */ }) {
            Text("Add")
        }

        Button(onClick = { /* Settle */ }) {
            Text("Settle")
        }

        OutlinedButton(onClick = { /* Edit */ }) {
            Text("Edit")
        }
    }
}

@Composable
fun TransactionSection(txnList: List<TransactionEntity>) {

    Column(modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        if (txnList.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn {
                items(txnList) { txn ->
                    TxnRow(
                        amount = txn.amount,
                        type = txn.type,
                        transactedAt = txn.transactedAt,
                        categoryId = txn.categoryId,
                        note = txn.note,
                        currency = txn.currency
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(txn: TransactionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(txn.note?.ifEmpty { "Transaction" } ?: "Transaction")
            Text(
                Payee.formatRelativeTime(txn.transactedAt),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = txn.formatCompact(txn.amount),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
fun PreviewPayeeDetails() {
    PayeeDetailsScreen(
        Payee(
            id = 1,
            name = "Limca",
            avatarUri = null,
            phone = "+91 99999 11111",
            upiId = "limca@upi",
            createdAt = System.currentTimeMillis() - (60*60*24*7*100)
        )
    )
}