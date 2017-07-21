package org.xefix.efis;

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

import static javafx.application.Application.launch;
import static javafx.util.Duration.millis;

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

        Slider speedSlider = new Slider(0, 250, 0);
        speedSlider.setOrientation(Orientation.VERTICAL);
//        st.airSpeedProperty().bind(speedSlider.valueProperty());

        //Parent root = new HBox(st, ai);
//        StackPane root = new StackPane(ai, st);
        AnchorPane root = new AnchorPane(ai, st);

        st.setLayoutX(50);
        st.setLayoutY(50);
        st.setOpacity(.85);

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
