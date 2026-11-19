package ru.example.tickets.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import ru.example.tickets.dto.request.VenueRequest;
import ru.example.tickets.dto.view.VenueView;
import ru.example.tickets.entity.Venue;
import ru.example.tickets.exception.BusinessException;
import ru.example.tickets.mapper.ViewMapper;
import ru.example.tickets.repository.TicketRepository;
import ru.example.tickets.repository.VenueRepository;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE)
public class VenueService {
    private final VenueRepository repository;
    private final TicketRepository links;
    private final UniqueNames uniqueNames;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<VenueView> list() {
        return repository.findAll(Sort.by("id")).stream().map(ViewMapper::of).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public VenueView get(long id) {
        return ViewMapper.of(requireVenue(id));
    }

    public VenueView save(Long id, VenueRequest request) {
        uniqueNames.check("Venue", request.name(), id);
        Venue entity = id == null ? new Venue() : requireVenue(id);
        entity.setName(request.name().trim());
        entity.setCapacity(request.capacity());
        entity.setType(request.type());
        return ViewMapper.of(repository.saveAndFlush(entity));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public long links(long id) {
        return links.countByVenue(requireVenue(id));
    }

    public void delete(long id, Long replacementId) {
        if (replacementId != null && replacementId == id) {
            throw new BusinessException(400, "Для замены выберите другой объект");
        }
        Venue source = requireForUpdate(id);
        Venue replacement = replacementId == null ? null : requireVenue(replacementId);
        long count = links.countByVenue(source);
        if (count > 0) {
            if (replacement == null) {
                throw new BusinessException(409, "Объект используется. Выберите замену.");
            }
            links.reassignVenue(source, replacement);
        }
        repository.delete(source);
        repository.flush();
    }

    private Venue requireVenue(long id) {
        if (id <= 0 || id > Integer.MAX_VALUE) throw new BusinessException(404, "Объект не найден: " + id);
        return repository.findById((int) id)
                .orElseThrow(() -> new BusinessException(404, "Объект не найден: " + id));
    }

    private Venue requireForUpdate(long id) {
        if (id <= 0 || id > Integer.MAX_VALUE) throw new BusinessException(404, "Объект не найден: " + id);
        return repository.findForUpdate((int) id)
                .orElseThrow(() -> new BusinessException(404, "Объект не найден: " + id));
    }
}
