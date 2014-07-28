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
 * FileLock class
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

import java.io.File;
import java.io.IOException;

/**
 * @author Ricardo Lorenzo
 */
public class FileLock {
    private final File lock_file;

    public FileLock(final File file) {
        this.lock_file = new File(file.getAbsolutePath() + ".lock");
    }

    private void checkLock() throws FileLockException, FileAlreadyLockedException, InterruptedException {
        mkdirParent();
        for(int i = 16; --i >= 0; ) {
            if(!this.lock_file.exists()) {
                return;
            }
            Thread.sleep(250L);
        }
        throw new FileAlreadyLockedException("permanently locked file");
    }

    private void mkdirParent() throws FileLockException {
        File parent = this.lock_file.getParentFile();
        if(parent != null && !parent.exists()) {
            if(parent.mkdirs()) {
                throw new FileLockException("cannot create parent directory [" + parent.getAbsolutePath() + "]");
            }
        }
    }

    public void lock() throws FileLockException {
        try {
            checkLock();
        } catch(final InterruptedException e) {
            throw new FileLockException("fail to sleep thread in check");
        }
        try {
            this.lock_file.createNewFile();
        } catch(final IOException e) {
            throw new FileLockException("cannot create file lock");
        }
    }

    public void unlock() throws FileLockException {
        if(this.lock_file.exists()) {
            if(!this.lock_file.delete()) {
                throw new FileLockException("cannot delete file lock");
            }
        }
    }

    public void unlockQuietly() {
        if(this.lock_file.exists()) {
            if(!this.lock_file.delete()) {
                // nothing
            }
        }
    }
}