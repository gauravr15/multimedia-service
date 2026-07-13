package com.odin.multimedia.service.status;
public class StatusAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Category { INVALID_REQUEST, NOT_FOUND, REPAIR_REQUIRED, DEPENDENCY_FAILURE }
    private final Category category;
    public StatusAccessException(Category category, String message) { super(message); this.category = category; }
    public StatusAccessException(Category category, String message, Throwable cause) { super(message, cause); this.category = category; }
    public Category getCategory() { return category; }
}
