package org.jenkinsci.plugins.plaincredentials;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hudson.model.User;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortSpecification;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import hudson.security.ACL;
import hudson.util.Secret;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class BaseTest {

    private static final String UPDATED_CRED_ID = "Custom-ID-Updated";
    private static final String CRED_ID = "Custom-ID";

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void secretTextGlobalScopeTest() throws IOException {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), credential.getSecret()); 
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretTextSystemScopeTest() throws IOException {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.SYSTEM, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), credential.getSecret());
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretTextPersonalScopeTest() throws IOException {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.USER, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), credential.getSecret());
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretTextDomainTest() throws IOException {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), credential.getSecret());

        DomainSpecification[] ar = { new HostnamePortSpecification("include.com:80", "exclude.com:90")};
        Domain d = new Domain("mydomain", "", Arrays.asList(ar));

        testCreateUpdateDelete(credential, updatedCredential, d);
    }
    
    @Test
    public void secretFileGlobalScopeTest() throws IOException, URISyntaxException {
        DiskFileItem fileItem = createEmptyFileItem();

        FileCredentialsImpl credential = new FileCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret file", fileItem, "keys.txt", SecretBytes.fromBytes(fileItem.get()));
        FileCredentialsImpl updatedCredential = new FileCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), fileItem, credential.getFileName(), credential.getSecretBytes());
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretFileSystemScopeTest() throws IOException, URISyntaxException {
        DiskFileItem fileItem = createEmptyFileItem();

        FileCredentialsImpl credential = new FileCredentialsImpl(CredentialsScope.SYSTEM, CRED_ID, "Test Secret file", fileItem, "keys.txt", SecretBytes.fromBytes(fileItem.get()));
        FileCredentialsImpl updatedCredential = new FileCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), fileItem, credential.getFileName(), credential.getSecretBytes());
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretFilPersonalScopeTest() throws IOException, URISyntaxException {
        DiskFileItem fileItem = createEmptyFileItem();

        FileCredentialsImpl credential = new FileCredentialsImpl(CredentialsScope.USER, CRED_ID, "Test Secret file", fileItem, "keys.txt", SecretBytes.fromBytes(fileItem.get()));
        FileCredentialsImpl updatedCredential = new FileCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), fileItem, credential.getFileName(), credential.getSecretBytes());
        testCreateUpdateDelete(credential, updatedCredential, null);
    }

    @Test
    public void secretFileDomainTest() throws IOException, URISyntaxException {
        DiskFileItem fileItem = createEmptyFileItem();

        FileCredentialsImpl credential = new FileCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret file", fileItem, "keys.txt", SecretBytes.fromBytes(fileItem.get()));
        FileCredentialsImpl updatedCredential = new FileCredentialsImpl(credential.getScope(), UPDATED_CRED_ID, credential.getDescription(), fileItem, credential.getFileName(), credential.getSecretBytes());

        DomainSpecification[] ar = { new HostnamePortSpecification("include.com:80", "exclude.com:90")};
        Domain d = new Domain("mydomain", "", Arrays.asList(ar));

        testCreateUpdateDelete(credential, updatedCredential, d);
    }
    
    /**
     * Creates, updates and deletes credentials and perform different assertions
     * 
     * @param credential the credential to create
     * @param updatedCredential the credential that will replace the first one during update
     * @param d the domain the credentials will belong to (or null for global one)
     * @throws IOException
     */
    private <T extends BaseStandardCredentials> void testCreateUpdateDelete(T credential, T updatedCredential, Domain d) throws IOException {
        boolean globalDomain = d == null;
        Domain domainToUse = (globalDomain) ? Domain.global() : d;
        CredentialsStore storeToUse = null;
        SecurityContext ctx = null;
        Authentication auth = null;

        if (CredentialsScope.USER != credential.getScope()) {
            storeToUse = CredentialsProvider.lookupStores(r.jenkins).iterator().next();;
            auth = ACL.SYSTEM;
        } else {
            r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
            User alice = User.get("alice");
            auth = alice.impersonate();
            storeToUse = CredentialsProvider.lookupStores(alice).iterator().next();
            ctx = ACL.impersonate(auth);
        }

        try {
            // Add a credential
            if (!globalDomain) {
                storeToUse.addDomain(d);
            }

            storeToUse.addCredentials(domainToUse, credential);

            // Look up all credentials
            if (!globalDomain) {
                DomainRequirement[] wrongIncludeDomainRequirements = {new HostnamePortRequirement("inclssude.com", 80)};
                List<BaseStandardCredentials> credentialsWithWrongInclude = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, auth, wrongIncludeDomainRequirements);
                assertThat(credentialsWithWrongInclude.size(), is(0));

                DomainRequirement[] excludeDomainRequirements = {new HostnamePortRequirement("exclude.com", 90)};
                List<BaseStandardCredentials> credentialsWithExclude = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, auth, excludeDomainRequirements);
                assertThat(credentialsWithExclude.size(), is(0));
            }

            List<BaseStandardCredentials> credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, auth, Collections.<DomainRequirement>emptyList());

            // There is one credential
            assertThat(credentials.size(), is(1));
            BaseStandardCredentials cred = credentials.get(0);
            assertThat(cred, instanceOf(credential.getClass()));
            assertThat(cred.getId(), is(CRED_ID));

            // Update credential
            storeToUse.updateCredentials(domainToUse, cred, updatedCredential);

            // Look up all credentials again
            credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, auth, Collections.<DomainRequirement>emptyList());

            // There is still 1 credential but the ID has been updated
            assertThat(credentials.size(), is(1));
            cred = credentials.get(0);
            assertThat(cred, instanceOf(credential.getClass()));
            assertThat(cred.getId(), is(UPDATED_CRED_ID));

            // Delete credential
            storeToUse.removeCredentials(domainToUse, cred);

            // Look up all credentials again
            credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, r.jenkins, auth, Collections.<DomainRequirement>emptyList());

            // There are no credentials anymore
            assertThat(credentials.size(), is(0));
        } finally {
            if (ctx != null) {
                SecurityContextHolder.setContext(ctx);
            }
        }
    }
    
    /**
     * Creates an empty FileItem for testing purposes
     *
     * @return
     * @throws URISyntaxException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private DiskFileItem createEmptyFileItem() throws URISyntaxException, FileNotFoundException, IOException {
        DiskFileItem fileItem = (DiskFileItem) new DiskFileItemFactory().createItem("fileData", "text/plain", true, "fileName");
        OutputStream os = fileItem.getOutputStream();
        os.flush();
        return fileItem;
    }
}
