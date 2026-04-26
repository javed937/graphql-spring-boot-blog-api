package com.learn.graphql.dto;

import com.learn.graphql.entity.Post;

public record PostEdge(Post node, String cursor) {}
