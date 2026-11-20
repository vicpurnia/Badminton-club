#!/usr/bin/env python3
"""Check the v28.3 UI overlay without altering the verified financial baseline.

Android instrumentation separately exercises the visible actions. This source gate
keeps the package, schema, ledger codec, accounting and approved PDF bytes stable.
"""
from hashlib import sha256
from pathlib import Path
import re

root = Path('app/src/main/java/com/vinayak/badminton/nativepreview')
activity = (root / 'MainActivity.java').read_text(encoding='utf-8')
helper = root / 'PremiumV283Ui.java'
helper_source = Path('src/premium/v28_3/PremiumV283Ui.java')
assert helper.is_file() and helper_source.is_file(), 'Premium UI helper was not assembled'
assert helper.read_bytes() == helper_source.read_bytes(), 'Assembled premium UI helper differs from reviewed source'
form_helper = root / 'PremiumV283FormBuilder.java'
assert form_helper.read_bytes() == Path('src/premium/v28_3/PremiumV283FormBuilder.java').read_bytes(), (
    'Retained form helper differs from reviewed source')
assert 'PremiumV283Ui' in activity, 'Native activity does not use the premium UI helper'
assert 'v28.3 DEVELOPMENT CANDIDATE' in activity, 'v28.3 build identity is missing'

helper_text = helper.read_text(encoding='utf-8')
for term in ('INK=0xFF14243B', 'ACCENT=0xFF315DCC', 'IVORY=0xFFF5F3EE',
             'class LineIcon extends Drawable', 'setMinimumHeight(dp(view.getContext(),48))',
             'setMinimumWidth(dp(view.getContext(),48))', 'IMPORTANT_FOR_ACCESSIBILITY_YES',
             'config.screenWidthDp<360||config.fontScale>1.3f', 'codePointAt(0)', 'Character.toChars'):
    assert term in helper_text, 'Missing premium presentation/accessibility feature: ' + term
for term in ('PremiumV283Ui.INK', 'PremiumV283Ui.ACCENT', 'PremiumV283Ui.IVORY',
             'PremiumV283Ui.initials', 'PremiumV283Ui.icon', 'PremiumV283Ui.touch'):
    assert term in activity, 'Native premium UI integration is missing: ' + term

for term in ('restorePremiumUiState(b)', '"v283.ui.tab"', '"v283.ui.month"',
             '"v283.ui.memberSearch"', '"v283.ui.memberFilter"',
             '"v283.ui.reportSearch"', '"v283.ui.reportGroup"',
             '"v283.ui.receiptLibrary"', '"v283.ui.receiptSearch"',
             '"v283.ui.receiptFilter"', '"v283.ui.receiptLimit"',
             'private void postRecoveryUi(Runnable ready,Runnable abandoned)',
             'registerOnBackInvokedCallback(', 'unregisterOnBackInvokedCallback(',
             'memberPhotoWork.compareAndSet(false,true)', 'memberPhotoWork.set(false)',
             'memberPhotoActivity=new java.lang.ref.WeakReference<>(this)',
             'private ScrollView premiumForm(View body)',
             'chip.setSelected(option.equals(memberFilter))'):
    assert term in activity, 'Missing reviewed state/accessibility/photo behavior: ' + term
assert 'private void error(Exception e){if(isFinishing()||isDestroyed())return;' in activity, (
    'An obsolete Activity must not attach an error dialog after recreation')
assert activity.count('.setView(premiumForm(') == 6, 'All six input forms must scroll'
assert activity.count('new PremiumV283FormBuilder(this)') == 6, 'All six input editors must retain rejected drafts'
assert 'private void savePremiumForm(' in activity, 'Input editors must track transaction completion'

styles = Path('app/src/main/res/values/premium_v283_styles.xml')
assert styles.read_bytes() == Path('src/premium/v28_3/styles.xml').read_bytes(), (
    'Premium Android theme differs from reviewed source')
manifest = Path('app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
assert '@style/PremiumV283Theme' in manifest, 'Premium native theme is not active'
assert 'android:enableOnBackInvokedCallback="true"' in manifest, (
    'Receipt Library must receive native system Back on Android 13 and later')

library_start = activity.index('private void showReceiptLibrary(){')
library_end = activity.index('private void showPayments(){', library_start)
library = activity[library_start:library_end]
assert 'screenEpoch++;tab="Collect";drawNav();' in library, (
    'Opening Receipt Library from Reports must select the Collect navigation context')
assert '()->selectTab("Collect")' in library, 'Receipt Library Back must route through navigation'
assert 'setReceiptLibraryOpen(true)' in library and 'search.setText(receiptSearch)' in library, (
    'Receipt Library must restore its subpage and search state')
assert '"‹ Back to Collect",this::showPayments' not in library, (
    'Receipt Library Back still bypasses the selected tab/epoch')

payment_start = activity.index('private void openPayment(')
payment_end = activity.index('private void savePayment(', payment_start)
payment = activity[payment_start:payment_end]
assert '.setOnCancelListener(cancelled->reviewing.set(false))' in payment, (
    'Canceling the duplicate-payment confirmation must release the reviewing flag')
assert 'reviewing.compareAndSet(false,true)' in payment, 'Duplicate submission guard was removed'
assert 'final String submissionId="pay-"+UUID.randomUUID();' in payment, (
    'Payment form must retain its single immutable submission identity')

# Derived from the exact generated app at 9fd4ec9b5218d41b62927ba59665e8aaca0fc454.
# These modules are outside the visual/route repair scope of this upgrade.
protected = {
    'LedgerDatabase.java': 'a4aa2f796e7c15b6a9f30e6246b9dfabd6bb55dfed70e0eb3b9b12e1c2cc2011',
    'LedgerDao.java': '2908b2e5e7033a7bdc78944af6c7385548f1d2d0fe03ac66f2f3ae3ed3376a38',
    'Records.java': '1a01ca1eaa7c3b49e47f8695e1b2211bbe94466dea980bfa4835210c6f3c2d18',
    'MigrationCodec.java': '32dae12bab608eac82c76f986dcca260a53498ed3e52c11e16f80e53d6c4d3e0',
    'ReportMath.java': '9db2b912fdfa69b15dff5963cedfcccefe48f30aa7ac26ba26dfbf0443459360',
    'ClassicLedgerV231.java': 'd3dc7876cee9878bbb8cf13fbcddbe7f408e12407b67bf9701806f47547c69e5',
}
for name, expected in protected.items():
    assert sha256((root / name).read_bytes()).hexdigest() == expected, (
        'UI upgrade changed protected financial/schema/PDF module: ' + name)

gradle = Path('app/build.gradle').read_text(encoding='utf-8')
assert "applicationId 'com.vinayak.badminton.premiumv24'" in gradle, 'Existing application ID changed'
assert re.search(r'\bminSdk\s+26\b', gradle), 'Existing Android minimum version changed'
assert '28.3-dev.' in gradle, 'Expected v28.3 development build before test signing'
version = re.search(r'\bversionCode\s+(\d+)', gradle)
assert version and int(version.group(1)) >= 30000, 'Version code does not use v28.3 upgrade range'

print('PASS v28.3 native palette/theme, accessible UI helpers, receipt navigation, duplicate-review cancellation and exact financial/schema/Classic PDF baseline')
