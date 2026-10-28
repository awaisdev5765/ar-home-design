# Firebase Schema v1

## Collections Overview

- `users`
- `projects`
- `rooms`
- `designVersions`
- `materialCatalog`
- `estimates`
- `shareLinks`

## users/{userId}

- `displayName`: string
- `email`: string
- `phone`: string?
- `countryCode`: string (for UAE/US targeting)
- `createdAt`: timestamp
- `updatedAt`: timestamp

## projects/{projectId}

- `ownerId`: string
- `title`: string
- `description`: string?
- `propertyType`: string (`apartment`, `villa`, `office`, etc.)
- `status`: string (`draft`, `active`, `archived`)
- `thumbnailUrl`: string?
- `createdAt`: timestamp
- `updatedAt`: timestamp

Suggested subcollections:

- `projects/{projectId}/members/{userId}`
- `projects/{projectId}/activity/{activityId}`

## rooms/{roomId}

- `projectId`: string
- `ownerId`: string
- `name`: string
- `scanDevice`: string
- `scanMetadata`: map
  - `widthM`: number?
  - `lengthM`: number?
  - `heightM`: number?
  - `planeCount`: number
- `anchorDataUrl`: string? (Storage path reference)
- `createdAt`: timestamp
- `updatedAt`: timestamp

## designVersions/{versionId}

- `projectId`: string
- `roomId`: string
- `ownerId`: string
- `versionNumber`: number
- `wallStyle`: map
- `floorStyle`: map
- `furnitureItems`: array
- `snapshotUrl`: string?
- `notes`: string?
- `createdAt`: timestamp

## materialCatalog/{materialId}

- `name`: string
- `category`: string (`paint`, `tile`, `wood`, `carpet`, `furniture`)
- `unit`: string (`sqm`, `pcs`, etc.)
- `priceUae`: number
- `priceUs`: number
- `currencyUae`: string (`AED`)
- `currencyUs`: string (`USD`)
- `imageUrl`: string?
- `active`: boolean
- `updatedAt`: timestamp

## estimates/{estimateId}

- `projectId`: string
- `roomId`: string
- `versionId`: string
- `ownerId`: string
- `region`: string (`UAE`, `US`)
- `lineItems`: array
- `subtotal`: number
- `tax`: number
- `total`: number
- `currency`: string
- `generatedBy`: string (`system`)
- `createdAt`: timestamp

## shareLinks/{shareId}

- `projectId`: string
- `versionId`: string
- `ownerId`: string
- `tokenHash`: string
- `expiresAt`: timestamp
- `createdAt`: timestamp

## Firestore Indexes (Initial)

1. `projects`: `ownerId ASC, updatedAt DESC`
2. `rooms`: `projectId ASC, updatedAt DESC`
3. `designVersions`: `roomId ASC, versionNumber DESC`
4. `estimates`: `projectId ASC, createdAt DESC`

## Security Rules Principles

- Users can read/write only their own documents unless shared as project member.
- Catalog can be read by authenticated users; write only admin role.
- Estimate and share link creation only through Cloud Functions.

Pseudo-rule guidance:

- `request.auth != null`
- `resource.data.ownerId == request.auth.uid`
- immutable fields check for `ownerId` after creation

## Storage Paths

- `users/{uid}/projects/{projectId}/rooms/{roomId}/anchors/...`
- `users/{uid}/projects/{projectId}/versions/{versionId}/snapshots/...`
- `catalog/materials/...`
