package ru.example.tickets.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.example.tickets.dto.request.EventRequest;
import ru.example.tickets.dto.request.VenueRequest;
import ru.example.tickets.entity.Event;
import ru.example.tickets.entity.Venue;
import ru.example.tickets.exception.BusinessException;

/** Resolves shared references inside the caller's import transaction. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ImportReferenceResolver {
    private final EventService events;
    private final VenueService venues;
    @PersistenceContext private EntityManager em;

    public long event(EventRequest input) {
        var matches = em.createQuery("select e from Event e", Event.class).getResultList().stream()
            .filter(e -> UniqueNames.key(e.getName()).equals(UniqueNames.key(input.name()))).toList();
        if (matches.isEmpty()) return events.save(null, input).id();
        var e = matches.get(0);
        if (matches.size() != 1 || !Objects.equals(e.getTicketsCount(), input.ticketsCount()) || e.getEventType() != input.eventType())
            throw new BusinessException(409, "Событие с таким названием имеет другие поля");
        return e.getId();
    }

    public int venue(VenueRequest input) {
        var matches = em.createQuery("select e from Venue e", Venue.class).getResultList().stream()
            .filter(e -> UniqueNames.key(e.getName()).equals(UniqueNames.key(input.name()))).toList();
        if (matches.isEmpty()) return venues.save(null, input).id();
        var e = matches.get(0);
        if (matches.size() != 1 || !Objects.equals(e.getCapacity(), input.capacity()) || e.getType() != input.type())
            throw new BusinessException(409, "Площадка с таким названием имеет другие поля");
        return e.getId();
    }
}
