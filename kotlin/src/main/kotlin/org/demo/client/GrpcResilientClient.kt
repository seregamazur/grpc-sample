package org.demo.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.ByteString.copyFrom
import io.grpc.Channel
import io.grpc.Grpc
import io.grpc.StatusRuntimeException
import io.grpc.TlsChannelCredentials
import io.grpc.stub.StreamObserver
import org.demo.interceptor.ClientJwtInterceptor
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoInteractStreamUpdate
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoStreamUpdate
import org.demo.server.AudioChunk
import org.demo.server.InteractStreamUpdate
import org.demo.server.SocialMediaStreamServiceGrpc
import org.demo.server.StartStreamResponse
import org.demo.server.StreamUpdate
import org.demo.server.VideoFrame
import org.demo.server.WatchStreamRequest
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GrpcResilientClient(channel: Channel) {

    companion object {
        private val log = LoggerFactory.getLogger(GrpcResilientClient::class.java)
        private const val CALL_DEADLINE: Long = 5

        @JvmStatic
        fun getRetryingServiceConfig(): Map<String, Any> {
            return ObjectMapper().readValue(
                Files.newInputStream(Paths.get("retrying_config.json")),
                object : TypeReference<Map<String, Any>>() {}
            )
        }

        @JvmStatic
        fun main(args: Array<String>) {
            //use secure channel with TLS certificates
            val tlsChannelCredentials = TlsChannelCredentials.newBuilder().trustManager(
                Files.newInputStream(Paths.get("tls_credentials/" + System.getenv("SERVER_HOST") + ".crt"))
            )
                .build()

            val retryServiceConfig: Map<String, Any> = getRetryingServiceConfig()

            val channel = Grpc.newChannelBuilderForAddress(System.getenv("SERVER_HOST"), Integer.parseInt(System.getenv("SERVER_PORT")), tlsChannelCredentials)
                .defaultServiceConfig(retryServiceConfig)
                .enableRetry()
                .intercept(ClientJwtInterceptor())
                .build()
            val client = GrpcResilientClient(channel)
            client.downloadStream()
            client.watchStream()
            client.startStream()
            client.joinInteractStream()
            channel.shutdown()
        }
    }

    private val blockingStub = SocialMediaStreamServiceGrpc.newBlockingStub(channel)

    //only async stub can be used for streaming operations
    private val asyncStub = SocialMediaStreamServiceGrpc.newStub(channel)

    fun downloadStream() {
        val request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build()

        try {
            log.info("Sending request to download stream from {} using quality {}", request.getProviderName(), request.getQuality())
            blockingStub.withDeadlineAfter(CALL_DEADLINE, TimeUnit.SECONDS).downloadStream(request)
            log.info("Downloading stream...")
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }

    fun watchStream() {
        val request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build()

        try {
            log.info("Sending request to watch stream from {} using quality {}", request.getProviderName(), request.getQuality())
            val streamUpdateIterator: Iterator<StreamUpdate> =
                blockingStub.withDeadlineAfter(CALL_DEADLINE, TimeUnit.SECONDS).watchStream(request)
            while (streamUpdateIterator.hasNext()) {
                val data: StreamUpdate = streamUpdateIterator.next()
                log.info("40_tonn said: {}. Very wise!", fromProtoStreamUpdate(data))
            }
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }

    fun startStream() {
        //use CountDownLatch to wait for response from server (make it blocking)
        val finishLatch = CountDownLatch(1)
        val responseObserver = object : StreamObserver<StartStreamResponse> {
            override fun onNext(streamUpdate: StartStreamResponse) {
                log.info("Got message from the server regarding our stream: {}", streamUpdate.getMessage())
            }

            override fun onError(throwable: Throwable) {
                log.error("An error received while trying to stream...", throwable)
                finishLatch.countDown()
            }

            override fun onCompleted() {
                log.info("Server aware regarding the stream end...")
                finishLatch.countDown()
            }
        }

        val requestObserver: StreamObserver<StreamUpdate> =
            asyncStub.withDeadlineAfter(CALL_DEADLINE, TimeUnit.SECONDS).startStream(responseObserver)

        try {
            log.info("Sending requests to stream...")
            for (i in 3 downTo 0 step 1) {
                requestObserver.onNext(
                    StreamUpdate.newBuilder()
                        .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("ClientAudio$i").toByteArray())))
                        .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("ClientVideo$i").toByteArray())))
                        .build()
                )
            }
            requestObserver.onCompleted()
            if (!finishLatch.await(1, TimeUnit.SECONDS)) {
                println("The call did not finish within 1 second")
            }
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }

    fun joinInteractStream() {
        log.info("Sending request to server to join interact stream...")
        //use CountDownLatch to wait for response from server (make it blocking)
        val finishLatch = CountDownLatch(1)
        val responseObserver = object : StreamObserver<InteractStreamUpdate> {

            override fun onNext(streamUpdate: InteractStreamUpdate) {
                log.info(
                    "Got audio and video from the server interact stream: {}",
                    fromProtoInteractStreamUpdate(streamUpdate)
                )
            }

            override fun onError(throwable: Throwable) {
                log.error("An error received while trying to stream...", throwable)
                finishLatch.countDown()
            }

            override fun onCompleted() {
                log.info("Server aware regarding the stream end...")
                finishLatch.countDown()
            }
        }
        val requestObserver = asyncStub.withDeadlineAfter(CALL_DEADLINE, TimeUnit.SECONDS).joinInteractStream(responseObserver)

        try {
            requestObserver.onNext(
                InteractStreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom("Hey".toByteArray())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom("ClientSmileFace".toByteArray())))
                    .build()
            )
            for (i in 3 downTo 0 step 1) {
                requestObserver.onNext(
                    InteractStreamUpdate.newBuilder()
                        .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("Audio$i").toByteArray())))
                        .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("ClientSmileFace$i").toByteArray())))
                        .build()
                )
            }
            requestObserver.onCompleted()
            if (!finishLatch.await(1, TimeUnit.SECONDS)) {
                println("The call did not finish within 1 second")
            }
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }
}
