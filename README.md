# graphql-spring-boot-blog-api

A hands-on blog API GraphQL using Spring Boot 3, Spring Data JPA, and H2. Covers schema design, queries, mutations, nested resolvers, and the N+1 problem.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features Implemented](#features-implemented)
- [Domain Model](#domain-model)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Users for Testing](#users-for-testing)
- [URLs](#urls)
- [GraphQL Schema](#graphql-schema)
- [API Reference](#api-reference)
  - [Queries](#queries)
  - [Mutations](#mutations)
- [The N+1 Problem and DataLoader](#the-n1-problem-and-dataloader)
- [Testing Real-Time Subscriptions](#testing-real-time-subscriptions)

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.3.0 |
| Spring for GraphQL | (via starter) |
| Spring Data JPA | (via starter) |
| H2 Database | in-memory |
| Lombok | latest |

## Features Implemented

| Feature | Details |
|---------|---------|
| GraphQL Schema | Queries, mutations, subscriptions, input types |
| CRUD | Users, Posts, Comments with full relationships |
| N+1 Fix | `DataLoader` + `BatchLoaderRegistry` batching |
| Pagination | Relay connection pattern with cursor-based navigation |
| Error Handling | `DataFetcherExceptionResolver` with structured error codes |
| Subscriptions | Real-time `commentAdded` via WebSocket (`graphql-ws`) |
| Security | Spring Security with role-based `@PreAuthorize` on mutations |

## Domain Model

```
User ──< Post ──< Comment
 └──────────────────────┘ (author)
```

Three entities: `User`, `Post`, `Comment` with full one-to-many relationships exposed as a GraphQL graph.

## Project Structure

```
src/main/
├── java/com/learn/graphql/
│   ├── GraphqlSpringLearningApplication.java
│   ├── DataSeeder.java                        ← seeds data on startup
│   ├── entity/         User, Post, Comment
│   ├── repository/     UserRepository, PostRepository, CommentRepository
│   ├── service/        BlogService
│   └── controller/     BlogController         ← GraphQL resolvers
└── resources/
    ├── application.properties
    ├── graphql/schema.graphqls                 ← GraphQL schema
    └── static/graphiql.html                   ← GraphiQL UI
```

## Getting Started

### Prerequisites

- Java 21
- No Maven install needed — Maven wrapper included

### Run

```bash
./mvnw spring-boot:run
```

On Windows CMD/PowerShell:

```cmd
mvnw.cmd spring-boot:run
```

### Debug (VS Code)

Press **F5** → select **"Run Spring Boot"**. Set breakpoints in `BlogController.java` or `BlogService.java` before firing queries.

## Users (for testing)

| Username | Password | Role |
|----------|----------|------|
| `user` | `password` | USER — can query, createPost, addComment |
| `admin` | `admin` | ADMIN — full access including delete, createUser |

Login at **http://localhost:8080/login** before using secured mutations.

## URLs

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/graphiql.html` | GraphiQL explorer |
| `http://localhost:8080/h2-console` | H2 database console |
| `http://localhost:8080/graphql` | GraphQL HTTP endpoint |

### H2 Console credentials

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:blogdb` |
| User Name | `sa` |
| Password | *(blank)* |

> Data is in-memory and resets on every restart. `DataSeeder` loads 2 users, 2 posts, and 2 comments automatically.

## GraphQL Schema

```graphql
type Query {
    "Returns all users. Nested posts are batch-loaded via DataLoader (no N+1)."
    users: [User!]!

    "Returns a single user by ID. Throws NOT_FOUND if the ID does not exist."
    user(id: ID!): User

    "Returns all posts. Author and comments are batch-loaded via DataLoader."
    posts: [Post!]!

    "Returns a single post by ID. Throws NOT_FOUND if the ID does not exist."
    post(id: ID!): Post

    "Returns all posts written by a specific user. Returns an empty list if the user has no posts."
    postsByUser(userId: ID!): [Post!]!

    "Returns all comments on a specific post."
    commentsByPost(postId: ID!): [Comment!]!

    "Returns a single comment by ID. Throws NOT_FOUND if the ID does not exist."
    commentsById(commentId: ID!): Comment

    """
    Returns a cursor-paginated list of posts using the Relay Connection pattern.
    - first: number of posts to return (default 10)
    - after: opaque cursor from a previous response's endCursor; omit to start from the beginning
    """
    postsConnection(first: Int, after: String): PostConnection!
}

type Mutation {
    "Creates a new user. Requires ADMIN role. Returns the created user."
    createUser(input: CreateUserInput!): User!

    "Creates a new post. Requires authentication. authorId must reference an existing user."
    createPost(input: CreatePostInput!): Post!

    """
    Deletes a post and all its comments (cascade). Requires ADMIN role.
    Returns true if deleted, false if the ID was not found.
    """
    deletePost(id: ID!): Boolean!

    """
    Adds a comment to a post. Requires authentication.
    Publishes the saved comment to the commentAdded subscription for real-time delivery.
    """
    addComment(input: AddCommentInput!): Comment!

    "Deletes a comment by ID. Requires ADMIN role. Returns true if deleted, false if not found."
    deleteComment(id: ID!): Boolean
}

"Real-time event pushed over WebSocket to all active subscribers whenever a new comment is saved."
type Subscription {
    "Subscribe to new comments on a specific post. Emits each new Comment as it is saved."
    commentAdded(postId: ID!): Comment!
}

"A registered user of the blog."
type User {
    id: ID!
    "Display name of the user."
    name: String!
    "Unique email address."
    email: String!
    "All posts authored by this user. Batch-loaded to avoid N+1."
    posts: [Post!]!
}

"A blog post authored by a user."
type Post {
    id: ID!
    title: String!
    content: String!
    "The user who wrote this post. Batch-loaded to avoid N+1."
    author: User!
    "All comments on this post. Batch-loaded to avoid N+1."
    comments: [Comment!]!
}

"A comment left on a post by a user."
type Comment {
    id: ID!
    "The comment body."
    text: String!
    "The user who wrote this comment. Batch-loaded to avoid N+1."
    author: User!
    "The post this comment belongs to."
    post: Post!
}

"A paginated list of posts. Wraps edges, pageInfo, and totalCount."
type PostConnection {
    "The posts on this page, each wrapped in an edge with its cursor."
    edges: [PostEdge!]!
    "Metadata about the current page position in the full list."
    pageInfo: PageInfo!
    "Total number of posts across all pages."
    totalCount: Int!
}

"An envelope around a single Post that carries its cursor alongside the data."
type PostEdge {
    "The Post on this edge."
    node: Post!
    "Opaque base64 cursor pointing to this specific item. Pass as `after` to start the next page from here."
    cursor: String!
}

"Metadata about the current page in a connection query."
type PageInfo {
    "True if there are more items after this page."
    hasNextPage: Boolean!
    "True if there are items before this page."
    hasPreviousPage: Boolean!
    "Cursor of the first item on this page."
    startCursor: String
    "Cursor of the last item on this page. Pass as `after` to fetch the next page."
    endCursor: String
}

"Fields required to create a new user."
input CreateUserInput {
    name: String!
    email: String!
}

"Fields required to create a new post."
input CreatePostInput {
    title: String!
    content: String!
    "ID of the user who will be set as the author."
    authorId: ID!
}

"Fields required to add a comment to a post."
input AddCommentInput {
    "The comment body."
    text: String!
    "ID of the post to comment on."
    postId: ID!
    "ID of the user posting the comment."
    authorId: ID!
}
```

## API Reference

All operations are HTTP POST to `http://localhost:8080/graphql` with `Content-Type: application/json`. Authentication uses HTTP Basic auth.

| Role | Header value | Plaintext |
|------|-------------|-----------|
| USER | `Basic dXNlcjpwYXNzd29yZA==` | `user:password` |
| ADMIN | `Basic YWRtaW46YWRtaW4=` | `admin:admin` |

> **Postman tip:** In Postman click **Import → Paste Raw Text**, paste any curl command below, and it will populate the method, URL, headers, and body automatically.

---

### Queries

Queries have no role guard — any authenticated user can call them.

#### `users` — list all users

Returns all users. Nested `posts` are batch-loaded (no N+1).

```graphql
query {
  users {
    id
    name
    email
    posts { id title }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ users { id name email posts { id title } } }"}'
```

---

#### `user(id)` — single user by ID

Returns one user or a structured `NOT_FOUND` error if the ID does not exist.

```graphql
query {
  user(id: "1") {
    id
    name
    email
    posts { id title }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ user(id: \"1\") { id name email posts { id title } } }"}'
```

---

#### `posts` — list all posts

Returns all posts with nested author and comments. Author and comments are batch-loaded via DataLoader.

```graphql
query {
  posts {
    id
    title
    content
    author { id name }
    comments { id text author { name } }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ posts { id title content author { id name } comments { id text author { name } } } }"}'
```

---

#### `post(id)` — single post by ID

Returns one post or a `NOT_FOUND` error.

```graphql
query {
  post(id: "1") {
    id
    title
    content
    author { name email }
    comments { text author { name } }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ post(id: \"1\") { id title content author { name email } comments { text author { name } } } }"}'
```

---

#### `postsByUser(userId)` — all posts by a user

Filters posts by author ID. Returns an empty list (not an error) if the user has no posts.

```graphql
query {
  postsByUser(userId: "1") {
    id
    title
    content
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ postsByUser(userId: \"1\") { id title content } }"}'
```

---

#### `commentsByPost(postId)` — all comments on a post

Returns all comments for a given post. Each comment exposes its `post` back-reference if needed.

```graphql
query {
  commentsByPost(postId: "1") {
    id
    text
    author { name }
    post { title }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ commentsByPost(postId: \"1\") { id text author { name } post { title } } }"}'
```

---

#### `commentsById(commentId)` — single comment by ID

Returns one comment or a `NOT_FOUND` error.

```graphql
query {
  commentsById(commentId: "1") {
    id
    text
    author { name }
    post { id title }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ commentsById(commentId: \"1\") { id text author { name } post { id title } } }"}'
```

---

#### `postsConnection(first, after)` — cursor-paginated posts

##### Why cursor pagination instead of `LIMIT / OFFSET`?

Offset pagination (`page=2&size=10`) has a fundamental flaw: if a row is inserted or deleted between two requests, the window shifts and you either skip items or see duplicates. Cursor pagination solves this by pointing to a **specific item** in the list rather than a numeric position. The cursor moves forward through a stable, ordered sequence, so inserts and deletes between pages never corrupt your results.

---

##### The Relay Connection pattern

The [Relay Connection spec](https://relay.dev/graphql/connections.htm) defines a standard shape that every GraphQL client understands. Instead of returning a plain list, a connection query returns a **wrapper object** with three parts:

```
postsConnection
├── totalCount          ← total items in the full dataset
├── pageInfo            ← metadata about where you are in the list
│   ├── hasNextPage     ← true if there are more items after this page
│   ├── hasPreviousPage ← true if there are items before this page
│   ├── startCursor     ← cursor of the first item on this page
│   └── endCursor       ← cursor of the last item — pass this as `after` for the next page
└── edges               ← the items on this page, each wrapped in an "edge"
    └── edge
        ├── cursor      ← opaque pointer to THIS specific item
        └── node        ← the actual Post object
```

---

##### What is an Edge?

An **edge** is the envelope around a single item. It exists so the connection can carry **per-item metadata** alongside the item itself. In a social graph, the edge might carry `followedAt` or `relationshipType`. In this project the edge carries only `cursor`, but the pattern is extensible without changing the `Post` type.

```
edges [
  { cursor: "Y3Vyc29yOjA=",  node: { id: "1", title: "Hello GraphQL" } },
  { cursor: "Y3Vyc29yOjE=",  node: { id: "2", title: "Spring Boot Tips" } }
]
```

---

##### What is a Cursor?

A cursor is an **opaque, stable pointer** to one item. "Opaque" means the client treats it as a black box — it never parses or constructs it. It just stores the value and passes it back.

In this project cursors are base64-encoded strings of the form `cursor:<index>`:

```
"cursor:0"  →  base64 encode  →  "Y3Vyc29yOjA="
"cursor:1"  →  base64 encode  →  "Y3Vyc29yOjE="
```

The server decodes the cursor, extracts the index, derives the correct DB page number, and loads the next slice. The client never needs to know any of this.

---

##### Step-by-step walkthrough

**Request — first page (no cursor)**

```graphql
query {
  postsConnection(first: 2) {
    totalCount
    pageInfo {
      hasNextPage
      startCursor
      endCursor
    }
    edges {
      cursor
      node { id title author { name } }
    }
  }
}
```

**Response**

```json
{
  "data": {
    "postsConnection": {
      "totalCount": 2,
      "pageInfo": {
        "hasNextPage": false,
        "startCursor": "Y3Vyc29yOjA=",
        "endCursor":   "Y3Vyc29yOjE="
      },
      "edges": [
        { "cursor": "Y3Vyc29yOjA=", "node": { "id": "1", "title": "Hello GraphQL",   "author": { "name": "Alice" } } },
        { "cursor": "Y3Vyc29yOjE=", "node": { "id": "2", "title": "Spring Boot Tips", "author": { "name": "Bob"   } } }
      ]
    }
  }
}
```

`hasNextPage: false` means you are on the last page. If there were more posts, you would take `endCursor` (`"Y3Vyc29yOjE="`) and pass it as `after` in the next request:

**Request — next page**

```graphql
query {
  postsConnection(first: 2, after: "Y3Vyc29yOjE=") {
    pageInfo { hasNextPage endCursor }
    edges {
      cursor
      node { id title }
    }
  }
}
```

The server decodes `"Y3Vyc29yOjE="` → `"cursor:1"` → index `1`, then loads the next 2 items starting after index 1.

---

##### Curl commands

```bash
# First page — no cursor needed
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ postsConnection(first: 2) { totalCount pageInfo { hasNextPage startCursor endCursor } edges { cursor node { id title author { name } } } } }"}'

# Next page — pass endCursor from the previous response as `after`
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"{ postsConnection(first: 2, after: \"Y3Vyc29yOjE=\") { totalCount pageInfo { hasNextPage endCursor } edges { cursor node { id title } } } }"}'
```

---

### Mutations

#### `createUser` — create a user *(ADMIN only)*

Requires the `ADMIN` role. Returns the newly created user.

```graphql
mutation {
  createUser(input: { name: "Carol", email: "carol@example.com" }) {
    id
    name
    email
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d '{"query":"mutation { createUser(input: { name: \"Carol\", email: \"carol@example.com\" }) { id name email } }"}'
```

---

#### `createPost` — create a post *(authenticated)*

Any authenticated user can create a post. `authorId` must reference an existing user, otherwise a `NOT_FOUND` error is returned.

```graphql
mutation {
  createPost(input: { title: "My Post", content: "Hello GraphQL!", authorId: "1" }) {
    id
    title
    content
    author { name }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"mutation { createPost(input: { title: \"My Post\", content: \"Hello GraphQL!\", authorId: \"1\" }) { id title content author { name } } }"}'
```

Using GraphQL variables (recommended for dynamic values):

```graphql
mutation CreatePost($title: String!, $content: String!, $authorId: ID!) {
  createPost(input: { title: $title, content: $content, authorId: $authorId }) {
    id
    title
    author { name }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"mutation CreatePost($title: String!, $content: String!, $authorId: ID!) { createPost(input: { title: $title, content: $content, authorId: $authorId }) { id title author { name } } }","variables":{"title":"My Post","content":"Hello GraphQL!","authorId":"1"}}'
```

---

#### `deletePost(id)` — delete a post *(ADMIN only)*

Returns `true` if the post was deleted, `false` if the ID was not found. Also deletes all comments on that post (cascade).

```graphql
mutation {
  deletePost(id: "1")
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d '{"query":"mutation { deletePost(id: \"1\") }"}'
```

---

#### `addComment` — add a comment *(authenticated)*

Any authenticated user can comment. After saving, the server publishes the comment to the `commentAdded` subscription Flux — all active WebSocket subscribers for that `postId` receive it instantly.

```graphql
mutation {
  addComment(input: { text: "Great post!", postId: "1", authorId: "2" }) {
    id
    text
    author { name }
    post { title }
  }
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"mutation { addComment(input: { text: \"Great post!\", postId: \"1\", authorId: \"2\" }) { id text author { name } post { title } } }"}'
```

---

#### `deleteComment(id)` — delete a comment *(ADMIN only)*

Returns `true` if deleted, `false` if the ID was not found.

```graphql
mutation {
  deleteComment(id: "1")
}
```

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d '{"query":"mutation { deleteComment(id: \"1\") }"}'
```

## The N+1 Problem and DataLoader

### What is the N+1 Problem?

The N+1 problem occurs when GraphQL resolves a list of N items (1 query) and then fires a separate database query for each item to load a nested field — totalling **N+1 queries**. The number grows linearly with the dataset, making it a silent performance killer.

### Concrete Example — Before DataLoader

Take this query:

```graphql
query {
  users {
    id
    name
    posts { id title }
  }
}
```

With 2 users (Alice and Bob), a naive resolver fires:

```sql
-- Query 1: fetch all users
SELECT * FROM users;
-- result: Alice (id=1), Bob (id=2)

-- Query 2: fetch posts for Alice
SELECT * FROM posts WHERE author_id = 1;

-- Query 3: fetch posts for Bob
SELECT * FROM posts WHERE author_id = 2;
```

**3 queries for 2 users.** With 100 users it becomes 101 queries. The pattern is always 1 + N.

Now add nested comments:

```graphql
query {
  users {
    name
    posts {
      title
      comments { text }
    }
  }
}
```

With 2 users and 2 posts each:

```
1  query  → fetch all users
2  queries → fetch posts for each user      (N = users)
4  queries → fetch comments for each post   (N = posts)
─────────────────────────────────────────
7  total queries
```

Scale to 50 users with 10 posts each → **1 + 50 + 500 = 551 queries** for one GraphQL request.

---

### How DataLoader Solves It

DataLoader works in two steps:

1. **Collect** — instead of hitting the DB immediately, each resolver queues its required ID into a batch.
2. **Flush** — once all resolvers in the current execution wave have queued their IDs, DataLoader fires **one query** with an `IN (...)` clause covering all of them.

The same query with DataLoader:

```sql
-- Query 1: fetch all users
SELECT * FROM users;

-- Query 2: fetch ALL posts for ALL users in one shot
SELECT * FROM posts WHERE author_id IN (1, 2);
```

**2 queries regardless of how many users there are.** With 100 users it is still 2 queries.

---

### How It Is Implemented in This Project

**`DataLoaderConfig.java`** registers three batch loaders with Spring's `BatchLoaderRegistry`:

```java
// Collects all userIds seen in one request, fires ONE query: WHERE author_id IN (...)
registry.<Long, List<Post>>forName("postsForUser")
    .registerMappedBatchLoader((authorIds, env) ->
        Mono.fromCallable(() -> blogService.getPostsByAuthorIds(authorIds)));

// Collects all postIds seen in one request, fires ONE query: WHERE post_id IN (...)
registry.<Long, List<Comment>>forName("commentsForPost")
    .registerMappedBatchLoader((postIds, env) ->
        Mono.fromCallable(() -> blogService.getCommentsByPostIds(postIds)));

// Shared loader for Post.author and Comment.author — fires ONE query: WHERE id IN (...)
registry.<Long, User>forName("userById")
    .registerMappedBatchLoader((userIds, env) ->
        Mono.fromCallable(() -> blogService.getUsersByIds(userIds)));
```

Each `registerMappedBatchLoader` receives a full `Set` of IDs (all IDs seen across the whole request) and returns a `Map` so DataLoader can distribute results back to the right resolver.

**`BlogController.java`** — the resolver does not call the DB; it just queues an ID:

```java
@SchemaMapping(typeName = "User", field = "posts")
public CompletableFuture<List<Post>> postsForUser(User user,
                                                  DataLoader<Long, List<Post>> postsForUser) {
    return postsForUser.load(user.getId()); // ← queued, NOT fired yet
}
```

`postsForUser.load(userId)` returns a `CompletableFuture` that will be completed later — after Spring has called all `User` resolvers and collected every `userId`. Only then does it flush the batch and invoke the `registerMappedBatchLoader` with all IDs at once.

**`BlogService.java`** — the batch methods use `IN` queries:

```java
public Map<Long, List<Post>> getPostsByAuthorIds(Set<Long> authorIds) {
    return postRepository.findByAuthorIdIn(authorIds).stream()
        .collect(Collectors.groupingBy(p -> p.getAuthor().getId()));
}
```

`findByAuthorIdIn` translates to `WHERE author_id IN (1, 2, ...)`. The result is grouped into a `Map<userId, List<Post>>` so DataLoader can route each post list back to the correct user.

---

### The Three DataLoaders in This Project

| Name | Batches | SQL generated |
|------|---------|---------------|
| `postsForUser` | All `userId`s from `User.posts` fields | `SELECT * FROM posts WHERE author_id IN (...)` |
| `commentsForPost` | All `postId`s from `Post.comments` fields | `SELECT * FROM comments WHERE post_id IN (...)` |
| `userById` | All `userId`s from `Post.author` + `Comment.author` | `SELECT * FROM users WHERE id IN (...)` |

`userById` is shared across two fields — both `Post.author` and `Comment.author` feed into the same loader, so even a mixed query loading both still fires only one `SELECT` for users.

---

### SQL Logs — Before vs After

`spring.jpa.show-sql=true` is intentionally enabled so you can observe the difference in the console.

**Without DataLoader** — `users` with nested `posts`:
```
Hibernate: select * from users
Hibernate: select * from posts where author_id=1
Hibernate: select * from posts where author_id=2
```

**With DataLoader** — same query, same data:
```
Hibernate: select * from users
Hibernate: select * from posts where author_id in (?, ?)
```

---

## Testing Real-Time Subscriptions

Subscriptions use WebSocket (`graphql-ws` protocol) and push new comments to all active subscribers instantly.

### Step 1 — Log in

Go to **http://localhost:8080/login** and sign in with:

| Field | Value |
|-------|-------|
| Username | `user` |
| Password | `password` |

You will be redirected to GraphiQL automatically.

### Step 2 — Open two browser tabs

Open **http://localhost:8080/graphiql.html** in **two separate tabs** in the same browser (the login session cookie is shared).

### Step 3 — Start the subscription (Tab 1)

In the first tab, paste the following and click the **▶ Run** button:

```graphql
subscription {
  commentAdded(postId: "1") {
    id
    text
    author {
      name
    }
  }
}
```

You will see a **spinner / loading indicator** appear in the result panel — this means the WebSocket connection is open and the subscription is active.

> **Note:** The subscription listens for new comments on post `id=1` ("Hello GraphQL"). To listen to post `id=2` ("Spring Boot Tips"), change `postId` to `"2"`.

### Step 4 — Fire the mutation (Tab 2)

In the second tab, paste the following and click **▶ Run**:

```graphql
mutation {
  addComment(input: {
    text: "Hello from the browser!"
    postId: "1"
    authorId: "1"
  }) {
    id
    text
  }
}
```

### Step 5 — See the result in real time (Tab 1)

Switch back to Tab 1. The new comment appears immediately in the subscription result panel:

```json
{
  "data": {
    "commentAdded": {
      "id": "4",
      "text": "Hello from the browser!",
      "author": {
        "name": "Alice"
      }
    }
  }
}
```

Every time you run the mutation in Tab 2, a new entry is pushed to Tab 1 without refreshing.

### How it works

```
Tab 2 (mutation)                     Server                        Tab 1 (subscription)
──────────────────                   ──────────────────            ─────────────────────
addComment mutation  ──HTTP POST───► BlogService.addComment()
                                      │
                                      └─ CommentPublisher.publish()
                                           │
                                           └─ Reactor Sink ──WebSocket push──► commentAdded result
```

1. `addComment` saves the comment and calls `CommentPublisher.publish(comment, postId)`.
2. The Reactor `Sinks.Many` buffers the event and delivers it on a separate thread.
3. Spring for GraphQL resolves the subscription fields (`id`, `text`, `author { name }`).
4. The result is pushed over WebSocket to all active subscribers for that `postId`.

### Subscription with curl (for API testing)

You can also verify the full pipeline without a browser. Start the WebSocket subscriber in one terminal:

```bash
# Install dependencies (one-time)
mkdir /tmp/ws-test && cd /tmp/ws-test
npm init -y && npm install graphql-ws ws

# subscriber.mjs
cat > subscriber.mjs << 'EOF'
import { createClient } from 'graphql-ws';
import { WebSocket } from 'ws';

const client = createClient({
  url: 'ws://localhost:8080/graphql-ws',
  webSocketImpl: WebSocket,
});

client.subscribe(
  { query: `subscription { commentAdded(postId: "1") { id text author { name } } }` },
  {
    next: (data) => console.log('RECEIVED:', JSON.stringify(data, null, 2)),
    error: (err) => console.error('ERROR:', err),
    complete: () => console.log('Subscription closed.'),
  }
);

setTimeout(() => process.exit(0), 60000);
EOF

node subscriber.mjs
```

In a second terminal, fire the mutation with HTTP Basic auth:

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -d '{"query":"mutation{addComment(input:{text:\"curl test\",postId:\"1\",authorId:\"1\"}){id text}}"}'
```

The first terminal will print the pushed event within milliseconds.
