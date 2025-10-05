package simu.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import simu.controller.IControllerVtoM;
import simu.framework.Clock;

public class VisualisationToolbar extends HBox {
    private final Button slowBtn   = new Button("Decrease speed");
    private final Button fastBtn   = new Button("Increase speed");
    private final Button pauseBtn  = new Button("Pause / Resume");
    private final Button stopBtn   = new Button("End simulation");
    private final Label timeLabel  = new Label("0.000");
    private final Timeline timeUpdater;
    private boolean paused = false;

    public VisualisationToolbar(IControllerVtoM controller) {
        setSpacing(12);
        setPadding(new Insets(8,10,8,10));
        getStyleClass().add("sim-toolbar");

        setAlignment(Pos.CENTER_LEFT);

        timeLabel.getStyleClass().add("value-chip");

        slowBtn.setOnAction(e -> controller.decreaseSpeed());
        fastBtn.setOnAction(e -> controller.increaseSpeed());

        pauseBtn.setOnAction(e -> {
            if (!paused) {
                controller.pauseSimulation();
                paused = true;
                pauseBtn.setText("Resume");
            } else {
                controller.resumeSimulation();
                paused = false;
                pauseBtn.setText("Pause");
            }
        });

        stopBtn.setOnAction(e -> controller.stopSimulation());

        getChildren().addAll(slowBtn, fastBtn, pauseBtn, stopBtn, new Region(), new Label("Time:"), timeLabel);

        // update time every 200 ms
        timeUpdater = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            double t = Clock.getInstance().getClock();
            timeLabel.setText(String.format("%.3f", t));
        }));
        timeUpdater.setCycleCount(Timeline.INDEFINITE);
        timeUpdater.play();
    }

    public void disableAll() {
        slowBtn.setDisable(true);
        fastBtn.setDisable(true);
        pauseBtn.setDisable(true);
        stopBtn.setDisable(true);
        timeUpdater.stop();
    }
}
