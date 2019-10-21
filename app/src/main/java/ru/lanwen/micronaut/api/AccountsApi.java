package ru.lanwen.micronaut.api;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.lanwen.micronaut.events.AccountEvent;
import ru.lanwen.micronaut.repository.AccountAggregator;
import ru.lanwen.micronaut.service.AccountService;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("/accounts")
public class AccountsApi {

    @Inject
    AccountService accountService;

    @Get("/{id}")
    public Mono<AccountResponse> account(@PathVariable UUID id) {
        return accountService.account(id).collectList()
                .map(events -> new AccountResponse(
                        id,
                        events.stream()
                                .map(AccountAggregator.accountBalanceMapper(id))
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        events
                ));
    }


    @Value
    static class AccountResponse {
        UUID id;

        BigDecimal balance;

        List<AccountEvent> events;
    }
}