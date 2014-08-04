/*
 * Copyright 2014 Ricardo Lorenzo<unshakablespirit@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/*
 * FileUtils class
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Author: Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
package utils.file;

import java.io.*;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ricardo Lorenzo
 */
public class FileUtils {
    /**
     * Compress bytes into different formats
     *
     * @throws NoSuchMethodException , IOException
     */
    public static byte[] compress(final int type, final byte[] input) throws IOException, NoSuchMethodException {
        final ByteArrayInputStream is = new ByteArrayInputStream(input);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            IOStreamUtils.compress(type, is, os);
        } finally {
            IOStreamUtils.closeQuietly(is);
            IOStreamUtils.closeQuietly(os);
        }
        return os.toByteArray();
    }

    /**
     * Compress bytes into different formats
     *
     * @throws NoSuchMethodException , IOException
     */
    public static void compress(final int type, final File input, final File output) throws IOException,
            NoSuchMethodException {
        final FileInputStream is = new FileInputStream(input);
        final FileOutputStream os = new FileOutputStream(output);
        try {
            IOStreamUtils.compress(type, is, os);
        } finally {
            IOStreamUtils.closeQuietly(is);
            IOStreamUtils.closeQuietly(os);
        }
    }

    /**
     * Copy the file content into another
     *
     * @throws NoSuchMethodException , IOException
     */
    public static final void copyFile(final File f1, final File f2) throws IOException, FileLockException {
        if(f1.exists() && f1.isDirectory()) {
            if(!f2.exists()) {
                f2.mkdirs();
            }
            for(final File f : f1.listFiles()) {
                copyFile(f, new File(f2.getAbsolutePath() + File.separator + f.getName()));
            }
        } else if(f1.exists() && f1.isFile()) {
            final FileLock fl = new FileLock(f2);
            try {
                fl.lock();
                final InputStream is = new BufferedInputStream(new FileInputStream(f1));
                final OutputStream os = new BufferedOutputStream(new FileOutputStream(f2));

                try {
                    IOStreamUtils.write(is, os);
                } finally {
                    IOStreamUtils.closeQuietly(is);
                    IOStreamUtils.closeQuietly(os);
                }
            } finally {
                fl.unlock();
            }
        }
    }

    /**
     * Decompress bytes into different formats
     *
     * @throws NoSuchMethodException , IOException
     */
    public static byte[] decompress(final int type, final byte[] input) throws IOException, NoSuchMethodException {
        final ByteArrayInputStream is = new ByteArrayInputStream(input);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        decompress(type, is, os);
        return os.toByteArray();
    }

    /**
     * Decompress bytes into different formats
     *
     * @throws NoSuchMethodException , IOException
     */
    public static void decompress(final int type, final File input, final File output) throws IOException,
            NoSuchMethodException {
        final FileInputStream is = new FileInputStream(input);
        final FileOutputStream os = new FileOutputStream(output);
        decompress(type, is, os);
    }

    private static void decompress(final int type, final InputStream is, final OutputStream os)
            throws NoSuchMethodException, IOException {
        try {
            IOStreamUtils.compress(type, is, os);
        } finally {
            IOStreamUtils.closeQuietly(is);
            IOStreamUtils.closeQuietly(os);
        }
    }

    public static void emptyFile(final File file) throws IOException, FileLockException {
        BufferedOutputStream os = null;
        final FileLock fl = new FileLock(file);
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            fl.lock();
            os.write("".getBytes());
        } finally {
            fl.unlockQuietly();
            IOStreamUtils.closeQuietly(os);
        }
    }

    public static byte[] readFile(final File file) throws IOException {
        FileInputStream is = null;
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            is = new FileInputStream(file);
            IOStreamUtils.write(is, os);
        } finally {
            IOStreamUtils.closeQuietly(is);
            IOStreamUtils.closeQuietly(os);
        }
        return os.toByteArray();
    }

    public static Object readObjectFile(final File file) throws IOException, ClassNotFoundException {
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new FileInputStream(file));
            return is.readObject();
        } finally {
            IOStreamUtils.closeQuietly(is);
        }
    }

    public static String readFileAsString(final File file) throws IOException {
        return new String(readFile(file));
    }

    public static boolean updateFile(final File f1, final File f2) throws FileNotFoundException,
            NoSuchAlgorithmException, IOException, FileLockException {
        if(f1.exists()) {
            if(!FileSummation.compare(f1, f2)) {
                copyFile(f1, f2);
                return true;
            }
        }
        return false;
    }

    public static boolean writeObjectFile(final File file, final Object object) throws IOException, FileLockException {
        if(object == null) {
            return false;
        }

        ObjectOutputStream os = null;
        final FileLock fl = new FileLock(file);
        try {
            fl.lock();
            try {
                os = new ObjectOutputStream(new FileOutputStream(file));
                os.writeObject(object);
                return true;
            } catch(final IOException e) {
                return false;
            } finally {
                IOStreamUtils.closeQuietly(os);
            }
        } finally {
            fl.unlock();
        }
    }

    public static boolean writeFile(final File file, final byte[] content) throws IOException, FileLockException {
        if(content == null) {
            return false;
        }

        FileOutputStream os = null;
        final FileLock fl = new FileLock(file);
        try {
            fl.lock();
            final ByteArrayInputStream is = new ByteArrayInputStream(content);
            try {
                os = new FileOutputStream(file);
                IOStreamUtils.write(is, os);
                return true;
            } catch(final IOException e) {
                return false;
            } finally {
                IOStreamUtils.closeQuietly(is);
                IOStreamUtils.closeQuietly(os);
            }
        } finally {
            fl.unlock();
        }
    }

    public static boolean writeFile(final File file, final String content) throws IOException, FileLockException {
        if(content == null) {
            return false;
        }
        return writeFile(file, content.getBytes());
    }
}