package com.kakas.stockTrading.messaging;

import com.kakas.stockTrading.message.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class MessagingFactory {
    @Autowired
    MessageType messageType;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;


    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        // 这段代码主要是自动将KafkaTopic的主题进行创建。
        try (AdminClient client = KafkaAdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 查询目前的topics
            Set<String> allTopics = client.listTopics().names().get();
            // 自动加载新topic
            List<NewTopic> newTopics = new ArrayList<>();
            for (KafkaTopic topic : KafkaTopic.values()) {
                if (!allTopics.contains(topic)) {
                    newTopics.add(new NewTopic(topic.name(), 1, (short) 1));
                }
            }
            // 创建新topic
            if (!newTopics.isEmpty()) {
                client.createTopics(newTopics);
                newTopics.forEach(t -> log.warn("Auto create topic: {}", t.name()));
            }
        }
    }

    public <T extends Message> MessageProducer<T>  createMessageProducer(KafkaTopic topic) {
        log.info("Try create producer of topic : {}", topic.name());
        return message -> kafkaTemplate.send(topic.name(), messageType.serialize(message));
    }

    public <T extends Message> MessageConsumer  createBatchMessageConsumer(KafkaTopic topic, String groupId, BatchMessageHandler<T> messageHandler) {
        log.info("Try create consumer of topic : {}, groupId : {}", topic.name(), groupId);
        // 创建listener容器
        ConcurrentMessageListenerContainer<String, String> listenerContainer = listenerContainerFactory.createListenerContainer(
                new KafkaListenerEndpointAdapter() {
                    @Override
                    public String getGroupId() {return groupId;}
                    @Override
                    public Collection<String> getTopics() {return List.of(topic.name());}
                }
        );
        // 启动listener容器，并调用传入的处理方法处理消息。
        listenerContainer.setupMessageListener(new BatchMessageListener<String, String>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onMessage(List<ConsumerRecord<String, String>> data) {
                List<T> messages = new ArrayList<>(data.size());
                for (ConsumerRecord<String, String> cr : data) {
                    Message message = messageType.deserialize(cr.value());
                    messages.add((T) message);
                }
                messageHandler.processMessages(messages);
            }
        });
        listenerContainer.start();
        return listenerContainer::stop;
    }
}
