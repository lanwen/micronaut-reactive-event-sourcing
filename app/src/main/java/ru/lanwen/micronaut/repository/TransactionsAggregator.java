package ru.lanwen.micronaut.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;

import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Singleton
public class TransactionsAggregator implements Aggregator {

    Map<UUID, String> transactions = new ConcurrentHashMap<>();

    @Override
    public Flux<AccountEvent> process(AccountCommand command) {
        transactions.put(command.getId(), "opened");
        return Flux.just(new AccountEvent.TransactionOpenedEvent(command.getKey(), command.getId()));
    }

    @Override
    public Mono<Void> accept(AccountEvent event) {
        transactions.put(event.getId(), TRANSACTION_STATE_MAPPER.apply(event));
        return Mono.empty();
    }

    public static final Function<AccountEvent, String> TRANSACTION_STATE_MAPPER = (event) -> {
        if (event instanceof AccountEvent.TransactionOpenedEvent) {
            return "opened";
        }

        if (event instanceof AccountEvent.TransactionRejectedEvent) {
            return "rejected";
        }

        return "finished";
    };
}
