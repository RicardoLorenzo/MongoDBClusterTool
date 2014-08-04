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
 * IOStreamUtils class
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.*;

/**
 * @author Ricardo Lorenzo
 */
public class IOStreamUtils {
    public final static int COMPRESSION_GZIP = 1;
    public final static int COMPRESSION_DEFLATE = 2;
    public final static int COMPRESSION_ZIP = 3;

    public static final void closeQuietly(final InputStream is) {
        try {
            if(is != null) {
                is.close();
            }
        } catch(NullPointerException e) {
            // nothing
        } catch(IOException e) {
            // nothing
        }
    }

    public static final void closeQuietly(final OutputStream os) {
        try {
            if(os != null) {
                os.close();
            }
        } catch(NullPointerException e) {
            // nothing
        } catch(IOException e) {
            // nothing
        }
    }

    /**
     * Compress stream into different formats
     *
     * @throws NoSuchMethodException , IOException
     */
    public static void compress(final int type, final InputStream is, final OutputStream os) throws IOException,
            NoSuchMethodException {
        switch(type) {
            case COMPRESSION_GZIP: {
                GZIPOutputStream gzipped = new GZIPOutputStream(os);
                try {
                    write(is, gzipped);
                    gzipped.flush();
                } finally {
                    closeQuietly(gzipped);
                }
            }
            case COMPRESSION_DEFLATE: {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                DeflaterOutputStream deflated = new DeflaterOutputStream(output, new Deflater(
                        Deflater.DEFAULT_COMPRESSION, true));
                try {
                    write(is, deflated);
                    deflated.flush();
                } finally {
                    closeQuietly(deflated);
                }
            }
            case COMPRESSION_ZIP: {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zipped = new ZipOutputStream(output);
                try {
                    write(is, zipped);
                    zipped.flush();
                } finally {
                    closeQuietly(zipped);
                }
            }
            default:
                throw new NoSuchMethodException();
        }
    }

    /**
     * Decompress stream from different formats
     *
     * @throws DataFormatException
     * @throws NoSuchMethodException
     */
    public static void decompress(final int type, final InputStream is, final OutputStream os) throws IOException,
            NoSuchMethodException {
        switch(type) {
            case COMPRESSION_GZIP: {
                GZIPInputStream gzipped = new GZIPInputStream(is);
                try {
                    write(gzipped, os);
                } finally {
                    closeQuietly(gzipped);
                }
            }
            case COMPRESSION_DEFLATE: {
                InflaterOutputStream inflated = new InflaterOutputStream(os, new Inflater(false));
                try {
                    write(is, inflated);
                    inflated.flush();
                } finally {
                    closeQuietly(inflated);
                }

            }
            case COMPRESSION_ZIP: {
                ZipInputStream zipped = new ZipInputStream(is);
                try {
                    write(zipped, os);
                } finally {
                    closeQuietly(zipped);
                }
            }
            default:
                throw new NoSuchMethodException();
        }
    }

    private static int indexOf(byte c, byte[] array, int start) throws Exception {
        for(int i = start; i < array.length; i++) {
            if(new Byte(array[i]).intValue() == (int) c) {
                return i;
            }
        }
        return -1;
    }

    public static final byte[] read(final InputStream is, final int length) throws IOException {
        byte[] data = new byte[length];
        is.read(data, 0, data.length);
        return data;
    }

    public static final byte[] readUntilDataIsFound(final InputStream is, final byte[] data, Long maximumLength) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while(true) {
            int offset = 0;
            int c = is.read();
            out.write(c);
            for(; c == data[offset]; offset++) {
                if(offset == (data.length - 1)) {
                    return out.toByteArray();
                }
                c = is.read();
                if(out.size() >= maximumLength) {
                    return out.toByteArray();
                }
            }
            if(out.size() >= maximumLength) {
                return out.toByteArray();
            }
        }
    }

    public static final void write(final InputStream is, final OutputStream os, long length) throws IOException {
        long offset = 0;
        for(; offset < length; offset++) {
            os.write(is.read());
        }
        os.flush();
    }

    public static final void write(final InputStream is, final OutputStream os) throws IOException {
        byte[] buffer = new byte[2048];
        for(int rlenght = is.read(buffer); rlenght > 0; rlenght = is.read(buffer)) {
            os.write(buffer, 0, rlenght);
        }
        os.flush();
    }

    public static final void write(byte[] data, final OutputStream os) throws IOException {
        os.write(data, 0, data.length);
        os.flush();
    }

    public static final void write(final String content, final OutputStream os) throws IOException {
        os.write(content.getBytes());
    }
}
