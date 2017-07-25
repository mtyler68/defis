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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.WritableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.defis.efis.gauges.AltitudeTape;
import org.defis.efis.gauges.HeadingIndicatorGauge;
import org.flightgear.fgfsclient.FGFSConnection;

import static javafx.application.Application.launch;
import static javafx.util.Duration.millis;
import static javafx.application.Application.launch;
import static javafx.util.Duration.millis;

/**
 *
 * @author Matthew Tyler
 */
public class MainApp extends Application
{

    private static final int DEFAULT_FGFS_PORT = 9000;

    private FGFSConnection fgfsConn;
    private ScheduledService<Void> telemetryUpdateService;

    @Override
    public void init() throws Exception {

        if (getParameters().getNamed().containsKey("fgfshost")) {
            String host = getParameters().getNamed().get("fgfshost");
            int port = getParameters().getNamed().containsKey("fgfsport")
                    ? Integer.valueOf(getParameters().getNamed().get("fgfsport"))
                    : DEFAULT_FGFS_PORT;
            fgfsConn = new FGFSConnection(host, port);
        }
    }

    @Override
    public void stop() throws Exception {
        if (fgfsConn != null) {
            fgfsConn.close();
        }

        if (telemetryUpdateService != null && telemetryUpdateService.isRunning()) {
            telemetryUpdateService.cancel();
        }
    }

    private <T> Timeline createTimeline(WritableValue<T> target, long durationMs, T startValue, T endValue) {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(target, startValue)),
                new KeyFrame(new Duration(durationMs), new KeyValue(target, endValue))
        );
        return timeline;
    }

    @Override
    public void start(Stage stage) throws Exception {
        //Parent root = FXMLLoader.load(getClass().getResource("/fxml/PFDScreen.fxml"));

        SpeedTapeInstrument st = new SpeedTapeInstrument();
        AttitudeIndicatorInstrument ai = new AttitudeIndicatorInstrument();
        AltitudeTape alt = new AltitudeTape(60, 250);
        HeadingIndicatorGauge hi = new HeadingIndicatorGauge(200, 200);

        Slider slider = new Slider(0, 360, 45);
        Label sliderLabel = new Label();

        Timeline iasTimeline = createTimeline(st.airSpeedProperty(), 13000, 0, 120);
        Timeline bankTimeline = createTimeline(ai.bankAngleProperty(), 7500, -60, 60);
        Timeline pitchTimeline = createTimeline(ai.pitchAngleProperty(), 4000, -30, 30);
        Timeline altTimeline = createTimeline(alt.valueProperty(), 23000, -200, 2200);
        Timeline hiTimeline = createTimeline(hi.headingProperty(), 9300, 0, 360);

        AnchorPane root = new AnchorPane(ai, alt, hi, slider, sliderLabel, st);

        st.setLayoutX(160);
        st.setLayoutY(75);
        st.setOpacity(.85);

        alt.setLayoutX(580);
        alt.setLayoutY(75);
        alt.setOpacity(.85);

        hi.setLayoutX(300);
        hi.setLayoutY(260);
        hi.setOpacity(.85);
        hi.headingProperty().set(45);

        slider.setLayoutX(10);
        slider.setLayoutY(10);
        sliderLabel.setLayoutX(10);
        sliderLabel.setLayoutY(25);
//        hi.headingProperty().bind(slider.valueProperty());
//        sliderLabel.textProperty().bind(slider.valueProperty().asString("%3.0f"));

        Scene scene = new Scene(root);

        if (fgfsConn != null) {
            final FGFSIntegrator fgfsInt = new FGFSIntegrator();
            st.airSpeedProperty().bind(fgfsInt.airSpeedProperty());
            ai.bankAngleProperty().bind(fgfsInt.bankAnglProperty());
            ai.pitchAngleProperty().bind(fgfsInt.pitchAngProperty().negate());

            telemetryUpdateService = new ScheduledService<Void>()
            {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>()
                    {
                        @Override
                        protected Void call() throws Exception {
                            fgfsInt.update(fgfsConn);
                            return null;
                        }
                    };
                }
            };

            telemetryUpdateService.setPeriod(millis(25));
            telemetryUpdateService.start();
        }

        stage.setTitle("DEFIS");
        stage.setScene(scene);
        stage.show();

        bankTimeline.play();
        iasTimeline.play();
        pitchTimeline.play();
        altTimeline.play();
        hiTimeline.play();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
     * ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
