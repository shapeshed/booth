# Security Policy

## Supported versions

Booth has one active release line. Security fixes are made against the latest GitHub release and
the latest commit on `main`; older versions are not patched separately.

## Reporting a vulnerability

Please do not open a public GitHub issue for security vulnerabilities. Report them privately using
[GitHub's private vulnerability reporting](../../security/advisories/new), or contact the repository
owner privately.

Please include:

- A description of the vulnerability and its potential impact
- Steps to reproduce it
- The Booth version or commit affected
- Any relevant feed, provider, notification, download, or playback context

You should expect an initial response within 5 business days. If the report is confirmed, a fix
will be prepared before public disclosure. Please allow time for a fix to be released before
disclosing the issue publicly.

## Scope

Reports about Booth's feed parsing, catalogue providers, download handling, playback service,
notifications, local storage, or imported OPML data are in scope. Vulnerabilities in third-party
podcast feeds, catalogue services, artwork hosts, or media hosts should also be reported to the
operator concerned when the issue is outside Booth's code.
