package ru.example.tickets.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import ru.example.tickets.exception.BusinessException;

@Component
public class UniqueNames {
    @PersistenceContext private EntityManager em;

    // All callers run in SERIALIZABLE transactions: the predicate read and write are atomic.
    public void check(String entity, String name, Long excludedId) {
        if (!entity.equals("Event") && !entity.equals("Venue")) throw new IllegalArgumentException();
        var rows = em.createQuery("select e.id, e.name from " + entity + " e", Object[].class).getResultList();
        if (rows.stream().anyMatch(row -> key((String) row[1]).equals(key(name))
            && (excludedId == null || ((Number) row[0]).longValue() != excludedId))) {
            throw new BusinessException(409, "Название уже используется: " + name);
        }
    }

    public static String key(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
