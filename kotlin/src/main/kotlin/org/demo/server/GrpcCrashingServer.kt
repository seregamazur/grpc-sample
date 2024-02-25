package org.demo.server

import com.google.protobuf.ByteString.copyFrom
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.demo.client.GrpcResilientClient
import org.demo.interceptor.ServerJwtInterceptor
import org.demo.mapper.SocialMediaStreamMapper
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoAudioChunk
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoInteractStreamUpdate
import org.demo.mapper.SocialMediaStreamMapper.Companion.fromProtoStreamUpdate
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.random.Random

class GrpcCrashingServer : SocialMediaStreamServiceGrpc.SocialMediaStreamServiceImplBase() {

    companion object {
        private val log = LoggerFactory.getLogger(GrpcResilientClient::class.java)

        fun randomFailure(responseObserver: StreamObserver<*>?) {
            if (Random.nextInt(from = 0, until = 100) > 70) {
                val status = listOf(Status.CANCELLED, Status.UNAVAILABLE, Status.DEADLINE_EXCEEDED).random()
                responseObserver?.onError(status.withDescription("RPC cancelled").asRuntimeException())
                throw StatusRuntimeException(status.withDescription("RPC cancelled"))
            }
        }
    }

    override fun downloadStream(request: WatchStreamRequest?, responseObserver: StreamObserver<Recording>?) {
        log.info("Received request to download stream from {} using quality {}", request?.getProviderName(), request?.getQuality())
        randomFailure(responseObserver)
        val response = Recording.newBuilder()
            .setData(copyFrom("Recording Data".toByteArray()))
            .build()
        responseObserver?.onNext(response)
        responseObserver?.onCompleted()
    }

    override fun watchStream(request: WatchStreamRequest?, responseObserver: StreamObserver<StreamUpdate>?) {
        log.info("Received request to watch stream from {} using quality {}", request?.getProviderName(), request?.getQuality())

        randomFailure(responseObserver)
        for (i in 3 downTo 0 step 1) {
            responseObserver?.onNext(
                StreamUpdate.newBuilder()
                    .setAudioChunk(AudioChunk.newBuilder().setAudioData(copyFrom(("Audio$i").toByteArray())))
                    .setVideoFrame(VideoFrame.newBuilder().setFrameData(copyFrom(("Video$i").toByteArray())))
                    .build()
            )
        }
        responseObserver?.onCompleted()
    }

    override fun startStream(responseObserver: StreamObserver<StartStreamResponse>?): StreamObserver<StreamUpdate> {
        log.info("Received request from client to start stream...")
        randomFailure(responseObserver)
        return object : StreamObserver<StreamUpdate> {
            val updates: ArrayList<StreamUpdate> = arrayListOf()

            override fun onNext(streamUpdate: StreamUpdate) {
                log.info("Got audio and video from client stream: {}", fromProtoStreamUpdate(streamUpdate))
                updates.add(streamUpdate)
            }

            override fun onError(throwable: Throwable) {
                log.error("An error occurred while trying to get audio and video", throwable)
            }

            override fun onCompleted() {
                log.info("Client stream has finally ended...")
                responseObserver?.onNext(
                    StartStreamResponse.newBuilder()
                        .setMessage(
                            "We got your words from the stream! They are:" + updates.stream()
                                .map(StreamUpdate::getAudioChunk)
                                .map(SocialMediaStreamMapper::fromProtoAudioChunk)
                                .collect(Collectors.joining(","))
                        )
                        .build()
                )
                responseObserver?.onCompleted()
            }
        }
    }

    override fun joinInteractStream(responseObserver: StreamObserver<InteractStreamUpdate>?): StreamObserver<InteractStreamUpdate> {
        log.info("Received request to join interact stream...")
        randomFailure(responseObserver)
        return object : StreamObserver<InteractStreamUpdate> {
            override fun onNext(streamUpdate: InteractStreamUpdate) {
                log.info("Got audio and video from client during interact stream: {}", fromProtoInteractStreamUpdate(streamUpdate))
                if ("Hey".equals(fromProtoAudioChunk(streamUpdate.audioChunk))) {
                    responseObserver?.onNext(
                        InteractStreamUpdate.newBuilder()
                            .setAudioChunk(
                                AudioChunk.newBuilder()
                                    .setAudioData(copyFrom("Hey! How are you doing?".toByteArray()))
                                    .build()
                            )
                            .setVideoFrame(
                                VideoFrame.newBuilder()
                                    .setFrameData(copyFrom("ServerMuzzle".toByteArray()))
                                    .build()
                            )
                            .build()
                    )
                }
            }

            override fun onError(throwable: Throwable) {
                log.error("An error occurred while trying to get audio and video", throwable)
            }

            override fun onCompleted() {
                log.info("Interact stream has finally ended...")
                responseObserver?.onNext(
                    InteractStreamUpdate.newBuilder()
                        .setAudioChunk(
                            AudioChunk.newBuilder()
                                .setAudioData(copyFrom("It was a pleasure talking to you. Bye!".toByteArray()))
                                .build()
                        )
                        .setVideoFrame(
                            VideoFrame.newBuilder()
                                .setFrameData(copyFrom("ServerMuzzle".toByteArray()))
                                .build()
                        )
                        .build()
                )
                responseObserver?.onCompleted()
            }
        }
    }
}

fun main() {
    //grpc allows both GZIP and no compression by default
    val server = ServerBuilder.forPort(9030)
        //use secure channel with TLS certificates
        .useTransportSecurity(
            Files.newInputStream(Paths.get("tls_credentials/localhost.crt")),
            Files.newInputStream(Paths.get("tls_credentials/localhost.key"))
        )
        .addService(GrpcCrashingServer())
        .intercept(ServerJwtInterceptor())
        .build()
    server.start()
    server.awaitTermination()
}