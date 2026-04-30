package com.learn.graphql.config;

import com.learn.graphql.entity.Comment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class CommentPublisher {

    // onBackpressureBuffer + autoCancel=false: buffers items, sink never auto-terminates on cancel
    private record CommentEvent(Comment comment, Long postId) {}

    private final Sinks.Many<CommentEvent> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);

    // postId passed explicitly so the filter never touches the lazy JPA proxy
    public void publish(Comment comment, Long postId) {
        log.info("Publishing comment id={} for postId={}", comment.getId(), postId);
        sink.tryEmitNext(new CommentEvent(comment, postId));
    }

    public Flux<Comment> getFlux(Long postId) {
        return sink.asFlux()
                .doOnNext(e -> log.info("Sink emitting event postId={} to filter for postId={}", e.postId(), postId))
                .filter(event -> event.postId().equals(postId))
                .map(CommentEvent::comment)
                .publishOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> log.info("New subscriber for postId={}", postId))
                .doOnCancel(() -> log.info("Subscriber cancelled for postId={}", postId));
    }
}
