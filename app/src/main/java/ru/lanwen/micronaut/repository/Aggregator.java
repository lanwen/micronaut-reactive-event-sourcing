package ru.lanwen.micronaut.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;

public interface Aggregator {

    default Flux<AccountEvent> process(AccountCommand command) {
        if (command instanceof AccountCommand.AddCommand) {
            return process((AccountCommand.AddCommand) command);
        }

        if (command instanceof AccountCommand.TransferCommand) {
            return process((AccountCommand.TransferCommand) command);
        }

        return Flux.empty();
    }

    default Flux<AccountEvent> process(AccountCommand.AddCommand command) {
        return Flux.empty();
    }

    default Flux<AccountEvent> process(AccountCommand.TransferCommand command) {
        return Flux.empty();
    }

    default Mono<Void> accept(AccountEvent event) {
        return Mono.empty();
    }

}
