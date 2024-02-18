import logging as log
import time
from typing import Any

import grpc


class CircuitBreakerClientInterceptor(grpc.UnaryUnaryClientInterceptor, grpc.UnaryStreamClientInterceptor,
                                      grpc.StreamUnaryClientInterceptor,
                                      grpc.StreamStreamClientInterceptor):
    CLOSED = 'CLOSED'
    OPENED = 'OPENED'
    HALF_OPENED = 'HALF_OPENED'

    def __init__(self, failure_threshold, recovery_timeout):
        log.basicConfig(level=log.INFO, format='%(levelname)s - %(_failure_threshold) - %(message)s')
        self._failure_threshold = failure_threshold
        self._recovery_timeout = recovery_timeout
        self._failure_count = 0
        self._state = self.CLOSED
        self._recovery_time = time.time()

    @property
    def failure_count(self):
        # Getter continuation
        return self._failure_count

    @failure_count.setter
    def failure_count(self, new_value):
        if new_value == self._failure_threshold:
            self.state = self.OPENED
            self._failure_count = 0
        elif new_value > self._failure_count and self._state == self.HALF_OPENED:
            self.state = self.OPENED
            self._failure_count = 0
        else:
            self._failure_count = new_value

    @property
    def state(self):
        # Getter continuation
        return self._state

    @state.setter
    def state(self, new_value):
        self._state = new_value
        log.info('Circuit breaker is now %s!', new_value)
        if new_value == self.OPENED:
            self._recovery_time = time.time() + self._recovery_timeout
        elif new_value == self.CLOSED:
            self.failure_count = 0

    def _intercept_unary_call(
            self,
            continuation,
            call_details: grpc.ClientCallDetails,
            request_or_iterator: Any,
    ):
        if not self._is_open_circuit():
            log.info('Circuit breaker is closed. Making call...')
            return self._call_unary(continuation, call_details, request_or_iterator)
        else:
            log.error('Circuit breaker is opened. Cannot make any call to server for the next %d seconds.',
                      self._recovery_time - time.time())

    def _intercept_stream_call(
            self,
            continuation,
            call_details: grpc.ClientCallDetails,
            request_or_iterator: Any,
    ):
        if not self._is_open_circuit():
            log.info('Circuit breaker is closed. Making call...')
            return self._call_stream(continuation, call_details, request_or_iterator)
        else:
            log.error('Circuit breaker is opened. Cannot make any call to server for the next %d seconds.',
                      self._recovery_time - time.time())

    def _call_unary(self,
                    continuation,
                    call_details: grpc.ClientCallDetails,
                    request_or_iterator: Any,
                    ):
        response = continuation(call_details, request_or_iterator)
        if isinstance(response, grpc.RpcError):
            self.failure_count += 1
        elif self.state == self.HALF_OPENED:
            self.state = self.CLOSED
            self.failure_count = 0
        return response

    def _call_stream(self,
                     continuation,
                     call_details: grpc.ClientCallDetails,
                     request_or_iterator: Any,
                     ):
        try:
            response = continuation(call_details, request_or_iterator)
            for response in response:
                yield response
        except grpc.RpcError as e:
            if response.code() == grpc.StatusCode.CANCELLED:
                self.failure_count += 1
                return response
            elif self.state == self.HALF_OPENED:
                self.state = self.CLOSED
                self.failure_count = 0
                return list(response)
            else:
                return list(response)

    def _is_open_circuit(self):
        if self._recovery_time < time.time() and self.state == self.OPENED:
            self.state = self.HALF_OPENED
        return self.state == self.OPENED or self._recovery_time > time.time()

    def intercept_unary_unary(self, continuation, client_call_details, request):
        return self._intercept_unary_call(continuation, client_call_details, request)

    def intercept_unary_stream(self, continuation, client_call_details, request):
        return self._intercept_stream_call(continuation, client_call_details, request)

    def intercept_stream_unary(self, continuation, client_call_details, request_iterator):
        return self._intercept_unary_call(continuation, client_call_details, request_iterator)

    def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
        return self._intercept_stream_call(continuation, client_call_details, request_iterator)
