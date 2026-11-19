package ru.example.tickets.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.example.tickets.dto.view.PageView;
import ru.example.tickets.entity.ImportOperation;
import ru.example.tickets.service.ImportHistoryService;
import ru.example.tickets.service.ImportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportHistoryService history;
    private final ImportService service;

    @GetMapping
    public PageView<ImportOperation> list(Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return history.list(auth.getName(), admin, page, size);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImportOperation> upload(@RequestParam("file") MultipartFile file, Authentication auth) {
        var result = service.upload(auth.getName(), file);
        return ResponseEntity.status(result.status()).body(result.operation());
    }
}
