package com.example.todo.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = TodoMessageProducer.TODO_TOPIC,
        consumerGroup = "todo-consumer-group"
)
public class TodoMessageConsumer implements RocketMQListener<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TodoMessageConsumer.class);

    @Override
    public void onMessage(String todoId) {
        LOGGER.info("收到 Todo 创建消息，todoId={}", todoId);
    }
}
