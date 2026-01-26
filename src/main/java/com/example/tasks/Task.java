package com.example.tasks;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import java.time.Instant;

public class Task {
    private long id;
    private String title;
    private boolean done;
    private Instant createdAt;

    public Task() {
        // JSON-B
    }

    @JsonbCreator
    public Task(@JsonbProperty("id") long id,
                @JsonbProperty("title") String title,
                @JsonbProperty("done") boolean done,
                @JsonbProperty("createdAt") Instant createdAt) {
        this.id = id;
        this.title = title;
        this.done = done;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
