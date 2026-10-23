package com.replaymac.companion.data.network

import com.replaymac.companion.data.model.CompanionAuthChallenge
import com.replaymac.companion.data.model.CompanionAuthResponse
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionErrorMessage
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionTelemetryTick
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette
import com.replaymac.companion.data.model.CompanionThumbnailResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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

    /**
     * The theme is read on its own and leniently, so a malformed one gives
     * null instead of losing the whole snapshot.
     */
    fun parseStateSnapshot(root: JsonObject): CompanionStateSnapshot {
        val withoutTheme = JsonObject(root - "theme")
        return json.decodeFromJsonElement<CompanionStateSnapshot>(withoutTheme)
            .copy(theme = parseTheme(root["theme"]))
    }

    /** A `theme` object; anything that isn't an object is null, and a wrongly typed field takes its default. */
    fun parseTheme(element: JsonElement?): CompanionTheme? {
        val obj = element as? JsonObject ?: return null
        return CompanionTheme(
            atmosphere = obj.string("atmosphere"),
            atmosphereName = obj.string("atmosphereName"),
            colorScheme = obj.string("colorScheme"),
            accentMode = obj.string("accentMode"),
            surfaceStyle = obj.string("surfaceStyle"),
            dualTone = (obj["dualTone"] as? JsonPrimitive)?.booleanOrNull ?: false,
            dark = parseThemePalette(obj["dark"]),
            light = parseThemePalette(obj["light"])
        )
    }

    private fun parseThemePalette(element: JsonElement?): CompanionThemePalette? {
        val obj = element as? JsonObject ?: return null
        return CompanionThemePalette(
            canvas = obj.string("canvas"),
            card = obj.string("card"),
            accent = obj.string("accent"),
            accentSecondary = obj.string("accentSecondary")
        )
    }

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: ""

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
