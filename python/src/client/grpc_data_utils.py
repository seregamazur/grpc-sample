import social_media_stream_pb2 as grpc_message_type


def _create_stream_request(provider_name='40_tonn', quality='4k'):
    return grpc_message_type.WatchStreamRequest(provider_name=provider_name, quality=quality)


def _from_proto_stream_update(stream_update):
    audio_data = stream_update.audio_chunk.audio_data.decode('utf-8')
    video_data = stream_update.video_frame.frame_data.decode('utf-8')
    return f'{audio_data}, {video_data}'


def _from_proto_stream(stream):
    audio_data = stream.audio_chunk.audio_data.decode('utf-8')
    video_data = stream.video_frame.frame_data.decode('utf-8')
    return video_data, audio_data


def _ordinal_stream_update(order_number):
    return grpc_message_type.StreamUpdate(
        video_frame=grpc_message_type.VideoFrame(frame_data=bytes(f'video_frame_data{order_number}', 'utf-8')),
        audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes(f'audio_data{order_number}', 'utf-8'))
    )


def _ordinal_interact_stream_update(order_number):
    return grpc_message_type.InteractStreamUpdate(
        provider_name='Python Client',
        video_frame=grpc_message_type.VideoFrame(frame_data=bytes(f'video_frame_data{order_number}', 'utf-8')),
        audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes(f'audio_data{order_number}', 'utf-8'))
    )


def _generate_stream_data():
    for update in range(3):
        stream_update = _ordinal_stream_update(update)
        yield stream_update


def _generate_interact_stream_data():
    greeting_update = grpc_message_type.InteractStreamUpdate(
        provider_name='Python Client',
        video_frame=grpc_message_type.VideoFrame(frame_data=bytes('ClientSmileMuzzle', 'utf-8')),
        audio_chunk=grpc_message_type.AudioChunk(audio_data=bytes('Hey', 'utf-8'))
    )
    yield greeting_update

    for update in range(3):
        stream_update = _ordinal_interact_stream_update(update)
        yield stream_update
