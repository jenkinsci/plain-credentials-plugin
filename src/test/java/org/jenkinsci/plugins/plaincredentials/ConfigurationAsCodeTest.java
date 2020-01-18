package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ConfigurationAsCode.yaml")
    public void should_configure_file_credentials() throws Exception {
        FileCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(FileCredentials.class, j.jenkins, ACL.SYSTEM, (DomainRequirement) null),
                CredentialsMatchers.withId("secret-file"));
        assertNotNull(credentials);
        assertEquals("Some secret file", credentials.getDescription());
        assertEquals("my-secret-file", credentials.getFileName());
        assertEquals("FOO_BAR", IOUtils.toString(credentials.getContent()));
    }
}
