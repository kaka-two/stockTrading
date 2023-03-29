package com.kakas.stockTrading.messaging;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {
    // 获取config server发送过来的配置信息
    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.consumer.max-poll-records}")
    String batchSize;

    @Value("${spring.kafka.consumer.max-partition-fetch-bytes}")
    String byteSize;

    // 配置上下文
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // 配置生产者工厂
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    // 持有生产者工厂的高级api
    @Bean
    public KafkaTemplate kafkaTemplate(
            @Autowired ProducerFactory<String, String> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    // 配置消费者工厂
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        configs.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, byteSize);
//        //设置key反序列化器
//        properties.put("key.deserializer", Class.forName("org.apache.kafka.common.serialization.StringDeserializer"));
//
//        //设置值反序列化器
//        properties.put("value.deserializer", Class.forName("org.apache.kafka.common.serialization.StringDeserializer"));

        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // 持有消费者工厂的高级api，可以处理@KafkaListener，但是我们为了供多个模块使用，会从其他模块输入处理方法句柄，再自己创建一个ConcurrentKafkaListenerContainer返回。
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            @Autowired ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConcurrency(1);
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }



}
