package org.demo.client;

import java.util.Iterator;

import org.demo.interceptor.ClientJwtInterceptor;
import org.demo.server.AudioChunk;
import org.demo.server.InteractStreamUpdate;
import org.demo.server.SocialMediaStreamServiceGrpc;
import org.demo.server.StartStreamResponse;
import org.demo.server.StreamUpdate;
import org.demo.server.VideoFrame;
import org.demo.server.WatchStreamRequest;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.demo.mapper.SocialMediaStreamMapper.fromProtoInteractStreamUpdate;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoStreamUpdate;

import static com.google.protobuf.ByteString.copyFrom;

@Slf4j
@RequiredArgsConstructor
public class GrpcAuthClient {

    private SocialMediaStreamServiceGrpc.SocialMediaStreamServiceBlockingStub blockingStub;
    private SocialMediaStreamServiceGrpc.SocialMediaStreamServiceStub nonBlockingStub;

    public GrpcAuthClient(Channel channel) {
        this.blockingStub = SocialMediaStreamServiceGrpc.newBlockingStub(channel);
        this.nonBlockingStub = SocialMediaStreamServiceGrpc.newStub(channel);
    }

    public void downloadStream() {
        WatchStreamRequest request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build();

        try {
            log.info("Sending request to download stream from {} using quality {}", request.getProviderName(), request.getQuality());
            blockingStub.downloadStream(request);
            log.info("Downloading stream...");
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        }
    }

    public void watchStream() {
        WatchStreamRequest request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build();

        Iterator<StreamUpdate> streamUpdateIterator;
        try {
            log.info("Sending request to watch stream from {} using quality {}", request.getProviderName(), request.getQuality());
            streamUpdateIterator = blockingStub.watchStream(request);
            while (streamUpdateIterator.hasNext()) {
                StreamUpdate data = streamUpdateIterator.next();
                log.info("40_tonn said: {}. Very wise!", fromProtoStreamUpdate(data));
            }
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        }
    }

    public void startStream() {
        StreamObserver<StartStreamResponse> responseObserver = new StreamObserver<StartStreamResponse>() {
            @Override
            public void onNext(StartStreamResponse streamUpdate) {
                log.info("Got message from the server regarding our stream: {}", streamUpdate.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error received while trying to stream...", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Server aware regarding the stream end...");
            }
        };

        StreamObserver<StreamUpdate> requestObserver = nonBlockingStub.startStream(responseObserver);

        try {
            log.info("Sending requests to stream...");
            for (int i = 0; i < 3; i++) {
                requestObserver.onNext(StreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("ClientAudio" + i).getBytes())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("ClientVideo" + i).getBytes())))
                    .build());
            }
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        }
    }

    public void joinInteractStream() {
        log.info("Sending request to server to join interact stream...");
        StreamObserver<InteractStreamUpdate> responseObserver = new StreamObserver<InteractStreamUpdate>() {

            @Override
            public void onNext(InteractStreamUpdate streamUpdate) {
                log.info("Got audio and video from the server interact stream: {}",
                    fromProtoInteractStreamUpdate(streamUpdate));
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error received while trying to stream...", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Server aware regarding the stream end...");
            }
        };
        StreamObserver<InteractStreamUpdate> requestObserver = nonBlockingStub.joinInteractStream(responseObserver);

        try {
            requestObserver.onNext(InteractStreamUpdate.newBuilder()
                .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom("Hey".getBytes())))
                .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom("ClientSmileFace".getBytes())))
                .build());
            for (int i = 0; i < 3; i++) {
                requestObserver.onNext(InteractStreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("Audio" + i).getBytes())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("ClientSmileFace" + i).getBytes())))
                    .build());
            }
            requestObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        }
    }

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9030)
            .usePlaintext()
            .intercept(new ClientJwtInterceptor())
            .build();
        GrpcAuthClient client = new GrpcAuthClient(channel);
        client.downloadStream();
        client.watchStream();
        client.startStream();
        client.joinInteractStream();
        channel.shutdown();
    }

}
