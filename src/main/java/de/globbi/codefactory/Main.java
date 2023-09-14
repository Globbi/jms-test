package de.globbi.codefactory;

import de.globbi.codefactory.persistence.Monkey;
import de.globbi.codefactory.persistence.MonkeyDao;
import oracle.jdbc.pool.OracleConnectionCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
@ComponentScan
@EnableConfigurationProperties
@EnableScheduling
public class Main implements CommandLineRunner {

    private final UUID runId = UUID.randomUUID();

    @Value("${new-messages.count:0}")
    int newMessagesCount;

    @Value("${new-messages.sleep:0}")
    int newMessagesSleep;

    @Autowired
    DefaultMessageListenerContainer dmlc;

    @Autowired
    MonkeyDao monkeyDao;

    @Autowired
    JmsTemplate jmsTemplate;


    public static void main(String[] args) throws Exception {
//        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
//        ObjectName objectName = new ObjectName("com.example:type=Hello");
//        platformMBeanServer.registerMBean(OracleConnectionCacheManager.getConnectionCacheManagerInstance(), objectName);

        ConfigurableApplicationContext ctx = new SpringApplication(Main.class).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        createMessages(newMessagesCount, newMessagesSleep);

        System.out.println("Starting the Message Listener Container...");
        dmlc.start();
    }

    private void createMessages(int limit, int sleep) throws InterruptedException {
        Monkey[] monkeys = new Monkey[limit];
        for (int i = 0; i < limit; i++) {
            monkeys[i] = new Monkey("I am monkey No. " + i + " from session " + runId);
        }

        monkeyDao.saveAll(monkeys);

        for (int i = 0; i < limit; i++) {
            Monkey monkey = monkeys[i];
            System.out.printf("Create monkey message for Monkey (%d) (%d/%d)%n", monkey.getId(), i+1, limit);
            jmsTemplate.convertAndSend("Monkey: " + monkey.getId());
            TimeUnit.SECONDS.sleep(sleep);
        }
    }
}
