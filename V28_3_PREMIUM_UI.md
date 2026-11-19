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

All 38 source reconstruction and integration steps pass locally, along with 19 portable Java regression suites. The final CI also passed the complete financial/AES suite, both Android builds, all six instrumentation invocations, and the existing populated navigation checks. All five main screens were visually reviewed; the corrected collection card passed measured visibility assertions for its amount and Receipt button.

## Scope

This is an owner-signed preview APK, retaining the existing package and Room schema.
Device gallery providers, recipient apps, physical printers and installation on the owner's phone require actual-device acceptance. Member photos remain local to the phone and are not included in financial JSON or encrypted ledger backups.

## Follow-on usability and photograph reliability

- Month arrows are exposed as actionable accessibility controls; decorative icons remain excluded.
- Selected tab, reporting month, member search/filter and report search/category survive Activity recreation.
- Member status is below the name. At narrow widths or enlarged text, full member names and stacked receipt amount/actions remain readable.
- Profile, fee policy, concession, stock threshold and encrypted-export form bodies scroll while their native dialog buttons remain available.
- Photo mutation work is serialized across Activity recreation. Completion refreshes the live member list, and invalid-image errors cannot attach a window to the destroyed Activity.
- All eight EXIF orientations are normalized before saving; malformed replacements preserve the existing photo.

Focused acceptance adds real accessibility-node actions, recreation and filter restoration, 320 dp / 2.0 font layout, keyboard/form reachability, queued photo operations across recreation and coloured-quadrant EXIF fixtures. The source reconstruction checks continue to pin the six accounting/schema/Classic PDF modules above. Runtime acceptance for this follow-on revision is recorded below once the candidate passes.

## Accepted build — 19 November 2026

- Application source and tests: 60cb6f1c777311f0d4e3f5da4e357fab038afc4d.
- Signed preview: 28.3-rc-test.8, versionCode 30008.
- Package: com.vinayak.badminton.premiumv24, with the original permanent signing certificate.
- [Signed upgrade and Android workflow acceptance: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44453907610).
- [Debug build and regression checks: PASS](https://github.com/vicpurnia/Badminton-club/actions/runs/44453907674).
- Signed APK artifact: 14496342931. Runtime evidence artifact: 14496413318.
- APK size: 2,185,304 bytes.
- APK SHA-256: c37d9e626ba97407b0b8de3f1bda18469fe86cdcf7f90816385b1f507631f597.

The accepted runtime log includes V283_COLLECT_RECEIPT_FOOTER_VISIBLE_PASS, V283_PREMIUM_WORKFLOW_UI_PASS, V283_DUPLICATE_PAYMENT_BACK_RETRY_PASS, V283_REPORTS_RECEIPT_LIBRARY_BACK_PASS and V283_REPORT_SEARCH_AND_CATEGORIES_PASS. The inventory confirms 23 always-visible report entry points, with external-folder disconnection remaining conditional.

Test synchronization waits for dialog focus and dismissal before sending another Back key. A single, explicitly identified Pixel Launcher ANR can be closed in the synthetic photo test; Badminton and other error dialogs are never dismissed by that recovery. Every original photo, identity, financial and failure assertion remains enforced.
