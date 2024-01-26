import logging as log
from concurrent import futures

import grpc

import _credentials
import social_media_stream_pb2
import social_media_stream_pb2_grpc
from interceptor import grpc_server_auth_interceptor
from server.grpc_auth_server import GrpcAuthServer


class GrpcCrashingServer(social_media_stream_pb2_grpc.SocialMediaStreamServiceServicer):

    def __init__(self):
        self.original_server = GrpcAuthServer()
        self.failure_counts = {
            "downloadStream": 0,
            "watchStream": 0,
            "startStream": 0,
            "joinInteractStream": 0,
        }

    def simulate_failure(self, max_failures, method_name):
        return self.failure_counts[method_name] < max_failures

    def downloadStream(self, request, context):
        log.info(f'Received request to download stream from {request.provider_name} using quality {request.quality}')
        if self.simulate_failure(3, "downloadStream"):
            self.failure_counts["downloadStream"] += 1
            context.set_code(grpc.StatusCode.CANCELLED)
            context.set_details('SIMULATION')
            return social_media_stream_pb2.Recording()
        else:
            return self.original_server.downloadStream(request, context)

    def watchStream(self, request, context):
        if self.simulate_failure(2, "watchStream"):
            self.failure_counts["watchStream"] += 1
            # context.abort(grpc.StatusCode.CANCELLED, "Simulated failure for joinInteractStream")
            context.set_code(grpc.StatusCode.CANCELLED)
            context.set_details('SIMULATION')
            return social_media_stream_pb2.StreamUpdate()
        else:
            return self.original_server.watchStream(request, context)

    def startStream(self, request_iterator, context):
        log.info('Received request from client to start stream...')
        if self.simulate_failure(2, "startStream"):
            self.failure_counts["startStream"] += 1
            context.set_code(grpc.StatusCode.CANCELLED)
            context.set_details('SIMULATION')
            return social_media_stream_pb2.StartStreamResponse()
        else:
            return self.original_server.startStream(request_iterator, context)

    def joinInteractStream(self, request_iterator, context):
        log.info('Received request to join interact stream...')
        if self.simulate_failure(2, "joinInteractStream"):
            self.failure_counts["joinInteractStream"] += 1
            context.abort(grpc.StatusCode.CANCELLED, "Simulated failure for joinInteractStream")
        else:
            return self.original_server.joinInteractStream(request_iterator, context)


def serve():
    interceptors = [grpc_server_auth_interceptor.GrpcAuthServerInterceptor()]
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10), interceptors=interceptors)
    # Loading credentials
    server_credentials = grpc.ssl_server_credentials(
        (
            (
                _credentials.SERVER_CERTIFICATE_KEY,
                _credentials.SERVER_CERTIFICATE,
            ),
        )
    )
    social_media_stream_pb2_grpc.add_SocialMediaStreamServiceServicer_to_server(GrpcCrashingServer(), server)
    server.add_secure_port('localhost:9030', server_credentials)
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    log.basicConfig(level=log.INFO, format='%(funcName)s - %(levelname)s - %(message)s')
    serve()
