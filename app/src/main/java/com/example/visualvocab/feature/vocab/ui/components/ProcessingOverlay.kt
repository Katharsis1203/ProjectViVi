package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// a simple loading screen we show while vivi is thinking or saving things.
@Composable
fun ProcessingOverlay(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.scrim
                    .copy(alpha = 0.54f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.96f),
            tonalElevation = 4.dp,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 32.dp,
                    vertical = 26.dp
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = message,
                    style =
                        MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "This should only take a moment.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
