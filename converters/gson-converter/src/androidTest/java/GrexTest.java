/*
 * Copyright (C) GRIDSTONE 2014
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import au.com.gridstone.grex.GRexAndroidPersister;
import au.com.gridstone.grex.converters.GsonConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Horner
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class GrexTest {
    @Test
    public void putAndGetSingleObject() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "singleObjectTest", new GsonConverter());

        TestData inData = new TestData("inData", 7);

        TestData putData = persister.put("inData", inData).toBlocking().single();
        assertThat(putData).isEqualTo(inData);

        TestData getData = persister.get("inData", TestData.class).toBlocking().single();
        assertThat(getData).isEqualTo(inData);
    }

    @Test
    public void putAndGetList() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "multiObjectTest", new GsonConverter());

        List<TestData> inList = new ArrayList<>(5);

        for (int i = 0; i < 5; i++) {
            TestData data = new TestData("test" + i, + i);
            inList.add(data);
        }

        List<TestData> putList = persister.putList("inList", inList, TestData.class).toBlocking().single();
        assertThat(putList).containsAll(inList);

        List<TestData> getList = persister.getList("inList", TestData.class).toBlocking().single();
        assertThat(getList).containsAll(inList);
    }

    @Test
    public void removeObjectFromList() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "removeObjectTest", new GsonConverter());

        TestData data1 = new TestData("test1", 1);
        TestData data2 = new TestData("test2", 2);
        List<TestData> inList = new ArrayList<>(2);
        inList.add(data1);
        inList.add(data2);

        List<TestData> putList = persister.putList("inList", inList, TestData.class).toBlocking().single();
        assertThat(putList).containsAll(inList);

        List<TestData> modifiedList = persister.removeFromList("inList", data1, TestData.class).toBlocking().single();
        assertThat(modifiedList)
                .contains(data2)
                .doesNotContain(data1);

        List<TestData> getList = persister.getList("inList", TestData.class).toBlocking().single();
        assertThat(getList)
                .contains(data2)
                .doesNotContain(data1);
    }

    @Test
    public void removeIndexFromList() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "removeIndexTest", new GsonConverter());

        TestData data1 = new TestData("test1", 1);
        TestData data2 = new TestData("test2", 2);
        List<TestData> inList = new ArrayList<>(2);
        inList.add(data1);
        inList.add(data2);

        List<TestData> putList = persister.putList("inList", inList, TestData.class).toBlocking().single();
        assertThat(putList).containsAll(inList);

        List<TestData> modifiedList = persister.removeFromList("inList", 0, TestData.class).toBlocking().single();
        assertThat(modifiedList)
                .contains(data2)
                .doesNotContain(data1);

        List<TestData> getList = persister.getList("inList", TestData.class).toBlocking().single();
        assertThat(getList)
                .contains(data2)
                .doesNotContain(data1);
    }

    @Test
    public void clear() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "clearTest", new GsonConverter());

        TestData inData = new TestData("test", 1);

        TestData putData = persister.put("inData", inData).toBlocking().single();
        assertThat(putData).isEqualTo(inData);

        boolean clearOperationSuccess = persister.clear("inData").toBlocking().single();
        assertThat(clearOperationSuccess).isTrue();

        boolean isEmpty = persister.get("inData", TestData.class).isEmpty().toBlocking().single();
        assertThat(isEmpty).isTrue();
    }

    @Test
    public void unknownKeyReturnsEmpty() {
        GRexAndroidPersister persister = new GRexAndroidPersister(Robolectric.application, "unknownKeyTest", new GsonConverter());

        boolean unknownObjectKeyEmpty = persister.get("unknownObj", TestData.class).isEmpty().toBlocking().single();
        assertThat(unknownObjectKeyEmpty).isTrue();

        boolean unknownListKeyEmpty = persister.getList("unknownList", TestData.class).isEmpty().toBlocking().single();
        assertThat(unknownListKeyEmpty).isTrue();
    }

    public static class TestData {
        public String string;
        public int integer;

        public TestData() {}

        public TestData(String string, int integer) {
            this.string = string;
            this.integer = integer;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TestData)) {
                return false;
            }

            TestData otherData = (TestData) o;

            if (string != null)
                return string.equals(otherData.string) && integer == otherData.integer;

            return otherData.string == null && integer == otherData.integer;
        }
    }
}
