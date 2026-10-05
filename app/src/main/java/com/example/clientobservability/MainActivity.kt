package com.example.clientobservability

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clientobservability.VonageVideoConfig.description
import com.example.clientobservability.VonageVideoConfig.isValid
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.NetworkDegradationSource
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.TransportStats

@Suppress("SpellCheckingInspection")
class MainActivity : ComponentActivity() {

    private var publisherView by mutableStateOf<View?>(null)
    private var subscriberView by mutableStateOf<View?>(null)
    private var latestObservabilityStats by mutableStateOf<ObservabilityStats?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var latestMediaLinkSnapshot: MediaLinkSnapshot? = null
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    // ── Condition Mappers ─────────────────────────────────────────────────────
    // networkCondition and networkDegradationSource are Int constants in the SDK.
    // networkConditionReason is already a String — store directly.

    private fun mapCondition(value: Int): String = when (value) {
        TransportStats.NETWORK_CONDITION_EXCELLENT -> "Excellent"
        TransportStats.NETWORK_CONDITION_GOOD      -> "Good"
        TransportStats.NETWORK_CONDITION_FAIR      -> "Fair"
        TransportStats.NETWORK_CONDITION_WARNING   -> "Warning"
        TransportStats.NETWORK_CONDITION_CRITICAL  -> "Critical"
        TransportStats.NETWORK_CONDITION_UNKNOWN   -> "Unknown"
        else                                       -> "Unknown"
    }

    private fun mapDegradationSource(value: Int): String = when (value) {
        NetworkDegradationSource.NONE            -> "None"
        NetworkDegradationSource.LOCAL           -> "Local"
        NetworkDegradationSource.REMOTE          -> "Remote"
        NetworkDegradationSource.BOTH_OR_UNCLEAR -> "Both/Unclear"
        else                                     -> "Unknown"
    }

    // ── Publisher Listener ────────────────────────────────────────────────────

