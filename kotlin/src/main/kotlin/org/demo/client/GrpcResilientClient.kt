package org.demo.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.ByteString.copyFrom
import io.grpc.Channel
import io.grpc.Grpc
import io.grpc.StatusRuntimeException
import io.grpc.TlsChannelCredentials
import io.grpc.stub.StreamObserver
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoInteractStreamUpdate
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoStreamUpdate
import org.demo.org.demo.interceptor.ClientJwtInterceptor
import org.demo.server.AudioChunk
import org.demo.server.InteractStreamUpdate
import org.demo.server.SocialMediaStreamServiceGrpc
import org.demo.server.StartStreamResponse
import org.demo.server.StreamUpdate
import org.demo.server.VideoFrame
import org.demo.server.WatchStreamRequest
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import kotlin.text.Charsets.UTF_8

class GrpcResilientClient(channel: Channel) {

    companion object {
        private val log = LoggerFactory.getLogger(GrpcResilientClient::class.java)
    }

    private val blockingStub = SocialMediaStreamServiceGrpc.newBlockingStub(channel)
    private val nonBlockingStub = SocialMediaStreamServiceGrpc.newStub(channel)

    fun downloadStream() {
        val request = WatchStreamRequest.newBuilder()
            .setProviderName("40_tonn")
            .setQuality("4k").build()

        try {
            log.info("Sending request to download stream from {} using quality {}", request.getProviderName(), request.getQuality())
            blockingStub.downloadStream(request)
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
            val streamUpdateIterator: Iterator<StreamUpdate> = blockingStub.watchStream(request)
            while (streamUpdateIterator.hasNext()) {
                val data: StreamUpdate = streamUpdateIterator.next()
                log.info("40_tonn said: {}. Very wise!", fromProtoStreamUpdate(data))
            }
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }

    fun startStream() {
        val responseObserver = object : StreamObserver<StartStreamResponse> {
            override fun onNext(streamUpdate: StartStreamResponse) {
                log.info("Got message from the server regarding our stream: {}", streamUpdate.getMessage())
            }

            override fun onError(throwable: Throwable) {
                log.error("An error received while trying to stream...", throwable)
            }

            override fun onCompleted() {
                log.info("Server aware regarding the stream end...")
            }
        }

        val requestObserver: StreamObserver<StreamUpdate> = nonBlockingStub.startStream(responseObserver)

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
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }

    fun joinInteractStream() {
        log.info("Sending request to server to join interact stream...")
        val responseObserver = object : StreamObserver<InteractStreamUpdate> {

            override fun onNext(streamUpdate: InteractStreamUpdate) {
                log.info(
                    "Got audio and video from the server interact stream: {}",
                    fromProtoInteractStreamUpdate(streamUpdate)
                )
            }

            override fun onError(throwable: Throwable) {
                log.error("An error received while trying to stream...", throwable)
            }

            override fun onCompleted() {
                log.info("Server aware regarding the stream end...")
            }
        }
        val requestObserver = nonBlockingStub.joinInteractStream(responseObserver)

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
        } catch (e: StatusRuntimeException) {
            log.error("RPC failed: {}", e.status)
        }
    }
}

fun getRetryingServiceConfig(): Map<String, Any> {
    val mapper = ObjectMapper()

    return mapper.readValue(
        InputStreamReader(
            GrpcResilientClient.Companion::class.java.classLoader.getResourceAsStream("retrying_config.json")!!, UTF_8
        ), object : TypeReference<Map<String, Any>>() {}
    )
}

fun main() {
    val tlsChannelCredentials = TlsChannelCredentials.newBuilder().trustManager(
        GrpcResilientClient.Companion::class.java.classLoader.getResourceAsStream("tls_credentials/root.crt")
    )
        .build()
    val serviceConfig: Map<String, Any> = getRetryingServiceConfig()
    val channel = Grpc.newChannelBuilderForAddress("localhost", 9030, tlsChannelCredentials)
        .defaultServiceConfig(serviceConfig)
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