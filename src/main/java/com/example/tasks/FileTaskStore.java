package com.example.tasks;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileTaskStore implements TaskStore {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(FileTaskStore.class.getName());
    private final Path filePath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Jsonb jsonb = JsonbBuilder.create();
    private final AtomicLong sequence = new AtomicLong(0);

    public FileTaskStore(Path filePath) {
        this.filePath = filePath;
        init();
    }

    private void init() {
        try {
            LOGGER.info("Inicializando almacenamiento en " + filePath);
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                writeAll(new ArrayList<>());
            }
            List<Task> existing = readAll();
            long maxId = existing.stream().map(Task::getId).max(Comparator.naturalOrder()).orElse(0L);
            sequence.set(maxId);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inicializar el fichero de datos", e);
        }
    }

    @Override
    public List<Task> findAll() {
        lock.readLock().lock();
        try {
            LOGGER.info("Operacion findAll");
            return new ArrayList<>(readAll());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Task> findById(long id) {
        lock.readLock().lock();
        try {
            LOGGER.info("Operacion findById id=" + id);
            return readAll().stream().filter(task -> task.getId() == id).findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Task create(Task task) {
        lock.writeLock().lock();
        try {
            LOGGER.info("Operacion create");
            List<Task> tasks = readAll();
            long id = sequence.incrementAndGet();
            Task created = new Task(id, task.getTitle(), task.isDone(), Instant.now());
            tasks.add(created);
            writeAll(tasks);
            return created;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Task> update(long id, Task task) {
        lock.writeLock().lock();
        try {
            LOGGER.info("Operacion update id=" + id);
            List<Task> tasks = readAll();
            for (int i = 0; i < tasks.size(); i++) {
                Task existing = tasks.get(i);
                if (existing.getId() == id) {
                    Task updated = new Task(id,
                            task.getTitle() == null ? existing.getTitle() : task.getTitle(),
                            task.isDone(),
                            existing.getCreatedAt());
                    tasks.set(i, updated);
                    writeAll(tasks);
                    return Optional.of(updated);
                }
            }
            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean delete(long id) {
        lock.writeLock().lock();
        try {
            LOGGER.info("Operacion delete id=" + id);
            List<Task> tasks = readAll();
            boolean removed = tasks.removeIf(task -> task.getId() == id);
            if (removed) {
                writeAll(tasks);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Task> readAll() {
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }
            TaskList wrapper = jsonb.fromJson(json, TaskList.class);
            if (wrapper == null || wrapper.getTasks() == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(wrapper.getTasks());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el fichero de datos", e);
        }
    }

    private void writeAll(List<Task> tasks) {
        try {
            TaskList wrapper = new TaskList();
            wrapper.setTasks(tasks);
            String json = jsonb.toJson(wrapper);
            Files.writeString(filePath, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir el fichero de datos", e);
        }
    }

    public static class TaskList {
        private List<Task> tasks;

        public List<Task> getTasks() {
            return tasks;
        }

        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
        }
    }
}
