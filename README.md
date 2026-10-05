
# Client Observability — Vonage Video API Android Sample

A Jetpack Compose Android application that demonstrates the full **Client Observability** feature of the Vonage Video API. Two participants join a routed video session and the app displays live telemetry for publisher and subscriber audio/video stats, network condition scoring, sender-side bandwidth estimation, and degradation source attribution.

---

## SDK

- Vonage Video API Android SDK
- Jetpack Compose
- Kotlin

## Platform

- Android (Jetpack Compose)
- Session Type: **Routed** (Vonage Media Router required)

---

## Prerequisites

- A Vonage Video API account
- An **Application ID**, **Session ID**, and **Token** from the [Vonage Dashboard](https://dashboard.nexmo.com)
- Android Studio (latest stable)
- Android device or emulator (API 21+)

---

## Project Structure

```
app/src/main/java/com/example/clientobservability/
├── MainActivity.kt                  Session logic, all stats listeners, event handlers
├── ObservabilityStats.kt            Data model for all stats fields
├── VideoCallScreen.kt               Jetpack Compose UI, scrollable stats overlay
├── VideoChatPermissionWrapper.kt    Camera/microphone permission handling
└── VonageVideoConfig.kt             Credentials (Application ID, Session ID, Token)
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/vishakhamahawar-cyber/client-observability-demo-android.git
cd client-observability-demo-android
```

### 2. Add your credentials

Open `VonageVideoConfig.kt` and fill in your values:

```kotlin
object VonageVideoConfig {
    const val APP_ID     = "YOUR_APPLICATION_ID"
    const val SESSION_ID = "YOUR_SESSION_ID"
    const val TOKEN      = "YOUR_TOKEN"
}
```

Get these from the [Vonage Dashboard](https://dashboard.nexmo.com) → Applications → your app.

### 3. Build and run

Open the project in Android Studio → click **Run**.

---

## Testing

- Open the app on **two devices** (or one device + the [JS web demo](https://github.com/vishakhamahawar-cyber/client-observability-demo))
- Both must connect to the **same Session ID**
- Stats start populating within ~12 seconds of both clients joining
- Scroll the stats overlay to see all sections

---

## What It Shows

### Publisher — Video
| Field | Description |
|---|---|
| `pubVideoBytesSent` | Total video bytes sent since session start |
| `pubVideoPacketsSent` | Total RTP video packets sent |
| `pubVideoPacketsLost` | Estimated video packets lost |
| `pubVideoEncodedFrameRate` | Publisher-side encoded frame rate (fps) |
| `pubVideoCodec` | Active codec: VP8, VP9, H264, AV1 |
| `pubVideoQualityLimitation` | Reason for quality limitation if any |

### Publisher — Audio
| Field | Description |
|---|---|
| `pubAudioBytesSent` | Total audio bytes sent |
| `pubAudioPacketsSent` | Total RTP audio packets sent |
| `pubAudioPacketsLost` | Estimated audio packets lost |

### Publisher — Network Condition
| Field | Description |
|---|---|
| `pubEstimatedBandwidth` | Estimated uplink bandwidth (bps) |
| `pubNetworkCondition` | Uplink health: Excellent / Good / Fair / Warning / Critical |
| `pubNetworkConditionReason` | Root cause: None / Bandwidth / Packet Loss |

### Subscriber — Video
| Field | Description |
|---|---|
| `videoBytesReceived` | Total video bytes received |
| `videoPacketsLost` | Video packets lost on the downlink |
| `videoPacketsReceived` | Total video packets received |
| `subVideoWidth` / `subVideoHeight` | Decoded frame resolution (pixels) |
| `subDecodedFrameRate` | Frame rate produced by the decoder (fps) |
| `subBitrate` | Current received video bitrate (bps) |
| `subTotalBitrate` | Bitrate including RTP overhead (bps) |
| `subCodec` | Active decoder codec |
| `subFreezeCount` | Number of WebRTC-defined video freezes |
| `subTotalFreezesDuration` | Total cumulative freeze duration (ms) |
| `subPauseCount` | Number of video pauses (>5s since last frame) |
| `subTotalPausesDuration` | Total cumulative pause duration (ms) |

### Subscriber — Audio
| Field | Description |
|---|---|
| `audioBytesReceived` | Total audio bytes received |
| `audioPacketsLost` | Audio packets lost on the downlink |
| `audioPacketsReceived` | Total audio packets received |

### Sender-Side Stats
Sourced from `remotePublisherTransport` via `SubscriberKit.MediaLinkStatsListener` — reflects the **publisher's uplink** as seen from the subscriber side. Requires `.senderStatsTrack(true)` on the publisher.

| Field | Description |
|---|---|
| `senderEstimatedBandwidth` | Publisher's estimated uplink bandwidth (bps) |
| `remotePubNetworkCondition` | Publisher's uplink health: Excellent / Good / Fair / Warning / Critical |
| `remotePubNetworkConditionReason` | Root cause of publisher-side condition |

### Subscriber — Network Condition
| Field | Description |
|---|---|
| `localEstimatedBandwidth` | Subscriber's local downlink bandwidth estimate (bps) |
| `subNetworkCondition` | Subscriber's downlink health score |
| `subNetworkConditionReason` | Root cause of subscriber-side condition |
| `remoteEstimatedBandwidth` | Remote publisher uplink bandwidth (bps) |
| `networkDegradationSource` | Which side is causing issues: None / Local / Remote / Both/Unclear |

---

## How It Works

### Publisher initialisation — required flags

```kotlin
publisher = Publisher.Builder(this)
    .senderStatsTrack(true) // enables sender-side stats on all subscribers
    .build()
```

### Stats listeners wired up

```kotlin
// Publisher
publisher.setVideoStatsListener(publisherVideoStatsListener)
publisher.setAudioStatsListener(publisherAudioStatsListener)
publisher.setMediaLinkStatsListener(publisherMediaLinkStatsListener)

// Subscriber
subscriber.setVideoStatsListener(videoStatsListener)
subscriber.setAudioStatsListener(subscriberAudioStatsListener)
subscriber.setMediaLinkStatsListener(mediaLinkStatsListener)
```

### Sender-side stats source

Sender-side stats are received via `SubscriberKit.MediaLinkStatsListener` through `remotePublisherTransport` — this provides the publisher's uplink bandwidth and network condition as estimated from the subscriber side.

```kotlin
val senderBw   = ml.remotePublisherTransport?.connectionEstimatedBandwidth
val remoteCond = ml.remotePublisherTransport?.networkCondition
```

---

## Network Condition Scores

| Score | Description | Audio Fallback |
|---|---|---|
| Excellent | Optimal — bandwidth accommodates max bitrate | None |
| Good | Minor or temporary issues may occur | None |
| Fair | Video quality may be restricted | None |
| Warning | Poor — audio fallback warning triggered | Warning |
| Critical | Severe — SDK disables video if audio fallback enabled | Critical |

---

## Related

- [JS Web Sample](https://github.com/vishakhamahawar-cyber/client-observability-demo) — companion web demo with the same features
- [Client Observability Guide](https://developer.vonage.com/en/video/guides/client-observability/android)
- [Vonage Video API Docs](https://developer.vonage.com/en/video)
- [Android SDK Samples](https://github.com/Vonage/vonage-video-android-sdk-samples)
