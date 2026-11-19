package ru.example.tickets.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "import_operation")
@Getter @Setter
public class ImportOperation {
    @Id
    @SequenceGenerator(name = "import_ids", sequenceName = "import_operation_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_ids")
    private Long id;
    @Column(nullable = false) private String username;
    @Column(nullable = false) private String filename;
    @Column(nullable = false) private String status;
    @Column(name = "added_count") private Integer addedCount;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "finished_at") private LocalDateTime finishedAt;
    @Column(columnDefinition = "text") private String error;
}
