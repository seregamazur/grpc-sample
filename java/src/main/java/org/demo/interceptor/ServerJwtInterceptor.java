package org.demo.interceptor;

import java.util.List;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Validate incoming JWT
 */
public class ServerJwtInterceptor implements ServerInterceptor {

    private static final String BEARER_TYPE = "Bearer";
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    private static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");
    private static final List<String> CLIENTS = List.of("kotlin-client", "python-client", "java-client");

    private final JwtParser parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").getBytes())).build();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String value = headers.get(AUTHORIZATION_METADATA_KEY);

        Status status;
        try {
            String token = value.substring(BEARER_TYPE.length()).trim();
            Jws<Claims> claims = parser.parseSignedClaims(token);
            assert CLIENTS.stream().anyMatch(e -> e.equalsIgnoreCase(claims.getPayload().getSubject()));
            Context ctx = Context.current().withValue(CLIENT_ID_CONTEXT_KEY, claims.getPayload().getSubject());
            return Contexts.interceptCall(ctx, call, headers, next);
        } catch (JwtException | IllegalArgumentException e) {
            status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            call.close(status, headers);
        }
        return new ServerCall.Listener<>() {
        };
    }

}