    private val publisherListener = object : PublisherKit.PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "Publisher stream created: ${stream.streamId}")
        }
        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "Publisher stream destroyed: ${stream.streamId}")
        }
        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit onError: ${opentokError.message}")
        }
    }

    // ── Publisher Video Stats ─────────────────────────────────────────────────

    private val publisherVideoStatsListener = PublisherKit.VideoStatsListener { _, stats ->
        if (stats.isNullOrEmpty()) return@VideoStatsListener
        val s = stats[0]
        val layer = s.videoLayers?.firstOrNull()
        mainHandler.post {
            latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                pubVideoBytesSent = s.videoBytesSent,
                pubVideoPacketsSent = s.videoPacketsSent,
                pubVideoPacketsLost = s.videoPacketsLost,
                pubVideoEncodedFrameRate = layer?.encodedFrameRate ?: 0.0,
                pubVideoCodec = layer?.codec,
                pubVideoQualityLimitation = layer?.qualityLimitationReason,
            )
        }
    }

    // ── Publisher Audio Stats ─────────────────────────────────────────────────

    private val publisherAudioStatsListener = PublisherKit.AudioStatsListener { _, stats ->
        if (stats.isNullOrEmpty()) return@AudioStatsListener
        val s = stats[0]
        mainHandler.post {
            latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                pubAudioBytesSent = s.audioBytesSent,
                pubAudioPacketsSent = s.audioPacketsSent,
                pubAudioPacketsLost = s.audioPacketsLost,
            )
        }
    }

    // ── Publisher MediaLink Stats ─────────────────────────────────────────────

    private val publisherMediaLinkStatsListener = object : PublisherKit.MediaLinkStatsListener {
        override fun onMediaLinkStats(
            publisher: PublisherKit,
            mediaLinkStats: Array<out PublisherKit.PublisherMediaLinkStats>?,
        ) {
            if (mediaLinkStats.isNullOrEmpty()) return
            updateFromPublisherMediaLink(mediaLinkStats[0])
        }

        override fun onNetworkConditionChanged(
            publisher: PublisherKit,
            mediaLinkStats: PublisherKit.PublisherMediaLinkStats,
            reason: String,
        ) {
            Log.d(TAG, "Pub network condition changed: reason=$reason")
            updateFromPublisherMediaLink(mediaLinkStats)
        }

        private fun updateFromPublisherMediaLink(stats: PublisherKit.PublisherMediaLinkStats) {
            val transport = stats.transport ?: return
            mainHandler.post {
                latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                    pubEstimatedBandwidth = transport.connectionEstimatedBandwidth,
                    pubNetworkCondition = mapCondition(transport.networkCondition),
                    pubNetworkConditionReason = transport.networkConditionReason, // String
                )
            }
        }
    }

    // ── Session Listener ──────────────────────────────────────────────────────

    private val sessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "Connected to session: ${session.sessionId}")
            publisher = Publisher.Builder(this@MainActivity)
                .senderStatsTrack(true)
                .build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.setVideoStatsListener(publisherVideoStatsListener)
            publisher?.setAudioStatsListener(publisherAudioStatsListener)
            publisher?.setMediaLinkStatsListener(publisherMediaLinkStatsListener)
            publisher?.renderer?.setStyle(
                BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL,
            )
            publisherView = publisher?.view
            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }
            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "Disconnected: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "Stream received: ${stream.streamId}")
            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build()
                subscriber?.renderer?.setStyle(
                    BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL,
                )
                subscriber?.setSubscriberListener(subscriberListener)
                subscriber?.setVideoStatsListener(videoStatsListener)
                subscriber?.setAudioStatsListener(subscriberAudioStatsListener)
                subscriber?.setMediaLinkStatsListener(mediaLinkStatsListener)
                session.subscribe(subscriber)
                subscriberView = subscriber?.view
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "Stream dropped: ${stream.streamId}")
            if (subscriber != null) {
                subscriber = null
                subscriberView = null
                latestObservabilityStats = null
                latestMediaLinkSnapshot = null
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }
    }

    // ── Subscriber Listener ───────────────────────────────────────────────────

    private val subscriberListener = object : SubscriberKit.SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "Subscriber connected: ${subscriberKit.stream.streamId}")
        }
        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "Subscriber disconnected: ${subscriberKit.stream.streamId}")
        }
        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            finishWithMessage("SubscriberKit onError: ${opentokError.message}")
        }
    }

    // ── Subscriber Video Stats ────────────────────────────────────────────────

    private val videoStatsListener = SubscriberKit.VideoStatsListener { _, stats ->
        Log.d(TAG, "Sub video: bytesReceived=${stats.videoBytesReceived} codec=${stats.codec}")
        mainHandler.post {
            latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                videoBytesReceived = stats.videoBytesReceived,
                videoPacketsLost = stats.videoPacketsLost,
                videoPacketsReceived = stats.videoPacketsReceived,
                timeStamp = stats.timeStamp,
                subVideoWidth = stats.width,
                subVideoHeight = stats.height,
                subDecodedFrameRate = stats.decodedFrameRate,
                subBitrate = stats.bitrate,
                subTotalBitrate = stats.totalBitrate,
                subCodec = stats.codec,
                subFreezeCount = stats.freezeCount,
                subTotalFreezesDuration = stats.totalFreezesDuration,
                subPauseCount = stats.pauseCount,
                subTotalPausesDuration = stats.totalPausesDuration,
                // Preserve all MediaLink values already captured
                localEstimatedBandwidth = latestMediaLinkSnapshot?.localEstimatedBandwidth,
                remoteEstimatedBandwidth = latestMediaLinkSnapshot?.remoteEstimatedBandwidth,
                networkDegradationSource = latestMediaLinkSnapshot?.networkDegradationSource,
                subNetworkCondition = latestMediaLinkSnapshot?.subNetworkCondition,
                subNetworkConditionReason = latestMediaLinkSnapshot?.subNetworkConditionReason,
                remotePubNetworkCondition = latestMediaLinkSnapshot?.remotePubNetworkCondition,
                remotePubNetworkConditionReason = latestMediaLinkSnapshot?.remotePubNetworkConditionReason,
                senderEstimatedBandwidth = latestMediaLinkSnapshot?.senderEstimatedBandwidth,
            )
        }
    }

    // ── Subscriber Audio Stats ────────────────────────────────────────────────

    private val subscriberAudioStatsListener = SubscriberKit.AudioStatsListener { _, stats ->
        Log.d(TAG, "Sub audio: bytesReceived=${stats.audioBytesReceived}")
        mainHandler.post {
            latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                audioBytesReceived = stats.audioBytesReceived,
                audioPacketsLost = stats.audioPacketsLost,
                audioPacketsReceived = stats.audioPacketsReceived,
            )
        }
    }

    // ── Subscriber MediaLink Stats ────────────────────────────────────────────
    // Sender-side stats come from remotePublisherTransport (non-deprecated API).
    // This provides the publisher's uplink bandwidth and network condition
    // as seen from the subscriber side.

    private val mediaLinkStatsListener = object : SubscriberKit.MediaLinkStatsListener {
        override fun onMediaLinkStats(
            subscriber: SubscriberKit,
            mediaLinkStats: SubscriberKit.SubscriberMediaLinkStats,
        ) {
            updateFromSubscriberMediaLink(mediaLinkStats)
        }

        override fun onNetworkConditionChanged(
            subscriber: SubscriberKit,
            mediaLinkStats: SubscriberKit.SubscriberMediaLinkStats,
            reason: String,
        ) {
            Log.d(TAG, "Sub network condition changed: reason=$reason")
            updateFromSubscriberMediaLink(mediaLinkStats)
        }

        private fun updateFromSubscriberMediaLink(ml: SubscriberKit.SubscriberMediaLinkStats) {
            // Subscriber local downlink
            val localBw      = ml.transport?.connectionEstimatedBandwidth
            val subCond      = ml.transport?.let { mapCondition(it.networkCondition) }
            val subReason    = ml.transport?.networkConditionReason // String

            // Remote publisher uplink — this IS the sender-side stats (non-deprecated)
            val senderBw     = ml.remotePublisherTransport?.connectionEstimatedBandwidth
            val remoteCond   = ml.remotePublisherTransport?.let { mapCondition(it.networkCondition) }
            val remoteReason = ml.remotePublisherTransport?.networkConditionReason // String

            // Degradation source
            val degradSource = mapDegradationSource(ml.networkDegradationSource)

            mainHandler.post {
                val snapshot = MediaLinkSnapshot(
                    localEstimatedBandwidth = localBw,
                    remoteEstimatedBandwidth = senderBw,
                    networkDegradationSource = degradSource,
                    subNetworkCondition = subCond,
                    subNetworkConditionReason = subReason,
                    remotePubNetworkCondition = remoteCond,
                    remotePubNetworkConditionReason = remoteReason,
                    senderEstimatedBandwidth = senderBw,
                )
                latestMediaLinkSnapshot = snapshot
                latestObservabilityStats = (latestObservabilityStats ?: ObservabilityStats()).copy(
                    localEstimatedBandwidth = localBw,
                    remoteEstimatedBandwidth = senderBw,
                    networkDegradationSource = degradSource,
                    subNetworkCondition = subCond,
                    subNetworkConditionReason = subReason,
                    remotePubNetworkCondition = remoteCond,
                    remotePubNetworkConditionReason = remoteReason,
                    senderEstimatedBandwidth = senderBw,
                )
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isValid) {
            finishWithMessage("Invalid VonageVideoConfig. $description")
            return
        }
        setContent {
            MaterialTheme {
                VideoChatPermissionWrapper(
                    onPermissionsGranted = { connectToSession() },
                ) {
                    VideoCallScreen(
                        subscriberView = subscriberView,
                        publisherView = publisherView,
                        observabilityStats = latestObservabilityStats,
                    )
                }
            }
        }
    }

    override fun onPause() { super.onPause(); session?.onPause() }
    override fun onResume() { super.onResume(); session?.onResume() }

    private fun connectToSession() {
        if (session != null) return
        session = Session.Builder(
            this,
            VonageVideoConfig.APP_ID,
            VonageVideoConfig.SESSION_ID,
        ).build()
        session?.setSessionListener(sessionListener)
        session?.connect(VonageVideoConfig.TOKEN)
    }

    private fun finishWithMessage(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
