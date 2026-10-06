# Run and test

## Start

### Docker Compose

```bash
docker compose up --build
export BASE=http://localhost:8080
```

### Helm (what you are running now)

The chart Service is **ClusterIP**, so `localhost:8080` is closed until you forward it:

```bash
kubectl port-forward svc/cache-poc 8080:80
export BASE=http://localhost:8080
```

Leave that terminal open. Use a second terminal for curl/Swagger.

Wait until the app is healthy, then:


| What         | URL                                                                            |
| ------------ | ------------------------------------------------------------------------------ |
| Swagger UI   | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)         |
| Cache admin  | [http://localhost:8080/admin](http://localhost:8080/admin)                     |
| Health       | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |


Unit tests (no Docker): `mvn test`

Use **Swagger** to click through the same calls as the curl below. Sample org name is `FinancialCorp`.

Set `ID` after create:

```bash
export BASE=http://localhost:8080
```

---

## Scenario 1 — Create, then cache hit

First `GET` loads Postgres then Redis. Second `GET` should be served from Redis (`accounts::{id}`).

```bash
curl -s -X POST $BASE/api/accounts -H 'Content-Type: application/json' \
  -d '{"accountNumber":"ACC-1001","ownerName":"Alex Rivera","organizationName":"FinancialCorp","balance":250.00}'
# copy id from the response
export ID=1

curl -s $BASE/api/accounts/$ID

docker compose exec redis redis-cli KEYS 'accounts::*'
# Helm: kubectl exec deploy/cache-poc-redis -- redis-cli KEYS 'accounts::*'
```

---



## Scenario 2 — App write-through (PUT)

Update via the API. Redis must show the new balance immediately (all pods share Redis).

```bash
curl -s -X PUT $BASE/api/accounts/$ID -H 'Content-Type: application/json' \
  -d '{"accountNumber":"ACC-1001","ownerName":"Alex Rivera","organizationName":"FinancialCorp","balance":400.00}'

curl -s $BASE/api/accounts/$ID
```

---



## Scenario 3 — DB update refreshes cache

SQL-style balance change. Next `GET` must return the new amount, not a stale cache.

```bash
curl -s -X PATCH $BASE/api/accounts/$ID/balance -H 'Content-Type: application/json' \
  -d '{"balance":775.50}'

curl -s $BASE/api/accounts/$ID
```

Out-of-band Postgres (NOTIFY → Redis):

```bash
docker compose exec postgres psql -U app -d appdb \
  -c "UPDATE accounts SET balance = 999.00 WHERE id = $ID;"
# Helm:
# kubectl exec deploy/cache-poc-postgres -- psql -U app -d appdb \
#   -c "UPDATE accounts SET balance = 999.00 WHERE id = $ID;"

sleep 1
curl -s $BASE/api/accounts/$ID
```

---



## Scenario 4 — Cache admin flush / drop

UI: [http://localhost:8080/admin](http://localhost:8080/admin)  

Or Swagger tag **Cache admin**:

```bash
curl -s $BASE/admin/api/caches
curl -s $BASE/admin/api/caches
curl -s $BASE/api/accounts/$ID
# miss → DB → Redis is filled again

curl -s -X POST $BASE/admin/api/caches/flush-all -i
curl -s -X POST $BASE/admin/api/caches/flush-redis -i
```

---



## Scenario 5 — Delete evicts cache

```bash
curl -s -X DELETE $BASE/api/accounts/$ID -i
curl -s $BASE/api/accounts/$ID
docker compose exec redis redis-cli KEYS 'accounts::*'
# Helm: kubectl exec deploy/cache-poc-redis -- redis-cli KEYS 'accounts::*'
```

---



## Local JVM (Postgres + Redis already up)

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

Helm (cluster already running):

```bash
helm install cache-poc ./helm/cache-poc
kubectl port-forward svc/cache-poc 8080:80
```