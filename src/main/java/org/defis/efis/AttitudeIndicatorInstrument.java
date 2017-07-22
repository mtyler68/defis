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
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import static javafx.geometry.VPos.CENTER;
import static javafx.scene.text.TextAlignment.LEFT;
import static javafx.scene.text.TextAlignment.RIGHT;

/**
 * Items drawn relative to the bank angle: Bank Arc/Ticks
 *
 * Items drawn relative to the horizon: Horizon, Pitch Ticks
 *
 * Items drawn relative to the screen: Bank indicator bug, miniature plane.
 *
 * A bank that is to the left is in negative degrees from zero. A bank that is to the right is in positive from zero.
 *
 * @author Matthew Tyler
 */
public class AttitudeIndicatorInstrument extends Parent
{

    private final Canvas canvas;
    private double maxX;
    private double maxY;
    private final double cenX;
    private final double cenY;
    private final double maxHorizonRun;
    private double pitchDegToPixel = 5;

    /**
     * Positive angles are right banks and negative angles are left banks.
     */
    private final DoubleProperty bankAngle = new SimpleDoubleProperty(0);

    private final DoubleProperty pitchAngle = new SimpleDoubleProperty(0);

    private Group horizonGroup = new Group();
    private StackPane horizonPane = new StackPane();

    private Stop[] groundStops = new Stop[]{new Stop(0, Color.CHOCOLATE), new Stop(.15, Color.CHOCOLATE.darker().darker())};
    private Stop[] skyStops = new Stop[]{new Stop(0, Color.DODGERBLUE), new Stop(.15, Color.DODGERBLUE.darker().darker())};
    private LinearGradient groundGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, groundStops);
    private LinearGradient skyGradient = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, skyStops);

    /**
     * By subscript: 0=bank angle; 1=length; [2=disc center when length is -1].
     */
    private static final double[][] BANK_TICKS = {
        {-60, 15}, {-45, -7, 5}, {-30, 12}, {-20, 7}, {-10, 7},
        {60, 15}, {45, -7, 5}, {30, 12}, {20, 7}, {10, 7}
    //{0, 15}
    };

    public AttitudeIndicatorInstrument() {

        canvas = new Canvas(800, 480);
        maxX = canvas.getWidth();
        maxY = canvas.getHeight();
        cenX = maxX / 2;
        cenY = maxY / 2 - 65;

        maxHorizonRun = Math.sqrt(maxX * maxX + maxY * maxY);
        bankAngle.addListener(l -> update2());
        pitchAngle.addListener(l -> update2());

        getChildren().add(canvas);

        update2();
    }

    private void configureHorizonGroup() {

        horizonPane.setPrefSize(800, 480);
        horizonPane.setMaxSize(800, 480);

        Rectangle groundPlane = new Rectangle(1200, 1200);
        groundPlane.setFill(groundGradient);
        groundPlane.setTranslateY(240);
        groundPlane.setTranslateX(-200);

        Rectangle skyPlane = new Rectangle(1200, 1200);
        skyPlane.setFill(skyGradient);
        //skyPlane.setTranslateY(-1440);
        //skyPlane.setTranslateX(-200);

        horizonPane.getChildren().addAll(groundPlane, skyPlane);
    }

    private void update2() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        double bankRads = getBankAngle() / 180 * Math.PI + Math.PI / 2;
        gc.translate(cenX + getPitchAngle() * pitchDegToPixel * Math.cos(bankRads),
                cenY - getPitchAngle() * pitchDegToPixel * Math.sin(bankRads));
        gc.rotate(-getBankAngle());
        drawGroundPlan(gc);
        drawSkyPlane(gc);
        drawPitchTicks(gc); // Needs to be the last draw op because uses clipping or refactor and make own op
        gc.restore();

        gc.save();
        gc.translate(cenX, cenY);
        gc.rotate(-getBankAngle());
        drawBankArc(gc);
        gc.restore();

        gc.save();
        drawBankPointer(gc);
        gc.restore();

        gc.save();
        drawMiniatureAirplane(gc);
        gc.restore();
    }

    private void drawGroundPlan(GraphicsContext gc) {
        gc.setFill(groundGradient);
        gc.fillRect(-maxHorizonRun, 0, maxHorizonRun * 2, maxHorizonRun * 2);
    }

    private void drawSkyPlane(GraphicsContext gc) {
        gc.setFill(skyGradient);
        gc.fillRect(-maxHorizonRun, -maxHorizonRun * 2, maxHorizonRun * 2, maxHorizonRun * 2);
    }

    private void drawBankArc(GraphicsContext gc) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);

        gc.beginPath();
        gc.arc(0, 0, 150, 150, 30, 120);

        // Bank angle ticks
        for (double[] ticks : BANK_TICKS) {
            double tickRads = ticks[0] / 180 * Math.PI + Math.PI / 2;
            if (ticks[1] < 0) {
                gc.setFill(Color.WHITE);
                gc.fillOval(-2 + (150 - ticks[1]) * Math.cos(tickRads), -2 - (150 - ticks[1]) * Math.sin(tickRads),
                        ticks[2], ticks[2]);
            } else {
                gc.moveTo(150 * Math.cos(tickRads), -150 * Math.sin(tickRads));
                gc.lineTo((150 + ticks[1]) * Math.cos(tickRads), -(150 + ticks[1]) * Math.sin(tickRads));
            }
        }
        gc.stroke();

        // Level bank bug
        gc.beginPath();
        gc.setFill(Color.WHITE);
        gc.moveTo(0, -152);
        gc.lineTo(-10, -162);
        gc.lineTo(10, -162);
        gc.closePath();
        gc.fill();
    }

    private void drawBankPointer(GraphicsContext gc) {
        gc.translate(cenX, cenY - 148);

        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(10, 10);
        gc.lineTo(- 10, 10);
        gc.closePath();
        gc.setFill(Color.WHITE);
        gc.fill();
    }

    private void drawMiniatureAirplane(GraphicsContext gc) {
        gc.translate(cenX - 3, cenY - 3);

        gc.setFill(Color.BLACK);
        gc.fillOval(0, 0, 6, 6);

        gc.beginPath();
        gc.moveTo(-35, 2);
        gc.lineTo(-85, 2);

        gc.moveTo(35, 2);
        gc.lineTo(85, 2);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.stroke();
    }

    private void drawPitchTicks(GraphicsContext gc) {

        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);

