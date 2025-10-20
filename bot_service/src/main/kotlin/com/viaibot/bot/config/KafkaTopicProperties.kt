package com.viaibot.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "spring.kafka.bot-service")
@Configuration
data class KafkaTopicProperties(
    val topics: Map<String, TopicConfig>
) {

    data class TopicConfig(
        val name: String,
        val partitions: Int,
        val replicas: Int
    )
}