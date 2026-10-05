package com.example.clientobservability

data class ObservabilityStats(
    // ── Subscriber Video ──────────────────────────────────────────────────────
    val videoBytesReceived: Int = 0,
    val videoPacketsLost: Int = 0,
    val videoPacketsReceived: Int = 0,
    val timeStamp: Double = 0.0,
    val subVideoWidth: Int = 0,
    val subVideoHeight: Int = 0,
    val subDecodedFrameRate: Double = 0.0,
    val subBitrate: Long? = null,
    val subTotalBitrate: Long? = null,
    val subCodec: String? = null,
    val subFreezeCount: Long? = null,
    val subTotalFreezesDuration: Long? = null,
    val subPauseCount: Long? = null,
    val subTotalPausesDuration: Long? = null,
    // ── Subscriber Audio ──────────────────────────────────────────────────────
    val audioBytesReceived: Int = 0,
    val audioPacketsLost: Int = 0,
    val audioPacketsReceived: Int = 0,
    // ── Publisher Video ───────────────────────────────────────────────────────
    val pubVideoBytesSent: Long = 0L,
    val pubVideoPacketsSent: Long = 0L,
    val pubVideoPacketsLost: Long = 0L,
    val pubVideoEncodedFrameRate: Double = 0.0,
    val pubVideoCodec: String? = null,
    val pubVideoQualityLimitation: String? = null,
    // ── Publisher Audio ───────────────────────────────────────────────────────
    val pubAudioBytesSent: Long = 0L,
    val pubAudioPacketsSent: Long = 0L,
    val pubAudioPacketsLost: Long = 0L,
    // ── Publisher Network Condition ───────────────────────────────────────────
    val pubEstimatedBandwidth: Long? = null,
    val pubNetworkCondition: String? = null,
    val pubNetworkConditionReason: String? = null,
    // ── Sender-Side (remotePublisherTransport) ────────────────────────────────
    val senderEstimatedBandwidth: Long? = null,
    val remotePubNetworkCondition: String? = null,
    val remotePubNetworkConditionReason: String? = null,
    // ── Subscriber Network Condition ──────────────────────────────────────────
    val localEstimatedBandwidth: Long? = null,
    val subNetworkCondition: String? = null,
    val subNetworkConditionReason: String? = null,
    val remoteEstimatedBandwidth: Long? = null,
    val networkDegradationSource: String? = null,
)

data class MediaLinkSnapshot(
    val localEstimatedBandwidth: Long?,
    val remoteEstimatedBandwidth: Long?,
    val networkDegradationSource: String?,
    val subNetworkCondition: String?,
    val subNetworkConditionReason: String?,
    val remotePubNetworkCondition: String?,
    val remotePubNetworkConditionReason: String?,
    val senderEstimatedBandwidth: Long?,
)
