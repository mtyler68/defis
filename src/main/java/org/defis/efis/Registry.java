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
package org.defis.efis;

import javafx.beans.Observable;

/**
 * Central hub for all telemetry and configuration settings. Back systems are managed by the SystemsManager.
 *
 * Among other things, the Registry creates an intermediary binding between the source and the consumer. This allows for
 * the publisher to unpublish without the subscriber requiring to re-subscribe. When a publisher publishes or
 * unpublishes, subscribers need to be notified so that they can change their in-op status accordingly.
 *
 * TODO: Make a protocol and protocol handler to receive FGFS data asynchronously. Should be faster and less invasive.
 *
 * TODO: Make a module manager that scans all classes at start-up for publisher and subscriber telemetry as well as
 * configuration settings.
 *
 * Do not apply smoothing at the registry level. Smoothing is for display purposes.
 *
 * TODO: Add /redundency directory where telemetry is mapped in priority order to fallback telemetry sources. For
 * example: /redundency/indicatedAirSpeed would be mapped to /pitot[0]/airspeed-kts,/pitot[1]/airspeed-kts
 *
 * /gps[0] = ADS-B In/Out GPS, /gps[1] = EFIS 1 GPS, /gps[2] = EFIS 2 GPS
 *
 * @author Matthew Tyler
 */
public class Registry
{

    public void publish(String path, Observable property) {

    }

    public void unpublish(String path) {

    }

    public Observable subscribe(String path) {
        return null;
    }

}
