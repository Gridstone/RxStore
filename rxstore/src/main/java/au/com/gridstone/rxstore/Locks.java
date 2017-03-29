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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class Locks {
  private Locks() {
    throw new AssertionError("No instances.");
  }

  static void runInReadLock(ReentrantReadWriteLock readWriteLock, ThrowingRunnable runnable) {
    Lock readLock = readWriteLock.readLock();
    readLock.lock();

    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      readLock.unlock();
    }
  }

  static void runInWriteLock(ReentrantReadWriteLock readWriteLock, ThrowingRunnable runnable) {
    Lock readLock = readWriteLock.readLock();
    int readCount = readWriteLock.getWriteHoldCount() == 0 ? readWriteLock.getReadHoldCount() : 0;

    for (int i = 0; i < readCount; i++) {
      readLock.unlock();
    }

    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      for (int i = 0; i < readCount; i++) {
        readLock.lock();
      }
      writeLock.unlock();
    }
  }
}
