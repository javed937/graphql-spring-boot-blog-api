package com.learn.graphql.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        if (ex instanceof AccessDeniedException) {
            log.warn("Access denied on field [{}]", env.getField().getName());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.FORBIDDEN)
                    .message("Access denied — you don't have permission to perform this action")
                    .extensions(java.util.Map.of("code", "FORBIDDEN"))
                    .build();
        }

        if (ex instanceof ResourceNotFoundException notFound) {
            log.warn("Not found: {} id={}", notFound.getResourceType(), notFound.getId());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(notFound.getMessage())
                    .extensions(java.util.Map.of(
                            "code", "NOT_FOUND",
                            "resource", notFound.getResourceType(),
                            "id", String.valueOf(notFound.getId())
                    ))
                    .build();
        }

        if (ex instanceof IllegalArgumentException) {
            log.warn("Bad input: {}", ex.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "BAD_REQUEST"))
                    .build();
        }

        // Unknown errors — log the full stack trace, return generic message
        log.error("Unhandled GraphQL error at field [{}]", env.getField().getName(), ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("An internal error occurred")
                .extensions(java.util.Map.of("code", "INTERNAL_ERROR"))
                .build();
    }
}
