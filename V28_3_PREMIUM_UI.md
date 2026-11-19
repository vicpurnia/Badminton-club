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
- Advanced stock setup and physical counting in an expandable section.
- Searchable Reports catalogue with All, Reports, Club and Backups categories.
- All 24 original report handlers preserved, including conditional external-folder disconnect.

## Functional repairs

1. Receipt Library selects its Collect navigation context and owns a new screen epoch. Back routes through normal navigation, so monthly receipts load when the library was opened from Reports.
2. Canceling duplicate-payment review with Android Back or outside dismissal releases its review flag. Save can be tried again without reopening the form.
3. Initials use Unicode code points and a safe fallback, including the accepted name Dr.
4. Receipt Library indicates its active filter and accepts the current display name as well as the immutable recorded name. Home and Collect also use the current profile display name.

PDF callbacks continue to receive the original recorded Member object.

## Verification

The new check_v283_integrated.py gate checks UI integration, navigation/cancel repairs, package/version identity and exact SHA-256 preservation of six accounting, schema and Classic PDF modules.

V283WorkflowUiTest.premiumScreensAndActionsRemainUsable exercises the actual navigation/report controls, report and receipt search, category filters, duplicate-review cancellation/retry, stock forms, member/profile entry, fee/concession entry, month-end review, dashboard, cash-date audit, backup verification and encrypted-backup entry. It requires an explicitly synthetic club and checks canonical ledger equality afterward.

The existing signed upgrade, complete-field ledger, encrypted-backup, photo and navigation suites remain in the workflow. Emulator screenshots are retained for all five main tabs.

All 38 source reconstruction and integration steps pass locally, along with 19 portable Java regression suites. Android compilation and runtime acceptance are recorded in the branch's GitHub Actions runs; final run references will be added after completion.

## Scope

This is an owner-signed preview APK, retaining the existing package and Room schema.
Device gallery providers, recipient apps, physical printers and installation on the owner's phone require actual-device acceptance. Member photos remain local to the phone and are not included in financial JSON or encrypted ledger backups.
