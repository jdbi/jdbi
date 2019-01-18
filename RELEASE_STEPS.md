# How to release Jdbi

## Prerequisites

You have followed the steps on https://central.sonatype.org/pages/ossrh-guide.html
to set up your Sonatype account, and have uploaded your GNUPG key to the key server
at http://pool.sks-keyservers.net/

## Create a release branch off of the latest master

Name it `jdbi3-<version>-release`.

Example:

```bash
$ git checkout master
$ git pull
$ git checkout -b jdbi-<version>-release 
```

## Build the release on your workstation

Create the release artifacts in Maven, and deploy them to Sonatype staging repository

```bash
$ mvn release:prepare release:perform
```

Change the release version if needed, or just press Enter if the suggested version is good.

Accept the release tag and snapshot versions suggested by Maven.

Go to lunch.

When you get back, you will see if the release succeeded.

If so, you will see two new commits on your branch:

- a release commit with the release version
- a snapshot commit with the next snapshot version

It also creates a release tag, pointing to the release commit.

## Publish the release to Central in oss.sonatype.org

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
  - Type in something to the description like "Jdbi release v<version>"
- Click Refresh until the repository status changes again, which will make
  it disappear from the search.

## Push release release branch and tag

```bash
$ git push -u origin jdbi-<version>-release
$ git push --tags
```

## Update release notes

Double check and make sure all the most important changes are noted in the release notes.

## Publish the docs

Check out the release tag from Git, and run the `publishDocs.sh` script to send doc updates to jdbi.org

```bash
$ git checkout v<version>
$ cd docs
$ ./publish-docs.sh
```

## Publish release announcement

Write up a release announcement (similar to past announcements on the mailing list) and hit Send

Include in the message:

- The new version
- A link to the new artifacts on Maven Central
- The release notes since the last announcement

Send it to the mailing list: jdbi@googlegroups.com
