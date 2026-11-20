# v28.3 — Premium native UI

Development branch: dev/v28-3-premium-ui-20261119

Based on v28.2 source 9fd4ec9b5218d41b62927ba59665e8aaca0fc454.

## User experience

- Warm ivory background, deep navy balance card, cobalt actions and restrained gold branding.
- Native line icons, readable secondary text, consistent form fields and 48 dp minimum action targets.
- Exactly one selected bottom navigation destination, with accessible system-bar contrast.
- Everyday Home actions above the longer backup information.
- Add player before the roster, cleaner member cards, and the existing direct photo actions.
- Clear primary/secondary actions for collections and shuttle activity.
- Collection cards put the amount and Receipt button on their own row so a long member name cannot hide them.
- Advanced stock setup and physical counting in an expandable section.
- Searchable Reports catalogue with All, Reports, Club and Backups categories.
- All 24 original report handlers preserved, including conditional external-folder disconnect.

## Functional repairs

1. Receipt Library selects its Collect navigation context and owns a new screen epoch. Back routes through normal navigation, so monthly receipts load when the library was opened from Reports.
2. Canceling duplicate-payment review with Android Back or outside dismissal releases its review flag. Save can be tried again without reopening the form.
3. Initials use Unicode code points and a safe fallback, including the accepted name Dr.
4. Receipt Library indicates its active filter and accepts the current display name as well as the immutable recorded name. Home and Collect also use the current profile display name.
5. The Backups category includes Back up now, and an empty member list finishes loading with an accurate no-ledger state.
6. Long member names no longer crowd out the amount and sharing control on monthly collection cards. The UI check verifies their measured, visible bounds.

PDF callbacks continue to receive the original recorded Member object.

## Verification

The new check_v283_integrated.py gate checks UI integration, navigation/cancel repairs, package/version identity and exact SHA-256 preservation of six accounting, schema and Classic PDF modules.

V283WorkflowUiTest.premiumScreensAndActionsRemainUsable exercises the actual navigation/report controls, report and receipt search, category filters, duplicate-review cancellation/retry, stock forms, member/profile entry, fee/concession entry, month-end review, dashboard, cash-date audit, backup verification and encrypted-backup entry. It requires an explicitly synthetic club and checks canonical ledger equality afterward.

The existing signed upgrade, complete-field ledger, encrypted-backup, photo and navigation suites remain in the workflow. Emulator screenshots are retained for all five main tabs.

All 38 source reconstruction and integration steps pass locally. The latest accepted CI passed the existing Java regressions, complete financial/AES suite, both Android builds, all ten instrumentation invocations, and populated navigation checks. All five normal screens and the enlarged-text evidence were visually reviewed; receipt amounts and actions passed measured visibility assertions.

## Scope

This is an owner-signed preview APK, retaining the existing package and Room schema.
Device gallery providers, recipient apps, physical printers and installation on the owner's phone require actual-device acceptance. Member photos remain local to the phone and are not included in financial JSON or encrypted ledger backups.

## Follow-on usability and photograph reliability

- Month arrows are exposed as actionable accessibility controls; decorative icons remain excluded.
- Selected tab, reporting month, member search/filter and report search/category survive Activity recreation.
- Member status is below the name. At narrow widths or enlarged text, full member names and stacked receipt amount/actions remain readable.
- Compact headers give the club name its own row. Enlarged navigation uses two rows of labelled buttons so words remain whole at 320 dp / 200% text.
- Profile, fee policy, concession, stock threshold and encrypted-export form bodies scroll while their native dialog buttons remain available.
- Photo mutation work is serialized across Activity recreation. Completion refreshes the live member list, and invalid-image errors cannot attach a window to the destroyed Activity.
- All eight EXIF orientations are normalized before saving; malformed replacements preserve the existing photo.

Focused acceptance adds real accessibility-node actions, recreation and filter restoration, 320 dp / 2.0 font layout, keyboard/form reachability, queued photo operations across recreation and coloured-quadrant EXIF fixtures. The source reconstruction checks continue to pin the six accounting/schema/Classic PDF modules above. The follow-on revision passed the signed runtime audit and final visual review below.

