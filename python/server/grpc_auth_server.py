import logging as log
from concurrent import futures

import grpc

import social_media_stream_pb2
import social_media_stream_pb2_grpc
from interceptor import grpc_server_auth_interceptor


class GrpcAuthServer(social_media_stream_pb2_grpc.SocialMediaStreamServiceServicer):

    def downloadStream(self, request, context):
        log.info(f'Received request to download stream from {request.provider_name} using quality {request.quality}')

        response = social_media_stream_pb2.Recording(data=b'Recording Data')
        return response

    def watchStream(self, request, context):
        log.info(f'Received request to watch stream from {request.provider_name} using quality {request.quality}')

        for i in range(3):
            log.info(f'Returned response {i} to watch stream...')
            yield social_media_stream_pb2.StreamUpdate(audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=f'Audio{i}'.encode()),
                                                       video_frame=social_media_stream_pb2.VideoFrame(frame_data=f'Video{i}'.encode()))

    def startStream(self, request_iterator, context):
        log.info('Received request from client to start stream...')

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

        try:
            for stream_update in request_iterator:
                log.info(f'Got audio and video from client during interact stream: {stream_update}')
                if 'Hey' == stream_update.audio_chunk:
                    yield social_media_stream_pb2.InteractStreamUpdate(
                        audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=b'Hey! How are you doing?'),
                        video_frame=social_media_stream_pb2.VideoFrame(frame_data=b'ServerMuzzle')
                    )
        except grpc.RpcError as e:
            log.error(f'An error occurred while trying to get audio and video: {e}')

        log.info('Interact stream has finally ended...')

        return social_media_stream_pb2.InteractStreamUpdate(
            audio_chunk=social_media_stream_pb2.AudioChunk(audio_data=b'It was a pleasure talking to you. Bye!'),
            video_frame=social_media_stream_pb2.VideoFrame(frame_data=b'ServerMuzzle')
        )


def serve():
    interceptors = [grpc_auth_server_interceptor.GrpcAuthServerInterceptor()]
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10), interceptors=interceptors)
    social_media_stream_pb2_grpc.add_SocialMediaStreamServiceServicer_to_server(GrpcAuthServer(), server)
    server.add_insecure_port('localhost:9030')
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    log.basicConfig(level=log.INFO, format='%(funcName)s - %(levelname)s - %(message)s')
    serve()
