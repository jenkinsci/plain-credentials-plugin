package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_configure_file_credentials() throws Exception {
        ConfigurationAsCode.get().configure(getClass().getResource("ConfigurationAsCode.yaml").toString());
        final FileCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(FileCredentials.class, j.jenkins, ACL.SYSTEM, (DomainRequirement) null),
                CredentialsMatchers.withId("secret-file"));
        Assert.assertNotNull(credentials);
        Assert.assertEquals("Some secret file", credentials.getDescription());
        Assert.assertEquals("my-secret-file", credentials.getFileName());
        Assert.assertEquals("FOO_BAR", IOUtils.toString(credentials.getContent()));
    }
}
