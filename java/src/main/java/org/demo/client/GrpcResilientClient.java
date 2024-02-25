package org.demo.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.demo.interceptor.ClientJwtInterceptor;
import org.demo.server.AudioChunk;
import org.demo.server.InteractStreamUpdate;
import org.demo.server.SocialMediaStreamServiceGrpc;
import org.demo.server.StartStreamResponse;
import org.demo.server.StreamUpdate;
import org.demo.server.VideoFrame;
import org.demo.server.WatchStreamRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.demo.mapper.SocialMediaStreamMapper.fromProtoInteractStreamUpdate;
import static org.demo.mapper.SocialMediaStreamMapper.fromProtoStreamUpdate;

import static com.google.protobuf.ByteString.copyFrom;


@Slf4j
@RequiredArgsConstructor
public class GrpcResilientClient {

    private static final String RETRY_CONFIG = "retrying_config.json";
    private static final String TLS_CRT = "tls_credentials/root.crt";
    private static final long CALL_DEADLINE = 5;

    private SocialMediaStreamServiceGrpc.SocialMediaStreamServiceBlockingStub blockingStub;

    //only async stub can be used for streaming operations
    private SocialMediaStreamServiceGrpc.SocialMediaStreamServiceStub asyncStub;

    public GrpcResilientClient(Channel channel) {
        this.blockingStub = SocialMediaStreamServiceGrpc.newBlockingStub(channel);
        this.asyncStub = SocialMediaStreamServiceGrpc.newStub(channel);
    }

    public void downloadStream() {
        WatchStreamRequest request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build();

        try {
            log.info("Sending request to download stream from {} using quality {}", request.getProviderName(), request.getQuality());
            blockingStub.withDeadlineAfter(CALL_DEADLINE, TimeUnit.SECONDS).downloadStream(request);
            log.info("Stream has been downloaded, data={}", request);
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
            streamUpdateIterator = blockingStub.withDeadlineAfter(CALL_DEADLINE, java.util.concurrent.TimeUnit.SECONDS).watchStream(request);
            while (streamUpdateIterator.hasNext()) {
                StreamUpdate data = streamUpdateIterator.next();
                log.info("40_tonn said: {}. Very wise!", fromProtoStreamUpdate(data));
            }
            log.info("Watch stream has ended");
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        }
    }

    public void startStream() {
        //use CountDownLatch to wait for response from server (make it blocking)
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<StartStreamResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(StartStreamResponse streamUpdate) {
                log.info("Got message from the server regarding our stream: {}", streamUpdate.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error received while trying to stream...", throwable);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Server aware regarding the stream end...");
                finishLatch.countDown();
            }
        };

        StreamObserver<StreamUpdate> requestObserver = asyncStub.withDeadlineAfter(CALL_DEADLINE, java.util.concurrent.TimeUnit.SECONDS).startStream(responseObserver);

        try {
            log.info("Sending streaming requests...");
            for (int i = 0; i < 3; i++) {
                requestObserver.onNext(StreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("ClientAudio" + i).getBytes())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("ClientVideo" + i).getBytes())))
                    .build());
            }
            requestObserver.onCompleted();
            if (!finishLatch.await(1, TimeUnit.SECONDS)) {
                log.warn("The call did not finish within 1 second");
            }
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void joinInteractStream() {
        log.info("Sending streaming requests interact...");
        //use CountDownLatch to wait for response from server (make it blocking)
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<InteractStreamUpdate> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(InteractStreamUpdate streamUpdate) {
                log.info("Server response during interact streaming: {}",
                    fromProtoInteractStreamUpdate(streamUpdate));
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("An error received while trying to stream...", throwable);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Server aware regarding the stream end...");
                finishLatch.countDown();
            }
        };
        StreamObserver<InteractStreamUpdate> requestObserver = asyncStub.withDeadlineAfter(CALL_DEADLINE, java.util.concurrent.TimeUnit.SECONDS).joinInteractStream(responseObserver);

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
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                log.warn("The call did not finish within 1 second");
            }
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Map<String, ?> getRetryingServiceConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(new InputStreamReader(
            Files.newInputStream(Paths.get(RETRY_CONFIG)), UTF_8), Map.class);
    }

    public static void main(String[] args) throws IOException {
        //use secure channel with TLS certificates
        ChannelCredentials tlsChannelCredentials = TlsChannelCredentials.newBuilder().trustManager(
                Files.newInputStream(Paths.get(TLS_CRT)))
            .build();
        Map<String, ?> serviceConfig = getRetryingServiceConfig();
        ManagedChannel channel = Grpc.newChannelBuilderForAddress("localhost", 9030, tlsChannelCredentials)
            .defaultServiceConfig(serviceConfig)
            .enableRetry()
            .intercept(new ClientJwtInterceptor())
            .build();
        GrpcResilientClient client = new GrpcResilientClient(channel);
        client.downloadStream();
        client.watchStream();
        client.startStream();
        client.joinInteractStream();
        channel.shutdown();
    }

}
