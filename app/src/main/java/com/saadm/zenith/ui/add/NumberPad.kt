package com.saadm.zenith.ui.add

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithTheme

@Composable
fun NumPad() {
    val rowsContent = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "kys")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        rowsContent.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    Button(
                        onClick = {
                            // TODO: what them fingers do?
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            ,
                        colors = ButtonColors(
                            containerColor = Color(210, 210, 210),
                            contentColor = Color(20, 20, 20),
                            disabledContainerColor = Color(30, 30, 30),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNumPad() {
    ZenithTheme(darkTheme = true) {
        NumPad()
    }

}