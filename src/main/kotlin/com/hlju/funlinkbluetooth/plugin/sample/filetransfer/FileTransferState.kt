package com.hlju.funlinkbluetooth.plugin.sample.filetransfer

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

data class IncomingFileMeta(
    val endpointId: String,
    val payloadId: Long,
    val fileName: String,
    val uri: android.net.Uri?
)

class FileTransferState {
    val incomingFiles = mutableMapOf<Long, IncomingFileMeta>()
    val currentTransferProgress = mutableFloatStateOf(0f)
    val currentTransferLabel = mutableStateOf("")
}
