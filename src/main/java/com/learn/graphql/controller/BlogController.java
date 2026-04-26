package com.learn.graphql.controller;

import com.learn.graphql.entity.Comment;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import com.learn.graphql.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

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
    public List<Comment> commentsByPost(@Argument Long postId) {
        log.info("Query: commentsByPost(postId={})", postId);
        return blogService.getCommentsByPost(postId);
    }

    @QueryMapping
    public Comment commentsById(@Argument Long commentId) {
        log.info("Query: commentsById(commentId={})", commentId);
        return blogService.getCommentsById(commentId);
    }

    // ---- Nested resolvers (avoid N+1 for now — Phase 4 adds DataLoader) ----

    @SchemaMapping(typeName = "User", field = "posts")
    public List<Post> postsForUser(User user) {
        log.debug("SchemaMapping: User.posts for userId={}", user.getId());
        return blogService.getPostsByUser(user.getId());
    }

    @SchemaMapping(typeName = "Post", field = "author")
    public User authorForPost(Post post) {
        log.debug("SchemaMapping: Post.author for postId={}", post.getId());
        return blogService.getUserById(post.getAuthor().getId());
    }

    @SchemaMapping(typeName = "Post", field = "comments")
    public List<Comment> commentsForPost(Post post) {
        log.debug("SchemaMapping: Post.comments for postId={}", post.getId());
        return blogService.getCommentsByPost(post.getId());
    }

    @SchemaMapping(typeName = "Comment", field = "author")
    public User authorForComment(Comment comment) {
        log.debug("SchemaMapping: Comment.author for commentId={}", comment.getId());
        return blogService.getUserById(comment.getAuthor().getId());
    }
    @SchemaMapping(typeName = "Comment", field = "post")
    public Post postForComment(Comment comment) {
        log.debug("SchemaMapping: Comment.post for commentId={}", comment.getId());
        return blogService.getPostById(comment.getPost().getId());
    }

    // ---- Mutations ----

    @MutationMapping
    public User createUser(@Argument Map<String, String> input) {
        log.info("Mutation: createUser(name={}, email={})", input.get("name"), input.get("email"));
        return blogService.createUser(input.get("name"), input.get("email"));
    }

    @MutationMapping
    public Post createPost(@Argument Map<String, String> input) {
        log.info("Mutation: createPost(title={}, authorId={})", input.get("title"), input.get("authorId"));
        return blogService.createPost(
                input.get("title"),
                input.get("content"),
                Long.parseLong(input.get("authorId"))
        );
    }

    @MutationMapping
    public boolean deletePost(@Argument Long id) {
        log.info("Mutation: deletePost(id={})", id);
        return blogService.deletePost(id);
    }

    @MutationMapping
    public boolean deleteComment(@Argument Long id){
        log.info("Mutation: deleteComment(id={})", id);
        return blogService.deleteComment(id);
    }
    

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
