/*
 * The MIT License
 *
 * Copyright 2013-2016 Jesse Glick, Stephen Connolly and CloudBees, Inc.
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
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Default implementation of {@link FileCredentials}.
 *
 * @since 1.0
 */
public final class FileCredentialsImpl extends BaseStandardCredentials implements FileCredentials {

    /**
     * The legacy key used to encrypt the bytes held in the {@link #data} field.
     */
    @Deprecated
    private static final CryptoConfidentialKey KEY = new CryptoConfidentialKey(FileCredentialsImpl.class.getName());
    /**
     * Our logger.
     */
    private static final Logger LOGGER = Logger.getLogger(FileCredentialsImpl.class.getName());
    /**
     * Standardize serialization (this value is for the 1.2 version of the class).
     */
    private static final long serialVersionUID = -7448141713963432962L;

    /**
     * The filename.
     */
    @NonNull
    private final String fileName;
    /**
     * The secret bytes.
     *
     * @since 1.3
     */
    @NonNull
    private final SecretBytes secretBytes;
    /**
     * The legacy encrypted version of the secret bytes.
     */
    @CheckForNull
    @Deprecated
    private transient byte[] data;

    /**
     * Constructor for Stapler form binding.
     *
     * @param scope       the scope of the credentials.
     * @param id          the id of the credentials.
     * @param description the description of the credentials.
     * @param file        the uploaded file.
     * @param fileName    the name of the file.
     * @param data        the content of the file.
     * @throws IOException when things go wrong.
     * @deprecated use {@link #FileCredentialsImpl(CredentialsScope, String, String, FileItem, String, SecretBytes)} for
     * stapler or {@link #FileCredentialsImpl(CredentialsScope, String, String, String, SecretBytes)} for programatic
     * instantiation.
     */
    @Deprecated
    public FileCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                               @CheckForNull String description, @NonNull FileItem file, @CheckForNull String fileName,
                               @CheckForNull String data) throws IOException {
        super(scope, id, description);
        String name = file.getName();
        if (name.length() > 0) {
            this.fileName = name.replaceFirst("^.+[/\\\\]", "");
            this.secretBytes = SecretBytes.fromBytes(file.get());
        } else {
            this.fileName = fileName;
            this.secretBytes = SecretBytes.fromString(data);
        }
        if (this.fileName == null || this.fileName.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No FileName was provided or resolved. " +
                            "Input file item was %s and input file name was %s.", file.toString(), fileName)
            );
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "for {0} have {1} of length {2} after upload of ‘{3}’",
                    new Object[]{getId(), this.fileName, this.secretBytes.getPlainData().length, name});
        }
    }

    /**
     * Constructor for Stapler form binding.
     *
     * @param scope       the scope of the credentials.
     * @param id          the id of the credentials.
     * @param description the description of the credentials.
     * @param file        the uploaded file.
     * @param fileName    the name of the file.
     * @param secretBytes the content of the file.
     * @throws IOException when things go wrong.
     */
    @DataBoundConstructor
    public FileCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                               @CheckForNull String description, @CheckForNull FileItem file, @CheckForNull String fileName,
                               @CheckForNull SecretBytes secretBytes) throws IOException {
        super(scope, id, description);
        String name = file != null ? file.getName() : "";
        if (name.length() > 0) {
            this.fileName = name.replaceFirst("^.+[/\\\\]", "");
            this.secretBytes = SecretBytes.fromRawBytes(file.get());
        } else {
            if (secretBytes == null) {
                throw new IllegalArgumentException("No content provided or resolved.");
            }
            this.fileName = fileName;
            this.secretBytes = secretBytes;
        }
        if (this.fileName == null || this.fileName.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No FileName was provided or resolved. " +
                            "Input file item was %s and input file name was %s.", file.toString(), fileName)
            );
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "for {0} have {1} of length {2} after upload of ‘{3}’",
                    new Object[]{getId(), this.fileName, this.secretBytes.getPlainData().length, name});
        }
    }

    /**
     * Constructor for everyone besides Stapler.
     *
     * @param scope       the scope of the credentials.
     * @param id          the id of the credentials.
     * @param description the description of the credentials.
     * @param fileName    the name of the file.
     * @param secretBytes the content of the file.
     * @since 1.3
     */
    public FileCredentialsImpl(@CheckForNull CredentialsScope scope,
                               @CheckForNull String id,
                               @CheckForNull String description, @NonNull String fileName,
                               @NonNull SecretBytes secretBytes) {
        super(scope, id, description);
        this.fileName = fileName;
        this.secretBytes = secretBytes;
    }

    /**
     * Migrate {@link #data} to {@link #secretBytes}
     *
     * @return the object.
     * @throws ObjectStreamException if the data cannot be migrated.
     */
    private Object readResolve() throws ObjectStreamException {
        if (data != null) {
            // migrate legacy data
            try {
                return new FileCredentialsImpl(getScope(), getId(), getDescription(), fileName,
                        SecretBytes.fromBytes(KEY.decrypt().doFinal(data)));
            } catch (GeneralSecurityException e1) {
                InvalidObjectException e2 = new InvalidObjectException(e1.toString());
                e2.initCause(e1);
                throw e2;
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * Exposes the encrypted content to jelly.
     *
     * @return the encrypted content.
     */
    @Restricted(DoNotUse.class) // for Jelly only // TODO consider adding to API
    public SecretBytes getSecretBytes() {
        return secretBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getContent() throws IOException {
        return new ByteArrayInputStream(secretBytes.getPlainData());
    }

    /**
     * Our descriptor.
     */
    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.FileCredentialsImpl_secret_file();
        }

    }

}
