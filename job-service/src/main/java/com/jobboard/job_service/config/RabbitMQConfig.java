package com.jobboard.job_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "jobboard.exchange";
    public static final String QUEUE_JOB_CREATED = "notification.job.created";
    public static final String ROUTING_KEY_JOB_CREATED = "job.created";

    @Bean
    public TopicExchange jobboardExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue jobCreatedQueue() {
        return new Queue(QUEUE_JOB_CREATED, true);
    }

    @Bean
    public Binding jobCreatedBinding(Queue jobCreatedQueue, TopicExchange jobboardExchange) {
        return BindingBuilder.bind(jobCreatedQueue)
                .to(jobboardExchange)
                .with(ROUTING_KEY_JOB_CREATED);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
