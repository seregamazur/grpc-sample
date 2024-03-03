import logging as log
import random
from concurrent import futures

import grpc

from src.utils import credentials
import social_media_stream_pb2
import social_media_stream_pb2_grpc
from src.client.grpc_data_utils import _from_proto_stream
from src.interceptor import grpc_server_auth_interceptor


class GrpcCrashingServer(social_media_stream_pb2_grpc.SocialMediaStreamServiceServicer):

    @staticmethod
    def random_failure(context):
        if random.randint(0, 100) > 70:
            context.set_code(random.choice([grpc.StatusCode.CANCELLED, grpc.StatusCode.UNAVAILABLE, grpc.StatusCode.DEADLINE_EXCEEDED]))
            context.set_details('SIMULATION')
            raise grpc.RpcError('RPC cancelled')

    def downloadStream(self, request, context):
        log.info(f'Received request to download stream from {request.provider_name} using quality {request.quality}')
        self.random_failure(context)
        return social_media_stream_pb2.Recording(data=b'Recording Data')

    def watchStream(self, request, context):
        self.random_failure(context)
        for i in range(3):
            log.info(f'Returned response {i} to watch stream...')
            yield social_media_stream_pb2.StreamUpdate(audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=f'Audio{i}'.encode()),
                                                       video_frame=social_media_stream_pb2.VideoFrame(frame_data=f'Video{i}'.encode()))

    def startStream(self, request_iterator, context):
        log.info('Received request from client to start stream...')
        self.random_failure(context)
        updates = []
        try:
            for stream_update in request_iterator:
                log.info(f'Got audio and video from client stream: {stream_update}')
                updates.append(stream_update)
        except grpc.RpcError as e:
            log.error(f'An error occurred while trying to get audio and video: {e}')

        log.info('Client stream has finally ended...')

        message = f'We got your words from the stream! They are:{updates}'

        return social_media_stream_pb2.StartStreamResponse(message=message)

    def joinInteractStream(self, request_iterator, context):
        log.info('Received request to join interact stream...')
        self.random_failure(context)
        try:
            for stream_update in request_iterator:
                log.info(f'Got audio and video from client during interact stream: {stream_update}')
                if 'Hey' == _from_proto_stream(stream_update)[1]:
                    log.info(f'Sending Hey during interact stream: {stream_update}')
                    yield social_media_stream_pb2.InteractStreamUpdate(
                        audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=b'Hey! How are you doing?'),
                        video_frame=social_media_stream_pb2.VideoFrame(frame_data=b'ServerMuzzle')
                    )
        except grpc.RpcError as e:
            log.error(f'An error occurred while trying to get audio and video: {e}')

        finally:
            log.info('Interact stream has finally ended...')
            yield social_media_stream_pb2.InteractStreamUpdate(
                audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=b'It was a pleasure talking to you. Bye!'),
                video_frame=social_media_stream_pb2.VideoFrame(frame_data=b'ServerMuzzle')
            )


def serve():
    interceptors = [grpc_server_auth_interceptor.GrpcAuthServerInterceptor()]
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10), interceptors=interceptors)
    # Loading credentials
    server_credentials = grpc.ssl_server_credentials(
        (
            (
                credentials.SERVER_CERTIFICATE_KEY,
                credentials.SERVER_CERTIFICATE,
            ),
        )
    )
    # grpc allows both GZIP and no compression by default
    social_media_stream_pb2_grpc.add_SocialMediaStreamServiceServicer_to_server(GrpcCrashingServer(), server)
    server.add_secure_port('localhost:9030', server_credentials)
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    log.basicConfig(level=log.INFO, format='%(funcName)s - %(levelname)s - %(message)s')
    serve()
