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

class ListStoreTest {
  @Rule @JvmField val tempDir = TemporaryFolder().apply { create() }

  private fun newTestStore(): ListStore<TestData> =
      ListStore(tempDir.newFile(), TestData.converter, TestData::class.java)

  @Test fun getOnEmptyReturnsEmpty() {
    val store = newTestStore()
    assertThat(store.blockingGet()).isEmpty()
  }

  @Test fun addToEmptyList() {
    val store = newTestStore()
    val value = TestData("test", 1)
    store.add(value, Schedulers.trampoline())
    assertThat(store.blockingGet()).containsExactly(value)
  }

  @Test fun addToExistingList() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())
    assertThat(store.blockingGet()).isEqualTo(list)

    val newValue = TestData("3", 3)
    store.add(newValue, Schedulers.trampoline())
    assertThat(store.blockingGet()).isEqualTo(list.plus(newValue))
  }

  @Test fun removeFromList() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.remove(TestData("1", 1), Schedulers.trampoline())
    assertThat(store.blockingGet()).containsExactly(TestData("2", 2))
  }

  @Test fun removeFromListWithPredicate_oneItemRemoved() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.remove(Schedulers.trampoline()) { it.integer == 1 }
    assertThat(store.blockingGet()).containsExactly(TestData("2", 2))
  }

  @Test fun removeFromListWithPredicate_noItemRemoved() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.remove(Schedulers.trampoline()) { it.integer == 3 }
    assertThat(store.blockingGet()).isEqualTo(list)
  }

  @Test fun removeFromListByIndex() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.remove(0, Schedulers.trampoline())
    assertThat(store.blockingGet()).containsExactly(TestData("2", 2))
  }

  @Test fun replaceInList_itemReplaced() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.replace(TestData("3", 3), Schedulers.trampoline()) { it.integer == 2 }
    assertThat(store.blockingGet()).containsExactly(TestData("1", 1), TestData("3", 3))
  }

  @Test fun replaceInList_noItemReplaced() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.replace(TestData("3", 3), Schedulers.trampoline()) { it.integer == 3 }
    assertThat(store.blockingGet()).isEqualTo(list)
  }

  @Test fun addOrReplace_itemAdded() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.addOrReplace(TestData("3", 3), Schedulers.trampoline()) { it.integer == 3 }
    assertThat(store.blockingGet()).isEqualTo(list.plus(TestData("3", 3)))
  }

  @Test fun addOrReplace_itemReplaced() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    store.addOrReplace(TestData("3", 3), Schedulers.trampoline()) { it.integer == 2 }
    assertThat(store.blockingGet()).containsExactly(TestData("1", 1), TestData("3", 3))
  }

  @Test fun updatesToListTriggerObservable() {
    val store = newTestStore()
    val testObserver = store.observe().test()

    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val newValue = TestData("3", 3)
    store.add(newValue, Schedulers.trampoline())

    store.clear(Schedulers.trampoline())

    testObserver.assertValues(emptyList(),
                              list,
                              list.plus(newValue),
                              emptyList())
    testObserver.assertNotComplete()
  }

  @Test fun observePutProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    val producedList = store.observePut(list).timeout(1, SECONDS).blockingGet()
    assertThat(producedList).isEqualTo(list)
  }

  @Test fun observeAddProducesItem() {
    val store = newTestStore()
    val value = TestData("1", 1)
    val producedList = store.observeAdd(value).timeout(1, SECONDS).blockingGet()
    assertThat(producedList).containsExactly(value)
  }

  @Test fun observeReplaceProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeReplace(TestData("3", 3)) { it.integer == 2 }
        .timeout(1, SECONDS)
        .blockingGet()

    assertThat(producedList).containsExactly(TestData("1", 1), TestData("3", 3))
  }

  @Test fun observeAddOrReplaceProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeAddOrReplace(TestData("3", 3)) { it.integer == 3 }
        .timeout(1, SECONDS)
        .blockingGet()

    assertThat(producedList).isEqualTo(list.plus(TestData("3", 3)))
  }

  @Test fun observeRemoveByValueProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeRemove(TestData("1", 1))
        .timeout(1, SECONDS)
        .blockingGet()

    assertThat(producedList).containsExactly(TestData("2", 2))
  }

  @Test fun observeRemoveByIndexProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeRemove(0)
        .timeout(1, SECONDS)
        .blockingGet()

    assertThat(producedList).containsExactly(TestData("2", 2))
  }

  @Test fun observeRemoveByPredicateProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeRemove { it.integer == 1 }
        .timeout(1, SECONDS)
        .blockingGet()

    assertThat(producedList).containsExactly(TestData("2", 2))
  }

  @Test fun observeClearProducesItem() {
    val store = newTestStore()
    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())

    val producedList = store.observeClear().timeout(1, SECONDS).blockingGet()
    assertThat(producedList).isEmpty()
  }
}
