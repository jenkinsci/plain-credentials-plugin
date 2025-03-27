package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("ConfigurationAsCode.yaml")
    void should_configure_file_credentials(JenkinsConfiguredWithCodeRule j) throws Exception {
        FileCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(FileCredentials.class, j.jenkins, ACL.SYSTEM2, null),
                CredentialsMatchers.withId("secret-file"));
        assertNotNull(credentials);
        assertEquals("Some secret file", credentials.getDescription());
        assertEquals("my-secret-file", credentials.getFileName());
        assertEquals("FOO_BAR", IOUtils.toString(credentials.getContent(), StandardCharsets.UTF_8));
    }
}
