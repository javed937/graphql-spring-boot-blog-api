package com.learn.graphql.config;

import com.learn.graphql.entity.Comment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
public class CommentPublisher {

    // multicast sink — multiple subscribers can listen, events are buffered if no subscriber yet
    private final Sinks.Many<Comment> sink = Sinks.many().multicast().onBackpressureBuffer();

    // called by BlogService whenever a comment is saved
    public void publish(Comment comment) {
        log.info("Publishing comment id={} for postId={}", comment.getId(), comment.getPost().getId());
        sink.tryEmitNext(comment);
    }

    // each subscription gets the full stream filtered to their postId
    public Flux<Comment> getFlux(Long postId) {
        return sink.asFlux()
                .filter(comment -> comment.getPost().getId().equals(postId))
                .doOnSubscribe(s -> log.info("New subscriber for postId={}", postId))
                .doOnCancel(() -> log.info("Subscriber cancelled for postId={}", postId));
    }
}
