### Catalog Service will be responsible for supporting the following use case:
1. View the list of books in the catalog.
2. Search books by their International Standard Book Number (ISBN).
3. Add a new book to the catalog.
4. Edit information for an existing book.
5. Remove a book from the catalog.

### Specification for the REST API that will be exposed by Catalog Service

| Endpoint      | HTTP Method | Request Body | Status | Response Body | Description                               |
|---------------|-------------|--------------|--------|---------------|-------------------------------------------|
| /books        | GET         |              | 200    | Book[]        | Get all the books in the catalog.         |
| /books        | POST        | Book         | 201    | Book          | Add a new book to the catalog.            |
|               |             |              | 422    |               | A book with the same ISBN already exists. |
| /books/{isbn} | GET         |              | 200    | Book          | Get the book with the given ISBN.         |
|               |             |              | 404    |               | No book with the given ISBN exists.       |
| /books/{isbn} | PUT         | Book         | 200    | Book          | Update the book with the given ISBN.      |
|               |             |              | 201    | Book          | Create a book with the given ISBN.        |
| /books/{isbn} | DELETE      |              | 204    |               | Delete the book with the given ISBN.      |
