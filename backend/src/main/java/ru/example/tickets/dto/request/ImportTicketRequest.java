package ru.example.tickets.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.example.tickets.enums.Color;
import ru.example.tickets.enums.TicketType;

public record ImportTicketRequest(
    @NotNull @Size(min = 1) String name,
    @NotNull @Valid CoordinatesRequest coordinates,
    @Valid ImportPerson person,
    @NotNull @Valid EventRequest event,
    @NotNull @Positive Integer price,
    TicketType type,
    @NotNull @Min(1) @Max(100) Long discount,
    @Positive Long number,
    @NotNull @Valid VenueRequest venue) {
    public record ImportPerson(Color eyeColor, Color hairColor,
        @NotNull @Valid LocationRequest location, @Positive Long weight) {}
}
