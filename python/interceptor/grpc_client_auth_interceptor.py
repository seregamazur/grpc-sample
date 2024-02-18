import os

import grpc
import jwt


class AuthInterceptor(grpc.AuthMetadataPlugin):
    def __call__(self, context, callback):
        token = self._create_jwt_token()
        # NOTE: The metadata keys provided to the callback must be lower-cased.
        callback((('authorization', f'Bearer {token}'),), None)

    def _create_jwt_token(self):
        payload = {'sub': 'python-client'}
        return jwt.encode(payload, os.environ.get('JWT_SECRET'), algorithm='HS256')
