# Security Policy

## Supported Versions

Jdbi encourages users to upgrade to the [latest version](https://github.com/jdbi/jdbi/releases).

Only the latest released version of Jdbi is actively supported. When reporting bugs or security issues, we encourage you to first upgrade to the latest version and confirm that the problem still exists.

We patch bugs and security relevant issues in the latest version and recommend upgrading. Jdbi goes to great length to ensure backwards compatibility so upgrading is almost always painless.

The current version of Jdbi requires Java 11 or better. 

We will backport security fixes if needed to the [last version that supports Java 8](https://github.com/jdbi/jdbi/releases/tag/v3.39.1).

| Version | Supported          |
| ------- | ------------------ |
| [current latest version](https://github.com/jdbi/jdbi/releases) | :white_check_mark: |
| [latest version that supports Java 8](https://github.com/jdbi/jdbi/releases/tag/v3.39.1) | :white_check_mark: |
| Jdbi 2.x   | :x:                |

## Release cadence

Historically, we have maintained a cadence of a release about every four weeks. The actual release cadence will depend on whether we have specific bug reports or new features that we want to release.

## Reporting a Vulnerability

Security vulnerabilities should be reported by drafting a [Security Advisory](https://github.com/jdbi/jdbi/security/advisories/new) directly on github. 

