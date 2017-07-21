/*
 *  Copyright 2017 xefis
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
package org.xefix.efis.instrument;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import static javafx.geometry.VPos.CENTER;

/**
 * TODO: Add display altitude as FL (would be implemented at generate label in subclass).
 *
 * @author matthewt
 */
public abstract class AbstractTapeInstrument extends Parent
{

    private final DoubleProperty value = new SimpleDoubleProperty(0);
    private final Canvas canvas;
    private DisplayDirection displayDirection = DisplayDirection.LEFT;
    private double tapeWidth;
    private Color backgroundColor = Color.gray(0.4);
    private double unitsToMinorTick = 20;
    private double unitsToMajorTick = 100;
    private boolean negativeDrawn = true;

    /**
     * The number of units shown from one end of the tape to the other. This allows the tape to calculate its display
     * scale.
     */
    private double unitsShown = 500;

    protected AbstractTapeInstrument(double width, double height) {
        canvas = new Canvas(width, height);
        tapeWidth = width - 10;

        value.addListener(l -> update());

        getChildren().add(canvas);
        update();
    }

    protected void update() {
        GraphicsContext gc = getGraphicsContext();

        clear(gc);

        gc.save();
        drawBackground(gc);
        gc.restore();

        gc.save();
        drawTicks(gc);
        gc.restore();
    }

    protected GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public double getWidth() {
        return canvas.getWidth();
    }

    public double getHeight() {
        return canvas.getHeight();
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public DisplayDirection getDisplayDirection() {
        return displayDirection;
    }

    public double getTapeWidth() {
        return tapeWidth;
    }

    private void clear(GraphicsContext gc) {
        gc.clearRect(0, 0, getWidth(), getHeight());
    }

    private void drawBackground(GraphicsContext gc) {
        gc.setFill(backgroundColor);

        double tx = 0, ty = 0, width = 0, height = 0;
        switch (getDisplayDirection()) {
            case RIGHT:
                tx = getWidth() - tapeWidth;
            case LEFT:
                width = tapeWidth;
                height = getHeight();
                break;
            case DOWN:
                ty = getHeight() - tapeWidth;
            case UP:
                width = getWidth();
                height = tapeWidth;
        }

        gc.translate(tx, ty);
        gc.fillRect(0, 0, width, height);
    }

    public double getUnitsToMinorTick() {
        return unitsToMinorTick;
    }

    public void setUnitsToMinorTick(double unitsToMinorTick) {
        this.unitsToMinorTick = unitsToMinorTick;
    }

    public double getUnitsToMajorTick() {
        return unitsToMajorTick;
    }

    public void setUnitsToMajorTick(double unitsToMajorTick) {
        this.unitsToMajorTick = unitsToMajorTick;
    }

    public boolean isNegativeDrawn() {
        return negativeDrawn;
    }

    public void setNegativeDrawn(boolean negativeDrawn) {
        this.negativeDrawn = negativeDrawn;
    }

    public double getUnitsShown() {
        return unitsShown;
    }

    public void setUnitsShown(double unitsShown) {
        this.unitsShown = unitsShown;
    }

    public double getValue() {
        return value.get();
    }

    private void drawTicks(GraphicsContext gc) {
        double pixelsToUnit = calcDisplayRange() / getUnitsShown();
        double tickDir = 1, tx = 0, ty = 0;
        gc.setTextBaseline(CENTER);
        gc.setTextAlign(TextAlignment.LEFT);

        switch (getDisplayDirection()) {
            case RIGHT:
                tx = getWidth();
                tickDir = -1;
                gc.setTextAlign(TextAlignment.RIGHT);
            case LEFT:
                ty = getHeight() / 2;
                break;
            case DOWN:
                tickDir = -1;
                ty = getHeight();
            case UP:
                tx = getWidth() / 2;
        }

        gc.translate(tx, ty);
        gc.beginPath();
        gc.setStroke(Color.WHITE);

        for (double displayValue = getValue() - getUnitsShown() / 1.75; displayValue < getValue() + getUnitsShown() / 1.75; displayValue += getUnitsToMinorTick()) {

            double normalizedValue = displayValue - (displayValue % getUnitsToMinorTick());

            // TODO: Note only drawing for vertical tape display
            double tickLoc = pixelsToUnit * (getValue() - normalizedValue);
            double tickLength = normalizedValue % getUnitsToMajorTick() == 0 ? 10 : 5;

            gc.moveTo(0, tickLoc);
            gc.lineTo(tickDir * tickLength, tickLoc);

            String label = generateValueLabel(normalizedValue);
            if (normalizedValue % getUnitsToMajorTick() == 0) {
                gc.strokeText(label,
                        tickLength * tickDir + 3 * tickDir, tickLoc - 1);
            }
        }

        gc.stroke();
    }

    protected String generateValueLabel(double val) {
        return String.format("%(,.0f", val);
    }

    /**
     * Determines the width or height based on the display direction.
     *
     * @return
     */
    protected double calcDisplayRange() {
        switch (getDisplayDirection()) {
            case LEFT:
            case RIGHT:
                return getHeight();
            default:
                return getWidth();
        }
    }

}
