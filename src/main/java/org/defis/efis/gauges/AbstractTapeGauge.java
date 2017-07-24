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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static javafx.scene.text.TextAlignment.LEFT;
import static javafx.scene.text.TextAlignment.RIGHT;
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
     * The scale of values that roll in the indicator window. For example, the airspeed scale may be 1 in order to
     * scroll sequential numbers while the altimeter may be 20 to scroll altitude increments in steps of 20.
     */
    @Getter(PROTECTED)
    private final double indicatorStep;

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
    @Setter(PROTECTED)
    private boolean negativeDrawn = false;

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String tickLabelFormat = "%(,.0f";

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String indicatorLabelFormat = "%(,.0f";

    @Getter(PROTECTED)
    private Setup setup;

    protected AbstractTapeGauge(double width, double height, DisplayOrientation orientation,
            double unitsToMajorTick, double unitsToMinorTick, double visibleRange, double indicatorStep) {
        canvas = new Canvas(width, height);
        this.orientation = orientation;
        this.unitsToMajorTick = unitsToMajorTick;
        this.unitsToMinorTick = unitsToMinorTick;
        this.visibleRange = visibleRange;
        this.indicatorStep = indicatorStep;

        tapeWidth = width - 10;

        valueProperty.addListener(l -> update());

        getChildren().add(canvas);
    }

    protected void update() {
        if (setup == null) {
            initialize();
        }

        GraphicsContext gc = getGraphicsContext();
        double value = getValue();

        clear(gc);

        gc.save();
        drawBackground(gc);
        gc.restore();

        gc.save();
        drawTicks(gc);
        gc.restore();

        gc.save();
        drawMagnifier(gc);
        gc.restore();

        gc.save();
        drawCurrentValue(gc, value);
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
            if (!isNegativeDrawn() && normalizedValue < 0) {
                continue;
            }

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

    /**
     * Converts a major tick value into the text that will be displayed. A descendent class may override this when using
     * simple String.format() isn't sufficient.
     *
     * @param val
     * @return
     */
    protected String generateTickLabel(double val) {
        return String.format(getTickLabelFormat(), val);
    }

    protected String generateIndicatorLabel(double val) {
        return String.format(getIndicatorLabelFormat(), val);
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
        setup.numRollingDigits = Integer.toString((int) unitsToMinorTick).length();
        setup.valueX = 35;
    }

    private void drawMagnifier(GraphicsContext gc) {
        gc.setStroke(Color.WHITE);
        gc.setFill(Color.BLACK);

        gc.translate(setup.tX, setup.tY);

        leftFacingIndicator(gc);

        gc.fill();
        gc.stroke();
    }

    protected void leftFacingIndicator(GraphicsContext gc) {
        // Left facing magnifier
        gc.beginPath();
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
    }

    private void drawCurrentValue(GraphicsContext gc, double value) {

        gc.translate(setup.valueX, setup.tY);
        gc.setTextAlign(RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        gc.setStroke(Color.WHITE);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font(gc.getFont().getSize() + 8));

        String label = generateIndicatorLabel(value);

        strokeAndFillText(gc, label);

        gc.beginPath();
        gc.rect(0, -20, getWidth() - 35, 40);
        gc.clip();

        gc.setTextAlign(LEFT);
        for (double rollingValue = value - 2 * getIndicatorStep();
                rollingValue <= value + 2 * getIndicatorStep();
                rollingValue += getIndicatorStep()) {
            double normalizedValue = rollingValue - (rollingValue % getIndicatorStep());
            label = generateIndicatorLabel(normalizedValue);
            label = label.substring(label.length() - 2);

            double lY = -36 * (normalizedValue - getValue()) / (2 * getIndicatorStep());
            strokeAndFillText(gc, 0, lY, label);
            //strokeAndFillText(gc, 0, -33 * (rollingValue - getValue()) / (2 * getIndicatorStep()), label);
        }
    }

    protected void strokeAndFillText(GraphicsContext gc, double x, double y, String label) {
        gc.fillText(label, x, y - 2);
        gc.strokeText(label, x, y - 2);
    }

    protected void strokeAndFillText(GraphicsContext gc, String label) {
        gc.fillText(label.substring(0, label.length() - getSetup().numRollingDigits), 0, -2);
        gc.strokeText(label.substring(0, label.length() - getSetup().numRollingDigits), 0, -2);
    }

    protected class Setup
    {

        public double bgTx, bgTy, bgWidth, bgHeight;
        public double pixelsToUnit, tickDir = 1, tX = 0, tY = 0;
        public VPos textBaseline = VPos.CENTER;
        public TextAlignment textAlignment = TextAlignment.LEFT;
        public int numRollingDigits;
        public double valueX;
    }

}
