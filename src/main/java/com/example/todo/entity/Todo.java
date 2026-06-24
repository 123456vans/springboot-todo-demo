package com.example.todo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Todo {

    private Long id;
    private Long userId;
    private String title;
    private Integer status;
    private LocalDateTime createTime;
}
