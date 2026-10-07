# Distributed cache POC

Migrate in-process HashMaps to Redis/Valkey so every Kubernetes pod sees the same account data.

## Demo

All five cases from [`SCENARIOS.md`](SCENARIOS.md) recorded against the live cluster:

<video src="docs/cache-poc-demo.mp4" controls width="100%"></video>

If the player does not show, open [`docs/cache-poc-demo.mp4`](docs/cache-poc-demo.mp4).

| Case | What you should see |
| --- | --- |
| 1 Create + cache hit | POST then GET; Redis key `accounts::{id}` |
| 2 PUT write-through | GET returns `400.00` immediately |
| 3 DB update → cache | PATCH `775.50`, then SQL `999.00` via NOTIFY |
| 4 Admin flush | Keys cleared; GET refills from Postgres |
| 5 DELETE evicts | GET `404`; Redis key gone |

## Run

```bash
kubectl port-forward svc/cache-poc 8080:80
export BASE=http://localhost:8080
```

| What | URL |
| --- | --- |
| Swagger | http://localhost:8080/swagger-ui.html |
| Cache admin | http://localhost:8080/admin |
| Health | http://localhost:8080/actuator/health |

Step-by-step curl: [`SCENARIOS.md`](SCENARIOS.md)

```bash
helm install cache-poc ./helm/cache-poc
```
