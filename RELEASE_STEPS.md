# How to release Jdbi

## Prerequisites

You have followed the steps on https://central.sonatype.org/pages/ossrh-guide.html
to set up your Sonatype account, and have uploaded your GNUPG key to the key server
at http://pool.sks-keyservers.net/

## Before you begin

These are the release steps that need to be done:

Mandatory:

* Create a release branch off the latest master
* Update release notes and documentation page
* Build the release locally
* Publish the release to Maven Central
* Release the code changes to github
* Publish the documentation
* Add a release announcement to github

Optional:

* Publish release announcement to the mailing list
* Release additional Modules

All build steps are described in detail below. If unsure, please follow the instructions below.


## Create a release branch off the latest master

Name it `jdbi-<version>-release`.

Example:

```bash
$ git clone git@github.com:jdbi/jdbi
$ cd jdbi
$ git checkout -b jdbi-<version>-release master
```

or

```bash
$ cd jdbi
$ git pull
$ git checkout -b jdbi-<version>-release master
```

## Update release notes and documentation page

Double check that release notes file (`RELEASE_NOTES.md` in the root
folder) contains all the most important changes for the release.

Open the main documentation file (`docs/src/adoc/index.adoc`), find the `== Release documentation` line and
add a new link to the permanent release documentation for the new release:

```
* https://jdbi.org/releases/<version>[Release <version> - <date>^]
```

where `<version>` is the release version and `<date>` the current date in ISO8601 format.

_*PLEASE DO NOT RELEASE BEFORE UPDATING THE `RELEASE_NOTES.md` and the `index.adoc` file!*_


## Build the release on your workstation

Create the release artifacts in Maven, and deploy them to Sonatype staging repository.
Use the latest Java LTS version (currently 17) to build the artifacts.

```bash
$ make release
```

Change the release version if needed, or just press Enter if the suggested version is good.

Accept the release tag and snapshot versions suggested by Maven.

Grab a coffee. A release build takes about six minutes on a reasonably current laptop.

If the release succeeds, there will be two new commits on the branch:

- a release commit with the release version
- a snapshot commit with the next snapshot version

It also creates a release tag, pointing to the release commit.


### Releasing without docker

First, please don't. But if you really have to, it is possible to run

```bash
$ MAVEN_CONFIG=-Dno-docker=true make release
```

Which will skip all docker related tests. This is explicitly
unsupported and a workaround in very specific situations and may break
at any point.


## Publish the release to Maven Central

*THIS IS NOT THE LAST RELEASE STEP!* After pushing the code out, please wrap up
by pushing the code changes below and also publish the documentation web site!

- Open oss.sonatype.org and log in
- Click Staging Repositories
- Search for jdbi (top right corner)
- Select the repository and click Close
  - Closing the repository means closing it for further modification
  - Nexus will check that all files uploaded to the staging repository
    meet Maven Central publishing requirements
- Click Refresh until the repository status changes to "closed"
  - If this fails, find out what rule was not satisfied, and start over! Yay!
- Click Release to submit the release to Maven Central.
  - Type in something to the description like "Jdbi release v\<version\>"
- Click Refresh until the repository status changes again, which will make
  it disappear from the search.


## Release the code changes to github

Push release release branch and tag to Github, then merge the release branch back to the master:

```bash
$ git push -u origin jdbi-<version>-release
$ git push --tags
$ git checkout master
$ git merge --ff-only jdbi-<version>-release
$ git push master
```

## Publish the documentation

Check out the release tag from Git, and run publish target to update the docs on jdbi.org:

```bash
$ git checkout v<version>
$ MAVEN_CONFIG=-Pjdbi-release make publish-docs
```

## Add a release announcement to github

- open https://github.com/jdbi/jdbi, click on "Releases"
- "Draft a new Release"
- Select the just pushed Tag, use Release title *JDBI <version>*
- paste the line items from the Release notes into the "Describe this release" text box
- Please make sure that the `Set as the latest release` and `Create a discussion for the release` checkboxes are selected
- Click "Publish release"


## Publish release announcement to the mailing list

Write up a release announcement (similar to past announcements on the mailing list) and hit Send

Include in the message:

- The new version
- A link to the new artifacts on Maven Central
- The release notes since the last announcement

Send it to the mailing list: jdbi@googlegroups.com


## Release additional Modules

A small number of JDBI3 modules is maintained outside the main build. Consider releasing those as well:

* [JDBI3 Oracle support](https://github.com/jdbi/jdbi3-oracle12)
* [JDBI3 Guava cache module](https://github.com/jdbi/jdbi3-guava-cache)
