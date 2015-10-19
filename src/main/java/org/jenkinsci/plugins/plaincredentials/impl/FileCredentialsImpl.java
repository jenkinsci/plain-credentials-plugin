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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.IOException2;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;

public final class FileCredentialsImpl extends BaseStandardCredentials implements FileCredentials {

    private static final CryptoConfidentialKey KEY = new CryptoConfidentialKey(FileCredentialsImpl.class.getName());
    private static final Logger LOGGER = Logger.getLogger(FileCredentialsImpl.class.getName());

    private final @Nonnull String fileName;
    private final @Nonnull byte[] data;

    @DataBoundConstructor public FileCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, @Nonnull FileItem file, @CheckForNull String fileName, @CheckForNull String data) throws IOException {
        super(scope, id, description);
        String name = file.getName();
        if (name.length() > 0) {
            this.fileName = name.replaceFirst("^.+[/\\\\]", "");
            byte[] unencrypted = file.get();
            try {
                this.data = KEY.encrypt().doFinal(unencrypted);
            } catch (GeneralSecurityException x) {
                throw new IOException2(x);
            }
        } else {
            this.fileName = fileName;
            this.data = Base64.decodeBase64(data);
        }
        if (this.fileName == null || this.fileName.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No FileName was provided or resolved. " +
                            "Input file item was %s and input file name was %s.", file.toString(), fileName)
            );
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "for {0} have {1} of length {2} after upload of ‘{3}’", new Object[] {getId(), this.fileName, unencrypted().length, name});
        }
    }

    @Override public String getFileName() {
        return fileName;
    }

    @Restricted(DoNotUse.class) // for Jelly only
    public String getData() {
        return Base64.encodeBase64String(data);
    }

    private byte[] unencrypted() throws IOException {
        try {
            return KEY.decrypt().doFinal(data);
        } catch (GeneralSecurityException x) {
            throw new IOException2(x);
        }
    }

    @Override public InputStream getContent() throws IOException {
        return new ByteArrayInputStream(unencrypted());
    }

    @Extension public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override public String getDisplayName() {
            return Messages.FileCredentialsImpl_secret_file();
        }

    }

}
