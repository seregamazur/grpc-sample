import abc
import logging as log
import time
from random import randint
from typing import List

import grpc


class SleepingPolicy(abc.ABC):
    @abc.abstractmethod
    def sleep(self, try_i: int):
        """
        How long to sleep in milliseconds.
        :param try_i: the number of retry (starting from zero)
        """
        assert try_i >= 0


class ExponentialBackoff(SleepingPolicy):
    def __init__(self, *, init_backoff_ms: int, max_backoff_ms: int, multiplier: int):
        self.init_backoff = randint(0, init_backoff_ms)
        self.max_backoff = max_backoff_ms
        self.multiplier = multiplier

    def sleep(self, try_i: int):
        sleep_range = min(
            self.init_backoff * self.multiplier ** try_i, self.max_backoff
        )
        sleep_ms = randint(0, sleep_range)
        log.debug(f"Sleeping for {sleep_ms}")
        time.sleep(sleep_ms / 1000)


class RetryOnRpcErrorClientInterceptor(
    grpc.UnaryUnaryClientInterceptor, grpc.UnaryStreamClientInterceptor, grpc.StreamUnaryClientInterceptor,
    grpc.StreamStreamClientInterceptor
):

    def __init__(
            self,
            *,
            max_attempts: int,
            sleeping_policy: SleepingPolicy,
            status_for_retry: List[grpc.StatusCode]
    ):
        self.max_attempts = max_attempts
        self.sleeping_policy = sleeping_policy
        self.status_for_retry = status_for_retry

    def _intercept_unary_call(self, continuation, client_call_details, request_or_iterator):

        for try_i in range(self.max_attempts):
            response = continuation(client_call_details, request_or_iterator)
            if isinstance(response, grpc.RpcError):
                # Return if it was last attempt
                if try_i == (self.max_attempts - 1):
                    return response

                # If status code is not in retryable status codes
                if response.code() not in self.status_for_retry:
                    return response
                self.sleeping_policy.sleep(try_i)
            else:
                return response

    def _intercept_stream_call(self, continuation, client_call_details, request_or_iterator):

        for try_i in range(self.max_attempts):
            response = continuation(client_call_details, request_or_iterator)

            try:
                for response in response:
                    yield response
                break
            except grpc.RpcError as e:
                if response.code() not in self.status_for_retry or try_i == self.max_attempts - 1:
                    return response
                else:
                    self.sleeping_policy.sleep(try_i)

    def intercept_unary_unary(self, continuation, client_call_details, request):
        return self._intercept_unary_call(continuation, client_call_details, request)

    def intercept_stream_unary(
            self, continuation, client_call_details, request_iterator
    ):
        return self._intercept_unary_call(continuation, client_call_details, request_iterator)

    def intercept_unary_stream(self, continuation, client_call_details, request):
        return self._intercept_stream_call(continuation, client_call_details, request)

    def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
        return self._intercept_stream_call(continuation, client_call_details, request_iterator)
