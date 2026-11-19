package ru.example.tickets.service;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.example.tickets.dto.request.*;
import ru.example.tickets.entity.*;
import ru.example.tickets.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class ImportWorker {
    private final ImportFileReader reader;
    private final ImportReferenceResolver references;
    private final Validator validator;
    private final TicketService tickets;
    private final CoordinatesService coordinates;
    private final LocationService locations;
    private final PersonService persons;
    private final ImportHistoryService history;

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public ImportOperation run(long operationId, MultipartFile file) {
        var rows = reader.read(file);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (row == null) throw new BusinessException(400, "Запись " + (i + 1) + ": объект не может быть null");
            var violations = validator.validate(row);
            if (!violations.isEmpty()) {
                var messages = violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
                throw new BusinessException(400, "Запись " + (i + 1) + ": " + String.join("; ", messages));
            }
            try {
                var c = coordinates.save(null, row.coordinates());
                Long personId = null;
                if (row.person() != null) {
                    var p = row.person();
                    var l = locations.save(null, p.location());
                    personId = persons.save(null, new PersonRequest(p.eyeColor(), p.hairColor(), l.id(), p.weight())).id();
                }
                tickets.save(null, new TicketRequest(row.name(), c.id(), personId, references.event(row.event()),
                    row.price(), row.type(), row.discount(), row.number(), references.venue(row.venue())));
            } catch (BusinessException e) {
                throw new BusinessException(e.getStatus(), "Запись " + (i + 1) + ": " + e.getMessage());
            }
        }
        return history.success(operationId, rows.size());
    }

}
