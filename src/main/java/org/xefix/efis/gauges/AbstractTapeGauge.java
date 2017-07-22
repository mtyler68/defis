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
package org.xefix.efis.gauges;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static lombok.AccessLevel.PROTECTED;

/**
 * TODO: Add display altitude as FL (would be implemented at generate label in subclass).
 *
 * @author matthewt
 */
public abstract class AbstractTapeGauge extends Parent
{

    @Accessors(fluent = true)
    @Getter
    private final DoubleProperty valueProperty = new SimpleDoubleProperty(0);

    @Getter(PROTECTED)
    private final Canvas canvas;

    @Getter(PROTECTED)
    private final DisplayOrientation orientation;

    @Getter(PROTECTED)
    private final double unitsToMinorTick;

    @Getter(PROTECTED)
    private final double unitsToMajorTick;

    /**
     * The number of units shown from one end of the tape to the other. This allows the tape to calculate its display
     * scale.
     */
    @Getter(PROTECTED)
    private final double visibleRange;

    @Getter(PROTECTED)
    private double tapeWidth;

    @Getter(PROTECTED)
    private Color backgroundColor = Color.gray(0.4);

    @Getter(PROTECTED)
    private boolean negativeDrawn = true;

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String tickLabelFormat = "%(,.0f";

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String indicatorLabelFormat = "%(,.0f";

    @Getter(PROTECTED)
    private Setup setup;

    protected AbstractTapeGauge(double width, double height, DisplayOrientation orientation,
            double unitsToMajorTick, double unitsToMinorTick, double visibleRange) {
        canvas = new Canvas(width, height);
        this.orientation = orientation;
        this.unitsToMajorTick = unitsToMajorTick;
        this.unitsToMinorTick = unitsToMinorTick;
        this.visibleRange = visibleRange;

        tapeWidth = width - 10;

        valueProperty.addListener(l -> update());

        getChildren().add(canvas);
        update();
    }

    protected void update() {
        if (setup == null) {
            initialize();
        }

        GraphicsContext gc = getGraphicsContext();

        clear(gc);

        gc.save();
        drawBackground(gc);
        gc.restore();

        gc.save();
        drawTicks(gc);
        gc.restore();

        gc.save();
        drawIndicator(gc);
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

    /**
     * Clears the entire gauge area.
     *
     * @param gc
     */
    protected void clear(GraphicsContext gc) {
        gc.clearRect(0, 0, getWidth(), getHeight());
    }

    protected void drawBackground(GraphicsContext gc) {
        gc.setFill(getBackgroundColor());
        gc.translate(getSetup().bgTx, getSetup().bgTy);
        gc.fillRect(0, 0, getSetup().bgWidth, getSetup().bgHeight);
    }

    /**
     * Returns the value to display.
     *
     * @return
     */
    public double getValue() {
        return valueProperty.get();
    }

    protected void drawTicks(GraphicsContext gc) {

        // TODO: All these values can be pre-calculated once and re-used during the lifetime of the gauge
        gc.translate(setup.tX, setup.tY);
        gc.setStroke(Color.WHITE);
        gc.setTextAlign(setup.textAlignment);
        gc.setTextBaseline(setup.textBaseline);

        gc.beginPath();

        for (double displayValue = getValue() - getVisibleRange() / 1.75;
                displayValue < getValue() + getVisibleRange() / 1.75;
                displayValue += getUnitsToMinorTick()) {

            double normalizedValue = displayValue - (displayValue % getUnitsToMinorTick());

            // TODO: Note only drawing for vertical tape display
            double tickLoc = getSetup().pixelsToUnit * (getValue() - normalizedValue);
            double tickLength = normalizedValue % getUnitsToMajorTick() == 0 ? 10 : 5;

            gc.moveTo(0, tickLoc);
            gc.lineTo(setup.tickDir * tickLength, tickLoc);

            String label = generateTickLabel(normalizedValue);
            if (normalizedValue % getUnitsToMajorTick() == 0) {
                gc.strokeText(label,
                        tickLength * setup.tickDir + 3 * setup.tickDir, tickLoc - 1);
            }
        }

        gc.stroke();
    }

    protected String generateTickLabel(double val) {
        return String.format(getTickLabelFormat(), val);
    }

    /**
     * Determines the width or height based on the display direction.
     *
     * @return
     */
    protected double calcDisplayRange() {
        switch (getOrientation()) {
            case LEFT:
            case RIGHT:
                return getHeight();
            default:
                return getWidth();
        }
    }

    private void initialize() {
        setup = new Setup();

        // Translation for the background
        double tx = 0, ty = 0, width = 0, height = 0;
        switch (getOrientation()) {
            case RIGHT:
                setup.bgTx = getWidth() - getTapeWidth();
                setup.tX = getWidth();
                setup.tickDir = -1;
                setup.textAlignment = TextAlignment.RIGHT;
            case LEFT:
                setup.bgWidth = getTapeWidth();
                setup.bgHeight = getHeight();
                setup.tY = getHeight() / 2;
                break;
            case DOWN:
                setup.bgTy = getHeight() - getTapeWidth();
                setup.tickDir = -1;
            case UP:
                setup.bgWidth = getWidth();
                setup.bgHeight = getTapeWidth();
                setup.tX = getWidth() / 2;
        }

        setup.pixelsToUnit = calcDisplayRange() / getVisibleRange();
    }

    private void drawIndicator(GraphicsContext gc) {
        gc.setStroke(Color.WHITE);
        gc.setFill(Color.BLACK);

        gc.translate(setup.tX, setup.tY);
        gc.beginPath();

        // Left facing magnifier
        gc.moveTo(0, 0);
        gc.lineTo(8, -3);
        gc.lineTo(8, -10);
        gc.lineTo(35, -10);
        gc.lineTo(35, -20);
        gc.lineTo(getWidth(), -20);
        gc.lineTo(getWidth(), 20);
        gc.lineTo(35, 20);
        gc.lineTo(35, 10);
        gc.lineTo(8, 10);
        gc.lineTo(8, 3);
        gc.closePath();

        gc.fill();
        gc.stroke();
    }

    protected class Setup
    {

        public double bgTx, bgTy, bgWidth, bgHeight;
        public double pixelsToUnit, tickDir = 1, tX = 0, tY = 0;
        public VPos textBaseline = VPos.CENTER;
        public TextAlignment textAlignment = TextAlignment.LEFT;
    }

}
