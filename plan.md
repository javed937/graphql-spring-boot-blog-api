# GraphQL with Spring Boot + Spring Data JPA — Learning Plan

## Phase 1 — Foundations ✅
- [x] What GraphQL is vs REST: schema, queries, mutations, subscriptions
- [x] Core concepts: types, fields, resolvers, N+1 problem
- [x] Set up Spring Boot project with `spring-boot-starter-graphql` + `spring-boot-starter-data-jpa`
- [x] H2 in-memory database configured
- [x] GraphiQL UI enabled at `http://localhost:8080/graphiql`
- [x] Seed data loaded via `DataSeeder.java` on startup

---

## Phase 2 — Schema First ✅
- [x] Wrote `schema.graphqls` with types: `User`, `Post`, `Comment`
- [x] Defined input types: `CreateUserInput`, `CreatePostInput`, `AddCommentInput`
- [x] Mapped schema types to JPA entities
- [x] Implemented `@QueryMapping` in `BlogController`
- [x] Testable via GraphiQL

---

## Phase 3 — CRUD Operations ✅
- [x] Queries: `users`, `user(id)`, `posts`, `post(id)`, `postsByUser(userId)`, `commentsByPost(postId)`
- [x] Mutations: `createUser`, `createPost`, `deletePost`, `addComment`
- [x] Input types vs output types separation in schema
- [x] Service layer (`BlogService`) encapsulates all business logic
- [x] Nested resolvers via `@SchemaMapping` for relationships

---

## Phase 4 — Relationships & N+1 Fix ⬜
- [ ] Understand the N+1 problem (currently present in `@SchemaMapping` resolvers)
- [ ] Add `BatchLoaderRegistry` with `DataLoader`
- [ ] Replace per-entity `@SchemaMapping` with batch-aware loaders for:
  - `User.posts`
  - `Post.author`
  - `Post.comments`
  - `Comment.author`
- [ ] Verify reduced query count via SQL logs

---

## Phase 5 — Advanced ⬜
- [ ] Pagination: cursor-based using Spring Data `Page` + GraphQL connection pattern
- [ ] Error handling: implement `DataFetcherExceptionResolver` for structured errors
- [ ] Subscriptions via WebSocket (`spring-boot-starter-websocket`)
- [ ] Security: field-level authorization with Spring Security + `@PreAuthorize`

---

## Quick Reference

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/graphiql` | Interactive GraphQL explorer |
| `http://localhost:8080/h2-console` | DB viewer (JDBC URL: `jdbc:h2:mem:blogdb`) |

### Run the project
```bash
mvn spring-boot:run
```

### Sample queries to try

```graphql
# Get all users with their posts and comments
query {
  users {
    id
    name
    posts {
      title
      comments { text }
    }
  }
}

# Create a user
mutation {
  createUser(input: { name: "Carol", email: "carol@example.com" }) {
    id
    name
  }
}

# Create a post
mutation {
  createPost(input: { title: "My Post", content: "Hello!", authorId: "1" }) {
    id
    title
    author { name }
  }
}

# Add a comment
mutation {
  addComment(input: { text: "Nice!", postId: "1", authorId: "2" }) {
    id
    text
    author { name }
  }
}
```
