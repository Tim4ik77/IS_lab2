package ru.example.tickets.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.example.tickets.service.UniqueNames;

/** Reject pre-existing LR1 duplicates, including names affected by database collation. */
@Component
public class UniqueNameAudit implements ApplicationRunner {
    @PersistenceContext private EntityManager em;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public void run(ApplicationArguments args) {
        for (String entity : new String[] {"Event", "Venue"}) {
            var seen = new HashSet<String>();
            for (String name : em.createQuery("select e.name from " + entity + " e", String.class).getResultList()) {
                if (!seen.add(UniqueNames.key(name)))
                    throw new IllegalStateException("Resolve duplicate " + entity + " names before starting LR2: " + name);
            }
        }
    }
}
