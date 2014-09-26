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

package com.example.grex.helpers;

import com.example.grex.Dino;

import java.util.Random;

/**
 * @author Christopher Horner
 */
public class RandomDinoGenerator {
    private static final String[] NAMES = {
            "Twinkles", "Fluffy", "Tubs", "Bitey", "Rosie", "Dave", "Gregory", "Tabitha"
    };

    private RandomDinoGenerator() {
        //No instances
    }

    public static Dino spawn() {
        Random random = new Random();
        Dino dino = new Dino();

        dino.name = NAMES[random.nextInt(NAMES.length)];
        dino.armLength = random.nextInt(100);

        return dino;
    }
}
