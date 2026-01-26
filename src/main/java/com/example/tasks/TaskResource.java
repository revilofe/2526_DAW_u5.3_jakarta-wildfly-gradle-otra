package com.example.tasks;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(TaskResource.class.getName());
    private static final TaskStore STORE = new FileTaskStore(resolveDataFile());

    @GET
    public List<Task> list() {
        LOGGER.info("Listar tareas");
        return STORE.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        LOGGER.info("Obtener tarea id=" + id);
        Optional<Task> task = STORE.findById(id);
        return task.map(value -> Response.ok(value).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response create(Task task) {
        LOGGER.info("Crear tarea");
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            LOGGER.warning("Crear tarea fallida: titulo obligatorio");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload("El titulo es obligatorio"))
                    .build();
        }
        Task created = STORE.create(task);
        LOGGER.info("Tarea creada id=" + created.getId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, Task task) {
        LOGGER.info("Actualizar tarea id=" + id);
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            LOGGER.warning("Actualizar tarea fallida id=" + id + ": titulo obligatorio");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload("El titulo es obligatorio"))
                    .build();
        }
        Optional<Task> updated = STORE.update(id, task);
        if (updated.isPresent()) {
            LOGGER.info("Tarea actualizada id=" + id);
        } else {
            LOGGER.warning("Actualizar tarea fallida id=" + id + ": no encontrada");
        }
        return updated.map(value -> Response.ok(value).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        LOGGER.info("Eliminar tarea id=" + id);
        boolean removed = STORE.delete(id);
        if (removed) {
            LOGGER.info("Tarea eliminada id=" + id);
            return Response.noContent().build();
        }
        LOGGER.warning("Eliminar tarea fallida id=" + id + ": no encontrada");
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private static java.nio.file.Path resolveDataFile() {
        String configured = System.getProperty("tasks.file");
        if (configured == null || configured.trim().isEmpty()) {
            String dataDir = System.getProperty("jboss.server.data.dir");
            if (dataDir == null || dataDir.trim().isEmpty()) {
                dataDir = System.getProperty("java.io.tmpdir");
            }
            configured = dataDir + "/crud-file/tasks.json";
        }
        return java.nio.file.Path.of(configured);
    }

    public static class ErrorPayload {
        public String message;

        public ErrorPayload() {
        }

        public ErrorPayload(String message) {
            this.message = message;
        }
    }
}
