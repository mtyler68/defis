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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import static java.lang.Integer.valueOf;

/**
 * TODO: Remeber to use a PID for the auto-pilot.
 *
 * TODO: Apply a smoothing algorithm to changes in instrument values. This will prevent jumpiness in value changes due
 * to latency in sampling and behaves more like real analog instruments. Just need to determine how much time should be
 * used to animate between changes in value. As in, always use a constant short time, use a time based on the delta of
 * the new value, or use a time based on the a moving average of the ltency of data.
 *
 * @author Matthew Tyler
 */
public class SpeedTapeInstrument extends Parent
{

    private Canvas canvas;
    private double maxX;
    private double maxY;
    private DoubleProperty airSpeed = new SimpleDoubleProperty(0);

    private double tickSpacing = 4;
    private double tickStep = 2;

    public SpeedTapeInstrument() {
        canvas = new Canvas(60, 250);
        maxX = canvas.getWidth();
        maxY = canvas.getHeight();
        airSpeed.addListener(l -> update());

        getChildren().add(canvas);
        update();
    }

    public void update() {
//        System.out.println("SpeedTape.update(): " + getAirSpeed());
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.save();

        //gc.clearRect(0, 0, maxX, maxX);
        // Background
        gc.setFill(Color.gray(0.4));
        gc.fillRect(10, 0, maxX - 10, maxY);

        // Speed ticks
        gc.beginPath();
        gc.moveTo(maxX, 0);
        gc.lineTo(maxX, maxY);

        // -- Show up to 30 higher
        double currentSpeed = getAirSpeed();
        int tickSpeed = (int) currentSpeed + (int) (currentSpeed % 2);
        gc.setStroke(Color.WHITE);

        while (tickSpeed < currentSpeed + 40) {
            double yLoc = maxY / 2 - (tickSpeed - currentSpeed) * tickSpacing;
            gc.moveTo(maxX, yLoc);
            gc.lineTo(maxX - (tickSpeed % 10 == 0 ? 10 : 5), yLoc);

            // Draw the speed too
            if (tickSpeed % 10 == 0) {
                String label = Integer.toString(tickSpeed);
                gc.strokeText(label, maxX - 10 - 9 * label.length(), yLoc + 5);
            }

            tickSpeed += 2;
        }

        // -- Show down to 30 lower
        tickSpeed = (int) currentSpeed - (int) currentSpeed % 2;
        while (tickSpeed >= 0 && tickSpeed > currentSpeed - 40) {
            double yLoc = maxY / 2 + (currentSpeed - tickSpeed) * tickSpacing;
            gc.moveTo(maxX, yLoc);
            gc.lineTo(maxX - (tickSpeed % 10 == 0 ? 10 : 5), yLoc);

            // Draw the speed too
            if (tickSpeed % 10 == 0) {
                String label = Integer.toString(tickSpeed);
                gc.strokeText(label, maxX - 10 - 9 * label.length(), yLoc + 5);
            }

            tickSpeed -= 2;
        }

        // -- Stroke the tick marks
        gc.setStroke(Color.WHITE);
        gc.stroke();

        // Current Speed
        gc.beginPath();
        gc.translate(0, canvas.getHeight() / 2 - 20);
        gc.moveTo(0, 10);
        gc.lineTo(30, 10);
        gc.lineTo(30, 0);
        gc.lineTo(50, 0);

        gc.lineTo(50, 17);
        gc.lineTo(maxX, 20);
        gc.lineTo(50, 23);
        gc.lineTo(50, 40);

        gc.lineTo(30, 40);
        gc.lineTo(30, 30);
        gc.lineTo(0, 30);
        gc.closePath();

        gc.setStroke(Color.WHITE);
        gc.setFill(Color.BLACK);
        gc.fill();
        gc.stroke();
        gc.clip();

        //gc.translate(0, 0);
        // Display speed
        Font font = new Font(gc.getFont().getSize() + 8);
        gc.setFont(font);
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);

        double fracSpeed = currentSpeed - Math.floor(currentSpeed);
        String speedLabel = Integer.toString((int) Math.floor(currentSpeed));

        for (int ndx = 0; ndx < speedLabel.length(); ndx++) {
            int xLoc = 30 - 10 * (speedLabel.length() - ndx - 1);
            String digit = speedLabel.substring(ndx, ndx + 1);
            if (ndx == speedLabel.length() - 1) {
                int value = valueOf(digit);
                int beforeValue = (value - 1 < 0 ? 9 : value - 1);
                int afterValue = (value + 1 == 10 ? 0 : value + 1);
                int afterAfterValue = (afterValue + 1 == 10 ? 0 : afterValue + 1);

                gc.fillText(digit, xLoc, 27 + (fracSpeed * 20.5));
                gc.strokeText(digit, xLoc, 27 + (fracSpeed * 20.5));

                digit = Integer.toString(afterValue);
                gc.fillText(digit, xLoc, 27 + (fracSpeed * 20.5) - 20.5);
                gc.strokeText(digit, xLoc, 27 + (fracSpeed * 20.5) - 20.5);

                digit = Integer.toString(afterAfterValue);
                gc.fillText(digit, xLoc, 27 + (fracSpeed * 20.5) - 41);
                gc.strokeText(digit, xLoc, 27 + (fracSpeed * 20.5) - 41);

                if (currentSpeed >= 1) {
                    digit = Integer.toString(beforeValue);
                    gc.fillText(digit, xLoc, 27 + (fracSpeed * 20.5) + 20.5);
                    gc.strokeText(digit, xLoc, 27 + (fracSpeed * 20.5) + 20.5);
                }
            } else {
                gc.fillText(digit, xLoc, 27);
                gc.strokeText(digit, xLoc, 27);
            }
        }

        // Done
        gc.restore();
    }

    public double getAirSpeed() {
        return airSpeed.get();
    }

    public void setAirSpeed(double airSpeed) {
        this.airSpeed.set(airSpeed);
    }

    public DoubleProperty airSpeedProperty() {
        return airSpeed;
    }
}
