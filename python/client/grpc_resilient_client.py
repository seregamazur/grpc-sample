import logging as log
import time

import grpc

import _credentials
import social_media_stream_pb2_grpc as grpc_stubs
from client.grpc_data_utils import _create_stream_request, _from_proto_stream, _generate_stream_data, _generate_interact_stream_data, \
    _from_proto_stream_update
from interceptor.grpc_client_auth_interceptor import AuthInterceptor
from interceptor.grpc_client_circuit_breaker import CircuitBreakerClientInterceptor
from interceptor.grpc_client_retry_handler import RetryOnRpcErrorClientInterceptor, ExponentialBackoff


class GrpcResilientClient:

    def __init__(self):
        log.basicConfig(level=log.INFO, format='%(levelname)s - %(filename)s - %(message)s')
        tls_channel = self._create_tls_channel()
        interceptors = [
            RetryOnRpcErrorClientInterceptor(
                max_attempts=3, sleeping_policy=ExponentialBackoff(init_backoff_ms=500, max_backoff_ms=4_000, multiplier=2),
                status_for_retry=(grpc.StatusCode.CANCELLED,)),
            CircuitBreakerClientInterceptor(failure_threshold=3, recovery_timeout=5)
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
        response = self.stub.startStream(_generate_stream_data(), wait_for_ready=True, timeout=2)
        log.info('Server response after streaming: %s', response.message)

    def join_interact_stream(self):
        log.info('Sending streaming requests interact...')
        responses = self.stub.joinInteractStream(_generate_interact_stream_data(), wait_for_ready=True, timeout=2)
        for response in responses:
            log.info('Server response during interact streaming: %s', _from_proto_stream_update(response))

    @staticmethod
    def _create_tls_channel():
        call_credentials = grpc.metadata_call_credentials(
            AuthInterceptor(), name="auth gateway"
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

        tls_channel = grpc.secure_channel('localhost:9030', composite_credentials)
        return tls_channel


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
