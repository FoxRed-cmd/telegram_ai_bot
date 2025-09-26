package com.viaibot.ai.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicsConfig(val topicProperties: KafkaTopicProperties) {

    @Bean
    fun kafkaTopics(): List<NewTopic> {
        return topicProperties.topics.map { (key, config) ->
            TopicBuilder
                .name(config.name)
                .partitions(config.partitions)
                .replicas(config.replicas)
                .build()
        }
    }
}