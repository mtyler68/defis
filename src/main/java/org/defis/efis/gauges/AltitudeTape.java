/*
 *  Copyright 2017 DEFIS
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.defis.efis.gauges;

import static org.defis.efis.gauges.DisplayOrientation.LEFT;

/**
 * TODO: Think about the different types of altitude to show (pressure, density, true, absolute) TODO: Should this
 * include the Altimeter setting display (Kollsman window?)
 *
 * @author Matthew Tyler
 */
public class AltitudeTape extends AbstractTapeGauge
{

    public AltitudeTape(double width, double height) {
        super(width, height, LEFT, 100, 20, 500, 20);
        setIndicatorLabelFormat("%03.0f");
        setNegativeDrawn(true);
        update();
    }
}
