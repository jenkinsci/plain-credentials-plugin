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

package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

/**
 * Credentials consisting of a secret file.
 */
@NameWith(FileCredentials.NameProvider.class)
public interface FileCredentials extends StandardCredentials {

    /**
     * Indicates the intended naming of the secret content.
     * For example, {@code private-keys.zip} or {@code keystore}.
     * @return a simple file name (no path separators)
     */
    @Nonnull String getFileName();

    /**
     * Obtains the actual content of the secret file as a bytestream.
     * @return some binary data
     * @throws IOException if the data cannot be loaded
     */
    @Nonnull InputStream getContent() throws IOException;

    class NameProvider extends CredentialsNameProvider<FileCredentials> {

        @Override public String getName(FileCredentials c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return c.getFileName() + (description != null ? " (" + description + ")" : "");
        }

    }

}
