package de.globbi.codefactory.core;

import de.globbi.codefactory.persistence.Monkey;
import de.globbi.codefactory.persistence.MonkeyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.concurrent.TimeUnit;

@Service
public class MonkeyService {

    @Autowired
    MonkeyDao monkeyDao;

    @Transactional
    public void processMonkey(long id) {

        Monkey monkey = monkeyDao.readById(id);
        System.out.printf("Monkey (%d): %s%n", id, monkey);
        if (monkey != null) {
            monkeyDao.delete(monkey.getId());
        }
    }

}
