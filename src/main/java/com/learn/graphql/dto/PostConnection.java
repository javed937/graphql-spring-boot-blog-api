package com.learn.graphql.dto;

import java.util.List;

public record PostConnection(List<PostEdge> edges, PageInfo pageInfo, int totalCount) {}
