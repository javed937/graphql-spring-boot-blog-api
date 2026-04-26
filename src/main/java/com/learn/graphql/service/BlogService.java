package com.learn.graphql.service;

import com.learn.graphql.entity.Comment;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import com.learn.graphql.exception.ResourceNotFoundException;
import com.learn.graphql.config.CommentPublisher;
import com.learn.graphql.repository.CommentRepository;
import com.learn.graphql.repository.PostRepository;
import com.learn.graphql.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learn.graphql.dto.PageInfo;
import com.learn.graphql.dto.PostConnection;
import com.learn.graphql.dto.PostEdge;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentPublisher commentPublisher;

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
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Post", id));
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

    // --- Pagination (Phase 5) ---

    public PostConnection getPaginatedPosts(Integer first, String after) {
        int size = (first != null && first > 0) ? first : 10;
        int startIndex = (after != null) ? decodeCursor(after) + 1 : 0;
        int pageNumber = startIndex / size;

        Page<Post> page = postRepository.findAll(
                PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.ASC, "id")));

        List<Post> posts = page.getContent();
        int baseIndex = pageNumber * size;

        List<PostEdge> edges = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            edges.add(new PostEdge(posts.get(i), encodeCursor(baseIndex + i)));
        }

        PageInfo pageInfo = new PageInfo(
                page.hasNext(),
                pageNumber > 0,
                edges.isEmpty() ? null : edges.get(0).cursor(),
                edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor()
        );

        return new PostConnection(edges, pageInfo, (int) page.getTotalElements());
    }

    private static String encodeCursor(int index) {
        return Base64.getEncoder().encodeToString(("cursor:" + index).getBytes());
    }

    private static int decodeCursor(String cursor) {
        String decoded = new String(Base64.getDecoder().decode(cursor));
        return Integer.parseInt(decoded.replace("cursor:", ""));
    }

    // --- Batch loaders (Phase 4 — DataLoader) ---

    public Map<Long, List<Post>> getPostsByAuthorIds(Set<Long> authorIds) {
        log.debug("Batch: getPostsByAuthorIds({})", authorIds);
        return postRepository.findByAuthorIdIn(authorIds).stream()
                .collect(Collectors.groupingBy(p -> p.getAuthor().getId()));
    }

    public Map<Long, List<Comment>> getCommentsByPostIds(Set<Long> postIds) {
        log.debug("Batch: getCommentsByPostIds({})", postIds);
        return commentRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(c -> c.getPost().getId()));
    }

    public Map<Long, User> getUsersByIds(Set<Long> userIds) {
        log.debug("Batch: getUsersByIds({})", userIds);
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    // --- Comments ---

    public List<Comment> getCommentsByPost(Long postId) {
        log.debug("Service: getCommentsByPost(postId={})", postId);
        return commentRepository.findByPostId(postId);
    }

    public Comment getCommentsById(Long commentId) {
        log.debug("Service: getCommentsById(commentId={})", commentId);
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    }

    @Transactional
    public Comment addComment(String text, Long postId, Long authorId) {
        log.info("Service: addComment(postId={}, authorId={})", postId, authorId);
        Post post = getPostById(postId);
        User author = getUserById(authorId);
        Comment comment = Comment.builder().text(text).post(post).author(author).build();
        Comment saved = commentRepository.save(comment);
        log.info("Service: addComment -> saved with id={}", saved.getId());
        commentPublisher.publish(saved);
        return saved;
    }
}
