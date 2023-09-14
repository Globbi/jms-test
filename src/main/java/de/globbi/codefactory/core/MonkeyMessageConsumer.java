package de.globbi.codefactory.core;

import de.globbi.codefactory.persistence.Monkey;
import de.globbi.codefactory.persistence.MonkeyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.concurrent.TimeUnit;

@Component
public class MonkeyMessageConsumer implements MessageListener, ErrorHandler {

    @Autowired
    MonkeyService monkeyService;

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            try {
                // Get Message
                String text = ((TextMessage) message).getText();

                // Determine ID
                long id = Long.parseLong(text.split(":")[1].trim());

                // Process ID
                monkeyService.processMonkey(id);

                TimeUnit.SECONDS.sleep(3);
            } catch (JMSException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleError(Throwable t) {
        System.err.println("AN ERROR OCCURED: " + t.getMessage());
    }
}
