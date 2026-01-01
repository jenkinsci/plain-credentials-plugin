package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.model.FileParameterValue;
import hudson.security.ACL;
import hudson.util.Secret;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@WithJenkins
class BaseTest {

    private static final String CRED_ID = "Custom-ID";

    private JenkinsRule r;

    private CredentialsStore store;

    @BeforeEach
    void setup(JenkinsRule rule) {
        r = rule;
        store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
    }

    @Test
    void secretTextBaseTest() throws Exception {
        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, CRED_ID, "Test Secret Text", Secret.fromString("password"));
        StringCredentialsImpl updatedCredential = new StringCredentialsImpl(credential.getScope(), CRED_ID, "Updated Secret Text", credential.getSecret());
        testCreateUpdateDelete(credential, updatedCredential);
    }

    @Test
    void secretFileBaseTest() throws Exception {
        secretFileTest(false);
    }

    @Test
    void secretFileBaseTestWithDeprecatedCtor() throws Exception {
        secretFileTest(true);
    }

    private void secretFileTest(boolean useDeprecatedConstructor) throws Exception {
        FileItem fileItem = FileItem.fromFileUpload2FileItem(createEmptyFileItem());

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
     * @throws Exception
     */
    private <T extends BaseStandardCredentials> void testCreateUpdateDelete(T credential, T updatedCredential) throws Exception {
        // Add a credential
        store.addCredentials(Domain.global(), credential);

        // Look up all credentials
        List<BaseStandardCredentials> credentials = CredentialsProvider.lookupCredentialsInItemGroup(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM2, Collections.emptyList());

        // There is one credential
        assertThat(credentials.size(), is(1));
        BaseStandardCredentials cred = credentials.get(0);
        assertThat(cred, instanceOf(credential.getClass()));
        assertThat(cred.getId(), is(CRED_ID));
        // Update credential
        store.updateCredentials(Domain.global(), cred, updatedCredential);

        // Look up all credentials again
        credentials = CredentialsProvider.lookupCredentialsInItemGroup(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM2, Collections.emptyList());

        // There is still 1 credential but the description has been updated
        assertThat(credentials.size(), is(1));
        cred = credentials.get(0);
        assertThat(cred, instanceOf(credential.getClass()));
        assertThat(cred.getId(), is(CRED_ID));
        assertThat(cred.getDescription(), is(updatedCredential.getDescription()));

        // Delete credential
        store.removeCredentials(Domain.global(), cred);

        // Look up all credentials again
        credentials = CredentialsProvider.lookupCredentialsInItemGroup(BaseStandardCredentials.class, r.jenkins, ACL.SYSTEM2, Collections.emptyList());

        // There are no credentials anymore
        assertThat(credentials.size(), is(0));
    }

    /**
     * Creates an empty FileItem for testing purposes
     *
     * @return
     * @throws Exception
     */
    private static FileParameterValue.FileItemImpl2 createEmptyFileItem() throws Exception {
        return new FileParameterValue.FileItemImpl2(Files.createTempFile("credential-test", null).toFile());
    }
}
