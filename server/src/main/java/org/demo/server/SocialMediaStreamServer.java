package org.demo.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.demo.mapper.SocialMediaStreamMapper;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import static org.demo.mapper.SocialMediaStreamMapper.fromProtoAudioChunk;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoInteractStreamUpdate;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoStreamUpdate;

import static com.google.protobuf.ByteString.copyFrom;

@Slf4j
public class SocialMediaStreamServer extends SocialMediaStreamServiceGrpc.SocialMediaStreamServiceImplBase {

    @Override
    public void downloadStream(WatchStreamRequest request, StreamObserver<Recording> responseObserver) {
        log.info("Received request to download stream from {} using quality {}", request.getProviderName(), request.getQuality());

        Recording response = Recording.newBuilder()
            .setData(copyFrom("Recording Data".getBytes()))
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void watchStream(WatchStreamRequest request, StreamObserver<StreamUpdate> responseObserver) {
        log.info("Received request to watch stream from {} using quality {}", request.getProviderName(), request.getQuality());

        for (int i = 0; i < 3; i++) {
            responseObserver.onNext(StreamUpdate.newBuilder()
                .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("Audio" + i).getBytes())))
                .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("Video" + i).getBytes())))
                .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StreamUpdate> startStream(StreamObserver<StartStreamResponse> responseObserver) {
        log.info("Received request from client to start stream...");
        return new StreamObserver<StreamUpdate>() {
            final List<StreamUpdate> updates = new ArrayList<>();

            @Override
            public void onNext(StreamUpdate streamUpdate) {
                log.info("Got audio and video from client stream: {}", fromProtoStreamUpdate(streamUpdate));
                updates.add(streamUpdate);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error occurred while trying to get audio and video", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Client stream has finally ended...");
                responseObserver.onNext(StartStreamResponse.newBuilder()
                    .setMessage("We got your words from the stream! They are:" + updates.stream()
                        .map(StreamUpdate::getAudioChunk)
                        .map(SocialMediaStreamMapper::fromProtoAudioChunk)
                        .collect(Collectors.joining(",")))
                    .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<InteractStreamUpdate> joinInteractStream(StreamObserver<InteractStreamUpdate> responseObserver) {
        log.info("Received request to join interact stream...");
        return new StreamObserver<InteractStreamUpdate>() {
            @Override
            public void onNext(InteractStreamUpdate streamUpdate) {
                log.info("Got audio and video from client during interact stream: {}", fromProtoInteractStreamUpdate(streamUpdate));
                if ("Hey".equals(fromProtoAudioChunk(streamUpdate.getAudioChunk()))) {
                    responseObserver.onNext(InteractStreamUpdate.newBuilder()
                        .setAudioChunk(AudioChunk.newBuilder()
                            .setAudioData(copyFrom("Hey! How are you doing?".getBytes()))
                            .build())
                        .setVideoFrame(VideoFrame.newBuilder()
                            .setFrameData(copyFrom("ServerMuzzle".getBytes()))
                            .build())
                        .build());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error occurred while trying to get audio and video", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Interact stream has finally ended...");
                responseObserver.onNext(InteractStreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder()
                        .setAudioData(copyFrom("It was a pleasure talking to you. Bye!".getBytes()))
                        .build())
                    .setVideoFrame(VideoFrame.newBuilder()
                        .setFrameData(copyFrom("ServerMuzzle".getBytes()))
                        .build())
                    .build());
                responseObserver.onCompleted();
            }
        };
    }
}
