## Activities

### `LoginActivity`
- Public responsibilities:
  - Google Sign-In via `signInWithGoogle()` flow.
  - Email/password login via `loginUser()`.
- On success: calls `startMainActivity()` to open `MainActivity`.

Example: Start login
```java
startActivity(new Intent(context, LoginActivity.class));
```

### `MainActivity`
- Hosts core navigation and drawer.
- Public API:
  - `switchToCounterFragment()` — selects the Counter tab programmatically.
- Navigation helpers (internal):
  - `navigateToCustomersManagement()` — shows `CustomersFragment`.
  - `navigateToProductsManagement()` — shows `ProductsManagementFragment`.

Example: Switch to Counter from a Fragment
```java
((MainActivity) requireActivity()).switchToCounterFragment();
```

### `AddProductActivity`
- Create and persist `Product` to Firestore.
- Key flows:
  - `openImageChooser()` to pick an image.
  - `addProduct()` validates and writes to `products` collection.

Example: Launch from a Fragment
```java
startActivity(new Intent(getContext(), AddProductActivity.class));
```

### `EditProductActivity`
- Load, edit, and delete a `Product`.
- Key flows:
  - `loadProductData()` reads a product by `product_id` extra.
  - `saveProduct()` writes updates.
  - `deleteProduct()` removes the document.

Example: Launch with product id
```java
Intent i = new Intent(getContext(), EditProductActivity.class);
i.putExtra("product_id", product.getId());
startActivity(i);
```

### `SelectCustomerLocationActivity`
- Select or view a customer location on an OSM map.
- Intents:
  - Extras: `latitude`, `longitude`, `view_only` (boolean).
  - Result: `latitude`, `longitude`.

Example: Start for result
```java
Intent i = new Intent(getActivity(), SelectCustomerLocationActivity.class);
startActivityForResult(i, REQUEST_CODE);
```

### `ProfitDetailsActivity`
- Displays and changes the current date to navigate reports.