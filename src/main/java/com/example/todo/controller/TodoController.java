package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.entity.Todo;
import com.example.todo.interceptor.UserIdInterceptor;
import com.example.todo.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public ResponseEntity<Todo> createTodo(
            @RequestAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE) Long userId,
            @RequestBody CreateTodoRequest request) {
        Todo todo = todoService.createTodo(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(todo);
    }

    @GetMapping
    public List<Todo> getTodos(
            @RequestAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE) Long userId) {
        return todoService.getTodos(userId);
    }

    @PutMapping("/{id}/finish")
    public ResponseEntity<Void> finishTodo(
            @RequestAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long id) {
        todoService.finishTodo(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
            @RequestAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long id) {
        todoService.deleteTodo(userId, id);
        return ResponseEntity.noContent().build();
    }
}
