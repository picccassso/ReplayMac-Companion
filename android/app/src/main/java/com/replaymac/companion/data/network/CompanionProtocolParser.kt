package com.replaymac.companion.data.network

import com.replaymac.companion.data.model.CompanionAuthChallenge
import com.replaymac.companion.data.model.CompanionAuthResponse
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionErrorMessage
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionTelemetryTick
import com.replaymac.companion.data.model.CompanionThumbnailResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CompanionProtocolParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun parseStateSnapshot(root: JsonObject): CompanionStateSnapshot {
        return json.decodeFromJsonElement<CompanionStateSnapshot>(root)
    }

    fun parse(text: String): CompanionServerEvent {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            val type = root["type"]?.jsonPrimitive?.contentOrNull ?: ""

            when (type) {
                "auth_response" -> {
                    val auth = json.decodeFromJsonElement<CompanionAuthResponse>(root)
                    CompanionServerEvent.Auth(auth)
                }
                "auth_challenge" -> {
                    val challenge = json.decodeFromJsonElement<CompanionAuthChallenge>(root)
                    CompanionServerEvent.AuthChallenge(challenge)
                }
                "state_snapshot" -> {
                    val snapshot = parseStateSnapshot(root)
                    CompanionServerEvent.Snapshot(snapshot)
                }
                "state_changed" -> {
                    val snapshot = parseStateSnapshot(root)
                    CompanionServerEvent.StateChanged(snapshot)
                }
                "telemetry_tick" -> {
                    val tick = json.decodeFromJsonElement<CompanionTelemetryTick>(root)
                    CompanionServerEvent.Telemetry(tick)
                }
                "thumbnail_response" -> {
                    val resp = json.decodeFromJsonElement<CompanionThumbnailResponse>(root)
                    CompanionServerEvent.Thumbnail(resp.clipId, resp.thumbnailBase64)
                }
                "pong" -> CompanionServerEvent.Pong
                "error" -> {
                    val err = json.decodeFromJsonElement<CompanionErrorMessage>(root)
                    CompanionServerEvent.Error(err.message, err.code)
                }
                else -> CompanionServerEvent.Unknown(type, text)
            }
        } catch (e: Exception) {
            CompanionServerEvent.Error("JSON Parse failed: ${e.message}")
        }
    }

    fun encodeClientMessage(msg: CompanionClientMessage): String {
        return json.encodeToString(CompanionClientMessage.serializer(), msg)
    }
}
