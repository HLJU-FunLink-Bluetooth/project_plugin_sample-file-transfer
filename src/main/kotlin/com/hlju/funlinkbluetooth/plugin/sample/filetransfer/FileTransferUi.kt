package com.hlju.funlinkbluetooth.plugin.sample.filetransfer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hlju.funlinkbluetooth.core.designsystem.token.Spacing
import com.hlju.funlinkbluetooth.core.plugin.api.support.PluginPageLayout
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FileTransferUi(
    plugin: FileTransferPlugin,
    state: FileTransferState,
    onFileSelected: (Context, Uri) -> Unit,
    onCancelClick: () -> Unit,
    onContextAvailable: (Context) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(context) {
        onContextAvailable(context)
        onDispose {}
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onFileSelected(context, uri)
        }
    }

    PluginPageLayout(
        title = "文件传输",
        eventLogs = plugin.eventLogs,
        emptyMessage = "选择文件开始传输"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text("选择文件")
            }

            TextButton(
                text = "取消发送",
                onClick = { onCancelClick() },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.currentTransferLabel.value.isNotBlank()) {
            Text(
                text = "${state.currentTransferLabel.value}: ${(state.currentTransferProgress.floatValue * 100).toInt()}%",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary
            )
        }
    }
}
