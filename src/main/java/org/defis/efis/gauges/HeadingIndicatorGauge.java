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
import lombok.Getter;

import static lombok.AccessLevel.PROTECTED;

/**
 * A basic heading indicator gauge. Descendant classes should add things like a heading bug or full HSI.
 *
 * @author Matthew Tyler
 */
public class HeadingIndicatorGauge extends Parent
{

    @Getter(PROTECTED)
    private final Canvas canvas;
    private DoubleProperty headingProperty = new SimpleDoubleProperty(0);

    public HeadingIndicatorGauge(double width, double height) {
        canvas = new Canvas(width, height);
        headingProperty.addListener(l -> update());
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

        gc.save();
        transform(gc, heading);
        clear(gc);
        gc.restore();

        gc.save();
        transform(gc, heading);
        drawCompass(gc, heading);
        gc.restore();

    }

    protected void clear(GraphicsContext gc) {

        gc.beginPath();
        gc.arc(0, 0, getCanvas().getWidth() / 2, getCanvas().getHeight() / 2, 0, 360);
        gc.clip();
        gc.clearRect(-getCanvas().getWidth() / 2, -getCanvas().getHeight() / 2, getCanvas().getWidth(), getCanvas().getHeight());
    }

    protected void transform(GraphicsContext gc, double heading) {
        gc.translate(getCanvas().getWidth() / 2, getCanvas().getHeight() / 2);
    }

    protected void drawCompass(GraphicsContext gc, double heading) {

    }
}
