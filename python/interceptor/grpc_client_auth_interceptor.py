import os

import grpc
import jwt


class AuthGateway(grpc.AuthMetadataPlugin):
    def __call__(self, context, callback):
        """Implements authentication by passing metadata to a callback.

        Implementations of this method must not block.

        Args:
          context: An AuthMetadataContext providing information on the RPC that
            the plugin is being called to authenticate.
          callback: An AuthMetadataPluginCallback to be invoked either
            synchronously or asynchronously.
        """
        # Example AuthMetadataContext object:
        # AuthMetadataContext(
        #     service_url=u'https://localhost:50051/helloworld.Greeter',
        #     method_name=u'SayHello')
        token = self._create_jwt_token()
        # NOTE: The metadata keys provided to the callback must be lower-cased.
        callback((('authorization', f'Bearer {token}'),), None)

    def _create_jwt_token(self):
        payload = {'sub': 'python-client'}
        return jwt.encode(payload, os.environ.get('JWT_SECRET'), algorithm='HS256')


# class GrpcAuthClientInterceptor(
#     grpc.UnaryUnaryClientInterceptor, grpc.UnaryStreamClientInterceptor,
#     grpc.StreamUnaryClientInterceptor, grpc.StreamStreamClientInterceptor
# ):
#
#     def _intercept_call(self, continuation, client_call_details, request_or_iterator):
#         if not client_call_details.metadata or 'authorization' not in client_call_details.metadata:
#             token = self._create_jwt_token()
#             return continuation(client_call_details, request_or_iterator)
#
#     def _create_jwt_token(self):
#         payload = {'sub': 'python-client'}
#         return jwt.encode(payload, os.environ.get('JWT_SECRET'), algorithm='HS256')
#
#     def intercept_unary_unary(self, continuation, client_call_details, request):
#         return self._intercept_call(continuation, client_call_details, request)
#
#     def intercept_unary_stream(self, continuation, client_call_details, request):
#         return self._intercept_call(continuation, client_call_details, request)
#
#     def intercept_stream_unary(self, continuation, client_call_details, request_iterator):
#         return self._intercept_call(continuation, client_call_details, request_iterator)
#
#     def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
#         return self._intercept_call(continuation, client_call_details, request_iterator)