//        gc.beginPath();
        double pitchNormalized = -(getPitchAngle() - (getPitchAngle() % 2.5));
//        gc.rect(-20, 25 * pitchDegToPixel, 20, -25 * pitchDegToPixel);
//        gc.clip();
        gc.beginPath();
        for (double pitch = pitchNormalized - 30; pitch < pitchNormalized + 30; pitch += 2.5) {
//            if (pitch == 0) {
//                continue;
//            }

            double halfWidth = (pitch % 10 == 0 ? 20 : (pitch % 5 == 0 ? 10 : 5));
            gc.moveTo(-halfWidth, -pitch * pitchDegToPixel);
            gc.lineTo(halfWidth, -pitch * pitchDegToPixel);

            if (pitch % 10 == 0) {
                gc.lineTo(halfWidth, -pitch * pitchDegToPixel + 5 * Math.signum(pitch));
                gc.moveTo(-halfWidth, -pitch * pitchDegToPixel);
                gc.lineTo(-halfWidth, -pitch * pitchDegToPixel + 5 * Math.signum(pitch));

                gc.setTextAlign(RIGHT);
                gc.setTextBaseline(CENTER);
                gc.strokeText(
                        Integer.toString((int) Math.abs(pitch)), -halfWidth - 3,
                        -pitch * pitchDegToPixel + 3 * Math.signum(pitch));

                gc.setTextAlign(LEFT);
                gc.setTextBaseline(CENTER);
                gc.strokeText(
                        Integer.toString((int) Math.abs(pitch)), halfWidth + 3,
                        -pitch * pitchDegToPixel + 3 * Math.signum(pitch));
            }
        }

        gc.stroke();
    }

    private void update() {
//        System.out.printf("cenY=%f, cenX=%f, maxHalfHorizonRun=%f", cenY, cenX, maxHalfHorizonRun);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double rads = getBankAngle() / 180 * Math.PI;
        double leftNinety = 0.5 * Math.PI;
        double rightNinety = -0.5 * Math.PI;

        // Ground plane
        gc.beginPath();

        double toX = cenX + Math.cos(rads) * maxHorizonRun;
        double toY = cenY - Math.sin(rads) * maxHorizonRun;

        gc.moveTo(cenX, cenY);
        gc.lineTo(toX, toY);

        toX += Math.cos(rads + rightNinety) * maxHorizonRun;
        toY -= Math.sin(rads + rightNinety) * maxHorizonRun;
        gc.lineTo(toX, toY);

        toX = cenX - Math.cos(rads) * maxHorizonRun - Math.cos(rads + leftNinety) * maxHorizonRun;
        toY = cenY + Math.sin(rads) * maxHorizonRun + Math.sin(rads + leftNinety) * maxHorizonRun;
        gc.lineTo(toX, toY);

        toX = cenX - Math.cos(rads) * maxHorizonRun;
        toY = cenY + Math.sin(rads) * maxHorizonRun;
        gc.lineTo(toX, toY);

        gc.closePath();

        LinearGradient groundGradient = new LinearGradient(0, 0, Math.cos(rads + rightNinety), -Math.sin(rads + rightNinety),
                true, CycleMethod.NO_CYCLE, groundStops);
        gc.setFill(Color.CHOCOLATE);
        gc.fill();

        // Sky plane
        gc.setFill(Color.DODGERBLUE);
        gc.beginPath();

        toX = cenX + Math.cos(rads) * maxHorizonRun;
        toY = cenY - Math.sin(rads) * maxHorizonRun;

        gc.moveTo(cenX, cenY);
        gc.lineTo(toX, toY);

        toX += Math.cos(rads + leftNinety) * maxHorizonRun;
        toY -= Math.sin(rads + leftNinety) * maxHorizonRun;
        gc.lineTo(toX, toY);

        toX = cenX - Math.cos(rads) * maxHorizonRun - Math.cos(rads + rightNinety) * maxHorizonRun;
        toY = cenY + Math.sin(rads) * maxHorizonRun + Math.sin(rads + rightNinety) * maxHorizonRun;
        gc.lineTo(toX, toY);

        toX = cenX - Math.cos(rads) * maxHorizonRun;
        toY = cenY + Math.sin(rads) * maxHorizonRun;
        gc.lineTo(toX, toY);

        gc.closePath();
        gc.fill();

        // Bank Arc
        drawBankArc1(gc);
        drawPitchTicks1(gc);

        // Horizon line
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(cenX, cenY, cenX + Math.cos(rads) * maxHorizonRun,
                cenY - Math.sin(rads) * maxHorizonRun);
        gc.strokeLine(cenX, cenY, cenX - Math.cos(rads) * maxHorizonRun,
                cenY + Math.sin(rads) * maxHorizonRun);

        // Miniature Airplane
        gc.beginPath();
        gc.moveTo(cenX - 35, cenY + 1);
        gc.lineTo(cenX - 85, cenY + 1);

        gc.moveTo(cenX + 35, cenY + 1);
        gc.lineTo(cenX + 85, cenY + 1);

        gc.setLineWidth(3);
        gc.setStroke(Color.BLACK);
        gc.stroke();

        gc.setFill(Color.BLACK);
        gc.fillOval(cenX - 2, cenY - 2, 6, 6);

        // Done
        gc.restore();
    }

    public DoubleProperty bankAngleProperty() {
        return bankAngle;
    }

    public double getBankAngle() {
        return bankAngle.get();
    }

    public DoubleProperty pitchAngleProperty() {
        return pitchAngle;
    }

    public double getPitchAngle() {
        return pitchAngle.get();
    }

    private void drawBankArc1(GraphicsContext gc) {

        double bank = getBankAngle();
        double rads = getBankAngle() / 180 * Math.PI;

        gc.beginPath();

        gc.arc(cenX, cenY, 150, 150, 30 + bank, 120);

        // Bank angle ticks
        for (double[] ticks : BANK_TICKS) {
            double tickRads = rads - ticks[0] / 180 * Math.PI + Math.PI / 2;
            if (ticks[1] < 0) {
                gc.setFill(Color.WHITE);
                gc.fillOval(cenX - 2 + (150 - ticks[1]) * Math.cos(tickRads), cenY - 2 - (150 - ticks[1]) * Math.sin(tickRads),
                        ticks[2], ticks[2]);
            } else {
                gc.moveTo(cenX + 150 * Math.cos(tickRads), cenY - 150 * Math.sin(tickRads));
                gc.lineTo(cenX + (150 + ticks[1]) * Math.cos(tickRads), cenY - (150 + ticks[1]) * Math.sin(tickRads));
            }
        }
//        double tickRads = rads + Math.PI / 3 + Math.PI / 2;
//        gc.moveTo(cenX + 150 * Math.cos(tickRads), cenY - 150 * Math.sin(tickRads));
//        gc.lineTo(cenX + 160 * Math.cos(tickRads), cenY - 160 * Math.sin(tickRads));

        gc.setLineWidth(2);
        gc.setStroke(Color.WHITE);

        gc.stroke();

//        // Level pointer
//        gc.beginPath();
//        double tickRads = rads + Math.PI / 2;
//        gc.moveTo(cenX + 150 * Math.cos(tickRads), cenY - 150 * Math.sin(tickRads));
//        gc.lineTo(cenX + 160 * Math.cos(tickRads + 1 / 30 * Math.PI), cenY - 160 * Math.sin(tickRads + 1 / 30 * Math.PI));
//        gc.lineTo(cenX + 160 * Math.cos(tickRads - 1 / 30 * Math.PI), cenY - 160 * Math.sin(tickRads - 1 / 30 * Math.PI));
////
////        gc.lineTo(cenX + 160 * Math.cos(tickRads - Math.PI / 3), cenY - 160 * Math.sin(tickRads - Math.PI / 3));
//        gc.closePath();
        // Bank pointer
        gc.beginPath();
        gc.moveTo(cenX, cenY - 150);
        gc.lineTo(cenX + 10, cenY - 140);
        gc.lineTo(cenX - 10, cenY - 140);
        gc.closePath();
        gc.setFill(Color.WHITE);
        gc.fill();

    }

    /**
     * Transforms the bankAngle property from degrees left (negative) or degrees right (positive) to screen radians
     * inverted for the screen's coordinate system.
     *
     * @return
     */
    private double bankAngleScreenRads() {
        double bankRads = getBankAngle() * 180 / Math.PI;
        return 0;
    }

    private void drawPitchTicks1(GraphicsContext gc) {
        double bank = getBankAngle();

        // Rads is the centerline
        double rads = getBankAngle() / 180 * Math.PI + Math.PI / 2;

        gc.beginPath();
        gc.moveTo(cenX, cenY);
        gc.lineTo(cenX + 150 * Math.cos(rads), cenY - 150 * Math.sin(rads));

        double pitchDegToPixel = 5;
        for (double pitch = 0; pitch < 90; pitch += 2.5) {
            double halfWidth = (pitch % 10 == 0 ? 20 : (pitch % 5 == 0 ? 10 : 5));
            gc.moveTo(
                    cenX + pitchDegToPixel * pitch * Math.cos(rads) - halfWidth * Math.cos(rads + Math.PI / 2),
                    cenY - pitchDegToPixel * pitch * Math.sin(rads) + halfWidth * Math.sin(rads + Math.PI / 2));
            gc.lineTo(cenX + pitchDegToPixel * pitch * Math.cos(rads) + halfWidth * Math.cos(rads + Math.PI / 2),
                    cenY - pitchDegToPixel * pitch * Math.sin(rads) - halfWidth * Math.sin(rads + Math.PI / 2));
        }

//        gc.moveTo(
//                cenX + 50 * Math.cos(rads) - 25 * Math.cos(rads + Math.PI / 2),
//                cenY - 50 * Math.sin(rads) + 25 * Math.sin(rads + Math.PI / 2));
//        gc.lineTo(cenX + 50 * Math.cos(rads) + 25 * Math.cos(rads + Math.PI / 2),
//                cenY - 50 * Math.sin(rads) - 25 * Math.sin(rads + Math.PI / 2));
        gc.setStroke(Color.RED);
        gc.stroke();
    }

}
