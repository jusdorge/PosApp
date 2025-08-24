## Models

### `Invoice`
Fields:
- `id: String`
- `customerName: String`
- `customerPhone: String`
- `isPaid: boolean`
- `totalAmount: double`
- `date: Timestamp`
- `items: List<InvoiceItem>`
- `additionalInfo: Map<String,Object>`

Getters/Setters available for all fields.

### `InvoiceItem`
Fields:
- `productId: String`
- `productName: String`
- `price: double`
- `quantity: int`

Methods:
- `double getTotal()` returns `price * quantity`.

### `Product`
Fields:
- `id, name, category, defaultPrice, costPrice, quantity, minQuantity, barcode, imageUrl`

Methods:
- `double getUnitProfit()` returns profit per unit.
- `boolean isLowStock()` returns true if `quantity <= minQuantity`.

### `Customer`
Fields:
- `id, name, phone, totalDebt, debts: List<CustomerDebt>, latitude, longitude`

Methods:
- `addDebt(String invoiceId, double amount, Timestamp date)` appends a new debt and updates `totalDebt`.

### `CustomerDebt`
Fields:
- `id, amount, description, date, isPayment`