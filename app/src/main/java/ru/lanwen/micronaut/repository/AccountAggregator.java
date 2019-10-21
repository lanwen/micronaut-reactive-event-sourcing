package ru.lanwen.micronaut.repository;

import lombok.Value;
import reactor.core.publisher.Flux;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;

import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Singleton
public class AccountAggregator implements Aggregator {
    Map<UUID, State> accounts = new ConcurrentHashMap<>();

    @Override
    public Flux<AccountEvent> process(AccountCommand.AddCommand command) {
        accounts.compute(
                command.getKey(),
                (key, value) -> Objects.isNull(value)
                        ? new State(0, command.getAmount())
                        : new State(value.getVersion() + 1, value.getAmount().add(command.getAmount()))
        );
        return Flux.just(new AccountEvent.MoneyAddedEvent(command.getKey(), command.getId(), command.getAmount()));
    }

    @Override
    public Flux<AccountEvent> process(AccountCommand.TransferCommand command) {
        var from = accounts.get(command.getKey());

        if (Objects.isNull(from) || from.getAmount().compareTo(command.getAmount()) < 0) {
            return Flux.just(new AccountEvent.TransactionRejectedEvent(
                    command.getKey(), command.getId(), String.format("Not enough money to transfer %s", command.getAmount())
            ));
        }

        accounts.put(command.getKey(), new State(from.getVersion() + 1, from.getAmount().subtract(command.getAmount())));
        accounts.compute(command.getTo(), (key, value) -> Objects.isNull(value)
                ? new State(0, command.getAmount())
                : new State(value.getVersion() + 1, value.getAmount().add(command.getAmount())));

        return Flux.just(new AccountEvent.TransferFinishedEvent(command.getKey(), command.getId(), command.getTo(), command.getAmount()));
    }

    public static Function<AccountEvent, BigDecimal> accountBalanceMapper(UUID id) {
        return event -> {
            if (event instanceof AccountEvent.MoneyAddedEvent) {
                return ((AccountEvent.MoneyAddedEvent) event).getAmount();
            }

            if (!(event instanceof AccountEvent.TransferFinishedEvent)) {
                return BigDecimal.ZERO;
            }

            var transfer = (AccountEvent.TransferFinishedEvent) event;

            if (transfer.getKey().equals(id)) {
                return transfer.getAmount().negate();
            }

            return transfer.getAmount();
        };
    }

    @Value
    static class State {
        long version;

        BigDecimal amount;
    }
}
