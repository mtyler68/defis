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
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Getter;

import static javafx.geometry.VPos.BOTTOM;
import static javafx.scene.text.TextAlignment.CENTER;
import static lombok.AccessLevel.PROTECTED;

/**
 * A basic heading indicator gauge. Descendant classes should add things like a heading bug or full HSI.
 *
 * TODO: Change TICK rendering to pure trig and don't use rotate/transform for each tick mark. Should be faster!
 *
 * @author Matthew Tyler
 */
public class HeadingIndicatorGauge extends Parent
{

    private static final Object CARDINAL_VALUES[][] = {
        {"N", 0d}, {"3", 30d}, {"6", 60d},
        {"E", 90d}, {"12", 120d}, {"15", 150d},
        {"S", 180d}, {"21", 210d}, {"24", 240d},
        {"W", 270d}, {"30", 300d}, {"33", 330d}
    };

    private static final double TICKS[] = {
        10, 20, 40, 50, 70, 80, 100, 110, 130, 140, 160, 170, 190, 200, 220, 230, 250, 260, 280, 290, 310, 320, 340, 350
    };

    private static final double MINOR_TICKS[] = {
        5, 15, 25, 35, 45, 55, 65, 75, 85, 95, 105, 115, 125, 135, 145, 155, 165, 175,
        185, 195, 205, 215, 225, 235, 245, 255, 265, 275, 285, 295, 305, 315, 325, 335, 345, 355
    };

    @Getter(PROTECTED)
    private final Canvas canvas;
    private DoubleProperty headingProperty = new SimpleDoubleProperty(0);

    public HeadingIndicatorGauge(double width, double height) {
        canvas = new Canvas(width, height);
        headingProperty.addListener(l -> update());
        getChildren().add(canvas);
        update();
    }

    public DoubleProperty headingProperty() {
        return headingProperty;
    }

    public double getHeading() {
        return headingProperty.get();
    }

    protected void update() {
        double heading = getHeading();
        GraphicsContext gc = getCanvas().getGraphicsContext2D();

        clear(gc);

        gc.save();
        transform(gc, heading);
        drawCompass(gc, heading);
        gc.restore();

        for (Object[] cardinals : CARDINAL_VALUES) {
            gc.save();
            drawCardinalLabel(gc, heading, (String) cardinals[0], (double) cardinals[1]);
            gc.restore();
        }

        for (double tick : TICKS) {
            gc.save();
            drawTick(gc, heading, tick, 6);
            gc.restore();
        }

        for (double tick : MINOR_TICKS) {
            gc.save();
            drawTick(gc, heading, tick, 3);
            gc.restore();
        }

        gc.save();
        drawDirectionIndicator(gc);
        gc.restore();
    }

    protected void clear(GraphicsContext gc) {
        gc.clearRect(0, 0, getCanvas().getWidth(), getCanvas().getHeight());
    }

    protected void transform(GraphicsContext gc, double heading) {

        gc.translate(getCanvas().getWidth() / 2, getCanvas().getHeight() / 2);
        gc.rotate(toTransformDegrees(heading));
    }

    protected void drawCompass(GraphicsContext gc, double heading) {
        gc.setStroke(Color.DIMGRAY);
        gc.setFill(Color.DIMGRAY);

        gc.beginPath();
        gc.arc(0, 0, getCanvas().getWidth() / 2, getCanvas().getHeight() / 2, 0, 360);
        gc.stroke();
        gc.fill();

//        gc.setStroke(Color.WHITE);
//        gc.beginPath();
//        gc.moveTo(0, 0);
//        gc.lineTo(20, 0);
//        gc.stroke();
    }

    protected void drawCardinalLabel(GraphicsContext gc, double heading, String label, double bearing) {
        gc.setStroke(Color.WHITE);
        gc.setTextAlign(CENTER);
        gc.setTextBaseline(BOTTOM);

        translateRotateForDraw(gc, heading, bearing);

        gc.strokeText(label, 0, 0);

        gc.setLineWidth(2);
        gc.strokeLine(0, -15, 0, -24);
    }

    protected void drawTick(GraphicsContext gc, double heading, double bearing, double length) {
        translateRotateForDraw(gc, heading, bearing);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(0, -24 + length, 0, -24);
    }

    protected void translateRotateForDraw(GraphicsContext gc, double heading, double bearing) {
        double rotate = (360 - heading + bearing) % 360;
        double rads = Math.PI * (rotate - 90) / 180;
        double dist = canvas.getWidth() / 2 - 24;
        gc.translate(getCanvas().getWidth() / 2 + dist * Math.cos(rads), getCanvas().getHeight() / 2 + dist * Math.sin(rads));
        gc.rotate(rotate);
    }

    protected double toTransformDegrees(double direction) {
        return 270 - direction;
    }

    /**
     * Draws the direction indicator (miniature airplane).
     *
     * @param gc
     */
    protected void drawDirectionIndicator(GraphicsContext gc) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);

        translateToCenter(gc);

        gc.beginPath();
        gc.moveTo(40, 0);
        gc.lineTo(76, 0);
        gc.moveTo(-40, 0);
        gc.lineTo(-76, 0);
        gc.moveTo(0, -40);
        gc.lineTo(0, -canvas.getWidth() / 2);
        gc.moveTo(0, 40);
        gc.lineTo(0, canvas.getWidth() / 2);

        gc.stroke();
    }

    /**
     * Makes the center of the gauge 0,0
     *
     * @param gc
     */
    protected void translateToCenter(GraphicsContext gc) {
        gc.translate(getCanvas().getWidth() / 2, getCanvas().getHeight() / 2);
    }
}
