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
import hudson.util.Secret;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

public final class StringCredentialsImpl extends BaseStandardCredentials implements StringCredentials {

    private static final long serialVersionUID = 4239232115673493707L;

    private final @Nonnull Secret secret;

    @DataBoundConstructor public StringCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, @Nonnull String secret) {
        super(scope, id, description);
        this.secret = Secret.fromString(secret);
    }

    public StringCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, @Nonnull Secret secret) {
        super(scope, id, description);
        this.secret = secret;
    }

    @Override public Secret getSecret() {
        return secret;
    }

    @Extension public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override public String getDisplayName() {
            return Messages.StringCredentialsImpl_secret_text();
        }

    }

}
