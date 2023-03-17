package com.kakas.stockTrading.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakas.stockTrading.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class MessageType {

    @Autowired
    ObjectMapper objectMapper;
    String messagePackage = "com.kakas.stockTrading.message";
    Map<String, Class<? extends Message>> messageType = new HashMap<>();
    private final String ESP = "#";

    // 该代码主要用于在运行时自动识别并加载某个包下的所有子类，以便进行相应的处理。
    public void init() {
        // 使用 ClassPathScanningCandidateComponentProvider 对象扫描指定包路径下的所有类，筛选出继承自 Message 类的子类，
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return Message.class.isAssignableFrom(clazz);
        });
        // 将这些子类的 Class 对象存入一个 messageType 中
        Set<BeanDefinition> beans = provider.findCandidateComponents(messagePackage);
        for (BeanDefinition bean : beans) {
            try {
                Class<?> clazz = Class.forName(bean.getBeanClassName());
                log.info("成功载入message类: {}", clazz.getName());
                messageType.put(clazz.getName(), (Class<? extends Message>) clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 将message序列化，并将类型信息存入。
    public String serialize(Message message){
        String type = message.getClass().getName();
        String json = null;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warn("无法写成json", e);
            throw new RuntimeException(e);
        }
        return type + ESP + json;
    }

    // 将data中类型信息取出，并反序列化为message
    public Message deserialize(String data){
        String type = data.substring(0, data.indexOf(ESP));
        String json = data.substring(data.indexOf(ESP));
        Class<? extends Message> clazz = messageType.get(type);
        if (clazz == null) {
            throw new RuntimeException("Message type not found!");
        }
        Message message = null;
        try {
            message =  objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("无法读取json" + json, e);
            throw new RuntimeException(e);
        }
        return message;
    }

}
