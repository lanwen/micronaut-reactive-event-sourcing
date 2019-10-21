package ru.lanwen.micronaut.commands;

import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountCommand {
    UUID getId();

    UUID getKey();

    @Value
    class AddCommand implements AccountCommand {

        UUID id = UUID.randomUUID();

        UUID key;

        BigDecimal amount;
    }

    @Value
    class TransferCommand implements AccountCommand {

        UUID id = UUID.randomUUID();

        UUID key;

        UUID to;

        BigDecimal amount;
    }
}
