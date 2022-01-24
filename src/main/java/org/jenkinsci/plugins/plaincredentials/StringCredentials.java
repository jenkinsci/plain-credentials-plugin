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
import hudson.util.Secret;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Credentials consisting only of a single secret, such as a password or token.
 */
@NameWith(StringCredentials.NameProvider.class)
public interface StringCredentials extends StandardCredentials {

    /**
     * Returns the wrapped secret value.
     * @return the encrypted value
     */
    @NonNull Secret getSecret();

    class NameProvider extends CredentialsNameProvider<StringCredentials> {

        @Override public String getName(StringCredentials c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            String ID = c.getId();
            return description != null ? description : (!isUUID(ID) ? ID : Messages.StringCredentials_string_credentials());
        }

        /**
         * Checks whether an ID has UUID format
         * 
         * @param ID the ID to check
         * @return true if the ID has UUID format. False otherwise.
         */
        private static boolean isUUID(String ID) {
            try {
                UUID.fromString(ID);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }

}
