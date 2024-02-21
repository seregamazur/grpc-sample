package org.demo.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.demo.interceptor.ServerJwtInterceptor;
import org.demo.mapper.SocialMediaStreamMapper;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import static org.demo.mapper.SocialMediaStreamMapper.fromProtoAudioChunk;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoInteractStreamUpdate;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoStreamUpdate;

import static com.google.protobuf.ByteString.copyFrom;

@Slf4j
public class GrpcCrashingServer extends SocialMediaStreamServiceGrpc.SocialMediaStreamServiceImplBase {

    private static final String TLS_CRT = "tls_credentials/localhost.crt";
    private static final String TLS_KEY = "tls_credentials/localhost.key";

    private final AtomicInteger downloadStreamFails = new AtomicInteger();
    private final AtomicInteger watchStreamFails = new AtomicInteger();
    private final AtomicInteger startStreamFails = new AtomicInteger();
    private final AtomicInteger joinInteractStreamFails = new AtomicInteger();

    @Override
    public void downloadStream(WatchStreamRequest request, StreamObserver<Recording> responseObserver) {
        log.info("Received request to download stream from {} using quality {}", request.getProviderName(), request.getQuality());
        if (downloadStreamFails.get() < 2) {
            log.warn("Cancelling downloadStream request");
            responseObserver.onError(Status.UNAVAILABLE.withDescription("RPC cancelled").asRuntimeException());
            downloadStreamFails.incrementAndGet();
        } else {
            Recording response = Recording.newBuilder()
                .setData(copyFrom("Recording Data".getBytes()))
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void watchStream(WatchStreamRequest request, StreamObserver<StreamUpdate> responseObserver) {
        log.info("Received request to watch stream from {} using quality {}", request.getProviderName(), request.getQuality());

        if (watchStreamFails.get() < 1) {
            log.warn("Cancelling watchStream request");
            responseObserver.onError(Status.UNAVAILABLE.withDescription("RPC cancelled").asRuntimeException());
            watchStreamFails.incrementAndGet();
        } else {
            for (int i = 0; i < 3; i++) {
                responseObserver.onNext(StreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("Audio" + i).getBytes())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("Video" + i).getBytes())))
                    .build());
            }
            responseObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<StreamUpdate> startStream(StreamObserver<StartStreamResponse> responseObserver) {
        log.info("Received request from client to start stream...");
        if (startStreamFails.get() < 1) {
            log.warn("Cancelling startStream request");
            responseObserver.onError(Status.UNAVAILABLE.withDescription("RPC cancelled").asRuntimeException());
            startStreamFails.incrementAndGet();
            return new StreamObserver<StreamUpdate>() {
                @Override
                public void onNext(StreamUpdate streamUpdate) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                }
            };
        } else {
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
    }

    @Override
    public StreamObserver<InteractStreamUpdate> joinInteractStream(StreamObserver<InteractStreamUpdate> responseObserver) {
        log.info("Received request to join interact stream...");
        if (joinInteractStreamFails.get() < 2) {
            log.warn("Cancelling joinInteractStream request");
            responseObserver.onError(Status.UNAVAILABLE.withDescription("RPC cancelled").asRuntimeException());
            joinInteractStreamFails.incrementAndGet();
            return new StreamObserver<InteractStreamUpdate>() {
                @Override
                public void onNext(InteractStreamUpdate streamUpdate) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                }
            };
        }
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

    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = ServerBuilder.forPort(9030)
            .useTransportSecurity(Files.newInputStream(Paths.get(TLS_CRT)), Files.newInputStream(Paths.get(TLS_KEY)))
            .addService(new GrpcCrashingServer())
            .intercept(new ServerJwtInterceptor())
            .build();
        server.start();
        Thread.sleep(10_000);
        server.shutdownNow();
    }
}
