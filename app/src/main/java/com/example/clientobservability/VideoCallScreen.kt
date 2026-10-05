package com.example.clientobservability

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoCallScreen(
    subscriberView: View?,
    publisherView: View?,
    observabilityStats: ObservabilityStats? = null,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { container -> container.setSingleChild(subscriberView) },
                modifier = Modifier.fillMaxSize(),
            )
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(Color.parseColor("#CCCCCC"))
                        setPadding(2, 2, 2, 2)
                    }
                },
                update = { container -> container.setSingleChild(publisherView) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(90.dp, 120.dp),
            )
            if (observabilityStats != null) {
                StatsOverlay(
                    stats = observabilityStats,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
        }
    }
}

private fun ViewGroup.setSingleChild(view: View?) {
    removeAllViews()
    if (view == null) return
    (view.parent as? ViewGroup)?.removeView(view)
    addView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
}

@Composable
private fun StatsOverlay(stats: ObservabilityStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 210.dp)
            .heightIn(max = 600.dp)
            .background(ComposeColor(0xCC000000))
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
    ) {

        // ── Publisher Video ──────────────────────────────────────────────────
        SectionHeader("Publisher — Video")
        StatRow("Bytes Sent", stats.pubVideoBytesSent.toString())
        StatRow("Packets Sent", stats.pubVideoPacketsSent.toString())
        StatRow("Packets Lost", stats.pubVideoPacketsLost.toString())
        if (stats.pubVideoEncodedFrameRate > 0)
            StatRow("Frame Rate", "%.1f fps".format(stats.pubVideoEncodedFrameRate))
        stats.pubVideoCodec?.let { StatRow("Codec", it) }
        stats.pubVideoQualityLimitation?.let { StatRow("Quality Limit", it) }

        // ── Publisher Audio ──────────────────────────────────────────────────
        SectionHeader("Publisher — Audio")
        StatRow("Bytes Sent", stats.pubAudioBytesSent.toString())
        StatRow("Packets Sent", stats.pubAudioPacketsSent.toString())
        StatRow("Packets Lost", stats.pubAudioPacketsLost.toString())

        // ── Publisher Network Condition ──────────────────────────────────────
        SectionHeader("Publisher — Network Condition")
        StatRow("Est. Bandwidth", stats.pubEstimatedBandwidth?.let { fmtBps(it) } ?: "—")
        StatRow("Condition", stats.pubNetworkCondition ?: "—")
        StatRow("Reason", stats.pubNetworkConditionReason ?: "—")

        // ── Subscriber Video ─────────────────────────────────────────────────
        SectionHeader("Subscriber — Video")
        StatRow("Bytes Recv", stats.videoBytesReceived.toString())
        StatRow("Packets Lost", stats.videoPacketsLost.toString())
        StatRow("Packets Recv", stats.videoPacketsReceived.toString())
        StatRow("Resolution", "${stats.subVideoWidth}×${stats.subVideoHeight}")
        StatRow("Decoded FPS", "%.1f fps".format(stats.subDecodedFrameRate))
        StatRow("Bitrate", stats.subBitrate?.let { fmtBps(it) } ?: "—")
        StatRow("Total Bitrate", stats.subTotalBitrate?.let { fmtBps(it) } ?: "—")
        stats.subCodec?.let { StatRow("Codec", it) }
        StatRow("Freeze Count", stats.subFreezeCount?.toString() ?: "—")
        StatRow("Freeze Duration", stats.subTotalFreezesDuration?.let { fmtMs(it) } ?: "—")
        StatRow("Pause Count", stats.subPauseCount?.toString() ?: "—")
        StatRow("Pause Duration", stats.subTotalPausesDuration?.let { fmtMs(it) } ?: "—")

        // ── Subscriber Audio ─────────────────────────────────────────────────
        SectionHeader("Subscriber — Audio")
        StatRow("Bytes Recv", stats.audioBytesReceived.toString())
        StatRow("Packets Lost", stats.audioPacketsLost.toString())
        StatRow("Packets Recv", stats.audioPacketsReceived.toString())

        // ── Sender-Side Stats ─────────────────────────────────────────────────
        SectionHeader("Sender-Side — Stats")
        StatRow("Est. Bandwidth", stats.senderEstimatedBandwidth?.let { fmtBps(it) } ?: "—")
        StatRow("Condition", stats.remotePubNetworkCondition ?: "—")
        StatRow("Reason", stats.remotePubNetworkConditionReason ?: "—")

        // ── Subscriber Network Condition ──────────────────────────────────────
        SectionHeader("Subscriber — Network Condition")
        StatRow("Local Est. BW", stats.localEstimatedBandwidth?.let { fmtBps(it) } ?: "—")
        StatRow("Condition", stats.subNetworkCondition ?: "—")
        StatRow("Reason", stats.subNetworkConditionReason ?: "—")
        StatRow("Remote Est. BW", stats.remoteEstimatedBandwidth?.let { fmtBps(it) } ?: "—")
        StatRow("Degradation", stats.networkDegradationSource ?: "—")
    }
}

// ── UI Helpers ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = title,
        color = ComposeColor(0xFFFFD700),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = ComposeColor.White,
        fontSize = 10.sp,
    )
}

private fun fmtBps(value: Long): String = when {
    value >= 1_000_000 -> "%.2f Mbps".format(value / 1_000_000.0)
    value >= 1_000     -> "%.1f Kbps".format(value / 1_000.0)
    else               -> "$value bps"
}

private fun fmtMs(value: Long): String = when {
    value >= 1_000 -> "%.2f s".format(value / 1000.0)
    else           -> "$value ms"
}
