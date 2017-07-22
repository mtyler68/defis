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
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.flightgear.fgfsclient.FGFSConnection;
import org.defis.efis.gauges.AltitudeTape;

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

    @Override
    public void start(Stage stage) throws Exception {
        //Parent root = FXMLLoader.load(getClass().getResource("/fxml/PFDScreen.fxml"));

        SpeedTapeInstrument st = new SpeedTapeInstrument();
        AttitudeIndicatorInstrument ai = new AttitudeIndicatorInstrument();
        AltitudeTape alt = new AltitudeTape(60, 250);

        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(st.airSpeedProperty(), 0)),
                new KeyFrame(new Duration(13000), new KeyValue(st.airSpeedProperty(), 120))
        );

        Timeline bankTimeline = new Timeline();
        bankTimeline.setCycleCount(Timeline.INDEFINITE);
        bankTimeline.setAutoReverse(true);

        bankTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(ai.bankAngleProperty(), -60)),
                new KeyFrame(new Duration(7500), new KeyValue(ai.bankAngleProperty(), 60))
        );

        Timeline pitchTimeline = new Timeline();
        pitchTimeline.setCycleCount(Timeline.INDEFINITE);
        pitchTimeline.setAutoReverse(true);

        pitchTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(ai.pitchAngleProperty(), -30)),
                new KeyFrame(new Duration(4000), new KeyValue(ai.pitchAngleProperty(), 30))
        );

        Timeline altTimeline = new Timeline();
        altTimeline.setCycleCount(Timeline.INDEFINITE);
        altTimeline.setAutoReverse(true);

        altTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(alt.valueProperty(), -1200)),
                new KeyFrame(new Duration(17000), new KeyValue(alt.valueProperty(), 1200))
        );

        Slider speedSlider = new Slider(0, 250, 0);
        speedSlider.setOrientation(Orientation.VERTICAL);
//        st.airSpeedProperty().bind(speedSlider.valueProperty());

        //Parent root = new HBox(st, ai);
//        StackPane root = new StackPane(ai, st);
        AnchorPane root = new AnchorPane(ai, alt, st);

        st.setLayoutX(160);
        st.setLayoutY(75);
        st.setOpacity(.85);

        alt.setLayoutX(580);
        alt.setLayoutY(75);
        alt.setOpacity(.85);

        //root.setStyle("-fx-background-color: chocolate");
        Scene scene = new Scene(root);
//        stage.setMaxHeight(480);
//        stage.setMaxWidth(800);
//        scene.setFill(Color.CHOCOLATE);
        //scene.getStylesheets().add("/styles/pfdscreen.css");

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

        stage.setTitle("xEFIS");
        stage.setScene(scene);
        stage.show();

        bankTimeline.play();
        timeline.play();
        pitchTimeline.play();
        altTimeline.play();
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
