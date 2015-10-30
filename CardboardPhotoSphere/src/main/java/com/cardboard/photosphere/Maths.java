/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cardboard.photosphere;

/**
 * @author Sree Kumar A.V
 */

public final class Maths {

    /**
     * 180 in radians.
     */
    public static final double ONE_EIGHTY_DEGREES = Math.PI;

    /**
     * 360 in radians.
     */
    public static final double THREE_SIXTY_DEGREES = ONE_EIGHTY_DEGREES * 2;

    /**
     * 120 in radians.
     */
    public static final double ONE_TWENTY_DEGREES = THREE_SIXTY_DEGREES / 3;

    /**
     * 90 degrees, North pole.
     */
    public static final double NINETY_DEGREES = Math.PI / 2;

    /**
     * Used by power.
     */
    private static final long POWER_CLAMP = 0x00000000ffffffffL;

    /**
     * Constructor, although not used at the moment.
     */
    private Maths() {
    }

    /**
     * Quick integer power function.
     *
     * @param base  number to raise.
     * @param raise to this power.
     * @return base ^ raise.
     */
    public static int power(final int base, final int raise) {
        int p = 1;
        long b = raise & POWER_CLAMP;

        // bits in b correspond to values of powerN
        // so start with p=1, and for each set bit in b, multiply corresponding
        // table entry
        long powerN = base;

        while (b != 0) {
            if ((b & 1) != 0) {
                p *= powerN;
            }
            b >>>= 1;
            powerN = powerN * powerN;
        }

        return p;
    }
}
