import logging as log
import os

import grpc

import social_media_stream_pb2_grpc as grpc_stubs
from src.client.grpc_data_utils import _create_stream_request, _from_proto_stream, _generate_stream_data, _generate_interact_stream_data, \
    _from_proto_stream_update
from src.interceptor.grpc_client_auth_interceptor import AuthInterceptor
from src.interceptor.grpc_client_circuit_breaker import CircuitBreakerClientInterceptor
from src.interceptor.grpc_client_retry_handler import RetryOnRpcErrorClientInterceptor, ExponentialBackoff
from src.utils import credentials


class GrpcResilientClient:

    def __init__(self):
        log.basicConfig(level=log.INFO, format='%(levelname)s - %(filename)s - %(message)s')
        tls_channel = self._create_tls_channel()
        status_for_retry = [grpc.StatusCode.CANCELLED, grpc.StatusCode.UNAVAILABLE, grpc.StatusCode.DEADLINE_EXCEEDED]
        interceptors = [
            RetryOnRpcErrorClientInterceptor(
                max_attempts=3, sleeping_policy=ExponentialBackoff(init_backoff_ms=500, max_backoff_ms=5_000, multiplier=2),
                status_for_retry=status_for_retry),
            CircuitBreakerClientInterceptor(failure_threshold=3, recovery_timeout=5, status_for_retry=status_for_retry)
        ]
        # interceptor channel over the tls channel, so we take advantage from both
        self.channel = grpc.intercept_channel(tls_channel, *interceptors)
        self.stub = grpc_stubs.SocialMediaStreamServiceStub(self.channel)

    def download_stream(self):
        request = _create_stream_request()
        log.info('Sending request to download stream from %s using quality %s', request.provider_name, request.quality)
        response = self.stub.downloadStream(request, wait_for_ready=True, timeout=2)
        log.info('Stream has been downloaded, data=%s', response.data.decode('utf-8'))

    def watch_stream(self):
        request = _create_stream_request()
        log.info('Sending request to watch stream from %s using quality %s', request.provider_name, request.quality)
        responses = self.stub.watchStream(request, wait_for_ready=True, timeout=2)
        for response in responses:
            # Wow! Python can return tuple of few variables and use it next way to paste them into log!
            log.info('40_tonn showed %s and said: %s. Very wise!' % _from_proto_stream(response))
        log.info('Watch stream has ended')

    def start_stream(self):
        log.info('Sending streaming requests...')
        response = self.stub.startStream(_generate_stream_data(), wait_for_ready=True, timeout=5)
        log.info('Server response after streaming: %s', response.message)

    def join_interact_stream(self):
        log.info('Sending streaming requests interact...')
        responses = self.stub.joinInteractStream(_generate_interact_stream_data(), wait_for_ready=True, timeout=5)
        for response in responses:
            log.info('Server response during interact streaming: %s', _from_proto_stream_update(response))

    @staticmethod
    def _create_tls_channel():
        call_credentials = grpc.metadata_call_credentials(
            AuthInterceptor(), name="auth gateway"
        )
        # # Channel credential will be valid for the entire channel
        channel_credential = grpc.ssl_channel_credentials(
            credentials.SERVER_CERTIFICATE
        )
        # # Combining channel credentials and call credentials together
        composite_credentials = grpc.composite_channel_credentials(
            channel_credential,
            call_credentials,
        )

        tls_channel = grpc.secure_channel(f"{os.environ.get('SERVER_HOST')}:{os.environ.get('SERVER_PORT')}",
                                          composite_credentials, compression=grpc.Compression.Gzip)
        return tls_channel


if __name__ == '__main__':
    # run auth client
    auth_client = GrpcResilientClient()
    auth_client.download_stream()
    auth_client.watch_stream()
    auth_client.start_stream()
    auth_client.join_interact_stream()
