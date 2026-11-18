# v28.2 — Member photos directly in the member list

This increment builds on v28.1 at `be98994413215981bd9951ebc84edd83c3ea13ff`.

## Member workflow

1. Open **Members** and find a member.
2. Tap **Add photo** below the member's details, then choose an image using Android's image document picker.
3. The saved photograph appears beside the member's name. The same card now shows **Photo options**.
4. **Photo options** displays a larger preview with **Change photo**, **Remove photo** and **Done**. Removal has a confirmation.

The original member-details row still opens the member's history. Members without a saved photograph retain their initials. The photo action has a minimum 48 dp touch target and identifies the member in its accessibility description.

## Reliability

- Selection retains the immutable member ID; import checks that this original ID still exists.
- Pending selection and the member search, status filter and reporting month survive Android activity recreation while the image picker is open.
- Cancellation makes no change. Existing bounded, atomic photo replacement is reused.
- Concurrent photo actions are blocked until selection or saving finishes.
- A completed background save does not pull the user away from a screen they have opened in the meantime.
- Member photos remain local to this phone. Financial JSON and encrypted ledger backups do not contain photos.

## Scope

The code changes are confined to the photo UI overlay, its integration gate and the v28 development validation workflows. The Room schema, original member IDs, amounts, receipts, ledger import/export, package ID and permanent signing configuration are unchanged.

## Validation

The v28 debug workflow rebuilds the complete application and runs existing accounting, recovery, navigation and source gates. The owner-signed v28 upgrade workflow retains the existing synthetic v25.9.1 upgrade checks and direct Android photo-storage checks, then runs `V28MemberListPhotoUiTest#memberListPhotoActionsPreserveIdentityAndLedger` against the actual member list and picker callback.

The UI test uses synthetic images and records only. It covers the direct list action, scoped image intent, cancellation, activity recreation, successful import, replacement/removal, isolation between member IDs and equality of the financial ledger before and after photo operations. The Android picker intent is intercepted; a real gallery provider and the owner's physical phone remain separate device checks.

Build outputs remain signed test candidates on the existing development branch. Production `main` is unchanged.
