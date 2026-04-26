package com.learn.graphql.config;

import com.learn.graphql.entity.Comment;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import com.learn.graphql.service.BlogService;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class DataLoaderConfig {

    public DataLoaderConfig(BatchLoaderRegistry registry, BlogService blogService) {

        // Collects all userIds seen in one request, fires ONE query: WHERE author_id IN (...)
        registry.<Long, List<Post>>forName("postsForUser")
                .registerMappedBatchLoader((authorIds, env) ->
                        Mono.fromCallable(() -> blogService.getPostsByAuthorIds(authorIds)));

        // Collects all postIds seen in one request, fires ONE query: WHERE post_id IN (...)
        registry.<Long, List<Comment>>forName("commentsForPost")
                .registerMappedBatchLoader((postIds, env) ->
                        Mono.fromCallable(() -> blogService.getCommentsByPostIds(postIds)));

        // Shared loader for any field that needs a User by ID (Post.author, Comment.author)
        // Fires ONE query: WHERE id IN (...)
        registry.<Long, User>forName("userById")
                .registerMappedBatchLoader((userIds, env) ->
                        Mono.fromCallable(() -> blogService.getUsersByIds(userIds)));
    }
}
