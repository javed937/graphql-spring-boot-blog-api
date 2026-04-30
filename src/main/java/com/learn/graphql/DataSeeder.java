package com.learn.graphql;

import com.learn.graphql.service.BlogService;
import com.learn.graphql.entity.Post;
import com.learn.graphql.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BlogService blogService;

    @Override
    public void run(String... args) {
        User alice = blogService.createUser("Alice", "alice@example.com");
        User bob   = blogService.createUser("Bob",   "bob@example.com");

        Post post1 = blogService.createPost("Hello GraphQL", "GraphQL is awesome!", alice.getId());
        Post post2 = blogService.createPost("Spring Boot Tips", "Use @SchemaMapping for nested resolvers.", bob.getId());

        blogService.addComment("Great post!", post1.getId(), bob.getId());
        blogService.addComment("Thanks Bob!", post1.getId(), alice.getId());
        blogService.addComment("Very helpful!", post2.getId(), alice.getId());

        log.info("Seed data loaded. Visit http://localhost:8080/graphiql.html to explore.");
    }
}
