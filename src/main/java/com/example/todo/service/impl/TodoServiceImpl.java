package com.example.todo.service.impl;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.entity.Todo;
import com.example.todo.entity.TodoLog;
import com.example.todo.mapper.TodoLogMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.mq.TodoMessageProducer;
import com.example.todo.service.TodoService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TodoServiceImpl implements TodoService {

    private static final int TODO_UNFINISHED = 0;
    private static final int TODO_FINISHED = 1;
    private static final int RECENT_OPERATION_LIMIT = 20;

    private final TodoMapper todoMapper;
    private final TodoLogMapper todoLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final TodoMessageProducer todoMessageProducer;

    public TodoServiceImpl(TodoMapper todoMapper,
                           TodoLogMapper todoLogMapper,
                           StringRedisTemplate redisTemplate,
                           RedissonClient redissonClient,
                           TodoMessageProducer todoMessageProducer) {
        this.todoMapper = todoMapper;
        this.todoLogMapper = todoLogMapper;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.todoMessageProducer = todoMessageProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Todo createTodo(Long userId, CreateTodoRequest request) {
        String title = normalizeTitle(request);
        String lockKey = "lock:todo:" + userId + ":" + title;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "请勿重复提交同名任务");
            }

            if (todoMapper.countByUserIdAndTitle(userId, title) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "同名任务已存在");
            }

            LocalDateTime now = LocalDateTime.now();
            Todo todo = new Todo();
            todo.setUserId(userId);
            todo.setTitle(title);
            todo.setStatus(TODO_UNFINISHED);
            todo.setCreateTime(now);
            todoMapper.insert(todo);

            todoLogMapper.insert(newTodoLog(userId, todo.getId(), "CREATE", now));
            updateCachedTodoCount(userId);
            pushRecentOperation(userId, "CREATE:" + todo.getId());
            todoMessageProducer.sendTodoCreated(todo.getId());
            return todo;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "获取任务锁时线程被中断", exception);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<Todo> getTodos(Long userId) {
        List<Todo> todos = todoMapper.findByUserId(userId);
        String countKey = countKey(userId);
        if (redisTemplate.opsForValue().get(countKey) == null) {
            redisTemplate.opsForValue().set(countKey, String.valueOf(todos.size()));
        }
        return todos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishTodo(Long userId, Long todoId) {
        int affectedRows = todoMapper.updateStatus(todoId, userId, TODO_FINISHED);
        if (affectedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        todoLogMapper.insert(newTodoLog(
                userId, todoId, "FINISH", LocalDateTime.now()));
        pushRecentOperation(userId, "FINISH:" + todoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = todoMapper.findByIdAndUserId(todoId, userId);
        if (todo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        int affectedRows = todoMapper.deleteByIdAndUserId(todoId, userId);
        if (affectedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }

        todoLogMapper.insert(newTodoLog(
                userId, todoId, "DELETE", LocalDateTime.now()));
        updateCachedTodoCount(userId);
        pushRecentOperation(userId, "DELETE:" + todoId);
    }

    private String normalizeTitle(CreateTodoRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title 不能为空");
        }
        String title = request.getTitle().trim();
        if (title.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title 长度不能超过 255");
        }
        return title;
    }

    private TodoLog newTodoLog(Long userId,
                               Long todoId,
                               String action,
                               LocalDateTime createTime) {
        TodoLog todoLog = new TodoLog();
        todoLog.setUserId(userId);
        todoLog.setTodoId(todoId);
        todoLog.setAction(action);
        todoLog.setCreateTime(createTime);
        return todoLog;
    }

    private void updateCachedTodoCount(Long userId) {
        long count = todoMapper.countByUserId(userId);
        redisTemplate.opsForValue().set(countKey(userId), String.valueOf(count));
    }

    private void pushRecentOperation(Long userId, String operation) {
        String key = "todo:recent:" + userId;
        redisTemplate.opsForList().leftPush(key, operation);
        redisTemplate.opsForList().trim(key, 0, RECENT_OPERATION_LIMIT - 1);
    }

    private String countKey(Long userId) {
        return "todo:count:" + userId;
    }
}
