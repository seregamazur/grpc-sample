import os


def _load_credential_from_file(filepath):
    real_path = os.path.join(os.path.dirname(__file__), filepath)
    with open(real_path, "rb") as f:
        return f.read()


SERVER_CERTIFICATE_KEY = _load_credential_from_file("../../tls_credentials/grpc-crashing-server.key")
SERVER_CERTIFICATE = _load_credential_from_file("../../tls_credentials/grpc-crashing-server.crt")
