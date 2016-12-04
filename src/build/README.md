How to generated encrypted Travis build files:

Two files in this directory (`travis-settings.xml.enc`, and `travis-settings-security.xml.enc`) are encrypted so we can store them with the project.

When Travis executes the build, it decrypts them (using the keys from secret environment variables configured on the project in Travis).

These files are used to allow Maven to access the Oracle Maven Repository, where the Oracle JDBC driver jar can be acquired. I guess it's really important they keep those jar files secure, or something.

In case anybody gets hit by a bus or our credentials get burned for any reason, here's how you set this up:

# Follow the steps to set up your machine for Oracle development, in [CONTRIBUTING.md](../../CONTRIBUTING.md)

# Generate a random private key for encryption:

```shell
openssl enc -aes-256-cbc -k SomeRandomPasswordYouHaveToChooseEvenThoughItNeverComesUpAgain -P -md sha1
```

This generates a salt, key, and iv--whatever that means--and prints them to the console:

```
salt=F3FC85BE082E8B6D
key=239D2C53D5FF46FE67484B58C91A8C783C521485FCE8B630D77CBCEA17367EC8
iv =FD0B8F77145D7561BAAAC1B3152BF329
```

Store the key and iv in environment variables to make the next steps easier:

```shell
secure_files_key=239D2C53D5FF46FE67484B58C91A8C783C521485FCE8B630D77CBCEA17367EC8
secure_files_iv=FD0B8F77145D7561BAAAC1B3152BF329
```

(keep the shell open so you don't lose these variables)

# Encrypt `settings.xml`

Create a copy of your `~/.m2/settings.xml` file. Edit the file, and remove all Maven settings except for the Oracle repository server. Store it in e.g. `~/.m2/settings-oracle.xml`. (You can delete this file later after these steps are done)

Encrypt the sanitized settings file, and store it in `src/build/travis-settings.xml.enc` in the project:

```shell
openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in ~/.m2/settings-oracle.xml -out src/build/travis-settings.xml.enc
```

Decrypt the file again, to ensure it encrypted correctly:

```shell
openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in src/build/travis-settings.xml.enc -out src/build/travis-settings.xml -d
```

Compare the decrypted file to `~/.m2/settings-oracle.xml` and make sure they are the same.

Probably a good idea to delete `src/build/travis-settings.xml` (unencrypted) so you don't accidentally check it in.

# Encrypt `settings-security.xml`

Encrypt your `~/.m2/settings-security.xml` file, and store it in `src/build/travis-settings-security.xml.enc` in the project:

```shell
openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in ~/.m2/settings-security.xml -out src/build/travis-settings-security.xml.enc
```

Decrypt the file again, to ensure it encrypted correctly:

```shell
openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in src/build/travis-settings-security.xml.enc -out src/build/travis-settings-security.xml -d
```

Compare the decrypted file to `~/.m2/settings-security.xml` and make sure they are the same.

Delete `src/build/travis-settings-security.xml` (unencrypted) so you don't accidentally check it in.

# Login to Travis and modify the secure environment variables:

* Navigate to https://travis-ci.org/jdbi/jdbi/settings
* For the next two steps: make sure the "Display value in the log" toggle is set to "Off". 
* Set the `secure_files_key` environment variable to the generated key (`239D2C53D5FF46FE67484B58C91A8C783C521485FCE8B630D77CBCEA17367EC8` in this example)
* Set the `secure_files_iv` environment variable to the generated iv (`FD0B8F77145D7561BAAAC1B3152BF329` in this example)

These environment variables are used in `.travis.yml` to decrypt the files in the `before_install` step.

# Check in `travis-settings.xml` and `travis-settings-security.xml` to Git, and push to Github.
