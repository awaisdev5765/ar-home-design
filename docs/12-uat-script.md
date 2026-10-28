# UAT Script - AR Home Design MVP

## Scope

Validate end-to-end core flow before production submission.

## Preconditions

- Firebase `dev` and `prod` are configured.
- Cloud Functions deployed.
- Android and iOS builds signed and installable.

## Test Cases

### A. Authentication and Project Management

1. Register new user account.
2. Login and create project.
3. Logout/login and verify same project appears.

Expected:

- Auth and project persistence successful.

### B. AR Scan and Room Persistence

1. Start AR session.
2. Capture room scan with detected planes.
3. Save scan and reload.

Expected:

- `rooms` record persists and reloads correctly.

### C. Overlay and Material Workflow

1. Select active room.
2. Add overlays for wall/floor/ceiling.
3. Assign material preset and color.
4. Toggle preview, batch apply material.
5. Move/resize/rotate overlay.
6. Undo and redo overlay edits.

Expected:

- Overlay state and edits are saved and recoverable.

### D. Design Version and Compare

1. Save version with notes.
2. Modify overlays and save second version.
3. Run compare latest versions.

Expected:

- Version list and compare summary are correct.

### E. Estimation and Sharing

1. Generate estimate.
2. Build export summary.
3. Create share link.
4. Build share package payload.

Expected:

- Estimate totals generated.
- Share URL returned with expiry.
- Share package returns latest version/estimate/overlays.

## Sign-off Template

- Android QA: PASS / FAIL
- iOS QA: PASS / FAIL
- Backend QA: PASS / FAIL
- Product Sign-off: PASS / FAIL
- Release Decision: GO / NO-GO
