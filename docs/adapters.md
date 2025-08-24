## Adapters

### `InvoiceAdapter`
- Constructor: `InvoiceAdapter(List<InvoiceItem> items, OnInvoiceItemDeleteListener deleteListener)`
- Listeners:
  - `OnInvoiceItemDeleteListener { void onInvoiceItemDelete(int position); }`
  - `OnInvoiceItemClickListener { void onInvoiceItemClick(InvoiceItem item, int position); }`
- Methods:
  - `setOnInvoiceItemClickListener(listener)`

Usage:
```java
InvoiceAdapter adapter = new InvoiceAdapter(items, position -> remove(position));
adapter.setOnInvoiceItemClickListener((item, pos) -> edit(item, pos));
recyclerView.setAdapter(adapter);
```

### `InvoiceListAdapter`
- Constructor: `InvoiceListAdapter(List<Invoice> invoices)`
- Listener: `OnInvoiceClickListener { void onInvoiceClick(Invoice invoice, int position); }`
- Methods:
  - `setOnInvoiceClickListener(listener)`
  - `updateInvoices(List<Invoice> newInvoices)`

### `InvoiceItemAdapter`
- Constructor: `InvoiceItemAdapter(List<Product> products, OnProductAddListener listener)`
- Listener: `OnProductAddListener { void onProductAdd(Product product, int quantity); }`
- Methods:
  - `updateProducts(List<Product> newProducts)`

### `ProductManagementAdapter`
- Constructor: `ProductManagementAdapter(List<Product> productList)`
- Listener: `OnProductEditListener { void onProductEdit(Product product); }`
- Methods:
  - `setOnProductEditListener(listener)`
  - `updateProducts(List<Product> newProducts)`
  - `filterProducts(String query)`

### `CustomerAdapter`
- Constructor: `CustomerAdapter(List<Customer> customerList)`
- Listener: `OnCustomerClickListener { void onCustomerClick(Customer customer, int position); }`
- Methods:
  - `setOnCustomerClickListener(listener)`
  - `updateCustomers(List<Customer> newCustomers)`
  - `filterCustomers(String query)`