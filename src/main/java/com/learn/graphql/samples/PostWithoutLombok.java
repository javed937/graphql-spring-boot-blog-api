package com.learn.graphql.samples;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Builder pattern manually — exactly what Lombok generates
 * behind the scenes for the real Post entity.
 *
 * Run main() to see it in action.
 */
public class PostWithoutLombok {

    // -------------------------------------------------------------------------
    // Fields — private, no direct access from outside
    // -------------------------------------------------------------------------

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private List<String> comments;

    // -------------------------------------------------------------------------
    // @NoArgsConstructor — JPA needs this to instantiate objects from DB rows
    // -------------------------------------------------------------------------

    public PostWithoutLombok() {
        this.comments = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // @AllArgsConstructor — all fields as parameters (used internally by builder)
    // -------------------------------------------------------------------------

    private PostWithoutLombok(Long id, String title, String content,
                               String authorName, List<String> comments) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.comments = comments;
    }

    // -------------------------------------------------------------------------
    // @Getter — one per field
    // -------------------------------------------------------------------------

    public Long getId()           { return id; }
    public String getTitle()      { return title; }
    public String getContent()    { return content; }
    public String getAuthorName() { return authorName; }
    public List<String> getComments() { return comments; }

    // -------------------------------------------------------------------------
    // @Setter — one per field
    // -------------------------------------------------------------------------

    public void setId(Long id)                  { this.id = id; }
    public void setTitle(String title)          { this.title = title; }
    public void setContent(String content)      { this.content = content; }
    public void setAuthorName(String authorName){ this.authorName = authorName; }
    public void setComments(List<String> comments) { this.comments = comments; }

    // -------------------------------------------------------------------------
    // @Builder — static entry point that returns a Builder instance
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // The Builder class — each method sets one field and returns `this`
    // so calls can be chained fluently
    // -------------------------------------------------------------------------

    public static class Builder {

        private Long id;
        private String title;
        private String content;
        private String authorName;
        // @Builder.Default — initialise to empty list, not null
        private List<String> comments = new ArrayList<>();

        // private constructor — only PostWithoutLombok.builder() can create this
        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;         // returns itself so the next call can chain
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public Builder comments(List<String> comments) {
            this.comments = comments;
            return this;
        }

        // build() assembles the final object using the private all-args constructor
        public PostWithoutLombok build() {
            return new PostWithoutLombok(id, title, content, authorName, comments);
        }
    }

    @Override
    public String toString() {
        return "Post{id=" + id + ", title='" + title + "', author='" + authorName
                + "', comments=" + comments + "}";
    }

    // -------------------------------------------------------------------------
    // Demo
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        // Without builder — old way, verbose and easy to get argument order wrong
        PostWithoutLombok post1 = new PostWithoutLombok();
        post1.setId(1L);
        post1.setTitle("Old way");
        post1.setContent("Using setters one by one");
        post1.setAuthorName("Alice");
        System.out.println("Setter style : " + post1);

        // With builder — readable, order doesn't matter, skip optional fields
        PostWithoutLombok post2 = PostWithoutLombok.builder()
                .id(2L)
                .title("Builder way")
                .content("Clean and readable")
                .authorName("Bob")
                .comments(List.of("Great post!", "Thanks!"))
                .build();
        System.out.println("Builder style: " + post2);

        // Partial build — only set what you need, rest stay as defaults
        PostWithoutLombok post3 = PostWithoutLombok.builder()
                .title("Draft post")
                .authorName("Carol")
                .build();  // id is null, comments is empty list (not null)
        System.out.println("Partial build: " + post3);
    }
}