## Earlier accepted build — 19 November 2026 (rc-test.10)

- Application source and tests: b2481c29faa876250420692c99aa7a62cad730b6.
- Signed preview: 28.3-rc-test.10, versionCode 30010.
- Package: com.vinayak.badminton.premiumv24, with the original permanent signing certificate.
- Certificate SHA-256: 0e1c35c2da5a0582cc0e3be45319b027a38fbfe9d04ad78df89e10126a40ca8e.
- [Signed upgrade and Android workflow acceptance: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44546654031).
- [Debug build and regression checks: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44546654036).
- Signed APK artifact: 14540034088. Runtime evidence artifact: 14541448021.
- APK size: 2,186,716 bytes.
- APK SHA-256: 1de61b9572f06415c79c404995b26ea4ec0b88fb6b9d7d602708b4eabe2626df.
- Evidence ZIP SHA-256: 6e4f8d7dbaa5aa9d51fe54536e5bc466b6e351860e80aa50f56f266953dff71b.

The signed v25.9.1.21 / code 24021 to v28.3-rc-test.10 / code 30010 in-place update preserved the complete synthetic ledger. All nine named instrumentation invocations passed, and the captured logcat contains no FATAL EXCEPTION.

The accepted runtime log includes the original premium workflow, receipt-footer, duplicate Back/retry and report-filter markers, plus V283_ACCESSIBILITY_AND_SCREEN_STATE_PASS, V283_PHOTO_LIFECYCLE_PASS, V28_PHOTO_ALL_EXIF_ORIENTATIONS_PASS, V28_PHOTO_INVALID_REPLACEMENT_PRESERVED_PASS, V283_NARROW_PROFILE_KEYBOARD_REACHABILITY_PASS and V283_NARROW_320DP_LARGE_TEXT_200_PERCENT_PASS.

Compact-layout acceptance verifies a single-line club title and complete navigation labels at 320 dp / 200% text. Real Android screenshots wait for a hardware frame commit and record target bounds/scroll positions. The member name, photo action, receipt amount/action and profile Active field above the keyboard were inspected visually; transient test Toasts are drained before capture.

The inventory confirms 23 always-visible report entry points, with external-folder disconnection remaining conditional. A single explicitly identified Pixel Launcher ANR can be closed in the synthetic photo test; Badminton and other error dialogs are never dismissed by that recovery. Every financial, photo, identity and failure assertion remains enforced.

## Retained forms and reachable confirmation actions

The six input editors now retain their original views and entered values through validation errors and canceled reviews: Add player, member profile, fee policy, noncash concession, stock threshold, and encrypted backup export.

- Review Cancel and Android Back reveal the unchanged editor. Transaction rejection re-enables the same draft for correction.
- Submitting a save locks the editor until its transaction finishes. Reviewed metadata event IDs remain fixed for that submission, and repeated callbacks cannot create a second record.
- A committed transaction closes its editor independently of the following backup. Backup problems are reported as backup problems and cannot make the accepted change retryable.
- New-player text is captured on the UI thread. Duplicate-name and existing accounting validations remain in their original transaction.
- Password mismatch keeps both fields and the owned export session available for correction. Explicit cancellation clears the fields and releases the gate; accepted export retains ownership through the document picker and cleans up canceled staging.
- A local resize policy and bounded scroll body keep the native editor actions visible above the software keyboard.
- Profile, fee-policy and concession confirmations use the concise action label Save. Their review titles and messages identify the audited operation. This avoids the native stacked-button layout that clipped Cancel with the earlier long label at 200% text.

The keyboard acceptance test measures the real Android input-method window in screen coordinates, checks complete button and text bounds, waits for stable geometry, and injects touchscreen events. Dialog-relative IME insets remain diagnostic: a dialog wholly above the keyboard can have a zero overlap inset. Failure captures occur before activity teardown.

The added form-workflow case requires the exact synthetic club, known member identities and original receipt. It checks six forms, asynchronous rejections, review Cancel/Back, password correction and picker cancellation, plus corrected profile/threshold retries under a deterministic worker hold. It commits exactly two intended synthetic metadata events, preserves every original financial/roster row and original audit event, and runs last after the display is restored.

