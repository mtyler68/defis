package org.xefix.efis;

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
