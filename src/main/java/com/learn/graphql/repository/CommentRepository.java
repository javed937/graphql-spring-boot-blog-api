package com.learn.graphql.repository;

import com.learn.graphql.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
    List<Comment> findByPostIdIn(Collection<Long> postIds);
}
