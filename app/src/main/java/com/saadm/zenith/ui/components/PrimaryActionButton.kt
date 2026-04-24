package com.saadm.zenith.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryActionButton(
	icon: ImageVector,
	contentDescription: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	FloatingActionButton(
		onClick = onClick,
		modifier = modifier.size(56.dp),
		shape = CircleShape,
		containerColor = MaterialTheme.colorScheme.primary,
		contentColor = MaterialTheme.colorScheme.onPrimary,
		elevation = FloatingActionButtonDefaults.elevation(
			defaultElevation = 8.dp,
			pressedElevation = 12.dp
		),
		interactionSource = remember { MutableInteractionSource() }
	) {
		Icon(
			imageVector = icon,
			contentDescription = contentDescription
		)
	}
}
@Preview(showBackground = true)
@Composable
private fun PrimaryActionButtonPreview() {
	MaterialTheme {
		PrimaryActionButton(
			icon = Icons.Default.Add,
			contentDescription = "Add",
			onClick = {}
		)
	}
}
