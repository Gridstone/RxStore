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

package au.com.gridstone.rxstore

import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit.SECONDS

class ValueStoreTest {
  @Rule @JvmField val tempDir = TemporaryFolder().apply { create() }

  private fun newTestStore(): ValueStore<TestData> =
      ValueStore(tempDir.newFile(), TestData.converter, TestData::class.java)

  private fun TestData.asUpdate(): ValueStore.ValueUpdate<TestData> = ValueStore.ValueUpdate(this)

  @Test fun putAndClear() {
    val store = newTestStore()
    val value = TestData("test", 1)
    store.put(value, Schedulers.trampoline())
    assertThat(store.blockingGet()).isEqualTo(value)

    store.clear(Schedulers.trampoline())
    assertThat(store.blockingGet()).isNull()
  }

  @Test fun getSucceedsWithValueAndCompletesWithout() {
    val store = newTestStore()

    store.get().test()
        .assertComplete()
        .assertNoValues()

    val value = TestData("test", 1)
    store.put(value, Schedulers.trampoline())

    store.get().test()
        .assertComplete()
        .assertValue(value)
  }

  @Test fun blockingGetOnEmptyReturnsNull() {
    val store = newTestStore()
    assertThat(store.blockingGet()).isNull()
  }

  @Test fun updatesTriggerObservable() {
    val store = newTestStore()
    val value1 = TestData("test", 1)
    val value2 = TestData("test", 2)

    val testObserver = store.observe().test()

    store.put(value1, Schedulers.trampoline())
    store.put(value2, Schedulers.trampoline())
    store.clear(Schedulers.trampoline())

    testObserver.assertValues(ValueStore.ValueUpdate.empty(),
                              value1.asUpdate(),
                              value2.asUpdate(),
                              ValueStore.ValueUpdate.empty())
    testObserver.assertNotComplete()
  }

  @Test fun observePutProducesItem() {
    val value = TestData("test", 1)
    val store = newTestStore()
    val producedValue = store.observePut(value).timeout(1, SECONDS).blockingGet()
    assertThat(producedValue).isEqualTo(value)
  }

  @Test fun observeClearCompletes() {
    val value = TestData("test", 1)
    val store = newTestStore()
    store.put(value, Schedulers.trampoline())
    assertThat(store.blockingGet()).isEqualTo(value)

    val testObserver = store.observeClear().subscribeOn(Schedulers.trampoline()).test()
    testObserver.assertComplete()
  }
}
