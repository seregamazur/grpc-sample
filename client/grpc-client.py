import logging as log

import grpc

import social_media_stream_pb2_grpc as grpc_stubs, social_media_stream_pb2 as grpc_message_type


class GrpcClient:
    def __init__(self):
        self.channel = grpc.insecure_channel('localhost:9030')
        self.stub = grpc_stubs.SocialMediaStreamServiceStub(self.channel)
        log.basicConfig(level=log.INFO, format='%(funcName)s - %(levelname)s - %(message)s')

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
        responses = self.stub.watchStream(request)
        for response in responses:
            # Wow! Python can return tuple of few variables and use it next way to paste them into log!
            log.info('40_tonn showed %s and said: %s. Very wise!' % self._from_proto_stream(response))

    def start_stream(self):
        response = self.stub.startStream(self._generate_stream_data())
        log.info('Server response after streaming: %s', response.message)

    def join_interact_stream(self):
        responses = self.stub.joinInteractStream(self._generate_interact_stream_data())
        for response in responses:
            log.info('Server response during interact streaming: %s', self._from_proto_stream_update(response))


if __name__ == '__main__':
    client = GrpcClient()
    client.download_stream()
    client.watch_stream()
    client.start_stream()
    client.join_interact_stream()
