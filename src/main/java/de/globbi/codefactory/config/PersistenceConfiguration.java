package de.globbi.codefactory.config;

import de.globbi.codefactory.core.MonkeyMessageConsumer;
import de.globbi.codefactory.core.PoolMonitor;
import oracle.jdbc.pool.OracleConnectionCacheManager;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class PersistenceConfiguration {

    public static final String CONNECTION_CACHE_NAME = "poo";

    @Bean
    @ConditionalOnProperty(value = "pds.active")
    @ConfigurationProperties(prefix = "pds")
    PoolDataSource poolDataSource() throws SQLException {
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setURL("jdbc:oracle:thin:@localhost:49161:xe");
        pds.setMinPoolSize(0);
        pds.setInitialPoolSize(8);
        pds.setMaxPoolSize(16);
        pds.setValidateConnectionOnBorrow(true);
        Properties connProps = new Properties();
        connProps.put("oracle.net.disableOob", "true");
        connProps.put("oracle.net.CONNECTION_TIMEOUT", "10000");
        connProps.put("oracle.jdbc.ReadTimeout", "120000");
        pds.setConnectionProperties(connProps);
        return pds;
    }

    @Bean
    @ConditionalOnBean(PoolDataSource.class)
    PoolMonitor pdsPoolMonitor(PoolDataSource poolDataSource) {
        return new PoolMonitor("PDS",
            poolDataSource::getAvailableConnectionsCount,
            poolDataSource::getBorrowedConnectionsCount);
    }

    @Bean
    @ConditionalOnProperty(value = "ods.active")
    @ConfigurationProperties(prefix = "ods")
    OracleDataSource oracleDataSource() throws SQLException {
        OracleDataSource ods = new OracleDataSource();
        ods.setURL("jdbc:oracle:thin:@localhost:49161:xe");
        ods.setConnectionCachingEnabled(true);
        ods.setConnectionCacheName(CONNECTION_CACHE_NAME);
        Properties cacheProps = new Properties();
        cacheProps.put("MinLimit", "0");
        cacheProps.put("MaxLimit", "7");
        cacheProps.put("InitialLimit", "7");
        cacheProps.put("ValidateConnection", "true");
        cacheProps.put("ConnectionWaitTimeout", "300");
        cacheProps.put("PropertyCheckInterval", "5");
        cacheProps.put("InactivityTimeout", "5");
        cacheProps.put("AbandonedConnectionTimeout", "5");
        cacheProps.put("TimeToLiveTimeout", "5");
        ods.setConnectionCacheProperties(cacheProps);
        Properties connProps = new Properties();
        connProps.put("oracle.net.disableOob", "true");
        connProps.put("oracle.net.CONNECTION_TIMEOUT", "10000");
        connProps.put("oracle.jdbc.ReadTimeout", "120000");
        ods.setConnectionProperties(connProps);
        return ods;
    }

    @Bean
    @ConditionalOnBean(OracleDataSource.class)
    PoolMonitor occmPoolMonitor() throws SQLException {
        OracleConnectionCacheManager occm = OracleConnectionCacheManager.getConnectionCacheManagerInstance();
        return new PoolMonitor("OCCM",
            () -> occm.getNumberOfAvailableConnections(CONNECTION_CACHE_NAME),
            () -> occm.getNumberOfActiveConnections(CONNECTION_CACHE_NAME));
    }

    @Bean
    @ConfigurationProperties("jpa")
    JpaProperties jpaProperties() {
        return new JpaProperties();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaDialect(new HibernateJpaDialect());
        Properties jpaProps = new Properties();
        jpaProps.putAll(jpaProperties().getProperties());
        factoryBean.setJpaProperties(jpaProps);
        factoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        factoryBean.setPackagesToScan("de.globbi.codefactory.persistence");
        return factoryBean;
    }

    @Bean
    SharedEntityManagerBean sharedEntityManagerBean(EntityManagerFactory emf) {
        SharedEntityManagerBean sharedEntityManagerBean = new SharedEntityManagerBean();
        sharedEntityManagerBean.setEntityManagerFactory(emf);
        return sharedEntityManagerBean;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
        return jpaTransactionManager;
    }

    @Bean
    ConnectionFactory connectionFactory(DataSource dataSource) throws JMSException {
        SingleConnectionFactory connectionFactory = new SingleConnectionFactory();
        connectionFactory.setTargetConnectionFactory(AQjmsFactory.getQueueConnectionFactory(dataSource));
        connectionFactory.setReconnectOnException(true);
        return connectionFactory;
    }

    @Bean
    DestinationResolver oracleDestinationResolver(@Value("${codefactory.db.queue-user}") String queueUser) {
        return (session, destinationName, pubSubDomain) ->
            ((AQjmsSession) session).getQueue(queueUser, destinationName);
    }

    @Bean
    JmsTemplate jmsTemplate(
            ConnectionFactory connectionFactory,
            DestinationResolver destinationResolver,
            @Value("${codefactory.db.queue-name}") String queueName
    ) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDestinationResolver(destinationResolver);
        jmsTemplate.setDefaultDestinationName(queueName);
//        jmsTemplate.setSessionTransacted(true);
//        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setReceiveTimeout(1000);
        return jmsTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "jms-container")
    DefaultMessageListenerContainer defaultMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DestinationResolver destinationResolver,
            JpaTransactionManager jpaTransactionManager,
            MonkeyMessageConsumer monkeyMessageConsumer
    ) {
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setDestinationResolver(destinationResolver);
        container.setTransactionManager(jpaTransactionManager);
        container.setTransactionTimeout(5);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(2);
        container.setReceiveTimeout(1000);
        container.setAutoStartup(false);
        container.setMessageListener(monkeyMessageConsumer);
        container.setErrorHandler(monkeyMessageConsumer);
        return container;
    }

}
