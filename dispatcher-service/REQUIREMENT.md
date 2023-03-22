In the Bookshop system, we need to implement an event-driven solution to allow different applications to communicate with each other asynchronously while
reducing their coupling. These are the requirements:

* When an order is accepted:
  - Order Service should notify interested consumers of the event.
  - Dispatcher Service should execute some logic to dispatch the order.
* When an order is dispatched:
  - Dispatcher Service should notify consumers interested in such an event.
  - Order Service should update the order status in the database.
