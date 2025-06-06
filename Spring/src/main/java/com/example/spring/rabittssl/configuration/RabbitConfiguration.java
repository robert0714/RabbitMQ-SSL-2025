/*
 * Copyright 2017-2018 Andreas Sekulski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spring.rabittssl.configuration;

import com.example.spring.rabittssl.receiver.DeviceMonitoringReceiver;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Sample configuration class for rabbitmq connection handling.
 * @author Andreas Sekulski
 */
@Configuration
@PropertySource("classpath:rabbit.properties")
public class RabbitConfiguration {

    /**
     * Default sample channel name to respond for requests from clients.
     */
    public static final String DEFAULT_QUEUE = "sample_queue";

    /**
     * Use durable queues for persistent storage.
     */
    public static final boolean QueueDurable = true;

    /**
     * Use exclusive queues if they should be restricted to the connection that created them.
     */
    public static final boolean QueueExclusive = false;

    /**
     * Auto deletion from channel if not required anymore.
     */
    public static final boolean QueueAutoDelete = false;

    /**
     * Additional queue arguments to set.
     */
    public static final Map<String, Object> QueueArguments = null;

    /**
     * Environment properties file from rabbitmq configuration.
     */
    @Autowired
    private Environment env;

    /**
     * Establish a connection to a rabbit mq server.
     * @return Rabbit connection factory for rabbitmq access.
     * @throws IOException If wrong parameters are used for connection.
     */
//    @Bean
//    public RabbitConnectionFactoryBean connectionFactoryBean() throws IOException {
//        RabbitConnectionFactoryBean connectionFactoryBean = new RabbitConnectionFactoryBean();
//        connectionFactoryBean.setHost(Objects.requireNonNull(env.getProperty("rabbit.host")));
//        connectionFactoryBean.setPort(Integer.parseInt(Objects.requireNonNull(env.getProperty("rabbit.port"))));
//        connectionFactoryBean.setUsername(Objects.requireNonNull(env.getProperty("rabbit.username")));
//        connectionFactoryBean.setPassword(Objects.requireNonNull(env.getProperty("rabbit.password")));
//
//        // SSL-Configuration if set
//        if(env.getProperty("rabbit.ssl") != null) {
//            connectionFactoryBean.setUseSSL(true);
//            connectionFactoryBean.setSslAlgorithm(Objects.requireNonNull(env.getProperty("rabbit.ssl")));
//
//            // This information should be stored safely !!!
//            connectionFactoryBean.setKeyStore(Objects.requireNonNull(env.getProperty("rabbit.keystore.name")));
//            connectionFactoryBean.setKeyStorePassphrase(Objects.requireNonNull(env.getProperty("rabbit.keystore.password")));
//            connectionFactoryBean.setTrustStore(Objects.requireNonNull(env.getProperty("rabbit.truststore")));
//            connectionFactoryBean.setTrustStorePassphrase(Objects.requireNonNull(env.getProperty("rabbit.truststore.password")));
//        }
//
//        return connectionFactoryBean;
//    }
    @Bean
    public RabbitConnectionFactoryBean connectionFactoryBean(RabbitProperties properties) throws IOException {
        RabbitConnectionFactoryBean connectionFactoryBean = new RabbitConnectionFactoryBean();
        connectionFactoryBean.setHost(Objects.requireNonNull(properties.getHost()));
        
        connectionFactoryBean.setPort(Objects.requireNonNull(properties.getPort()));
        connectionFactoryBean.setUsername(Objects.requireNonNull(properties.getUsername()));
        connectionFactoryBean.setPassword(Objects.requireNonNull(properties.getPassword()));
        
        // SSL-Configuration if set
        if(properties.getSsl().getEnabled()) {
            connectionFactoryBean.setUseSSL(true); 

            // This information should be stored safely !!!
            connectionFactoryBean.setKeyStore(Objects.requireNonNull(properties.getSsl().getKeyStore()));
            connectionFactoryBean.setKeyStorePassphrase(Objects.requireNonNull(properties.getSsl().getKeyStorePassword()));
            connectionFactoryBean.setTrustStore(Objects.requireNonNull(properties.getSsl().getTrustStore()));
            connectionFactoryBean.setTrustStorePassphrase(Objects.requireNonNull(properties.getSsl().getTrustStorePassword()));
        }

        return connectionFactoryBean;
    }

    /**
     * Connection factory which established a rabbitmq connection used from a connection factory
     * @param connectionFactoryBean Connection factory bean to create connection.
     * @return A connection factory to create connections.
     * @throws Exception If wrong parameters are used for connection.
     */
    @Bean(name = "GEO_RABBIT_CONNECTION")
    public ConnectionFactory connectionFactory(RabbitConnectionFactoryBean connectionFactoryBean) throws Exception {
        return new CachingConnectionFactory(Objects.requireNonNull(connectionFactoryBean.getObject()));
    }

    /**
     * Queue initialization from rabbitmq to listen a queue.
     * @return A queue to listen for listen receiver.
     */
    @Bean
    public Queue queue() {
        // Create a new queue to handle incoming responds
        return new Queue(DEFAULT_QUEUE, QueueDurable, QueueExclusive, QueueAutoDelete, QueueArguments);
    }

    /**
     * Generates a simple message listener container.
     * @param connectionFactory Established connection to rabbitmq server.
     * @param listenerAdapter   Listener event adapter to listen for messages.
     * @return A simple message container for listening for requests.
     */
    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                                    MessageListenerAdapter listenerAdapter) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(DEFAULT_QUEUE);
        container.setMessageListener(listenerAdapter);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return container;

    }

    /**
     * Message listener adapter to generate a message listener.
     * @param deviceMonitoringReceiver Device receive to for listening.
     * @return A message listener adapter to receive messages.
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(DeviceMonitoringReceiver deviceMonitoringReceiver) {
        return new MessageListenerAdapter(deviceMonitoringReceiver, "receiveMessage");
    }
}