package de.globbi.codefactory.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

@Component
public class MonkeyDao {

    @Autowired(required = false)
    EntityManager entityManager;

    @Transactional
    public void saveAll(Monkey[] monkeys) {
        for (Monkey monkey : monkeys) {
            entityManager.persist(monkey);
        }
    }

    public Monkey readById(long id) {
        return entityManager.find(Monkey.class, id);
    }

    @Transactional
    public void delete(long id) {
        Query query = entityManager.createQuery("DELETE FROM Monkey m WHERE m.id = :id");
        query.setParameter("id", id);
        query.executeUpdate();

//        Monkey monkey = entityManager.find(Monkey.class, id);
//        entityManager.remove(monkey);
    }

}
