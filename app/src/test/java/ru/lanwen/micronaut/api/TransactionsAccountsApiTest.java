package ru.lanwen.micronaut.api;

import io.micronaut.http.HttpHeaders;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@MicronautTest
class TransactionsAccountsApiTest {

    @Inject
    EmbeddedServer embeddedServer;

    UUID first = UUID.randomUUID();

    @Test
    void shouldAdd() throws Exception {
        String location = request()
                .body(new TransactionsApi.TransferRequest(null, first, BigDecimal.valueOf(100)))
                .post("/transactions")
                .then()
                .statusCode(202)
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("finished"))
        );

        request().get("/accounts/{id}", first)
                .then()
                .statusCode(200)
                .body("balance", is(100));
    }


    @Test
    void shouldRequestStateWithSameResult() throws Exception {
        String location = request()
                .body(new TransactionsApi.TransferRequest(null, first, BigDecimal.valueOf(100)))
                .post("/transactions")
                .then()
                .statusCode(202)
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("finished"))
        );

        IntStream.range(0, 10).forEach(i -> {
            request().get("/accounts/{id}", first)
                    .then()
                    .statusCode(200)
                    .body("events", hasSize(1))
                    .body("balance", is(100));
        });
    }

    @Test
    void shouldTransfer() throws Exception {
        UUID second = UUID.randomUUID();
        request()
                .body(new TransactionsApi.TransferRequest(null, first, BigDecimal.valueOf(100)))
                .post("/transactions");

        String location = request()
                .body(new TransactionsApi.TransferRequest(first, second, BigDecimal.valueOf(99.9)))
                .post("/transactions")
                .then()
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("finished"))
        );

        request()
                .get("/accounts/{id}", first)
                .then()
                .body("balance", is(0.1f))
                .body("events", hasSize(2));

        request()
                .get("/accounts/{id}", second)
                .then()
                .body("balance", is(99.9f));
    }


    @Test
    void shouldTransferBack() throws Exception {
        UUID second = UUID.randomUUID();
        request()
                .body(new TransactionsApi.TransferRequest(null, first, BigDecimal.valueOf(100)))
                .post("/transactions");

        request()
                .body(new TransactionsApi.TransferRequest(first, second, BigDecimal.valueOf(99.9)))
                .post("/transactions")
                .then()
                .extract().header(HttpHeaders.LOCATION);


        String location = request()
                .body(new TransactionsApi.TransferRequest(second, first, BigDecimal.valueOf(99.9)))
                .post("/transactions")
                .then()
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("finished"))
        );

        request()
                .get("/accounts/{id}", first)
                .then()
                .body("balance.toString()", is("100.0")); // hack - still didn't get exactly the logic for the decimals
    }

    @Test
    void shouldNotTransferMoreThanHave() throws Exception {
        UUID second = UUID.randomUUID();

        request()
                .body(new TransactionsApi.TransferRequest(null, first, BigDecimal.valueOf(100)))
                .post("/transactions");

        String location = request()
                .body(new TransactionsApi.TransferRequest(first, second, BigDecimal.valueOf(200)))
                .post("/transactions")
                .then()
                .statusCode(202)
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("rejected"))
        );

        request()
                .get("/accounts/{id}", first)
                .then()
                .body("balance.toString()", is("100"));
    }

    @Test
    void shouldNotTransferIfNoAnyMoney() throws Exception {
        UUID second = UUID.randomUUID();

        String location = request()
                .body(new TransactionsApi.TransferRequest(first, second, BigDecimal.valueOf(200)))
                .post("/transactions")
                .then()
                .statusCode(202)
                .extract().header(HttpHeaders.LOCATION);

        await().untilAsserted(() -> request().get(location)
                .then()
                .statusCode(200)
                .body("state", is("rejected"))
        );

        request()
                .get("/accounts/{id}", first)
                .then()
                .body("balance", is(0));
    }

    @Test
    void shouldNotTransferToSameAcc() throws Exception {
        request()
                .body(new TransactionsApi.TransferRequest(first, first, BigDecimal.valueOf(200)))
                .post("/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldAlwaysKnowReceiver() throws Exception {
        request()
                .body(new TransactionsApi.TransferRequest(first, null, BigDecimal.valueOf(200)))
                .post("/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldNotTransferNegative() throws Exception {
        request()
                .body(new TransactionsApi.TransferRequest(first, null, BigDecimal.valueOf(-100)))
                .post("/transactions")
                .then()
                .statusCode(400);
    }

    private RequestSpecification request() {
        return given()
                .contentType(ContentType.JSON)
                .baseUri(embeddedServer.getURL().toString());
    }
}
