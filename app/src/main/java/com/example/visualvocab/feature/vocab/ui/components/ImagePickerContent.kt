package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.visualvocab.ui.theme.CreatorPurple
import com.example.visualvocab.ui.theme.DiscoveryMint
import com.example.visualvocab.ui.theme.ExplorerBlue
import com.example.visualvocab.ui.theme.ExplorerBlueDark
import com.example.visualvocab.ui.theme.NightInk
import com.example.visualvocab.ui.theme.NightSurface
import com.example.visualvocab.ui.theme.NightText
import com.example.visualvocab.ui.theme.QuestGold

// this is what you see when you haven't picked a photo yet. it has big buttons to open the gallery.
@Composable
fun ImagePickerContent(
    onSelectImage: () -> Unit,
    onOpenCreatorStudio: (() -> Unit)? = null,
    targetWord: String? = null,
    isTeachMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NightInk, NightSurface)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isTeachMode) {
                        CreatorPurple.copy(alpha = 0.22f)
                    } else {
                        ExplorerBlue.copy(alpha = 0.20f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isTeachMode) Icons.Rounded.Collections else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isTeachMode) Color(0xFFD7C8FF) else QuestGold,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isTeachMode) "CREATOR STUDIO" else "NEW DISCOVERY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NightText
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                ViviMascot(
                    pose = if (isTeachMode) ViviPose.CREATOR else ViviPose.SCANNING,
                    modifier = Modifier.size(154.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isTeachMode) "Teach vivi something new" else "What can you discover?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NightText,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = when {
                        isTeachMode && !targetWord.isNullOrBlank() -> {
                            "Choose a clear photo containing $targetWord, then confirm a detection or draw a new box."
                        }
                        isTeachMode -> {
                            "Choose a clear photo, then confirm detected objects or draw your own boxes."
                        }
                        else -> {
                            "Choose a photo of your surroundings. Vivi will highlight objects you can turn into vocabulary."
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // the big main button to open the photo picker.
                Button(
                    onClick = onSelectImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTeachMode) CreatorPurple else ExplorerBlue
                    ),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(imageVector = Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.size(9.dp))
                    Text(
                        text = "Choose a photo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // if we are in learning mode, show a link to the teaching mode.
                if (!isTeachMode) {
                    onOpenCreatorStudio?.let { openCreatorStudio ->
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = openCreatorStudio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = Color(0xFFD7C8FF)
                            )
                            Spacer(Modifier.size(9.dp))
                            Text(
                                text = "Open Creator Studio",
                                fontWeight = FontWeight.Bold,
                                color = NightText
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = DiscoveryMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tip: use a bright, clear photo with a few visible objects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.62f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
