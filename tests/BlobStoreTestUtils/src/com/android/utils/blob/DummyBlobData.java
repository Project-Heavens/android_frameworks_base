/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.utils.blob;

import static com.google.common.truth.Truth.assertThat;

import android.app.blob.BlobHandle;
import android.app.blob.BlobStoreManager;
import android.content.Context;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DummyBlobData {
    private static final long DEFAULT_SIZE_BYTES = 10 * 1024L * 1024L;
    private static final int BUFFER_SIZE_BYTES = 16 * 1024;

    private final Context mContext;
    private final Random mRandom;
    private final File mFile;
    private final long mFileSize;
    private final String mLabel;

    byte[] mFileDigest;
    long mExpiryTimeMs;

    public DummyBlobData(Context context) {
        this(context, new Random(0), "blob_" + System.nanoTime());
    }

    public DummyBlobData(Context context, long fileSize) {
        this(context, fileSize, new Random(0), "blob_" + System.nanoTime(), "Test label");
    }

    public DummyBlobData(Context context, Random random, String fileName) {
        this(context, DEFAULT_SIZE_BYTES, random, fileName, "Test label");
    }

    public DummyBlobData(Context context, Random random, String fileName, String label) {
        this(context, DEFAULT_SIZE_BYTES, random, fileName, label);
    }

    public DummyBlobData(Context context, long fileSize, Random random, String fileName,
            String label) {
        mContext = context;
        mRandom = random;
        mFile = new File(mContext.getFilesDir(), fileName);
        mFileSize = fileSize;
        mLabel = label;
    }

    public void prepare() throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(mFile, "rw")) {
            writeRandomData(file, mFileSize);
        }
        mFileDigest = FileUtils.digest(mFile, "SHA-256");
        mExpiryTimeMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
    }

    public BlobHandle getBlobHandle() throws Exception {
        return BlobHandle.createWithSha256(createSha256Digest(mFile), mLabel,
                mExpiryTimeMs, "test_tag");
    }

    public long getFileSize() throws Exception {
        return mFileSize;
    }

    public long getExpiryTimeMillis() {
        return mExpiryTimeMs;
    }

    public void delete() {
        mFile.delete();
    }

    public void writeToSession(BlobStoreManager.Session session) throws Exception {
        writeToSession(session, 0, mFileSize);
    }

    public void writeToSession(BlobStoreManager.Session session,
            long offsetBytes, long lengthBytes) throws Exception {
        try (FileInputStream in = new FileInputStream(mFile)) {
            in.getChannel().position(offsetBytes);
            try (FileOutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(
                    session.openWrite(offsetBytes, lengthBytes))) {
                copy(in, out, lengthBytes);
            }
        }
    }

    public void writeToFd(FileDescriptor fd, long offsetBytes, long lengthBytes) throws Exception {
        try (FileInputStream in = new FileInputStream(mFile)) {
            in.getChannel().position(offsetBytes);
            try (FileOutputStream out = new FileOutputStream(fd)) {
                copy(in, out, lengthBytes);
            }
        }
    }

    private void copy(InputStream in, OutputStream out, long lengthBytes) throws Exception {
        final byte[] buffer = new byte[BUFFER_SIZE_BYTES];
        long bytesWrittern = 0;
        while (bytesWrittern < lengthBytes) {
            final int toWrite = (bytesWrittern + buffer.length <= lengthBytes)
                    ? buffer.length : (int) (lengthBytes - bytesWrittern);
            in.read(buffer, 0, toWrite);
            out.write(buffer, 0, toWrite);
            bytesWrittern += toWrite;
        }
    }

    public void readFromSessionAndVerifyBytes(BlobStoreManager.Session session,
            long offsetBytes, int lengthBytes) throws Exception {
        final byte[] expectedBytes = new byte[lengthBytes];
        try (FileInputStream in = new FileInputStream(mFile)) {
            read(in, expectedBytes, offsetBytes, lengthBytes);
        }

        final byte[] actualBytes = new byte[lengthBytes];
        try (FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(
                session.openWrite(0L, 0L))) {
            read(in, actualBytes, offsetBytes, lengthBytes);
        }

        assertThat(actualBytes).isEqualTo(expectedBytes);

    }

    private void read(FileInputStream in, byte[] buffer,
            long offsetBytes, int lengthBytes) throws Exception {
        in.getChannel().position(offsetBytes);
        in.read(buffer, 0, lengthBytes);
    }

    public void readFromSessionAndVerifyDigest(BlobStoreManager.Session session)
            throws Exception {
        readFromSessionAndVerifyDigest(session, 0, mFile.length());
    }

    public void readFromSessionAndVerifyDigest(BlobStoreManager.Session session,
            long offsetBytes, long lengthBytes) throws Exception {
        final byte[] actualDigest;
        try (FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(
                session.openWrite(0L, 0L))) {
            actualDigest = createSha256Digest(in, offsetBytes, lengthBytes);
        }

        assertThat(actualDigest).isEqualTo(mFileDigest);
    }

    public void verifyBlob(ParcelFileDescriptor pfd) throws Exception {
        final byte[] actualDigest;
        try (FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
            actualDigest = FileUtils.digest(in, "SHA-256");
        }
        assertThat(actualDigest).isEqualTo(mFileDigest);
    }

    private byte[] createSha256Digest(FileInputStream in, long offsetBytes, long lengthBytes)
            throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        in.getChannel().position(offsetBytes);
        final byte[] buffer = new byte[BUFFER_SIZE_BYTES];
        long bytesRead = 0;
        while (bytesRead < lengthBytes) {
            int toRead = (bytesRead + buffer.length <= lengthBytes)
                    ? buffer.length : (int) (lengthBytes - bytesRead);
            toRead = in.read(buffer, 0, toRead);
            digest.update(buffer, 0, toRead);
            bytesRead += toRead;
        }
        return digest.digest();
    }

    private byte[] createSha256Digest(File file) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream in = new BufferedInputStream(
                Files.newInputStream(file.toPath()))) {
            final byte[] buffer = new byte[BUFFER_SIZE_BYTES];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return digest.digest();
    }

    private void writeRandomData(RandomAccessFile file, long fileSize)
            throws Exception {
        long bytesWritten = 0;
        final byte[] buffer = new byte[BUFFER_SIZE_BYTES];
        while (bytesWritten < fileSize) {
            mRandom.nextBytes(buffer);
            final int toWrite = (bytesWritten + buffer.length <= fileSize)
                    ? buffer.length : (int) (fileSize - bytesWritten);
            file.seek(bytesWritten);
            file.write(buffer, 0, toWrite);
            bytesWritten += toWrite;
        }
    }
}