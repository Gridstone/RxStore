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

import java.io.File
import java.lang.reflect.Type

data class TestData(val string: String, val integer: Int) {
  override fun toString() = "$string,$integer"

  companion object {
    fun fromString(string: String): TestData {
      val splitString = string.split(",")
      return TestData(splitString[0], splitString[1].toInt())
    }

    @Suppress("UNCHECKED_CAST") // Special converter just for testing. Casts will always work.
    val converter = object : Converter {
      override fun <T> write(data: T?, type: Type, file: File) {
        when (data) {
          null -> file.writeText("")
          is TestData -> file.writeText(data.toString())
          is List<*> -> {
            val list = data as List<T>
            // Separate each TestData instance by a "~" character.
            val listAsText = list.map { it.toString() }.reduce { acc, item -> "$acc~$item" }
            file.writeText(listAsText)
          }
        }
      }

      override fun <T> read(file: File, type: Type): T? {
        val storedString = file.readText()
        if (storedString.isBlank()) return null

        if (type is ListStore.ListType) {
          // Stored string contains each TestData separated by a "~" character.
          val splitString = storedString.split("~")
          val list = splitString.map { TestData.fromString(it) }

          return list as T
        } else {
          return TestData.fromString(storedString) as T
        }
      }
    }
  }
}
