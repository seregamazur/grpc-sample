import logging as log
import os

import grpc
import jwt


class GrpcAuthServerInterceptor(grpc.ServerInterceptor):
    clients = ["kotlin-client", "python-client", "java-client"]

    def intercept_service(self, continuation, handler_call_details):
        response = self.verify_jwt(continuation, handler_call_details)
        return response

    def intercept(self, continuation, call_details, context):
        # Call the RPC. It could be either unary or streaming
        response_or_iterator = self.verify_jwt(continuation, call_details)
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
                payload = jwt.decode(bearer.split(' ')[1], key=os.environ.get('JWT_SECRET'), algorithms=['HS256'])
                assert payload.get('sub') in self.clients

            return continuation(call_details)

        except Exception:
            log.error('An error occurred during decoding JWT token')
            raise

    def _intercept_streaming(self, iterator, context):
        try:
            for resp in iterator:
                yield resp
        except grpc.RpcError as e:
            context.set_code(resp.code())
            context.set_details(resp.details())
            raise
