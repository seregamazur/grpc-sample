# Dockerfile
FROM python:3.8-slim-buster
WORKDIR /app

COPY python /app
COPY proto /proto

RUN pip install --no-cache-dir -r requirements.txt && python -c "from grpc_tools import protoc; protoc.main(['', '--python_out=.', '--grpc_python_out=.', '--proto_path=/proto', 'social-media-stream.proto'])"

COPY tls_credentials /app/tls_credentials

CMD ["python", "-m", "src.server.grpc_crashing_server"]
