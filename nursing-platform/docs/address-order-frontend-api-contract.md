# Address and Order API Contract

## Purpose

This document is the frontend-backend contract for address selection and order creation. It supersedes the numeric ID examples in the older order-service documentation for these endpoints.

The contract prevents JavaScript precision loss for Snowflake IDs. Every field whose name ends in `Id`, every resource ID in a path, and every order ID is a decimal **string** in JSON and in frontend state. Do not convert these values to `number`.

Example of a valid ID:

```json
"2077640733917065216"
```

This value is larger than `Number.MAX_SAFE_INTEGER` (`9007199254740991`). A JavaScript number rounds it and subsequently causes the order service address lookup to fail with `3006`.

## Important Compatibility Requirement

The frontend cannot recover an ID once a JSON response has delivered it as a JavaScript number and the browser has rounded it.

The current address service Java DTOs expose `Long` IDs, which Spring/Jackson serializes as JSON numbers by default. The backend must therefore serialize all Long IDs as JSON strings before this contract can work for newly generated IDs. Request-side numeric strings are accepted by the current `Long` request DTOs, but the response-side serialization change is required.

Frontend must not send `X-User-Id` or `X-Gateway-Token`. Call these public endpoints through the gateway with the normal login `Authorization` header; the gateway injects the trusted headers for the downstream service.

## Common Envelope

Successful responses use:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Business errors use the same envelope and a nonzero `code`. Validation failures return HTTP `400`; address error `3006` returns HTTP `422`.

```json
{
  "code": 3006,
  "message": "地址不存在或已被删除",
  "data": null
}
```

`3006` means no active address satisfies all three conditions: the requested ID matches, the address belongs to the logged-in user, and `is_deleted = 0`.

## Type Definitions

```ts
type Id = string;

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

interface Address {
  addressId: Id;
  receiverName: string;
  receiverPhone: string;
  tag: "家" | "公司" | "学校" | "其他";
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  isDefault: 0 | 1;
}

interface AddressInput {
  receiverName: string;
  receiverPhone: string;
  tag: "家" | "公司" | "学校" | "其他";
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  isDefault?: 0 | 1;
}
```

## Address APIs

### List active addresses

`GET /api/v1/addresses`

Returns only the logged-in user's addresses with `is_deleted = 0`, ordered with the default address first.

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "addressId": "2077640733917065216",
      "receiverName": "张三",
      "receiverPhone": "13800138006",
      "tag": "家",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detailAddress": "示例路 1 号",
      "isDefault": 1
    }
  ]
}
```

Frontend rule: use `addressId` from this response unchanged as the selected address value. Refresh this list after creating, editing, deleting, or changing the default address. Do not cache an address ID across account logout/login.

### Create an address

`POST /api/v1/addresses`

Request body: `AddressInput`.

Field validation:

| Field | Requirement |
| --- | --- |
| `receiverName` | 2-16 characters |
| `receiverPhone` | Mainland China mobile pattern `^1\d{10}$` |
| `tag` | `家`, `公司`, `学校`, or `其他` |
| `province`, `city`, `district` | Required, maximum 32 characters each |
| `detailAddress` | 5-100 characters |
| `isDefault` | Optional `0` or `1` |

Response:

```json
{
  "code": 0,
  "message": "success",
  "data": { "addressId": "2077640733917065216" }
}
```

### Update an address

`PATCH /api/v1/addresses/{addressId}`

`addressId` is a decimal string path value. Send a complete `AddressInput` body even though the endpoint uses PATCH. A missing, deleted, or cross-user address returns `3013`.

### Delete an address

`DELETE /api/v1/addresses/{addressId}`

This is a logical delete. Remove the address from frontend state after success and choose another address before allowing order submission.

### Set the default address

`PUT /api/v1/addresses/{addressId}/default`

No request body. The service clears the previous default address for the current user.

## Order APIs

### Get an order idempotency token

`POST /api/v1/orders/prepay-token`

Call immediately before creating an order. Save `data.prepayToken` as a string and use it unchanged as the `Idempotent-Key` header for the following create-order request. The token expires after 10 minutes.

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "prepayToken": "pt_1234567890abcdef1234567890abcdef",
    "expireTime": "2026-07-16T14:35:32"
  }
}
```

### Create an order

`POST /api/v1/orders`

Required request headers:

```http
Content-Type: application/json
Idempotent-Key: pt_1234567890abcdef1234567890abcdef
```

Request body:

```json
{
  "serviceItemId": "201",
  "serviceSpecId": "301",
  "addressId": "2077640733917065216",
  "serviceDate": "2026-07-20",
  "serviceTimeSlot": "MORNING",
  "remark": "请提前电话联系"
}
```

| Field | Type | Requirement |
| --- | --- | --- |
| `serviceItemId` | string | Positive decimal ID, preserved as a string in frontend state |
| `serviceSpecId` | string | Positive decimal ID, preserved as a string in frontend state |
| `addressId` | string | Must be copied unchanged from `GET /api/v1/addresses` for the current account |
| `serviceDate` | string | ISO date (`YYYY-MM-DD`), later than today |
| `serviceTimeSlot` | string | `MORNING`, `AFTERNOON`, or `EVENING` |
| `remark` | string | Optional, maximum 200 characters |

Success response:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": "2077650000000000000",
    "orderNo": "ORD-20260716-001"
  }
}
```

Error handling:

| Code | Meaning | Frontend action |
| --- | --- | --- |
| `3001` | Invalid, expired, reused with a different payload, or cross-user idempotency token | Obtain a new token and submit once. Do not reuse it for changed order data. |
| `3002` | Selected user/service/date/time-slot is already occupied | Ask the user to choose another time slot. |
| `3003` | Service item unavailable | Refresh service detail and selection. |
| `3004` | Service specification unavailable | Refresh service detail and selection. |
| `3006` | Address is missing, deleted, belongs to another user, or was precision-truncated | Reload `GET /api/v1/addresses`, clear the selected address, and require the user to select one again. |

## Required Frontend Flow

1. After login, request `GET /api/v1/addresses` and store all IDs as `string`.
2. On the order-confirmation page, require a selected address from that latest list.
3. Immediately before submission, request `POST /api/v1/orders/prepay-token`.
4. Send `POST /api/v1/orders` with the token as `Idempotent-Key` and the unmodified string `addressId`.
5. Disable duplicate submit while the request is in flight. For a network timeout, retry only with the same body and same idempotency token.
6. On `3006`, do not retry with the old ID. Reload addresses and require a fresh selection.

## Frontend Guardrails

```ts
const isDecimalId = (value: unknown): value is string =>
  typeof value === "string" && /^[1-9]\d*$/.test(value);

function selectAddress(address: Address) {
  if (!isDecimalId(address.addressId)) {
    throw new Error("Invalid address ID returned by API");
  }
  selectedAddressId.value = address.addressId;
}
```

In browser DevTools, verify that both the address-list response and create-order request show quoted IDs, for example `"addressId":"2077640733917065216"`. An unquoted value indicates that the backend response contract has not yet been updated and the frontend must not use it as a JavaScript number.
