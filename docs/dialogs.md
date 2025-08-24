## Dialogs

### `AddCustomerDialog`
- Purpose: Create a new customer and persist to Firestore.
- Listener:
  - `interface OnCustomerAddedListener { void onCustomerAdded(Customer customer); }`
- Usage:
```java
AddCustomerDialog dialog = new AddCustomerDialog();
dialog.setOnCustomerAddedListener(customer -> reload());
dialog.show(getChildFragmentManager(), "AddCustomerDialog");
```
- Result: Invokes listener with created `Customer`.

### `CustomerDetailsDialog`
- Purpose: Show customer info, debts, invoices; choose customer for invoice.
- Factory:
  - `static CustomerDetailsDialog newInstance(String customerId)`
- Related APIs:
  - Calls `CounterFragment.setCustomer(customer)` and `CheckoutDialog.setCustomer(customer)`.
- Usage:
```java
CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(customerId);
dialog.show(getChildFragmentManager(), "CustomerDetailsDialog");
```

### `CheckoutDialog`
- Purpose: Confirm and persist an invoice; optional debt flow.
- Factory:
  - `static CheckoutDialog newInstance(List<InvoiceItem> items, double totalAmount)`
- Listener:
  - `interface OnInvoiceCompletedListener { void onInvoiceCompleted(); }`
- Static helper:
  - `setCustomer(Customer c)` pre-fills name/phone.
- Usage:
```java
CheckoutDialog dialog = CheckoutDialog.newInstance(items, total);
dialog.setOnInvoiceCompletedListener(() -> clearCart());
dialog.show(getChildFragmentManager(), "CheckoutDialog");
```

### `EditInvoiceItemDialog`
- Purpose: Edit price and quantity of an invoice item.
- Factory:
  - `static EditInvoiceItemDialog newInstance(InvoiceItem item, int position)`
- Listener:
  - `interface OnItemUpdatedListener { void onItemUpdated(InvoiceItem item, int position); }`
- Usage:
```java
EditInvoiceItemDialog dialog = EditInvoiceItemDialog.newInstance(item, pos);
dialog.setOnItemUpdatedListener((updated, p) -> apply(updated, p));
dialog.show(getChildFragmentManager(), "EditInvoiceItemDialog");
```

### `AddPaymentDialog`
- Purpose: Add a payment to reduce a customer's total debt.
- Factory:
  - `static AddPaymentDialog newInstance(Customer customer)`
- Listener:
  - `interface OnPaymentAddedListener { void onPaymentAdded(); }`
- Usage:
```java
AddPaymentDialog dialog = AddPaymentDialog.newInstance(customer);
dialog.setOnPaymentAddedListener(this::reloadCustomer);
dialog.show(getChildFragmentManager(), "AddPaymentDialog");
```