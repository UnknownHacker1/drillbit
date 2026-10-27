package dev.teima.drillbit.ui.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.core.codec.shareLink
import dev.teima.drillbit.core.qr.QrCode
import dev.teima.drillbit.platform.textClipEntry
import dev.teima.drillbit.platform.webAppUrl
import kotlinx.coroutines.launch

/** QR code + link for the current show. Performers scan it and land in their dot book. */
@Composable
fun ShareDialog(
    code: String,
    performerCount: Int,
    onDismiss: () -> Unit,
) {
    val link = remember(code) { shareLink(webAppUrl(), code) }
    val qr = remember(link) { runCatching { QrCode.encodeText(link) }.getOrNull() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hand the show to your band") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "All $performerCount performers can scan this with any phone camera. " +
                        "The whole show is inside the code: no account, no server, works offline once loaded.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (qr != null) {
                    QrImage(qr, Modifier.widthIn(max = 440.dp).fillMaxWidth())
                    Text(
                        "QR version ${qr.version} · ${code.length} characters",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("This show is too big for one QR code; share the link instead.")
                }
                Text(link, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    clipboard.setClipEntry(textClipEntry(link))
                    copied = true
                }
            }) { Text(if (copied) "Copied" else "Copy link") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Draws a [QrCode] with the four-module quiet zone scanners expect. */
@Composable
fun QrImage(
    qr: QrCode,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(4.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val modules = qr.size + QUIET_ZONE * 2
            val cell = size.width / modules
            for (y in 0 until qr.size) {
                for (x in 0 until qr.size) {
                    if (qr[x, y]) {
                        drawRect(
                            Color.Black,
                            topLeft = Offset((x + QUIET_ZONE) * cell, (y + QUIET_ZONE) * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
        }
    }
}

/** Paste a link or code someone shared. */
@Composable
fun ImportDialog(
    onImport: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open a shared show") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Paste a Drillbit link or code") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { if (onImport(text)) onDismiss() }, enabled = text.isNotBlank()) { Text("Open") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val QUIET_ZONE = 4
