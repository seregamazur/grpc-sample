package org.demo.mapper

import java.nio.charset.StandardCharsets

import org.demo.server.AudioChunk
import org.demo.server.InteractStreamUpdate
import org.demo.server.StreamUpdate

class SocialMediaStreamMapper {

    companion object {
        fun fromProtoAudioChunk(audioChunk: AudioChunk): String {
            return String(audioChunk.audioData.toByteArray(), StandardCharsets.UTF_8)
        }

        fun fromProtoInteractStreamUpdate(streamUpdate: InteractStreamUpdate): String {
            val audio = String(streamUpdate.audioChunk.audioData.toByteArray(), StandardCharsets.UTF_8)
            val video = String(streamUpdate.videoFrame.frameData.toByteArray(), StandardCharsets.UTF_8)
            return "$audio, $video"
        }

        fun fromProtoStreamUpdate(streamUpdate: StreamUpdate): String {
            val audio = String(streamUpdate.audioChunk.audioData.toByteArray(), StandardCharsets.UTF_8)
            val video = String(streamUpdate.videoFrame.frameData.toByteArray(), StandardCharsets.UTF_8)
            return "$audio, $video"
        }
    }

}