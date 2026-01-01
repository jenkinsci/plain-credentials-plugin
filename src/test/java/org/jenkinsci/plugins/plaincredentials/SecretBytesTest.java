/*
 * The MIT License
 *
 * Copyright 2016 Stephen Connolly and CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.cli.CreateCredentialsByXmlCommand;
import hudson.cli.CLICommandInvoker;
import hudson.security.ACL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@WithJenkins
class SecretBytesTest {

    private JenkinsRule r;

    @BeforeEach
    void setup(JenkinsRule rule) {
        r = rule;
    }

    /**
     * Verifies that {@link SecretBytes} will treat a Base64 encoded plain text content as the content to be encrypted
     * with the instance's secret key which gets applied when the {@link FileCredentialsImpl} is written to disk.
     * @throws Exception if things go wrong.
     */
    @Test
    @LocalData
    void loadUnencrypted() throws Exception {
        // these are the magic strings
        assumeTrue(Base64.getEncoder().encodeToString("This is Base64 encoded plain text\n".getBytes(StandardCharsets.UTF_8)).equals(
                "VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg=="));

        // first check that the file on disk contains the unencrypted text
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml"), StandardCharsets.UTF_8),
                containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg=="));

        // get the credential instance under test
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM2,
                        null
                ),
                CredentialsMatchers.withId("secret-file")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("a line"));

        // now check that the content has been read correctly
        assertThat(IOUtils.toString(c.getContent(), StandardCharsets.UTF_8), is("This is Base64 encoded plain text\n"));

        // now when we re-save the credentials this should encrypt with the instance's secret key
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml"), StandardCharsets.UTF_8),
                not(containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==")));
    }

    /**
     * Verifies that legacy data is converted correctly and that the new {@link SecretBytes} gets applied
     * when the {@link FileCredentialsImpl} is written to disk.
     * @throws Exception if things go wrong.
     */
    @Test
    @LocalData
    void migrateLegacyData() throws Exception {
        // first check that the file on disk contains the legacy format
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml"), StandardCharsets.UTF_8),
                allOf(containsString("</data>"), not(containsString("</secretBytes>"))));

        // get the credential instance under test
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM2,
                        null
                ),
                CredentialsMatchers.withId("legacyData")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("credential using legacy data format"));

        // now check that the content has been converted
        assertThat(IOUtils.toString(c.getContent(), StandardCharsets.UTF_8), is("This is a secret file from legacy encryption\n"));

        // now when we re-save the credentials this should persist in the new format
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml"), StandardCharsets.UTF_8),
                allOf(not(containsString("</data>")), containsString("</secretBytes>")));
    }

    @Test
    void createFileCredentialsByXml() throws Exception {
        // get the credential instance doesn't exist yet
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM2,
                        null
                ),
                CredentialsMatchers.withId("secret-file")
        );
        assertThat(c, nullValue());

        // create the credentials
        CreateCredentialsByXmlCommand cmd = new CreateCredentialsByXmlCommand();
        CLICommandInvoker invoker = new CLICommandInvoker(r, cmd);
        assertThat(invoker.withStdin(new ByteArrayInputStream(("""
                                <?xml version='1.0' encoding='UTF-8'?>
                                <org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl>
                                  <scope>GLOBAL</scope>
                                  <id>secret-file</id>
                                  <description>a line</description>
                                  <fileName>secret.txt</fileName>
                                  <secretBytes>VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==</secretBytes>
                                </org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl>
                                """)
                        .getBytes(StandardCharsets.UTF_8)))
                        .invokeWithArgs("system::system::jenkins", "_"),
                succeededSilently());

        // get the credential instance under test
        c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM2,
                        null
                ),
                CredentialsMatchers.withId("secret-file")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("a line"));

        // now check that the content has been read correctly
        assertThat(IOUtils.toString(c.getContent(), StandardCharsets.UTF_8), is("This is Base64 encoded plain text\n"));

        // now when we re-save the credentials this should encrypt with the instance's secret key
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml"), StandardCharsets.UTF_8),
                not(containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==")));
    }

}
