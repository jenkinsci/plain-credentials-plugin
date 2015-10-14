package org.jenkinsci.plugins.plaincredentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FileCredentialsTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnExceptionIfFileNameIsBlank() throws IOException {
        FileCredentials fileCredentials = new FileCredentialsImpl(CredentialsScope.GLOBAL, "1", "", new StubFileItem(), "", "");
    }

    private class StubFileItem implements FileItem {

        @Override
        public InputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public boolean isInMemory() {
            return false;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public byte[] get() {
            return new byte[0];
        }

        @Override
        public String getString(String encoding) throws UnsupportedEncodingException {
            return null;
        }

        @Override
        public String getString() {
            return null;
        }

        @Override
        public void write(File file) throws Exception {

        }

        @Override
        public void delete() {

        }

        @Override
        public String getFieldName() {
            return null;
        }

        @Override
        public void setFieldName(String name) {

        }

        @Override
        public boolean isFormField() {
            return false;
        }

        @Override
        public void setFormField(boolean state) {

        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        public FileItemHeaders getHeaders() {
            return null;
        }

        @Override
        public void setHeaders(FileItemHeaders headers) {

        }
    }

}
