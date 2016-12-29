Hi! Welcome to JDBI.

We're glad you're thinking about contributing to the projects.

Here's a few pointers to help you get set up:

# Setting up your machine to build `jdbi3-oracle12`

If you don't use Oracle, you can skip this section.

Oracle keeps their JDBC drivers on a password-gated Maven repository, so we exclude the `jdbi3-oracle12` artifact from Maven builds by default. This is so folks who don't use Oracle don't have to go through a bunch of setup that doesn't matter to them.

To build `jdbi3-oracle12` on your machine, you'll need to do the following:

## Set up an Oracle Single Sign-On account

Create an Oracle Single Sign-On account if you do not already have one: [Oracle Single Sign-On](https://login.oracle.com/mysso/signon.jsp)

## Accept the terms of the Oracle Maven Repository license agreement

Navigate here and accept the terms of the agreement: [Oracle Technology Network License Agreement - Oracle Maven Repository](https://www.oracle.com/webapps/maven/register/license.html)

## Set up `settings-security.xml`

Note: You can skip this step if you've already created a `settings-security.xml` file with a master password.

Maven has some adorable security layers to protect (read: obfuscate) your passwords.

This is not really that secure, but nevertheless slightly better than just storing passwords in plaintext.

Create a file `~/.m2/settings-security.xml` if it doesn't already exist.

Choose a master password, and encrypt that password with Maven:

```bash
mvn --encrypt-master-password correcthorsebatterystaple
```

This encrypts your password (`correcthorsebatterystaple` in this example) and outputs an encrypted password that looks like:
 
```
{We9SnhhYFcMHzAatG5k65KtuZzbbffGTva82+R83+RgUVVt/DxTxuO1KyrOFR9wb}
```

Copy and paste the encrypted password into `~/.m2/settings-security.xml`, so it looks like:

```xml
<settingsSecurity>  
  <master>{We9SnhhYFcMHzAatG5k65KtuZzbbffGTva82+R83+RgUVVt/DxTxuO1KyrOFR9wb}</master>  
</settingsSecurity> 
```

## Set up `settings.xml` to access the Oracle repository

Create a file `~/.m2/settings.xml` if it doesn't already exist.

Encrypt your Oracle SSO password with Maven:

```bash
mvn --encrypt-password MyOraclePassword
```

This outputs your encrypted password, which may look something like:

```
{owDCBCmHUKEH1KCgCPcynFctC/X0f02deOu5oEmB0LqhdfKvNtIJ0b4Jr7qdm3SV}
```

Edit your `settings.xml` file so it contains:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd">
  <servers>
    <server> 
      <id>maven.oracle.com</id>
      <username>address@domain.com</username>
      <password>{owDCBCmHUKEH1KCgCPcynFctC/X0f02deOu5oEmB0LqhdfKvNtIJ0b4Jr7qdm3SV}</password>
       <configuration>
         <basicAuthScope>
           <host>ANY</host>
           <port>ANY</port>
           <realm>OAM 11g</realm>
         </basicAuthScope>
         <httpConfiguration>
           <all>
             <params>
               <property>
                 <name>http.protocol.allow-circular-redirects</name>
                 <value>%b,true</value>
               </property>
             </params>
           </all>
         </httpConfiguration>
       </configuration>
    </server> 
  </servers>
</settings>
```

If you already had a `settings.xml` file, make sure to leave the existing settings intact, and simply add the `maven.oracle.com` server to the `servers` section.

Be sure to replace `address@domain.com` with the email address you used to create your Oracle Single Sign-On account, and the `<password>...</password>` with the encrypted password generated in the previous step.

## Run JDBI Maven builds with the `oracle` profile enabled

```bash
cd /path/to/jdbi/project
mvn clean install -U -Poracle
```

The `oracle` profile adds the `jdbi3-oracle12` artifact into the build process.

# Running Oracle unit tests

The previous setup will allow the `jdbi3-oracle12` artifact to build, but without an Oracle database running, all Oracle unit tests will simply be ignored.

Follow this guide: [How to install Oracle Database on Mac](https://dimitrisli.wordpress.com/2012/08/08/how-to-install-oracle-database-on-mac-os-any-version/). With luck, your unit tests should start working after Step 4 in the guide.