package ru.lanwen.micronaut.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class EventStream {

    @Inject
    final AccountAggregator accountAggregator;

    @Inject
    final TransactionsAggregator transactionsAggregator;

    @Getter
    AtomicInteger offset = new AtomicInteger(0);

    FluxProcessor<AccountCommand, AccountCommand> commandsProcessor = UnicastProcessor.create();

    FluxProcessor<AccountEvent, AccountEvent> eventsProcessor = ReplayProcessor.create(Integer.MAX_VALUE);

    @PostConstruct
    public void init() {
        commandsProcessor
                .publishOn(Schedulers.newSingle("commands"))
                .flatMap(command -> Flux.concat(
                        transactionsAggregator.process(command),
                        accountAggregator.process(command)
                ))
                .delayUntil(event -> Flux.concat(
                        accountAggregator.accept(event),
                        transactionsAggregator.accept(event)
                ))
                .doOnNext(event -> offset.getAndIncrement())
                .subscribe(eventsProcessor);
    }

    public Mono<Void> submit(AccountCommand command) {
        return Mono.fromRunnable(() -> commandsProcessor.onNext(command));
    }

    public Flux<AccountEvent> events() {
        return eventsProcessor.take(offset.get());
    }
}
