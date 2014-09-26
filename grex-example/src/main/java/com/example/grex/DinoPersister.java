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

package com.example.grex;

import android.content.Context;

import au.com.gridstone.grex.GRexPersister;
import rx.Observable;

/**
 * @author Christopher Horner
 */
public class DinoPersister extends GRexPersister {
    private static final String KEY_DINO = "dino";

    public DinoPersister(Context context) {
        super(context, "dinoPersister");
    }

    public Observable<Dino> getDino() {
        return get(KEY_DINO, Dino.class);
    }
}
