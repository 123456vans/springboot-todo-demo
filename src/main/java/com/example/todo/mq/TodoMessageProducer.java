package com.example.todo.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
public class TodoMessageProducer {

    public static final String TODO_TOPIC = "todo-topic";

    private final RocketMQTemplate rocketMQTemplate;

    public TodoMessageProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void sendTodoCreated(Long todoId) {
        rocketMQTemplate.convertAndSend(TODO_TOPIC, String.valueOf(todoId));
    }
}
