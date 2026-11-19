package ru.example.tickets.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.tickets.entity.ImportOperation;

public interface ImportOperationRepository extends JpaRepository<ImportOperation, Long> {
    Page<ImportOperation> findByUsername(String username, Pageable pageable);
}
