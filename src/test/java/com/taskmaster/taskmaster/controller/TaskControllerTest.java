package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.model.Task;
import com.taskmaster.taskmaster.model.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskController taskController;

    @Test
    void getAllTasks_returnsAllTasksFromRepository() {
        Task first = new Task(1L, "Write report", "Quarterly update", false);
        Task second = new Task(2L, "Review PR", "Code review", true);
        when(taskRepository.findAll()).thenReturn(List.of(first, second));

        List<Task> result = taskController.getAllTasks();

        assertThat(result).containsExactly(first, second);
        verify(taskRepository).findAll();
    }

    @Test
    void getTaskById_returnsTaskWhenPresent() {
        Task task = new Task(1L, "Write report", "Quarterly update", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        ResponseEntity<Task> response = taskController.getTaskById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(task);
        verify(taskRepository).findById(1L);
    }

    @Test
    void getTaskById_returnsNotFoundWhenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Task> response = taskController.getTaskById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(taskRepository).findById(99L);
    }

    @Test
    void createTask_savesAndReturnsCreatedTask() {
        Task input = new Task(null, "Write report", "Quarterly update", false);
        Task saved = new Task(1L, "Write report", "Quarterly update", false);
        when(taskRepository.save(input)).thenReturn(saved);

        Task result = taskController.createTask(input);

        assertThat(result).isEqualTo(saved);
        verify(taskRepository).save(input);
    }

    @Test
    void updateTask_updatesExistingTask() {
        Task existing = new Task(1L, "Old title", "Old description", false);
        Task updated = new Task(null, "New title", "New description", true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        ResponseEntity<Task> response = taskController.updateTask(1L, updated);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(existing);
        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getDescription()).isEqualTo("New description");
        assertThat(existing.isCompleted()).isTrue();
        verify(taskRepository).save(existing);
    }

    @Test
    void updateTask_returnsNotFoundWhenMissing() {
        Task updated = new Task(null, "New title", "New description", true);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Task> response = taskController.updateTask(99L, updated);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_returnsNoContentWhenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);

        ResponseEntity<Void> response = taskController.deleteTask(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_returnsNotFoundWhenTaskMissing() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        ResponseEntity<Void> response = taskController.deleteTask(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(taskRepository, never()).deleteById(anyLong());
    }
}
