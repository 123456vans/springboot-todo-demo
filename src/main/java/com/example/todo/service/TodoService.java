package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.entity.Todo;

import java.util.List;

public interface TodoService {

    Todo createTodo(Long userId, CreateTodoRequest request);

    List<Todo> getTodos(Long userId);

    void finishTodo(Long userId, Long todoId);

    void deleteTodo(Long userId, Long todoId);
}
