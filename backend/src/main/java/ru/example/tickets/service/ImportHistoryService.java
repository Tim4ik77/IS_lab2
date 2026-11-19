package ru.example.tickets.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ru.example.tickets.dto.view.PageView;
import ru.example.tickets.entity.ImportOperation;
import ru.example.tickets.exception.BusinessException;
import ru.example.tickets.repository.ImportOperationRepository;

@Service
@RequiredArgsConstructor
public class ImportHistoryService {
    private final ImportOperationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public long start(String username, String filename) {
        var op = new ImportOperation();
        op.setUsername(username);
        op.setFilename(filename == null ? "import.json" : filename.substring(0, Math.min(255, filename.length())));
        op.setStatus("RUNNING");
        op.setStartedAt(LocalDateTime.now());
        return repository.saveAndFlush(op).getId();
    }

    // Successful status is committed in the SAME transaction as the imported objects.
    @Transactional(propagation = Propagation.MANDATORY)
    public ImportOperation success(long id, int count) {
        var op = repository.findById(id).orElseThrow();
        op.setStatus("SUCCESS");
        op.setAddedCount(count);
        op.setFinishedAt(LocalDateTime.now());
        return repository.saveAndFlush(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ImportOperation failure(long id, String message) {
        var op = repository.findById(id).orElseThrow();
        op.setStatus("FAILED");
        op.setAddedCount(null);
        op.setError(message);
        op.setFinishedAt(LocalDateTime.now());
        return repository.saveAndFlush(op);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public PageView<ImportOperation> list(String username, boolean admin, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new BusinessException(400, "Некорректная страница");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var result = admin ? repository.findAll(pageable) : repository.findByUsername(username, pageable);
        return new PageView<>(result.getContent(), result.getTotalElements(), page, size);
    }
}
