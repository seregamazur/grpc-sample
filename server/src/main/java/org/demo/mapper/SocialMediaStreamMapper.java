package org.demo.mapper;

import java.nio.charset.StandardCharsets;

import org.demo.server.AudioChunk;
import org.demo.server.InteractStreamUpdate;
import org.demo.server.StreamUpdate;

public class SocialMediaStreamMapper {

    public static String fromProtoAudioChunk(AudioChunk audioChunk) {
        return new String(audioChunk.getAudioData().toByteArray(), StandardCharsets.UTF_8);
    }

    public static String fromProtoInteractStreamUpdate(InteractStreamUpdate streamUpdate) {
        String audio = new String(streamUpdate.getAudioChunk().getAudioData().toByteArray(), StandardCharsets.UTF_8);
        String video = new String(streamUpdate.getVideoFrame().getFrameData().toByteArray(), StandardCharsets.UTF_8);
        return audio + ", " + video;
    }

    public static String fromProtoStreamUpdate(StreamUpdate streamUpdate) {
        String audio = new String(streamUpdate.getAudioChunk().getAudioData().toByteArray(), StandardCharsets.UTF_8);
        String video = new String(streamUpdate.getVideoFrame().getFrameData().toByteArray(), StandardCharsets.UTF_8);
        return audio + ", " + video;
    }

}
