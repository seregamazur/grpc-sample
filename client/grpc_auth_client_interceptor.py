import os
from typing import Callable, Any

import grpc
import jwt
from grpc_interceptor import ClientCallDetails, ClientInterceptor


class GrpcAuthClientInterceptor(ClientInterceptor):

    def intercept(
            self,
            method: Callable,
            request_or_iterator: Any,
            call_details: grpc.ClientCallDetails,
    ):
        token = self.create_jwt_token()
        new_details = ClientCallDetails(
            call_details.method,
            call_details.timeout,
            [("authorization", f"Bearer {token}")],
            call_details.credentials,
            call_details.wait_for_ready,
            call_details.compression,
        )

        return method(request_or_iterator, new_details)

    def create_jwt_token(self):
        payload = {'sub': 'python-client'}
        return jwt.encode(payload, os.environ.get('JWT_SECRET'), algorithm='HS256')
