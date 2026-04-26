package com.learn.graphql.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object id;

    public ResourceNotFoundException(String resourceType, Object id) {
        super(resourceType + " not found: " + id);
        this.resourceType = resourceType;
        this.id = id;
    }

    public String getResourceType() { return resourceType; }
    public Object getId() { return id; }
}
