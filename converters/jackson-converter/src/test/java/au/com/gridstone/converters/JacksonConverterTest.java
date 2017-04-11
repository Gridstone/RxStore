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

package au.com.gridstone.converters;

import au.com.gridstone.rxstore.ListStore;
import au.com.gridstone.rxstore.RxStore;
import au.com.gridstone.rxstore.ValueStore;
import au.com.gridstone.rxstore.converters.JacksonConverter;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.google.common.truth.Truth.assertThat;

/**
 * This must remain as Java rather than Kotlin because vanilla Jackson seems to have issues with
 * Kotlin's data classes.
 */
public final class JacksonConverterTest {
  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @Test public void convertValue() throws IOException {
    ValueStore<TestData> store =
        RxStore.value(tempDir.newFile(), new JacksonConverter(), TestData.class);

    assertThat(store.blockingGet()).isNull();

    TestData value = new TestData("Test1", 1);
    store.put(value, Schedulers.trampoline());
    assertThat(store.blockingGet()).isEqualTo(value);
  }

  @Test public void convertList() throws IOException {
    ListStore<TestData> store =
        RxStore.list(tempDir.newFile(), new JacksonConverter(), TestData.class);

    assertThat(store.blockingGet()).isEmpty();

    List<TestData> list = Arrays.asList(new TestData("Test1", 1), new TestData("Test2", 2));
    store.put(list, Schedulers.trampoline());
    assertThat(store.blockingGet()).isEqualTo(list);
  }

  public static class TestData {
    public String string;
    public int integer;

    public TestData() {
    }

    public TestData(String string, int integer) {
      this.string = string;
      this.integer = integer;
    }

    @Override public boolean equals(Object o) {
      if (!(o instanceof TestData)) {
        return false;
      }

      TestData otherData = (TestData) o;

      if (string != null) {
        return string.equals(otherData.string) && integer == otherData.integer;
      }

      return otherData.string == null && integer == otherData.integer;
    }

    @Override public String toString() {
      return string + "," + integer;
    }
  }
}
