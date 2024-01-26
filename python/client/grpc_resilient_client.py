import logging as log
import time

import grpc

import _credentials
import social_media_stream_pb2 as grpc_message_type
import social_media_stream_pb2_grpc as grpc_stubs
from interceptor.grpc_client_auth_interceptor import AuthGateway
from interceptor.grpc_client_circuit_breaker import CircuitBreakerClientInterceptor
from interceptor.grpc_client_retry_handler import RetryOnRpcErrorClientInterceptor, ExponentialBackoff


class GrpcResilientClient:

    def __init__(self):
        log.basicConfig(level=log.INFO, format='%(funcName)s - %(levelname)s - %(message)s')
        call_credentials = grpc.metadata_call_credentials(
            AuthGateway(), name="auth gateway"
        )
        # # Channel credential will be valid for the entire channel
        channel_credential = grpc.ssl_channel_credentials(
            _credentials.ROOT_CERTIFICATE
        )
        # # Combining channel credentials and call credentials together
        composite_credentials = grpc.composite_channel_credentials(
            channel_credential,
            call_credentials,
        )
        interceptors = [
            RetryOnRpcErrorClientInterceptor(
                max_attempts=3, sleeping_policy=ExponentialBackoff(init_backoff_ms=500, max_backoff_ms=4_000, multiplier=2),
                status_for_retry=(grpc.StatusCode.CANCELLED,)),
            CircuitBreakerClientInterceptor()
        ]
        secure_channel = grpc.secure_channel('localhost:9030', composite_credentials)
        self.channel = grpc.intercept_channel(secure_channel, *interceptors)
        self.stub = grpc_stubs.SocialMediaStreamServiceStub(self.channel)

    def _receive_stream_request(self, provider_name='40_tonn', quality='4k'):
        return grpc_message_type.WatchStreamRequest(provider_name=provider_name, quality=quality)

    def _from_proto_stream_update(self, stream_update):
        audio_data = stream_update.audio_chunk.audio_data.decode('utf-8')
        video_data = stream_update.video_frame.frame_data.decode('utf-8')
        return f'{audio_data}, {video_data}'

    def _from_proto_stream(self, stream):
        audio_data = stream.audio_chunk.audio_data.decode('utf-8')
        video_data = stream.video_frame.frame_data.decode('utf-8')
        return video_data, audio_data

    def _ordinal_stream_update(self, order_number):
        return grpc_message_type.StreamUpdate(
            video_frame=grpc_message_type.VideoFrame(frame_data=bytes(f'video_frame_data{order_number}', 'utf-8')),
            audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes(f'audio_data{order_number}', 'utf-8'))
        )

    def _ordinal_interact_stream_update(self, order_number):
        return grpc_message_type.InteractStreamUpdate(
            provider_name='Python Client',
            video_frame=grpc_message_type.VideoFrame(frame_data=bytes(f'video_frame_data{order_number}', 'utf-8')),
            audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes(f'audio_data{order_number}', 'utf-8'))
        )

    def _generate_stream_data(self):
        for update in range(3):
            stream_update = self._ordinal_stream_update(update)
            log.info('Sending streaming request %d: %s', update, self._from_proto_stream_update(stream_update))
            yield stream_update

    def _generate_interact_stream_data(self):
        greeting_update = grpc_message_type.InteractStreamUpdate(
            provider_name='Python Client',
            video_frame=grpc_message_type.VideoFrame(frame_data=bytes('ClientSmileMuzzle', 'utf-8')),
            audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes('Hey', 'utf-8'))
        )
        log.info('Sending greeting request: %s', self._from_proto_stream_update(greeting_update))
        yield greeting_update

        for update in range(3):
            stream_update = self._ordinal_interact_stream_update(update)
            log.info('Sending streaming request %d: %s', update, self._from_proto_stream_update(stream_update))
            yield stream_update

    def download_stream(self):
        request = self._receive_stream_request()
        log.info('Sending request to download stream from %s using quality %s', request.provider_name, request.quality)
        response = self.stub.downloadStream(request)
        log.info('Stream has been downloaded, data=%s', response.data.decode('utf-8'))

    def watch_stream(self):
        request = self._receive_stream_request()
        log.info('Sending request to watch stream from %s using quality %s', request.provider_name, request.quality)
        # responses = self.stub.watchStream(request, wait_for_ready=True, timeout=7)
        responses = self.stub.watchStream(request)
        for response in responses:
            # Wow! Python can return tuple of few variables and use it next way to paste them into log!
            log.info('40_tonn showed %s and said: %s. Very wise!' % self._from_proto_stream(response))
        log.info('Watch stream has ended')

    def start_stream(self):
        response = self.stub.startStream(self._generate_stream_data())
        log.info('Server response after streaming: %s', response.message)

    def join_interact_stream(self):
        responses = self.stub.joinInteractStream(self._generate_interact_stream_data())
        for response in responses:
            log.info('Server response during interact streaming: %s', self._from_proto_stream_update(response))


if __name__ == '__main__':
    # run auth client
    auth_client = GrpcResilientClient()
    try:
        auth_client.download_stream()
    except Exception as e:
        print(f"Expected error in download stream method (circuit breaker should be opened): {e}")
    time.sleep(6)

    auth_client.download_stream()
    time.sleep(6)

    auth_client.watch_stream()
    time.sleep(6)

    try:
        auth_client.start_stream()
    except Exception as e:
        print(f"Expected error in start stream method (circuit breaker should be opened): {e}")
    time.sleep(6)
    auth_client.start_stream()
    auth_client.join_interact_stream()
