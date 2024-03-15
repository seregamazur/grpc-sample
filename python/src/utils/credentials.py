import os


def _load_credential_from_file(filepath):
    real_path = os.path.join(os.path.dirname(__file__), filepath)
    with open(real_path, "rb") as f:
        return f.read()


server_host = os.environ.get("SERVER_HOST")

if server_host == "localhost":
    SERVER_CERTIFICATE_KEY = _load_credential_from_file(f"../../../tls_credentials/{server_host}.key")
    SERVER_CERTIFICATE = _load_credential_from_file(f"../../../tls_credentials/{server_host}.crt")
else:
    SERVER_CERTIFICATE_KEY = _load_credential_from_file(f"../../tls_credentials/{server_host}.key")
    SERVER_CERTIFICATE = _load_credential_from_file(f"../../tls_credentials/{server_host}.crt")
