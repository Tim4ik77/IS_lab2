package ru.example.tickets.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.example.tickets.enums.EventType;

public record EventRequest(
        @jakarta.validation.constraints.NotBlank String name,
        @NotNull @Positive Long ticketsCount,
        @NotNull EventType eventType) {}
