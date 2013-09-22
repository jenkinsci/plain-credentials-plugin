/*
 * The MIT License
 *
 * Copyright 2013 jglick.
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

package org.jenkinsci.plugins.plaincredentials.impl;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.IOException2;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

public final class FileCredentialsImpl extends BaseStandardCredentials implements FileCredentials {

    private static final CryptoConfidentialKey KEY = new CryptoConfidentialKey(FileCredentialsImpl.class.getName());

    private final @Nonnull String filename;
    private final @Nonnull byte[] data;

    @DataBoundConstructor public FileCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, @Nonnull FileItem file) throws IOException {
        super(scope, id, description);
        filename = file.getName().replaceFirst("^.+[/\\\\]", "");
        byte[] unencrypted = file.get();
        try {
            this.data = KEY.encrypt().doFinal(unencrypted);
        } catch (GeneralSecurityException x) {
            throw new IOException2(x);
        }
        /* TODO failed attempt to handle optional upload:
        FileCredentialsImpl old = findExisting(id);
        if (old == null) {
            throw new IllegalArgumentException("must upload a file");
        }
        filename = old.filename;
        data = old.data;
    }

    private static FileCredentialsImpl findExisting(String id) {
        for (FileCredentialsImpl existing : CredentialsProvider.lookupCredentials(FileCredentialsImpl.class, Jenkins.getInstance(), null, Collections.<DomainRequirement>emptyList())) {
            if (existing.getId().equals(id)) {
                return existing;
            }
        }
        return null;
        */
    }

    @Override public String getFileName() {
        return filename;
    }

    @Override public InputStream getContent() throws IOException {
        try {
            return new ByteArrayInputStream(KEY.decrypt().doFinal(data));
        } catch (GeneralSecurityException x) {
            throw new IOException2(x);
        }
    }

    @Extension public static class DescriptorImpl extends CredentialsDescriptor {

        @Override public String getDisplayName() {
            return Messages.FileCredentialsImpl_secret_file();
        }

    }

}
