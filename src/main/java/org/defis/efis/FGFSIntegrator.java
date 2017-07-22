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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.flightgear.fgfsclient.FGFSConnection;

/**
 * Integrates FGFS with xEFIS.
 *
 * @author Matthew Tyler
 */
public class FGFSIntegrator
{

    private DoubleProperty airSpeed = new SimpleDoubleProperty();
    private DoubleProperty bankAngle = new SimpleDoubleProperty();
    private DoubleProperty pitchAngle = new SimpleDoubleProperty();

    public DoubleProperty airSpeedProperty() {
        return airSpeed;
    }

    public DoubleProperty bankAnglProperty() {
        return bankAngle;
    }

    public DoubleProperty pitchAngProperty() {
        return pitchAngle;
    }

    public void update(FGFSConnection conn) {

        try {
            airSpeed.set(conn.getDouble("/velocities/airspeed-kt"));
            bankAngle.set(conn.getDouble("/orientation/roll-deg"));
            pitchAngle.set(conn.getDouble("/orientation/pitch-deg"));
        } catch (IOException ex) {
            Logger.getLogger(FGFSIntegrator.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
}
