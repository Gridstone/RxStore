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

package au.com.gridstone.rxstore.converters

import au.com.gridstone.rxstore.RxStore
import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MoshiConverterTest {
  @Rule @JvmField val tempDir = TemporaryFolder().apply { create() }

  @Test fun convertValue() {
    val store = RxStore.value<TestData>(tempDir.newFile(), MoshiConverter(), TestData::class.java)
    assertThat(store.blockingGet()).isNull()

    store.put(TestData("1", 1), Schedulers.trampoline())
    assertThat(store.blockingGet()).isEqualTo(TestData("1", 1))
  }

  @Test fun convertList() {
    val store = RxStore.list<TestData>(tempDir.newFile(), MoshiConverter(), TestData::class.java)
    assertThat(store.blockingGet()).isEmpty()

    val list = listOf(TestData("1", 1), TestData("2", 2))
    store.put(list, Schedulers.trampoline())
    assertThat(store.blockingGet()).containsExactly(TestData("1", 1), TestData("2", 2))
  }

  data class TestData(val string: String, val integer: Int)
}