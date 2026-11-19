package ru.example.tickets.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.example.tickets.entity.ImportOperation;
import ru.example.tickets.exception.BusinessException;
import ru.example.tickets.exception.DatabaseErrors;

/** Deliberately non-transactional: records failure only AFTER the worker proxy rolls back. */
@Service
@RequiredArgsConstructor
public class ImportService {
    private final ImportHistoryService history;
    private final ImportWorker worker;

    public record ImportResult(int status, ImportOperation operation) {}

    public ImportResult upload(String username, MultipartFile file) {
        long id = history.start(username, file.getOriginalFilename());
        try {
            return new ImportResult(201, worker.run(id, file));
        } catch (RuntimeException e) {
            int status = e instanceof BusinessException b ? b.getStatus() : 500;
            String message = e instanceof BusinessException ? e.getMessage() : "Ошибка импорта. Ни один объект не добавлен.";
            if (DatabaseErrors.hasTransactionConflict(e)) {
                status = 409;
                message = "Конфликт параллельных изменений. Импорт полностью отменён; повторите загрузку.";
            }
            if (status == 500) org.slf4j.LoggerFactory.getLogger(getClass()).error("Import {} failed", id, e);
            return new ImportResult(status, history.failure(id, message));
        }
    }
}
