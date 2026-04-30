# graphql-spring-boot-blog-api

A hands-on blog API for learning GraphQL from scratch using Spring Boot 3, Spring Data JPA, and H2. Covers schema design, queries, mutations, nested resolvers, and the N+1 problem.

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.3.0 |
| Spring for GraphQL | (via starter) |
| Spring Data JPA | (via starter) |
| H2 Database | in-memory |
| Lombok | latest |

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
    users: [User!]!
    user(id: ID!): User
    posts: [Post!]!
    post(id: ID!): Post
    postsByUser(userId: ID!): [Post!]!
    commentsByPost(postId: ID!): [Comment!]!
}

type Mutation {
    createUser(input: CreateUserInput!): User!
    createPost(input: CreatePostInput!): Post!
    deletePost(id: ID!): Boolean!
    addComment(input: AddCommentInput!): Comment!
}
```

## Sample Queries

**Fetch all users with nested posts and comments:**

```graphql
query {
  users {
    id
    name
    posts {
      title
      comments {
        text
        author { name }
      }
    }
  }
}
```

**Create a user (requires ADMIN role):**

```graphql
mutation {
  createUser(input: { name: "Carol", email: "carol@example.com" }) {
    id
    name
  }
}
```

**Using variables:**

```graphql
mutation CreatePost($title: String!, $content: String!, $authorId: ID!) {
  createPost(input: { title: $title, content: $content, authorId: $authorId }) {
    id
    title
    author { name }
  }
}
```

Variables:

```json
{
  "title": "My Post",
  "content": "Hello GraphQL!",
  "authorId": "1"
}
```

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

## Learning Plan

| Phase | Topic | Status |
|-------|-------|--------|
| 1 | Foundations — schema, types, Spring Boot setup | ✅ |
| 2 | Schema first — `.graphqls`, JPA entities, `@QueryMapping` | ✅ |
| 3 | CRUD — queries, mutations, input types, service layer | ✅ |
| 4 | N+1 fix — `DataLoader`, `BatchLoaderRegistry` | ✅ |
| 5 | Advanced — pagination, error handling, subscriptions, security | ✅ |

See [plan.md](plan.md) for detailed task breakdown.

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

## Users (for testing)

| Username | Password | Role |
|----------|----------|------|
| `user` | `password` | USER — can query, createPost, addComment |
| `admin` | `admin` | ADMIN — full access including delete, createUser |

Login at **http://localhost:8080/login** before using secured mutations.

## Known Issues / Notes

- Spring Boot 3.3.0's built-in GraphiQL UI references `graphiql@5.2.2` on unpkg CDN which has broken file paths — `graphiql.min.js` was moved in v5. The built-in UI is disabled and replaced with a custom `static/graphiql.html` pinned to `graphiql@3.7.1`.
- SQL logging is enabled (`spring.jpa.show-sql=true`) intentionally — useful for observing the N+1 problem before Phase 4.
