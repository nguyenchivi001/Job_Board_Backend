package com.jobboard.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "jobboard.exchange";

    public static final String QUEUE_JOB_CREATED = "notification.job.created";
    public static final String QUEUE_APP_SUBMITTED = "notification.application.submitted";
    public static final String QUEUE_APP_STATUS_UPDATED = "notification.application.status.updated";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue jobCreatedQueue() {
        return new Queue(QUEUE_JOB_CREATED, true);
    }

    @Bean
    public Queue applicationSubmittedQueue() {
        return new Queue(QUEUE_APP_SUBMITTED, true);
    }

    @Bean
    public Queue applicationStatusUpdatedQueue() {
        return new Queue(QUEUE_APP_STATUS_UPDATED, true);
    }

    @Bean
    public Binding jobCreatedBinding(Queue jobCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(jobCreatedQueue).to(exchange).with("job.created");
    }

    @Bean
    public Binding applicationSubmittedBinding(Queue applicationSubmittedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(applicationSubmittedQueue).to(exchange).with("application.submitted");
    }

    @Bean
    public Binding applicationStatusUpdatedBinding(Queue applicationStatusUpdatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(applicationStatusUpdatedQueue).to(exchange).with("application.status.updated");
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
