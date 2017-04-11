/*
 * Copyright (C) GRIDSTONE 2017
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

package au.com.gridstone.rxstore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class WriteWrapper {
    protected static <T> void converterWrite(T value, Converter converter, Type type, File file)
            throws IOException {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        converter.write(value, type, tmpFile);

        // Blindly try to delete an existing file with the same name since renameTo will fail
        // to overwrite on some platforms
        file.delete();
        boolean success = tmpFile.renameTo(file);
        if (!success) throw new IOException("Rename operation on tmp file failed!");
    }

}
