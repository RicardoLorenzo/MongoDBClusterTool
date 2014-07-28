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

package conf;

import play.Play;

/**
 * @author Ricardo Lorenzo <ricardo.lorenzo@mongodb.com>
 */
public class PlayConfiguration {

    public static final String getProperty(final String key) {
        return Play.application().configuration().getString(key);
    }

    public static final boolean hasProperty(final String key) {
        return Play.application().configuration().getString(key) != null ? true : false;
    }
}
