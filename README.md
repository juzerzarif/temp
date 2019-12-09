cura-clinical-app [![Build Status](https://jenkins.cerner.com/careinsight/job/cura-clinical-app_nightly/badge/icon)](https://jenkins.cerner.com/careinsight/job/cura-clinical-app_nightly/)
========================

# Nursing (Millennium) App
## Project Description
Nursing (Millennium) App

## Prerequisites
* 	Clone repo
* 	Android Studio > Import Project > navigate to root of repo location
*  An environment variable in your bash profile pointing to the Android SDK named `ANDROID_HOME`. To determine if this is set, execute `echo $ANDROID_HOME` in the terminal

## Setup

### Perpare gradle settings 

1. Copy cerner-android-nonrelease.keystore which is contained in this project to /Users/[youruser]/.android/
(this can be anywhere but you will need to adjust location below)
2. Add a gradle.properties file under /Users/[youruser]/.gradle that includes
<pre>
MAC:
systemProp.nonreleaseKeystorePassword=cerner
systemProp.keystorePassword=cerner
systemProp.androidNonreleaseSigningCertLocation=/Users/[youruser]/.android/cerner-android-nonrelease.keystore
systemProp.androidSigningCertLocation=/Users/[youruser]/.android/cerner-android-nonrelease.keystore
    
WINDOWS:
systemProp.nonreleaseKeystorePassword=cerner
systemProp.keystorePassword=cerner
systemProp.androidNonreleaseSigningCertLocation=C:\\Users\\[youruser]\\.android\\cerner-android-nonrelease.keystore
systemProp.androidSigningCertLocation=C:\\Users\\[youruser]\\.android\\cerner-android-nonrelease.keystore
</pre>

### Perpare production release gradle settings (NOT required)

1. Get access to and copy the release keystore to /Users/[youruser]/.android/
2. Add a gradle.properties file under /Users/[youruser]/.gradle that includes
<pre>
systemProp.keystorePassword=[keystore password]
systemProp.androidSigningCertLocation=/Users/[youruser]/.android/cerner-android-release.keystore
or on Windows
systemProp.androidSigningCertLocation=C:\\Users\\[youruser]\\.android\\cerner-android-release.keystore
</pre>
Also configure the [cerner-gradle-deploy](http://github.cerner.com/ion/cerner-gradle-deploy/blob/master/README.md) plugin if you plan to upload archives or site documentation from your machine.

### JVM Tests

#### Command line

gradle clean test
or
gradle clean jacocoTestReport

#### IDE
Create new Run Configuration of type Gradle that executes the gradle task (see command line section, exclude "gradle")
* Add test or jacocoTestReport as the task to execute
* Gradle VM options can be set in the project's settings, under Gradle.  In order to run a single test class use something like "-Dtest.single=PPRFragmentTests"

Attach to cmd line running tests
* Run test with command similar to "gradlew -DtestDebug.debug=true testDebug"
* Launch "Remote" configuration in AndroidStudio (create on first run)

### Instrumentation Tests

#### Command line
gradle connectedInstrumentTestDebug

#### IDE
Create new Run Configuration of type Android Tests and Select the module "cura-clinical-app", select the Test of "All In Module" or a specific class

