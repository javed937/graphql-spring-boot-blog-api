package com.learn.graphql.controller;

import com.learn.graphql.entity.Comment;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import com.learn.graphql.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.learn.graphql.config.CommentPublisher;
import com.learn.graphql.dto.PostConnection;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final CommentPublisher commentPublisher;

    // ---- Queries ----

    @QueryMapping
    public List<User> users() {
        log.info("Query: users()");
        List<User> result = blogService.getAllUsers();
        log.info("Query: users() -> {} results", result.size());
        return result;
    }

    @QueryMapping
    public User user(@Argument Long id) {
        log.info("Query: user(id={})", id);
        return blogService.getUserById(id);
    }

    @QueryMapping
    public List<Post> posts() {
        log.info("Query: posts()");
        List<Post> result = blogService.getAllPosts();
        log.info("Query: posts() -> {} results", result.size());
        return result;
    }

    @QueryMapping
    public Post post(@Argument Long id) {
        log.info("Query: post(id={})", id);
        return blogService.getPostById(id);
    }

    @QueryMapping
    public List<Post> postsByUser(@Argument Long userId) {
        log.info("Query: postsByUser(userId={})", userId);
        return blogService.getPostsByUser(userId);
    }

    @QueryMapping
    public PostConnection postsConnection(@Argument Integer first, @Argument String after) {
        log.info("Query: postsConnection(first={}, after={})", first, after);
        return blogService.getPaginatedPosts(first, after);
    }

    @QueryMapping
    public List<Comment> commentsByPost(@Argument Long postId) {
        log.info("Query: commentsByPost(postId={})", postId);
        return blogService.getCommentsByPost(postId);
    }

    @QueryMapping
    public Comment commentsById(@Argument Long commentId) {
        log.info("Query: commentsById(commentId={})", commentId);
        return blogService.getCommentsById(commentId);
    }

    // ---- Nested resolvers — Phase 4: DataLoader batching (no more N+1) ----

    @SchemaMapping(typeName = "User", field = "posts")
    public CompletableFuture<List<Post>> postsForUser(User user,
                                                      DataLoader<Long, List<Post>> postsForUser) {
        log.debug("DataLoader: queuing User.posts for userId={}", user.getId());
        return postsForUser.load(user.getId());
    }

    @SchemaMapping(typeName = "Post", field = "author")
    public CompletableFuture<User> authorForPost(Post post,
                                                 DataLoader<Long, User> userById) {
        log.debug("DataLoader: queuing Post.author for postId={}", post.getId());
        return userById.load(post.getAuthor().getId());
    }

    @SchemaMapping(typeName = "Post", field = "comments")
    public CompletableFuture<List<Comment>> commentsForPost(Post post,
                                                            DataLoader<Long, List<Comment>> commentsForPost) {
        log.debug("DataLoader: queuing Post.comments for postId={}", post.getId());
        return commentsForPost.load(post.getId());
    }

    @SchemaMapping(typeName = "Comment", field = "author")
    public CompletableFuture<User> authorForComment(Comment comment,
                                                    DataLoader<Long, User> userById) {
        log.debug("DataLoader: queuing Comment.author for commentId={}", comment.getId());
        return userById.load(comment.getAuthor().getId());
    }

    @SchemaMapping(typeName = "Comment", field = "post")
    public Post postForComment(Comment comment) {
        log.debug("SchemaMapping: Comment.post for commentId={}", comment.getId());
        return blogService.getPostById(comment.getPost().getId());
    }

    // ---- Subscriptions ----

    @SubscriptionMapping
    public Flux<Comment> commentAdded(@Argument Long postId) {
        log.info("Subscription: commentAdded(postId={})", postId);
        return commentPublisher.getFlux(postId);
    }

    // ---- Mutations ----

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public User createUser(@Argument Map<String, String> input) {
        log.info("Mutation: createUser(name={}, email={})", input.get("name"), input.get("email"));
        return blogService.createUser(input.get("name"), input.get("email"));
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public Post createPost(@Argument Map<String, String> input) {
        log.info("Mutation: createPost(title={}, authorId={})", input.get("title"), input.get("authorId"));
        return blogService.createPost(
                input.get("title"),
                input.get("content"),
                Long.parseLong(input.get("authorId"))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public boolean deletePost(@Argument Long id) {
        log.info("Mutation: deletePost(id={})", id);
        return blogService.deletePost(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public boolean deleteComment(@Argument Long id) {
        log.info("Mutation: deleteComment(id={})", id);
        return blogService.deleteComment(id);
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public Comment addComment(@Argument Map<String, String> input) {
        log.info("Mutation: addComment(postId={}, authorId={})", input.get("postId"), input.get("authorId"));
        return blogService.addComment(
                input.get("text"),
                Long.parseLong(input.get("postId")),
                Long.parseLong(input.get("authorId"))
        );
    }
}
