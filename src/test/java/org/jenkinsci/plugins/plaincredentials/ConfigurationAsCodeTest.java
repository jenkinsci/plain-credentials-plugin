package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ConfigurationAsCodeTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        ConfigurationAsCode.get().configure(readFile("casc_fileCredentials.yml"));
    }

    @Test
    public void should_configure_file_credentials() throws Exception {
        FileCredentials credentials = lookupCredentials("secret-file");
        Assert.assertNotNull(credentials);
        Assert.assertEquals("secret-file.txt", credentials.getFileName());
        Assert.assertEquals("secret-file", credentials.getDescription());
        Assert.assertEquals("Hello World!", IOUtils.toString(credentials.getContent()));
    }

    private String readFile(String s) {
        return getClass().getResource(s).toString();
    }

    private FileCredentials lookupCredentials(String id) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(FileCredentials.class, j.jenkins, ACL.SYSTEM, (DomainRequirement) null),
                CredentialsMatchers.withId(id));
    }
}
