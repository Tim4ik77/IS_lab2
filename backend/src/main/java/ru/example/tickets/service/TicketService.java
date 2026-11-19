package ru.example.tickets.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import ru.example.tickets.dto.request.TicketRequest;
import ru.example.tickets.dto.view.PageView;
import ru.example.tickets.dto.view.TicketView;
import ru.example.tickets.entity.Ticket;
import ru.example.tickets.exception.BusinessException;
import ru.example.tickets.mapper.ViewMapper;
import ru.example.tickets.repository.CoordinatesRepository;
import ru.example.tickets.repository.EventRepository;
import ru.example.tickets.repository.PersonRepository;
import ru.example.tickets.repository.TicketRepository;
import ru.example.tickets.repository.TicketSpecifications;
import ru.example.tickets.repository.VenueRepository;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE)
public class TicketService {
    private final CoordinatesRepository coordinates;
    private final PersonRepository persons;
    private final EventRepository events;
    private final VenueRepository venues;
    private final TicketRepository tickets;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public PageView<TicketView> list(
            int page, int size, String sort, String direction, Map<String, String> filters) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException(400, "Некорректная страница: размер от 1 до 100");
        }
        var result =
                tickets.findAll(
                        TicketSpecifications.matching(filters),
                        PageRequest.of(page, size, TicketSpecifications.sort(sort, direction)));
        return new PageView<>(
                result.getContent().stream().map(ViewMapper::of).toList(),
                result.getTotalElements(),
                page,
                size);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public TicketView get(long id) {
        return ViewMapper.of(requireTicket(id));
    }

    public TicketView save(Long id, TicketRequest request) {
        Ticket ticket = id == null ? new Ticket() : requireTicket(id);
        ticket.setName(request.name());
        ticket.setCoordinates(coordinates.findById(request.coordinatesId())
                .orElseThrow(() -> new BusinessException(404, "Связанный объект не найден: Координаты")));
        ticket.setPerson(
                request.personId() == null
                        ? null
                        : persons.findById(request.personId())
                                .orElseThrow(() -> new BusinessException(404, "Человек не найден")));
        ticket.setEvent(events.findById(request.eventId())
                .orElseThrow(() -> new BusinessException(404, "Связанный объект не найден: Событие")));
        ticket.setPrice(request.price());
        ticket.setType(request.type());
        ticket.setDiscount(request.discount());
        ticket.setNumber(request.number());
        ticket.setVenue(
                venues.findById(request.venueId())
                .orElseThrow(() -> new BusinessException(404, "Площадка не найдена")));
        return ViewMapper.of(tickets.saveAndFlush(ticket));
    }

    public TicketView update(long id, long version, TicketRequest request) {
        checkVersion(requireTicket(id), version);
        return save(id, request);
    }

    private void checkVersion(Ticket ticket, long version) {
        if (ticket.getVersion() != version)
            throw new BusinessException(409, "Билет уже изменён. Обновите данные перед повторной операцией.");
    }

    public void delete(long id, long version) {
        Ticket ticket = requireTicket(id);
        checkVersion(ticket, version);
        tickets.delete(ticket);
        tickets.flush();
    }

    private Ticket requireTicket(long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Билет не найден: " + id));
    }
}