## Latest accepted build — 19 November 2026 (rc-test.13)

- Application source and tests: 0ae201bae6507cc847b1d67c0ecadb2041f5c0f6.
- Signed preview: 28.3-rc-test.13, versionCode 30013.
- Package: com.vinayak.badminton.premiumv24, with the original permanent signing certificate.
- Certificate SHA-256: 0e1c35c2da5a0582cc0e3be45319b027a38fbfe9d04ad78df89e10126a40ca8e.
- [Signed upgrade and Android workflow acceptance: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44559417023).
- [Debug build and regression checks: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44559417387).
- Signed APK artifact: 14547185941. Runtime evidence artifact: 14548382175.
- APK size: 2,190,552 bytes.
- APK SHA-256: db8b9a74976d14de64e8690653b2e3ab9bf9c9b03e490edab371a65995b6f0be.
- APK artifact ZIP SHA-256: a104aa760f322cee1278a0df7e0b83ff01938847f48a7b3fd848bc0c1778fb6f.
- Runtime evidence ZIP SHA-256: b7ad2b10105b854f684a8c1c722ae03ecce41a23e0a979300e469b5b197c9b50.

All ten named instrumentation invocations passed. The v25.9.1.21/code 24021 to v28.3-rc-test.13/code 30013 in-place upgrade preserved the complete synthetic ledger before the final test's two authorized synthetic metadata changes. Captured logcat contains no FATAL EXCEPTION.

At 320 dp / 200% text, the profile editor's Review and Cancel controls occupy y=548–658 px, fully above the real keyboard top at y=714 px. Real coordinate taps opened Review, canceled its confirmation, verified the retained draft, and canceled the original editor. The confirmation Cancel control is now fully visible at y=1035–1145 px in its foreground modal window. The member list, editor, retained draft and confirmation screenshots were inspected visually.

The final log includes V283_RETAINED_FORM_WORKFLOWS_PASS, V283_PROFILE_CORRECTED_RETRY_ONCE_PASS, V283_THRESHOLD_CORRECTED_RETRY_ONCE_PASS, V283_ENCRYPTED_FORM_RETRY_AND_CANCEL_PASS and the earlier photo, accessibility and premium workflow pass markers. The six protected accounting/schema/Classic PDF modules still match their exact baseline hashes.

rc-test.11 and rc-test.12 were not accepted. The first exposed an invalid test assumption about dialog-relative IME insets; the second exposed the native confirmation button clipping fixed above. The final gate retains complete visibility and real-touch requirements.

This remains a signed preview and draft change. Owner-phone ledger comparison, gallery/document providers, recipient apps, physical printing and second-device recovery remain subject to the existing device acceptance gate. Photos remain local and are excluded from financial JSON/encrypted ledger backups.

## Recovery lifecycle and Receipt Library navigation

- Backup, verification, restore-rehearsal and import-review results check their original Activity before opening a window. Accepted backup and import work still completes after recreation.
- Abandoned JSON/PDF export preparation removes its own temporary file and releases its own picker gate. A completed receipt preview also releases its bitmap and PDF when its original screen has expired.
- Plain and encrypted import reviews revoke their exact review token on obsolete-owner completion or Android Back. Existing empty-database and commit-time import validation is unchanged.
- The verified JSON import chooser reports an unavailable picker through the existing error dialog.
- Receipt Library retains its subpage, search, selected filter and display limit across recreation. Typing is captured even while the receipt index is loading.
- System Back returns from Receipt Library to Collect. The native Android 13+ callback is registered only while the library is open and released on navigation or destruction; the earlier-Android fallback remains available. At a top-level destination Android retains its normal Back behavior.
- The new receipt-navigation case exercises real platform Back, delayed indexing, recreation, filter state and stale result rejection. The recovery-lifecycle case delays real worker operations across recreation, checks temporary-file/gate cleanup and fresh retries, and verifies exact import-review cancellation against an isolated empty in-memory database. Both require the synthetic club and preserve the complete canonical ledger.

These two instrumentation cases run with normal display settings before the final form test's intentional synthetic metadata changes. Signed Android acceptance for this follow-on revision is recorded below after the workflow finishes.
