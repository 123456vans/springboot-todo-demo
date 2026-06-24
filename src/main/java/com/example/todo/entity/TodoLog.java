package com.example.todo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodoLog {

    private Long id;
    private Long userId;
    private Long todoId;
    private String action;
    private LocalDateTime createTime;
}
