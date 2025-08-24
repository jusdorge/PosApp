## Fragments

### `CounterFragment`
- Purpose: Build and manage the current invoice; checkout flow.
- Public API:
  - `static void addToInvoice(InvoiceItem item)` — add or increment item in current invoice.
  - `static void setCustomer(Customer customer)` — set current customer for invoice UI.
- Listeners implemented:
  - `InvoiceAdapter.OnInvoiceItemDeleteListener`
  - `EditInvoiceItemDialog.OnItemUpdatedListener`

Example: Add an item from `ItemsFragment`
```java
CounterFragment.addToInvoice(new InvoiceItem(product.getId(), product.getName(), product.getDefaultPrice(), 1));
((MainActivity) requireActivity()).switchToCounterFragment();
```

### `ItemsFragment`
- Purpose: Browse products and add to invoice.
- Public callbacks:
  - Implements `InvoiceItemAdapter.OnProductAddListener` → calls `CounterFragment.addToInvoice(...)`.

### `TodayFragment`
- Purpose: Show today's invoices and totals.
- Listener:
  - Implements `InvoiceListAdapter.OnInvoiceClickListener` to react on invoice taps.

### `ReportsFragment`
- Purpose: Show inventory and sales summaries (placeholder functions for now).

### `MoreFragment`
- Purpose: Entry to management screens.
- Navigation helpers to `CustomersFragment` and `ProductsManagementFragment`.

### `ProductsManagementFragment`
- Purpose: List, search, and edit products.
- Launches `AddProductActivity` and `EditProductActivity`.

### `CustomersFragment`
- Purpose: List, search, add, and select customers.
- Public behavior:
  - `showAddCustomerDialog()` displays `AddCustomerDialog` and refreshes on add.
  - On customer click, opens `CustomerDetailsDialog`.