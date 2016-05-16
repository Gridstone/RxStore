/*
 * Copyright (C) GRIDSTONE 2016
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

import au.com.gridstone.rxstore.StoreProvider;
import au.com.gridstone.rxstore.StoreProvider.ListStore;
import au.com.gridstone.rxstore.StoreProvider.ValueStore;
import au.com.gridstone.rxstore.converters.JacksonConverter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rx.schedulers.Schedulers;

import static com.google.common.truth.Truth.assertThat;

public final class JacksonConverterTest {
  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private StoreProvider storeProvider;

  @Before public void setup() throws IOException {
    storeProvider = StoreProvider.with(tempDir.newFolder("jacksonTest"))
        .schedulingWith(Schedulers.immediate())
        .using(new JacksonConverter());
  }

  @Test public void convertValue() {
    ValueStore<TestData> store = storeProvider.valueStore("value", TestData.class);
    assertThat(store.getBlocking()).isNull();

    TestData value = new TestData("Test1", 1);
    store.put(value);
    assertThat(store.getBlocking()).isEqualTo(value);

    store.clear();
    assertThat(store.getBlocking()).isNull();
  }

  @Test public void convertList() {
    ListStore<TestData> store = storeProvider.listStore("list", TestData.class);
    assertThat(store.getBlocking()).isEmpty();

    List<TestData> list = Arrays.asList(new TestData("Test1", 1), new TestData("Test2", 2));
    store.put(list);
    assertThat(store.getBlocking()).isEqualTo(list);

    store.clear();
    assertThat(store.getBlocking()).isEmpty();
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
