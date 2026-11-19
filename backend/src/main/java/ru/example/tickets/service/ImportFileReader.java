package ru.example.tickets.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.example.tickets.dto.request.ImportTicketRequest;
import ru.example.tickets.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class ImportFileReader {
    private static final int MAX_RECORDS = 1000;
    private final ObjectMapper mapper;

    public List<ImportTicketRequest> read(MultipartFile file) {
        List<ImportTicketRequest> rows;
        try (var input = file.getInputStream()) {
            rows = mapper.readerFor(new TypeReference<List<ImportTicketRequest>>() {})
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(input);
        } catch (IOException e) {
            throw new BusinessException(400, "Некорректный JSON: ожидается массив билетов с вложенными объектами");
        }
        if (rows == null || rows.isEmpty() || rows.size() > MAX_RECORDS)
            throw new BusinessException(400, "Файл должен содержать от 1 до 1000 билетов");
        return rows;
    }
}
