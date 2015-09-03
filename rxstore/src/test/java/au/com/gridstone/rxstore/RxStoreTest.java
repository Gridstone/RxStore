/*
 * Copyright (C) GRIDSTONE 2015
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class RxStoreTest {
  private static final String TEST_DIR_NAME = "rxStoreTest";

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private RxStore rxStore;

  @Before public void setup() throws IOException, ConverterException {
    Converter converter = Mockito.mock(Converter.class);

    // Mock Converter.write() that can only write TestData or List<TestData>.
    doAnswer(new Answer<Void>() {
      @Override public Void answer(InvocationOnMock invocation) throws IOException {
        Object[] args = invocation.getArguments();

        Writer writer = (Writer) args[1];

        if (args[0] instanceof TestData) {
          TestData testData = (TestData) args[0];
          writer.write(testData.toString());
        } else if (args[0] instanceof List) {
          @SuppressWarnings("unchecked") List<TestData> dataList = (List<TestData>) args[0];

          for (int i = 0, n = dataList.size(); i < n; i++) {
            if (i != 0) {
              // Separate each TestData instance by a "~" character.
              writer.write("~");
            }

            writer.write(dataList.get(i).toString());
          }
        }

        return null;
      }
    }).when(converter).write(any(), any(Writer.class));

    // Mock Converter.read() that can only read TestData or List<TestData>.
    when(converter.read(any(Reader.class), any(Type.class))).thenAnswer(new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        Reader reader = (Reader) args[0];
        Type type = (Type) args[1];
        String string = new BufferedReader(reader).readLine();

        if (type instanceof RxStore.ListOfSomething) {
          // Stored string contains each TestData separated by a "~" character.
          String[] splitString = string.split("~");
          List<TestData> list = new ArrayList<>(splitString.length);

          for (String itemString : splitString) {
            list.add(TestData.fromString(itemString));
          }

          return list;
        } else {
          return TestData.fromString(string);
        }
      }
    });

    rxStore = RxStore.with(tempDir.newFolder(TEST_DIR_NAME)).using(converter);
  }

  @Test public void putReturnsSameObject() throws ConverterException {
    TestData inData = new TestData("test", 1);
    TestData outData = rxStore.put("testKey", inData).toBlocking().single();

    assertThat(inData).isEqualTo(outData);
  }

  @Test public void getReturnsPutObject() {
    TestData inData = new TestData("test", 1);
    rxStore.put("testKey", inData).toBlocking().single();
    TestData outData = rxStore.get("testKey", TestData.class).toBlocking().single();

    assertThat(inData).isEqualTo(outData);
  }

  @Test public void getUnknownReturnsEmpty() {
    boolean empty = rxStore.get("unknown", TestData.class).isEmpty().toBlocking().single();
    assertThat(empty).isTrue();
  }

  @Test public void clearDeletesFile() {
    TestData testData = new TestData("test", 1);
    rxStore.put("testKey", testData).toBlocking().single();
    boolean clearResult = rxStore.clear("testKey").toBlocking().single();

    assertThat(clearResult).isTrue();

    File testDir = new File(tempDir.getRoot(), TEST_DIR_NAME);
    assertThat(testDir.list()).doesNotContain("testKey");
  }

  @Test public void putListReturnsSameList() {
    List<TestData> inList = createTestList();
    List<TestData> putList =
        rxStore.putList("testList", inList, TestData.class).toBlocking().single();

    assertThat(putList).containsAll(inList);
  }

  @Test public void getListReturnsPutList() {
    List<TestData> inList = createTestList();
    rxStore.putList("testList", inList, TestData.class).toBlocking().single();
    List<TestData> outList = rxStore.getList("testList", TestData.class).toBlocking().single();

    assertThat(outList).containsAll(inList);
  }

  @Test public void getUnknownListReturnsEmpty() {
    boolean empty = rxStore.getList("unknownList", TestData.class).isEmpty().toBlocking().single();
    assertThat(empty).isTrue();
  }

  @Test public void addToList() {
    List<TestData> inList = createTestList();
    rxStore.putList("testList", inList, TestData.class).toBlocking().single();

    TestData appendData = new TestData("appendData", 37);
    List<TestData> appendedList =
        rxStore.addToList("testList", appendData, TestData.class).toBlocking().single();

    assertThat(appendedList).contains(appendData);

    List<TestData> retrievedAppendList =
        rxStore.getList("testList", TestData.class).toBlocking().single();

    assertThat(retrievedAppendList).contains(appendData);
  }

  @Test public void removeObjectFromList() {
    List<TestData> inList = new ArrayList<>(2);
    TestData dataToRemove = new TestData("dataToRemove", 1);
    inList.add(dataToRemove);
    TestData dataToKeep = new TestData("dataToKeep", 2);
    inList.add(dataToKeep);

    rxStore.putList("testList", inList, TestData.class).toBlocking().single();
    List<TestData> modifiedList =
        rxStore.removeFromList("testList", dataToRemove, TestData.class).toBlocking().single();

    assertThat(modifiedList).contains(dataToKeep);
    assertThat(modifiedList).doesNotContain(dataToRemove);

    List<TestData> storedModifiedList =
        rxStore.getList("testList", TestData.class).toBlocking().single();

    assertThat(storedModifiedList).contains(dataToKeep);
    assertThat(storedModifiedList).doesNotContain(dataToRemove);
  }

  @Test public void removeByIndexFromList() {
    List<TestData> inList = new ArrayList<>(2);
    TestData dataToRemove = new TestData("dataToRemove", 1);
    inList.add(dataToRemove);
    TestData dataToKeep = new TestData("dataToKeep", 2);
    inList.add(dataToKeep);

    rxStore.putList("testList", inList, TestData.class).toBlocking().single();

    List<TestData> modifiedList =
        rxStore.removeFromList("testList", 0, TestData.class).toBlocking().single();

    assertThat(modifiedList).contains(dataToKeep);
    assertThat(modifiedList).doesNotContain(dataToRemove);

    List<TestData> storedModifiedList =
        rxStore.getList("testList", TestData.class).toBlocking().single();

    assertThat(storedModifiedList).contains(dataToKeep);
    assertThat(storedModifiedList).doesNotContain(dataToRemove);
  }

  @Test public void addToUnknownListCreatesNew() {
    boolean initiallyEmpty =
        rxStore.getList("newList", TestData.class).isEmpty().toBlocking().single();

    assertThat(initiallyEmpty).isTrue();

    TestData testData = new TestData("test", 1);

    List<TestData> modifiedList =
        rxStore.addToList("newList", testData, TestData.class).toBlocking().single();

    assertThat(modifiedList.contains(testData));

    List<TestData> storedList = rxStore.getList("newList", TestData.class).toBlocking().single();

    assertThat(storedList).contains(testData);
  }

  @Test public void removeFromUnknownListReturnsEmpty() {
    TestData testData = new TestData("test", 1);
    boolean empty = rxStore.removeFromList("unknownList", testData, TestData.class)
        .isEmpty()
        .toBlocking()
        .single();

    assertThat(empty).isTrue();

    empty =
        rxStore.removeFromList("unknownList", 0, TestData.class).isEmpty().toBlocking().single();

    assertThat(empty).isTrue();
  }

  @Test public void nullDirectoryFails() {
    try {
      RxStore.with((File) null);
      failBecauseExceptionWasNotThrown(NullPointerException.class);
    } catch (NullPointerException e) {
      assertThat(e).hasMessageContaining("null");
    }
  }

  @Test public void nullConverterFails() {
    try {
      File testDir = new File(tempDir.getRoot(), TEST_DIR_NAME);
      RxStore.with(testDir).using(null);
      failBecauseExceptionWasNotThrown(NullPointerException.class);
    } catch (NullPointerException e) {
      assertThat(e).hasMessageContaining("null");
    }
  }

  private static List<TestData> createTestList() {
    List<TestData> list = new ArrayList<>(5);

    for (int i = 0; i < 5; i++) {
      TestData data = new TestData("test" + i, i);
      list.add(data);
    }

    return list;
  }

  static class TestData {
    public final String string;
    public final int integer;

    TestData(String string, int integer) {
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

    public static TestData fromString(String string) {
      String[] splitString = string.split(",");
      return new TestData(splitString[0], Integer.parseInt(splitString[1]));
    }
  }
}
