/*
 * Copyright 2015 Joseph Cooper
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;

public class FileIODelegateTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void testGetReader_NonExistentFile() throws Exception {
        IODelegate delegate = new FileIODelegate(tmpDir.newFolder());

        Reader ret = delegate.getReader("non-existent");

        assertThat(ret).isNull();
    }

    @Test
    public void testGetReaderReturnsFileReader() throws Exception {
        tmpDir.newFile("TestKey");
        Reader ret = new FileIODelegate(tmpDir.getRoot()).getReader("TestKey");

        assertThat(ret).isInstanceOf(FileReader.class);
    }

    @Test
    public void testGetWriterReturnsFileWriter() throws Exception {
        tmpDir.newFile("TestKey");
        Writer ret = new FileIODelegate(tmpDir.getRoot()).getWriter("TestKey");

        assertThat(ret).isInstanceOf(FileWriter.class);
    }

    @Test
    public void testWriteDataThenClear() throws IOException {

        final FileIODelegate delegate =
                new FileIODelegate(tmpDir.getRoot());

        delegate.getWriter("TestKey").append("Test data").close();

        boolean ret = delegate.clear("TestKey");

        assertThat(ret).isTrue();

        assertThat(delegate.getReader("TestKey")).isNull();
    }
}
