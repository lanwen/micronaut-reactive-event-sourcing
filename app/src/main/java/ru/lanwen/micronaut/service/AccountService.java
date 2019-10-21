package ru.lanwen.micronaut.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;
import ru.lanwen.micronaut.repository.EventStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class AccountService {

    @Inject
    EventStream stream;

    public Mono<Void> accept(AccountCommand command) {
        return stream.submit(command);
    }

    public Flux<AccountEvent> account(UUID accountId) {
        return stream
                .events()
                .filter(event -> !(event instanceof AccountEvent.TransactionOpenedEvent))
                .filter(event -> event instanceof AccountEvent.TransferFinishedEvent && ((AccountEvent.TransferFinishedEvent) event).getTo().equals(accountId)
                        || event.getKey().equals(accountId)
                );
    }

    public Flux<AccountEvent> transaction(UUID id) {
        return stream
                .events()
                .filter(event -> event.getId().equals(id));
    }
}
