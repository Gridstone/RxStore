/*
 * Copyright 2014 Omricat Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.gridstone.grex;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


/**
 * Class which can provide {@link java.io.Reader} and {@link java.io.Writer}
 * instances as well as clear any storage for a given key.
 * <p/>
 * Most uses of this interface will probably use the {@link FileIODelegate}
 * implementation.
 *
 * @author Joseph Cooper
 */
public interface IODelegate {

    /**
     * Returns a {@link java.io.Reader} to allow reading from the storage
     * associated with the given key.
     * <p/>
     * Should return null if and only if there is no storage for the given key.
     *
     * @param key the key to look up in storage.
     */
    public Reader getReader(final String key) throws IOException;

    /**
     * Returns a {@link java.io.Writer} to allow writing to the storage
     * associated with the given key.
     * <p/>
     * Must not return null.
     *
     * @param key the key to use to write to storage.
     */
    public Writer getWriter(final String key) throws IOException;

    /**
     * Deletes the storage associated to the the given key.
     *
     * @param key the key to clear
     * @return true if the storage was deleted
     */
    public boolean clear(final String key);

}
