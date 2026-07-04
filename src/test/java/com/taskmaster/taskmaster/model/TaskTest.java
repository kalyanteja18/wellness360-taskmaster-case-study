package com.taskmaster.taskmaster.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    void defaultValues_areInitializedForNewTask() {
        Task task = new Task();

        assertThat(task.getId()).isNull();
        assertThat(task.getTitle()).isNull();
        assertThat(task.getDescription()).isNull();
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void setters_populateTaskValues() {
        Task task = new Task();

        task.setId(1L);
        task.setTitle("Write report");
        task.setDescription("Quarterly update");
        task.setCompleted(true);

        assertThat(task.getId()).isEqualTo(1L);
        assertThat(task.getTitle()).isEqualTo("Write report");
        assertThat(task.getDescription()).isEqualTo("Quarterly update");
        assertThat(task.isCompleted()).isTrue();
    }
}
