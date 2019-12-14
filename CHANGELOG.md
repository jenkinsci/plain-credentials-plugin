## Changelog

### Version 1.5 (2018-12-11)

-   Compatibility with [Configuration as Code
    Plugin](https://wiki.jenkins.io/display/JENKINS/Configuration+as+Code+Plugin)
    for file credentials.

### Version 1.4 (Feb 15, 2017)

-   Fix an NPE when uploading a secret file and logging is at level FINE
    or lower

### Version 1.3 (Sep 26, 2016)

-   JENKINS-36432 follow-up Switch to SecretBytes based storage of file
    credentials. ([PR
    \#6](https://github.com/jenkinsci/plain-credentials-plugin/pull/6){.external-link})

### Version 1.2 (May 19, 2016)

-   [JENKINS-30926](https://issues.jenkins-ci.org/browse/JENKINS-30926)
    Handle blank filename.
-   [JENKINS-27391](https://issues.jenkins-ci.org/browse/JENKINS-27391)
    Improved display of secret text credentials.

### Version 1.1 (Jan 15, 2015) (requires 1.580.x)

-   [JENKINS-26099](https://issues.jenkins-ci.org/browse/JENKINS-26099)
    Permitting ID to be specified on new credentials.
-   Display *Scope* control where appropriate.

### Version 1.0 (Jun 16, 2014)

-   First general release.

### Version 1.0 beta 4 (Oct 01, 2013)

-   Added random UUID into bound file paths.
-   Factored build wrapper into [Credentials Binding
    Plugin](https://wiki.jenkins.io/display/JENKINS/Credentials+Binding+Plugin),
    which you must install for that functionality to continue to work.

### Version 1.0 beta 3 (Sep 22, 2013)

-   Added a “secret ZIP file” binding, closest to the operation of the
    now-deprecated [Build Secret
    Plugin](https://wiki.jenkins.io/display/JENKINS/Build+Secret+Plugin).
-   Improved help text.
-   Moved binding SPI out of the implementation package; it is intended
    to be usable from other plugins.

### Version 1.0 beta 2 (Sep 22, 2013)

-   Backdated core dep to 1.509.2.
-   Fixed file reupload.

### Version 1.0 beta 1 (Sep 21, 2013)

-   Initial version.