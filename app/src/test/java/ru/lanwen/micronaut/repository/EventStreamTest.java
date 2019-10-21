package ru.lanwen.micronaut.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.lanwen.micronaut.commands.AccountCommand;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EventStreamTest {
    EventStream stream = new EventStream(new AccountAggregator(), new TransactionsAggregator());

    @BeforeEach
    void setUp() {
        stream.init();
    }

    @Test
    void shouldProcessCommands() {
        stream.submit(new AccountCommand.AddCommand(UUID.randomUUID(), BigDecimal.ONE)).block(Duration.ofSeconds(1));

        await().untilAsserted(() -> assertThat(stream.getOffset().intValue()).isEqualTo(2));
        assertThat(stream.events().collectList().block(Duration.ofSeconds(1))).hasSize(2);
    }
}