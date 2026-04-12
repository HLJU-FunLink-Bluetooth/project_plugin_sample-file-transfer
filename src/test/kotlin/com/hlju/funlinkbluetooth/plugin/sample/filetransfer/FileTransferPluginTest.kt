package com.hlju.funlinkbluetooth.plugin.sample.filetransfer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.hlju.funlinkbluetooth.core.model.NearbyEndpointInfo
import com.hlju.funlinkbluetooth.core.plugin.api.FunLinkPayload
import com.hlju.funlinkbluetooth.core.plugin.api.PluginHostBindings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileTransferPluginTest {

    @Test
    fun sendDocumentPayload_doesNotOpenDescriptorDuringPreflight() {
        val plugin = FileTransferPlugin()
        val bindings = RecordingBindings()
        plugin.bind(bindings)

        val uri = Uri.parse("content://docs/file")
        val contentResolver = mockk<ContentResolver>()
        val cursor = mockk<Cursor>()
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } returns Unit
        every { contentResolver.query(uri, any(), null, null, null) } returns cursor

        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.contentResolver } returns contentResolver

        plugin.sendDocumentPayload(context, uri)

        verify(exactly = 0) { contentResolver.openFileDescriptor(any(), any()) }
        assertEquals(1, bindings.sentPayloads.size)
    }

    private class RecordingBindings : PluginHostBindings {
        val sentPayloads = mutableListOf<FunLinkPayload>()

        override val connectedEndpointIds: List<String> = listOf("peer")
        override val connectedEndpoints: List<NearbyEndpointInfo> = emptyList()
        override val isConnected: Boolean = true
        override val maxBytesSize: Int = 32768

        override fun sendPayload(endpointIds: List<String>, payload: FunLinkPayload): Long? {
            sentPayloads += payload
            return 99L
        }

        override fun cancelPayload(payloadId: Long) = Unit
    }
}
