package ru.lanwen.micronaut.events;

import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountEvent {

    UUID getKey();

    UUID getId();

    String getType();

    @Value
    class MoneyAddedEvent implements AccountEvent {
        String type = "money/added";

        UUID key;

        UUID id;

        BigDecimal amount;

    }

    @Value
    class TransactionOpenedEvent implements AccountEvent {

        String type = "transaction/opened";

        UUID key;

        UUID id;

    }

    @Value
    class TransactionRejectedEvent implements AccountEvent {

        String type = "transaction/rejected";

        UUID key;

        UUID id;

        String reason;

    }

    @Value
    class TransferFinishedEvent implements AccountEvent {

        String type = "transfer/finished";

        UUID key;

        UUID id;

        UUID to;

        BigDecimal amount;

    }
}
