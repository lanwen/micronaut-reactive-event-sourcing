package ru.lanwen.micronaut.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.uri.UriTemplate;
import io.micronaut.validation.Validated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.lanwen.micronaut.commands.AccountCommand;
import ru.lanwen.micronaut.events.AccountEvent;
import ru.lanwen.micronaut.service.AccountService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static ru.lanwen.micronaut.repository.TransactionsAggregator.TRANSACTION_STATE_MAPPER;

@Slf4j
@Validated
@Controller("/transactions")
public class TransactionsApi {

    @Inject
    AccountService accountService;

    @Get("/{id}")
    public Mono<TransactionResponse> transaction(@PathVariable UUID id) {
        return accountService.transaction(id)
                .collectList()
                .map(events -> new TransactionResponse(id, TRANSACTION_STATE_MAPPER.apply(events.get(events.size() - 1)), events));
    }

    @Post
    public Mono<HttpResponse> transfer(@Valid @Body TransferRequest request) {
        log.info("{} -> {} ({})", request.getFrom(), request.getTo(), request.getAmount());

        if (Objects.equals(request.getFrom(), request.getTo())) { // we don't allow to send money to the same account
            return Mono.just(HttpResponse.badRequest());
        }

        var transaction = Objects.isNull(request.getFrom())
                ? new AccountCommand.AddCommand(request.getTo(), request.getAmount())
                : new AccountCommand.TransferCommand(request.getFrom(), request.getTo(), request.getAmount());

        return accountService
                .accept(transaction)
                .thenReturn(HttpResponse.accepted(
                        URI.create(UriTemplate.of("/transactions/{id}").expand(Map.of("id", transaction.getId())))
                ));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TransferRequest {
        UUID from;

        @NotNull
        UUID to;

        @Min(0)
        BigDecimal amount;
    }

    @Value
    static class TransactionResponse {
        UUID id;

        String state;

        List<AccountEvent> events;
    }
}