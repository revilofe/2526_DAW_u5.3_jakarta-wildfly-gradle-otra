package com.example.tasks;

import java.util.List;
import java.util.Optional;

public interface TaskStore {
    List<Task> findAll();

    Optional<Task> findById(long id);

    Task create(Task task);

    Optional<Task> update(long id, Task task);

    boolean delete(long id);
}
