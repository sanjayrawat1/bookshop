### Order service will be responsible for supporting the following use case:
1. Retrieve existing book orders.
2. Submit a new order. Each order can be related to one book only and up to five copies.

### Specifications for the REST API that Order Service will expose:
| Endpoint | HTTP Method | Request Body | Status | Response Body | Description                                               |
|----------|-------------|--------------|--------|---------------|-----------------------------------------------------------|
| /orders  | GET         |              | 200    | Order[]       | Retrieves all the orders.                                 |
| /orders  | POST        | OrderRequest | 200    | Order         | Submits a new order for a given book in a given quantity. |
