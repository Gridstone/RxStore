/*
 * Copyright 2014 Joseph Cooper
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


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


/**
 * Implementation of IODelegate which returns {@link java.io.FileReader}
 * and {@link java.io.FileWriter}.
 */
public class FileIODelegate implements IODelegate {

    private final File directory;

    public FileIODelegate(final File directory) {
        this.directory = directory;
    }

    // I would massively prefer to use something like Optional<Reader> as the
    // return type here, but I don't want to pull in a huge dependency like
    // Guava and this needs to be Java 7 compatible for Android use.
    @Override
    public Reader getReader(final String key) throws IOException {
        final File file = getFile(key);
        if (!file.exists()) {
            return null; //eurgh, returning null!
        } else {
            return new FileReader(file);
        }
    }

    @Override
    public Writer getWriter(final String key) throws IOException {
        return new FileWriter(getFile(key));
    }

    @Override public boolean clear(final String key) {
        return getFile(key).delete();
    }

    private File getFile(final String key) {
        return new File(directory, key);
    }
}
