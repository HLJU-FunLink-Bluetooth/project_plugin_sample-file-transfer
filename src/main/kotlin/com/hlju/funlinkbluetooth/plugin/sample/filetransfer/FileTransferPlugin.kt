package com.hlju.funlinkbluetooth.plugin.sample.filetransfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hlju.funlinkbluetooth.core.plugin.api.FunLinkPayload
import com.hlju.funlinkbluetooth.core.plugin.api.GamePlugin
import com.hlju.funlinkbluetooth.core.plugin.api.PluginManifest
import com.hlju.funlinkbluetooth.core.plugin.api.TransferStatus
import com.hlju.funlinkbluetooth.core.plugin.api.TransferUpdate
import com.hlju.funlinkbluetooth.core.plugin.api.support.OutgoingTransferManager
import com.hlju.funlinkbluetooth.core.plugin.api.support.PayloadHelper
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.io.FileOutputStream

class FileTransferPlugin : GamePlugin(PluginManifest(id = "demo_file", name = "文件传输")) {

    private val state = FileTransferState()
    private val transferManager = OutgoingTransferManager()
    private var appContext: Context? = null

    override fun onPayloadReceived(endpointId: String, payload: FunLinkPayload) {
        when (payload) {
            is FunLinkPayload.File -> {
                val fileLabel = resolveDisplayName(payload.fileName)
                state.incomingFiles[payload.id] = IncomingFileMeta(
                    endpointId = endpointId,
                    payloadId = payload.id,
                    fileName = fileLabel,
                    uri = payload.uri
                )
                appendLog("[$endpointId] 收到文件载荷: $fileLabel (id=${payload.id})")
            }
            else -> Unit
        }
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: TransferUpdate) {
        val payloadId = update.payloadId

        if (update.status == TransferStatus.IN_PROGRESS) {
            val checkpoint = PayloadHelper.calcProgressCheckpoint(update)
            if (checkpoint != null) {
                val old = transferManager.progressCheckpoints[payloadId]
                if (old != checkpoint) {
                    transferManager.progressCheckpoints[payloadId] = checkpoint
                    state.currentTransferProgress.floatValue = checkpoint / 10f
                    val label = transferManager.outgoingTrackers[payloadId]?.label
                        ?: state.incomingFiles[payloadId]?.fileName
                        ?: "payload#$payloadId"
                    state.currentTransferLabel.value = label
                    appendLog("[$endpointId] $label 传输进度: ${checkpoint * 10}%")
                }
            }
        }

        if (update.status == TransferStatus.SUCCESS ||
            update.status == TransferStatus.FAILURE ||
            update.status == TransferStatus.CANCELED
        ) {
            val incoming = state.incomingFiles.remove(payloadId)
            if (incoming != null && update.status == TransferStatus.SUCCESS) {
                persistIncomingFile(incoming)
            }

            transferManager.handleTransferUpdate(
                endpointId = endpointId,
                update = update,
                appendLog = { appendLog(it) },
                onTerminalStatus = { _, _ -> },
                resolveLabel = {
                    transferManager.outgoingTrackers[it]?.label
                        ?: incoming?.fileName
                        ?: "payload#$it"
                }
            )

            state.currentTransferProgress.floatValue = 0f
            state.currentTransferLabel.value = ""
        }
    }

    override fun onEndpointDisconnected(endpointId: String) {
        appendLog("--- 端点 $endpointId 已断开 ---")
    }

    fun sendDocumentPayload(context: Context, uri: Uri) {
        appContext = context.applicationContext
        val displayName = resolveDisplayName(context, uri)
        val payload = FunLinkPayload.File(uri = uri, fileName = displayName)

        PayloadHelper.sendPayloadToConnectedEndpoints(
            hostBindings = hostBindings,
            payload = payload,
            label = "文件 $displayName",
            shouldCloseWhenFinished = false,
            outgoingTrackers = transferManager.outgoingTrackers,
            lastOutgoingPayloadId = transferManager.lastOutgoingPayloadId,
            appendLog = { appendLog(it) }
        )
    }

    fun cancelLastPayload() {
        PayloadHelper.cancelLastPayload(
            hostBindings = hostBindings,
            lastOutgoingPayloadId = transferManager.lastOutgoingPayloadId,
            appendLog = { appendLog(it) }
        )
    }

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
    }

    private fun persistIncomingFile(meta: IncomingFileMeta) {
        val context = appContext ?: return
        val uri = meta.uri ?: return

        val safeName = meta.fileName
            .replace('/', '_')
            .replace('\\', '_')
            .ifBlank { "incoming_${meta.payloadId}" }

        val outputDir = File(context.cacheDir, "funlink_incoming")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, safeName)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            appendLog("[${meta.endpointId}] 文件接收完成，已保存到 ${outputFile.absolutePath}")
        } catch (exception: Exception) {
            appendLog("[${meta.endpointId}] 文件保存失败：${exception.localizedMessage ?: "未知错误"}")
        }
    }

    private fun resolveDisplayName(name: String?): String {
        return name?.takeIf { it.isNotBlank() } ?: "shared_${System.currentTimeMillis()}"
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        return name ?: "shared_${System.currentTimeMillis()}"
    }

    @Composable
    override fun AppIcon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Add,
            contentDescription = name,
            modifier = modifier,
            tint = MiuixTheme.colorScheme.primary
        )
    }

    @Composable
    override fun Content() {
        FileTransferUi(
            plugin = this,
            state = state,
            onFileSelected = { context, uri -> sendDocumentPayload(context, uri) },
            onCancelClick = { cancelLastPayload() },
            onContextAvailable = { context -> setAppContext(context) }
        )
    }
}
