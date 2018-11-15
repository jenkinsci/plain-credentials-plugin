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
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.cli.CreateCredentialsByXmlCommand;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.ExtensionList;
import hudson.cli.CLICommandInvoker;
import hudson.security.ACL;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import hudson.diagnosis.OldDataMonitor;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class SecretBytesTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    /**
     * Verifies that {@link SecretBytes} will treat a Base64 encoded plain text content as the content to be encrypted
     * with the instance's secret key which gets applied when the {@link FileCredentialsImpl} is written to disk.
     * @throws Exception if things go wrong.
     */
    @Test
    @LocalData
    public void loadUnencrypted() throws Exception {
        // these are the magic strings
        assumeThat(Base64.encodeBase64("This is Base64 encoded plain text\n".getBytes("UTF-8")), is(
                "VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==".getBytes("US-ASCII")));

        // first check that the file on disk contains the unencrypted text
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml")),
                containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg=="));

        // get the credential instance under test
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM,
                        (List<DomainRequirement>) null
                ),
                CredentialsMatchers.withId("secret-file")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("a line"));

        // now check that the content has been read correctly
        assertThat(IOUtils.toString(c.getContent(), "UTF-8"), is("This is Base64 encoded plain text\n"));

        // now when we re-save the credentials this should encrypt with the instance's secret key
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml")),
                not(containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==")));
    }

    /**
     * Verifies that legacy data is converted correctly and that the new {@link SecretBytes} gets applied 
     * when the {@link FileCredentialsImpl} is written to disk.
     * @throws Exception if things go wrong.
     */
    @Test
    @LocalData
    @SuppressWarnings( {"ResultOfObjectAllocationIgnored", "deprecation"})
    public void migrateLegacyData() throws Exception {
        // first check that the file on disk contains the legacy format
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml")),
                allOf(containsString("</data>"), not(containsString("</secretBytes>"))));

        assertThat(SystemCredentialsProvider.getConfigFile().asString(), containsString("<id>legacyData</id>"));
        assertThat(ExtensionList.lookup(OldDataMonitor.class).get(0).getData().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().extra).collect(Collectors.toList()), empty());
        assertEquals(272, new File(r.jenkins.getRootDir(), "secrets/org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl").length());
        new FileCredentialsImpl(CredentialsScope.GLOBAL, "legacyData", "credential using legacy data format", "secret.txt",
                SecretBytes.fromBytes(FileCredentialsImpl.KEY.decrypt().doFinal(Base64.decodeBase64("DMG4Q+h/SWXBvMJQy7vMACNZgmCVggCvjP5qeNqsAQo8o7dC69vHlHOjReE1MDIr"))));
        assertThat(((SystemCredentialsProvider) SystemCredentialsProvider.getConfigFile().read()).getDomainCredentialsMap().toString(),
                containsString("{com.cloudbees.plugins.credentials.domains.Domain@0=[org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl@"));
        assertThat(SystemCredentialsProvider.getInstance().getDomainCredentialsMap().toString(), containsString("{com.cloudbees.plugins.credentials.domains.Domain@0=[org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl@"));
        assertThat(SystemCredentialsProvider.getInstance().getCredentials().stream().map(CredentialsNameProvider::name).collect(Collectors.toList()), contains("secret.txt (credential using legacy data format)"));

        // get the credential instance under test
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM,
                        (List<DomainRequirement>) null
                ),
                CredentialsMatchers.withId("legacyData")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("credential using legacy data format"));

        // now check that the content has been converted
        assertThat(IOUtils.toString(c.getContent(), "UTF-8"), is("This is a secret file from legacy encryption\n"));

        // now when we re-save the credentials this should persist in the new format
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml")),
                allOf(not(containsString("</data>")), containsString("</secretBytes>")));
    }

    @Test
    public void createFileCredentialsByXml() throws Exception {
        // get the credential instance doesn't exist yet
        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM,
                        (List<DomainRequirement>) null
                ),
                CredentialsMatchers.withId("secret-file")
        );
        assertThat(c, nullValue());

        // create the credentials
        CreateCredentialsByXmlCommand cmd = new CreateCredentialsByXmlCommand();
        CLICommandInvoker invoker = new CLICommandInvoker(r, cmd);
        assertThat(invoker.withStdin(new ByteArrayInputStream(("<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl>\n"
                        + "  <scope>GLOBAL</scope>\n"
                        + "  <id>secret-file</id>\n"
                        + "  <description>a line</description>\n"
                        + "  <fileName>secret.txt</fileName>\n"
                        + "  <secretBytes>VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==</secretBytes>\n"
                        + "</org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl>\n")
                        .getBytes(Charset.forName("UTF-8"))))
                        .invokeWithArgs("system::system::jenkins", "_"),
                succeededSilently());

        // get the credential instance under test
        c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class,
                        r.jenkins,
                        ACL.SYSTEM,
                        (List<DomainRequirement>) null
                ),
                CredentialsMatchers.withId("secret-file")
        );

        // we have the credential instance we think we should have
        assertThat(c, notNullValue());
        assertThat(c.getFileName(), is("secret.txt"));
        assertThat(c.getDescription(), is("a line"));

        // now check that the content has been read correctly
        assertThat(IOUtils.toString(c.getContent(), "UTF-8"), is("This is Base64 encoded plain text\n"));

        // now when we re-save the credentials this should encrypt with the instance's secret key
        SystemCredentialsProvider.getInstance().save();
        assertThat(FileUtils.readFileToString(new File(r.jenkins.getRootDir(), "credentials.xml")),
                not(containsString("VGhpcyBpcyBCYXNlNjQgZW5jb2RlZCBwbGFpbiB0ZXh0Cg==")));
    }

}
