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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.io.IOException;
import org.jenkinsci.plugins.plaincredentials.Binding;
import org.jenkinsci.plugins.plaincredentials.BindingDescriptor;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

public class FileBinding extends Binding<FileCredentials> {

    @DataBoundConstructor public FileBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override protected Class<FileCredentials> type() {
        return FileCredentials.class;
    }

    @Override public Environment bind(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        FilePath secrets = build.getBuiltOn().getRootPath().child("secretFiles");
        secrets.mkdirs();
        secrets.chmod(/*0700*/448);
        FileCredentials credentials = getCredentials(build.getProject());
        final FilePath secret = secrets.child(credentials.getFileName());
        secret.copyFrom(credentials.getContent());
        return new Environment() {
            @Override public String value() {
                return secret.getRemote();
            }
            @Override public void unbind() throws IOException, InterruptedException {
                secret.delete();
            }
        };
    }

    @Extension public static class DescriptorImpl extends BindingDescriptor<FileCredentials> {

        @Override protected Class<FileCredentials> type() {
            return FileCredentials.class;
        }

        @Override public String getDisplayName() {
            return Messages.FileCredentialsImpl_secret_file();
        }

    }

}
