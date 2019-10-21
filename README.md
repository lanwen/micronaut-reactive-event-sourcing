# Micronaut and Event Sourcing

>Simple web app to transfer money from one account to another.

Actually, micronaut here as the alternative to Spring with the same reactive stack
to play around on something that looks like real to some extent.

### How to play around 

To build having java 11 `./gradlew build`

If you don't have java, but a lucky owner of a docker environment:

`docker build -t lmres .`

then 

`docker run -it --rm -p8080:8080 lmres`

#### Add money

```bash
curl -v 'http://localhost:8080/transactions/' -H "Content-Type: application/json" -XPOST -d '{
    "to": "279383f2-378a-4ef8-893d-a17e602cfe88",
    "amount": 100
}'
```

### See what's up
```bash
curl -v 'http://localhost:8080/accounts/279383f2-378a-4ef8-893d-a17e602cfe88'
```