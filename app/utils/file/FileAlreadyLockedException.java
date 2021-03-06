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
 * FileLockAlreadyLockedException class
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

/**
 * @author Ricardo Lorenzo
 */
public class FileAlreadyLockedException extends FileLockException {
    private static final long serialVersionUID = 4431961009546335011L;

    public FileAlreadyLockedException() {
    }

    public FileAlreadyLockedException(String message) {
        super(message);
    }

    public FileAlreadyLockedException(Throwable cause) {
        super(cause);
    }

    public FileAlreadyLockedException(String message, Throwable cause) {
        super(message, cause);
    }

}
