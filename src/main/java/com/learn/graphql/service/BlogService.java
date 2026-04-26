package com.learn.graphql.service;

import com.learn.graphql.entity.Comment;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import com.learn.graphql.repository.CommentRepository;
import com.learn.graphql.repository.PostRepository;
import com.learn.graphql.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // --- Users ---

    public List<User> getAllUsers() {
        log.debug("Service: getAllUsers()");
        List<User> users = userRepository.findAll();
        log.debug("Service: getAllUsers() -> {} users", users.size());
        return users;
    }

    public User getUserById(Long id) {
        log.debug("Service: getUserById({})", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Service: User not found: {}", id);
                    return new RuntimeException("User not found: " + id);
                });
    }

    @Transactional
    public User createUser(String name, String email) {
        log.info("Service: createUser(name={}, email={})", name, email);
        User user = User.builder().name(name).email(email).build();
        User saved = userRepository.save(user);
        log.info("Service: createUser -> saved with id={}", saved.getId());
        return saved;
    }

    // --- Posts ---

    public List<Post> getAllPosts() {
        log.debug("Service: getAllPosts()");
        List<Post> posts = postRepository.findAll();
        log.debug("Service: getAllPosts() -> {} posts", posts.size());
        return posts;
    }

    public Post getPostById(Long id) {
        log.debug("Service: getPostById({})", id);
        return postRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Service: Post not found: {}", id);
                    return new RuntimeException("Post not found: " + id);
                });
    }

    public List<Post> getPostsByUser(Long userId) {
        log.debug("Service: getPostsByUser(userId={})", userId);
        return postRepository.findByAuthorId(userId);
    }

    @Transactional
    public Post createPost(String title, String content, Long authorId) {
        log.info("Service: createPost(title={}, authorId={})", title, authorId);
        User author = getUserById(authorId);
        Post post = Post.builder().title(title).content(content).author(author).build();
        Post saved = postRepository.save(post);
        log.info("Service: createPost -> saved with id={}", saved.getId());
        return saved;
    }

    @Transactional
    public boolean deletePost(Long id) {
        log.info("Service: deletePost(id={})", id);
        if (!postRepository.existsById(id)) {
            log.warn("Service: deletePost -> post {} not found", id);
            return false;
        }
        postRepository.deleteById(id);
        log.info("Service: deletePost -> deleted id={}", id);
        return true;
    }

    @Transactional
    public boolean deleteComment(Long id) {
        log.info("Service: deleteComment(id={})", id);
        if (!commentRepository.existsById(id)) {
            log.warn("Service: deleteComment -> comment {} not found", id);
            return false;
        }
        commentRepository.deleteById(id);
        log.info("Service: deleteComment -> deleted id={}", id);
        return true;
    }

    // --- Comments ---

    public List<Comment> getCommentsByPost(Long postId) {
        log.debug("Service: getCommentsByPost(postId={})", postId);
        return commentRepository.findByPostId(postId);
    }

    public Comment getCommentsById(Long commentId) {
        log.debug("Service: getCommentsById(commentId={})", commentId);
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Service: Comment not found: {}", commentId);
                    return new RuntimeException("Comment not found: " + commentId);
                });
    }

    @Transactional
    public Comment addComment(String text, Long postId, Long authorId) {
        log.info("Service: addComment(postId={}, authorId={})", postId, authorId);
        Post post = getPostById(postId);
        User author = getUserById(authorId);
        Comment comment = Comment.builder().text(text).post(post).author(author).build();
        Comment saved = commentRepository.save(comment);
        log.info("Service: addComment -> saved with id={}", saved.getId());
        return saved;
    }
}
