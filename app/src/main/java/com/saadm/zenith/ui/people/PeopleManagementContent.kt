package com.saadm.zenith.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.domain.model.Payee
import kotlinx.coroutines.launch

/**
 * Composable for managing payees: listing, creating, editing, and deleting.
 * Payees are displayed as cards showing identity info and derived balance summaries.
 */
@Composable
fun PeopleManagementContent() {
    val context = LocalContext.current
    val appDatabase = remember(context.applicationContext) {
        DatabaseProvider.getInstance(context.applicationContext)
    }
    val payeeBalanceDao = remember(appDatabase) { appDatabase.payeeBalanceDao() }
    val payeeDao = remember(appDatabase) { appDatabase.payeeDao() }
    val coroutineScope = rememberCoroutineScope()

    val payeeBalances by payeeBalanceDao.observeAllActive().collectAsState(initial = emptyList())
    val payees = remember(payeeBalances) {
        payeeBalances.map { balance ->
            Payee(
                id = balance.id,
                name = balance.name,
                avatarUri = balance.avatarUri,
                phone = balance.phone,
                upiId = balance.upiId,
                createdAt = balance.createdAt,
                dueToAmount = balance.dueToAmount,
                dueFromAmount = balance.dueFromAmount,
                netBalance = balance.netBalanceAmount(),
                transactionCount = balance.transactionCount,
                lastInteractionAt = balance.lastInteractionAt
            )
        }
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingPayee by remember { mutableStateOf<Payee?>(null) }

    Text(
        text = "Add, edit, or remove people",
        style = MaterialTheme.typography.bodyMedium
    )

    Button(
        onClick = { showCreateDialog = true },
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text("Add person")
    }

    if (payees.isEmpty()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = "No people yet. Add someone to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Column {
                payees.forEachIndexed { index, payee ->
//					PayeeRow(
//						payee = payee,
//						onEditClick = { editingPayee = payee },
//						onDeleteClick = {
//							coroutineScope.launch {
//								payeeDao.softDelete(payee.id)
//							}
//						}
//					)
//					if (index != payees.lastIndex) {
//						HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
//					}
                    PayeeCard(
                        payee = payee,
                        onEditClick = { editingPayee = payee },
                        onDeleteClick = {
                            coroutineScope.launch {
                                payeeDao.softDelete(payee.id)
                            }
                        }
                    )
                    if (index != payees.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PayeeEditorDialog(
            title = "Add person",
            onDismiss = { showCreateDialog = false },
            onSave = { name, phone, upiId, avatarUri ->
                coroutineScope.launch {
                    val newPayee = PayeeEntity(
                        name = name,
                        phone = phone,
                        upiId = upiId,
                        avatarUri = avatarUri,
                        createdAt = System.currentTimeMillis(),
                        isDeleted = false
                    )
                    payeeDao.upsert(newPayee)
                    showCreateDialog = false
                }
            }
        )
    }

    editingPayee?.let { payee ->
        PayeeEditorDialog(
            title = "Edit person",
            initialName = payee.name,
            initialPhone = payee.phone,
            initialUpiId = payee.upiId,
            initialAvatarUri = payee.avatarUri,
            onDismiss = { editingPayee = null },
            onSave = { name, phone, upiId, avatarUri ->
                coroutineScope.launch {
                    val updated = PayeeEntity(
                        id = payee.id,
                        name = name,
                        phone = phone,
                        upiId = upiId,
                        avatarUri = avatarUri,
                        createdAt = payee.createdAt,
                        isDeleted = false
                    )
                    payeeDao.update(updated)
                    editingPayee = null
                }
            }
        )
    }
}

@Composable
private fun PayeeRow(
    payee: Payee,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payee.name,
                style = MaterialTheme.typography.bodyLarge
            )

            // Show secondary info: phone, UPI, or interaction count
            val secondaryText = when {
                payee.phone != null -> payee.phone!!
                payee.upiId != null -> payee.upiId!!
                payee.transactionCount > 0 -> "${payee.transactionCount} transactions"
                else -> "No contact info"
            }

            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (payee.hasActiveBalance()) {
                Spacer(modifier = Modifier.size(4.dp))
                val balanceColor = when {
                    payee.netBalance > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = balanceColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = payee.balanceText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = balanceColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit ${payee.name}"
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete ${payee.name}"
                )
            }
        }
    }
}


@Composable
private fun PayeeCard(
    payee: Payee,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            )
    ) {
        Column(
            modifier = Modifier
                .clickable { onEditClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = payee.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    )
                    {
                        Box(
                            modifier = Modifier
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        MaterialTheme.colorScheme.tertiary.copy(0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .zIndex(0f)
                                    .align(Alignment.Center)
                            )

                            // Owed amount
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(start = 4.dp),
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = ""
                                )
                                Text(
                                    text = payee.formatCompact(payee.netBalance),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.StartEllipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .padding(10.dp)
                                )
                            }

                        }
                        Box(
                            modifier = Modifier
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .zIndex(0f)
                            )
                            Row(
                                modifier = Modifier,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(
                                    modifier = Modifier
                                        .padding(start = 4.dp),
                                    imageVector = Icons.Default.ArrowOutward,
                                    contentDescription = ""
                                )
                                Text(
                                    text = payee.formatCompact(payee.dueToAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.MiddleEllipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .padding(10.dp)
                                )
                            }

                        }

                        // Transaction count
                        Box{
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Color.Blue.copy(0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .zIndex(0f)
                            )
                            Row(
                                modifier = Modifier
                                    .zIndex(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(start = 4.dp),
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Transactions"
                                )
                                Text(
                                    text = "${payee.transactionCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.StartEllipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(10.dp)
                                )
                            }

                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .background(
                            MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .fillMaxHeight()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (payee.hasActiveBalance()) {
                        Text(
                            text = payee.balanceText(),
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                payee.netBalance < 0 -> MaterialTheme.colorScheme.error
                                else -> Color.Green
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                    else {
                        Text(
                            text = "settled",
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                }

            }
        }
    }
}

@Preview
@Composable
private fun PreviewPayeeCard() {
    PayeeCard(
        payee = Payee(
            id = 1,
            name = "Nimki",
            avatarUri = null,
            phone = "+91 9877669696",
            upiId = "hellomamacita@upi",
            createdAt = System.currentTimeMillis(),
            dueToAmount = 2500000,
            dueFromAmount = 1250000,
            transactionCount = 24
        ),
        onEditClick = { },
        onDeleteClick = { }
    )
}
