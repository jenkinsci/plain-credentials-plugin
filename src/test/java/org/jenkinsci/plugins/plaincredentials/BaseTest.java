package org.jenkinsci.plugins.plaincredentials;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import com.cloudbees.plugins.credentials.SecretBytes;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import hudson.model.FileParameterValue.FileItemImpl;
import hudson.security.ACL;
import hudson.util.Secret;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class BaseTest {

    private static final String CRED_ID = "Custom-ID";

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    private CredentialsStore store;
    
    @Before
    public void setup(){
        store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
    }
    
    @Test
    public void secretTextBaseTest() throws IOException {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), CRED_ID, "Updated Secret Text", credential.getSecret());
        testCreateUpdateDelete(credential, updatedCredential);
    }
    
    @Test
    public void secretFileBaseTest() throws IOException, URISyntaxException {
        secretFileTest(false);
    }

    @Test
    public void secretFileBaseTestWithDeprecatedCtor() throws IOException, URISyntaxException {
        secretFileTest(true);
    }

    private void secretFileTest(boolean useDeprecatedConstructor) throws IOException, URISyntaxException {
        FileItem fileItem = createEmptyFileItem();

        FileCredentialsImpl credential;

        if (useDeprecatedConstructor) {
            credential = new FileCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret file", fileItem, "keys.txt", Arrays.toString(Base64.getEncoder().encode(fileItem.get())));
        } else {
            credential = new FileCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret file", fileItem, "keys.txt", SecretBytes.fromBytes(fileItem.get()));
        }

        FileCredentialsImpl updatedCredential = new FileCredentialsImpl(credential.getScope(), CRED_ID, "Updated Secret File", fileItem, credential.getFileName(), credential.getSecretBytes());
        testCreateUpdateDelete(credential, updatedCredential);
    }
    
    /**
     * Creates, updates and deletes credentials and perform different assertions
     * 
     * @param credential the credential to create
     * @param updatedCredential the credential that will replace the first one during update
     * @throws IOException
     */
    private <T extends BaseStandardCredentials> void testCreateUpdateDelete(T credential, T updatedCredential) throws IOException {
        // Add a credential
        store.addCredentials(Domain.global(), credential);
        
        // Look up all credentials
        List<BaseStandardCredentials> credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
    
        // There is one credential
        assertThat(credentials.size(), is(1));
        BaseStandardCredentials cred = credentials.get(0);
        assertThat(cred, instanceOf(credential.getClass()));
        assertThat(cred.getId(), is(CRED_ID));
        // Update credential
        store.updateCredentials(Domain.global(), cred, updatedCredential);
        
        // Look up all credentials again
        credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
   
        // There is still 1 credential but the description has been updated
        assertThat(credentials.size(), is(1));
        cred = credentials.get(0);
        assertThat(cred, instanceOf(credential.getClass()));
        assertThat(cred.getId(), is(CRED_ID));
        assertThat(cred.getDescription(), is(updatedCredential.getDescription()));
        
        // Delete credential
        store.removeCredentials(Domain.global(), cred);
        
        // Look up all credentials again
        credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
     
        // There are no credentials anymore
        assertThat(credentials.size(), is(0));
    }
    
    /**
     * Creates an empty FileItem for testing purposes
     * 
     * @param fileName
     * @return
     * @throws URISyntaxException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private FileItem createEmptyFileItem() throws IOException {
        return new FileItemImpl(Files.createTempFile("credential-test", null).toFile());
    }
}
