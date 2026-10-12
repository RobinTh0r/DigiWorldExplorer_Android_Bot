# Handoff — 5.5.1 Beta 2 (12 October 2026)

Authoritative checkout remains this folder. Read HANDOFF_5.5.1_BETA1.md and its
referenced HANDOFF_5.5.0.md for earlier live testing and device/data constraints.
The user explicitly requested publication: "Neue beta raushauen". Current candidate
code88/name5.5.1-beta.2, planned prerelease v5.5.1-beta.2; stable5.5.0 stays unchanged.

Packages the previously local diagnostic/DWS and global Growth Guide fixes. Read
docs/DIAGNOSTIC_HARDENING_20261012.md and docs/GROWTH_GUIDE_RECOVERY_20261012.md
for precise evidence, implementation and limitations. Their old code87 candidate
hashes are historical checkpoints, not this release's artifact. English/German
in-app release notes and RELEASE_NOTES_5.5.1_BETA2.md updated.

No handset profile, ID/signing/settings migration or account-data reset. New recovery
preempts exclusive sessions only on specifically recognized Growth Guide; known
limitations (rejected stale action can park caller, exact stock suffixes unknown,
Clear Previous Difficulty unimplemented, fresh positive Dungeon ads/rewards) remain.
Diagnostic decision trace is DWS-specific; shared images/gesture evidence serve
other modes. No universal/device-live-certification claim.

No connected ADB device at release preparation. No emulator/phone launched/tapped,
settings changed or APK installed. USB debugging must remain OFF during phone
gameplay (00000038). Keep original APKs and private ZIP/configs untouched; do not
upload whole user diagnostics or private backups. New releases beyond this explicit
Beta request still require user authorization.

## Pre-publication verification

Exact code88 build:750tests in87suites, zero failures/errors/skips. Signed release,
vital lint and full debug lint pass (0errors,155warnings,1hint). Manifest verified
IDde.robinthor.digiworldexplorer/code88/name5.5.1-beta.2. CertificateSHA256
859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8 unchanged.
APK C:/Users/thor/AppData/Local/Temp/dwe-release-20261010/build/app/outputs/release/DigiWorldExplorer-Bot-v5.5.1-beta.2.apk,
25,812,504bytes,SHA256ce5a4294fd749681433aede918e5c0e5784eaff817e4017261c5089a37cde711.
Copy in artifacts under the same new name; original beta.1/stable files unchanged.
Built through release-build.init.gradle, outputs outside Nextcloud. No new live
test, installation or diagnosis-export validation. Prior remote mainb35e4e8e6962d4196351876a0113e7a7d4f46153;
v5.5.1-beta.2 absent before preparation. Publication confirmation follows.
