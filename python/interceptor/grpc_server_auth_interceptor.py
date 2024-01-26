import logging as log
import os

import grpc
import jwt
from grpc_interceptor.exceptions import GrpcException

'''
Unary RPC server and Streaming RPC server handled in different way.
In order to unify both interceptors in 1 file we need to handle the unary case and streaming case separately (intercept & _intercept_streaming)
'''


class GrpcAuthServerInterceptor(grpc.ServerInterceptor):

    def intercept_service(self, continuation, handler_call_details):
        response = self.verify_jwt(continuation, handler_call_details)
        return response

    def intercept(self, method, request, context, method_name):
        # Call the RPC. It could be either unary or streaming
        response_or_iterator = self.verify_jwt(context, method, request)
        if hasattr(response_or_iterator, "__iter__"):
            # Now we know it's a server streaming RPC, so the actual RPC method
            # hasn't run yet. Delegate to a helper to iterate over it so it runs.
            # The helper needs to re-yield the responses, and we need to return
            # the generator that produces.
            return self._intercept_streaming(response_or_iterator, context)
        else:
            # For unary cases, we are done, so just return the response.
            return response_or_iterator

    def verify_jwt(self, continuation, call_details):
        try:
            headers = dict(call_details.invocation_metadata)
            bearer = headers.get('authorization')

            if bearer:
                jwt.decode(bearer.split(' ')[1], key=os.environ.get('JWT_SECRET'), algorithms=['HS256'])

            return continuation(call_details)

        except GrpcException as e:
            # context.set_code(e.status_code)
            # context.set_details(e.details)
            raise

        except Exception:
            log.error('An error occurred during decoding JWT token')
            # context.set_code(StatusCode.UNAUTHENTICATED)
            # context.set_details('Unauthorized')
            # return context  # Return the context instead of response_or_iterator

    def _intercept_streaming(self, iterator, context):
        try:
            for resp in iterator:
                yield resp
        except GrpcException as e:
            context.set_code(e.status_code)
            context.set_details(e.details)
            raise
