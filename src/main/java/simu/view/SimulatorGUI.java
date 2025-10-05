package simu.view;

import java.text.DecimalFormat;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import simu.controller.Controller;
import simu.controller.IControllerVtoM;
import simu.framework.Trace;
import simu.framework.Trace.Level;
import simu.model.SimParameters;

public class SimulatorGUI extends Application implements ISimulatorUI {

    // --- Time & UI settings ---
    private final Spinner<Double>  simulationDurationSpinner = new Spinner<>();
    private final Spinner<Integer> uiDelaySpinner            = new Spinner<>();

    // --- Server counts ---
    private final Spinner<Integer> numMechanicsSpinner = new Spinner<>();
    private final Spinner<Integer> numWashersSpinner   = new Spinner<>();

    // --- Routing probabilities (0..1) ---
    private final Slider probNeedsMechanicSlider = makeProbSlider(0.70);
    private final Slider probNeedsWashSlider     = makeProbSlider(0.50);

    // --- Wash program split (0..1, should sum ~1) ---
    private final Slider probWashExteriorSlider  = makeProbSlider(0.50);
    private final Slider probWashInteriorSlider  = makeProbSlider(0.30);
    private final Slider probWashBothSlider      = makeProbSlider(0.20);

    private final Label washSplitSumLabel = new Label("Sum: 1.00");

    // --- Dynamic per-server speed sliders ---
    private final VBox mechanicSpeedsBox = new VBox(30); // Increased spacing for mechanics
    private final VBox washerSpeedsBox   = new VBox(30); // Increased spacing for washers

    // --- Model ---
    private final SimParameters params = new SimParameters();

    // --- Controller ---
    private IControllerVtoM controller;

    // --- Visualization ---
    private Visualisation visualisation;

    // -------- helpers --------
    private static Slider makeProbSlider(double def) {
        Slider s = new Slider(0, 1, def);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.setMajorTickUnit(0.25);
        s.setMinorTickCount(4);
        s.setBlockIncrement(0.01);
        s.setSnapToTicks(true);
        s.setMaxWidth(Double.MAX_VALUE);
        return s;
    }

    @Override
    public void init() {
        Trace.setTraceLevel(Level.INFO);
        controller = new Controller(this, params);
    }

    @Override
    public void start(Stage stage) {
        // Create visualization but don't show it yet
        visualisation = new Visualisation(1200, 800); // Increased canvas size

        // --- Spinners with explicit factories ---
        simulationDurationSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        1.0, Double.MAX_VALUE, params.simDurationProperty().get(), 100.0
                )
        );
        uiDelaySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 10_000, params.uiDelayMsProperty().get(), 10
                )
        );
        numMechanicsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 3, params.numMechanicsProperty().get(), 1
                )
        );
        numWashersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 3, params.numWashersProperty().get(), 1
                )
        );

        simulationDurationSpinner.setEditable(true);
        uiDelaySpinner.setEditable(true);
        numMechanicsSpinner.setEditable(true);
        numWashersSpinner.setEditable(true);

        // --- Sync UI <-> params ---
        simulationDurationSpinner.valueProperty().addListener((obs, oldV, newV) -> params.simDurationProperty().set(newV));
        params.simDurationProperty().addListener((obs, oldV, newV) -> simulationDurationSpinner.getValueFactory().setValue((Double) newV));

        uiDelaySpinner.valueProperty().addListener((obs, oldV, newV) -> params.uiDelayMsProperty().set(newV));
        params.uiDelayMsProperty().addListener((obs, oldV, newV) -> uiDelaySpinner.getValueFactory().setValue((Integer) newV));

        numMechanicsSpinner.valueProperty().addListener((obs, oldV, newV) -> params.numMechanicsProperty().set(newV));
        params.numMechanicsProperty().addListener((obs, oldV, newV) -> numMechanicsSpinner.getValueFactory().setValue((Integer) newV));

        numWashersSpinner.valueProperty().addListener((obs, oldV, newV) -> params.numWashersProperty().set(newV));
        params.numWashersProperty().addListener((obs, oldV, newV) -> numWashersSpinner.getValueFactory().setValue((Integer) newV));

        // Probabilities (bi-directional)
        params.pNeedsMechanicProperty().bindBidirectional(probNeedsMechanicSlider.valueProperty());
        params.pNeedsWashProperty().bindBidirectional(probNeedsWashSlider.valueProperty());
        params.pWashExteriorProperty().bindBidirectional(probWashExteriorSlider.valueProperty());
        params.pWashInteriorProperty().bindBidirectional(probWashInteriorSlider.valueProperty());
        params.pWashBothProperty().bindBidirectional(probWashBothSlider.valueProperty());

        washSplitSumLabel.textProperty().bind(
                params.pWashExteriorProperty()
                        .add(params.pWashInteriorProperty())
                        .add(params.pWashBothProperty())
                        .asString("Sum: %.2f")
        );

        // --- Rebuild dynamic per-server sliders ---
        params.numMechanicsProperty().addListener((o, a, b) -> rebuildMechanicSpeeds());
        params.numWashersProperty().addListener((o, a, b) -> rebuildWasherSpeeds());
        rebuildMechanicSpeeds();
        rebuildWasherSpeeds();

        // --- Distribution dialogs (write back to params) ---
        Button editInterBtn = new Button("Edit Inter-arrival…");
        Button editReceptionBtn = new Button("Edit Reception…");
        Button editMechanicBtn = new Button("Edit Mechanic…");
        Button editWashBtn = new Button("Edit Wash…");

        editInterBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Inter-arrival", params.interArrivalProperty().get())
                        .ifPresent(params.interArrivalProperty()::set)
        );
        editReceptionBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Reception service", params.receptionServiceProperty().get())
                        .ifPresent(params.receptionServiceProperty()::set)
        );
        editMechanicBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Mechanic service", params.mechanicServiceProperty().get())
                        .ifPresent(params.mechanicServiceProperty()::set)
        );
        editWashBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Wash service", params.washServiceProperty().get())
                        .ifPresent(params.washServiceProperty()::set)
        );

        // --- Layout ---
        Label heading = new Label("Car Kosovo Simulator");
        heading.setStyle("""
                    -fx-font-size: 30px;
                    -fx-font-weight: 900;             /* bolder */
                    -fx-text-fill: #ffffff;\s
                    -fx-border-color: #CD4527;
                    -fx-border-width: 1;
                    -fx-border-radius: 5;
                    -fx-padding: 10;
                """
        );
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");                   // style: grid class

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHalignment(HPos.LEFT);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHalignment(HPos.LEFT);
        col2.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().setAll(col1, col2);

        int r = 0;

// --- Section: General Options ---
        form.add(sectionHeader("General Options"), 0, r++, 2, 1);
        form.add(field("Simulation duration", simulationDurationSpinner), 0, r);
        form.add(field("UI delay (ms)", uiDelaySpinner),                 1, r++);

        form.add(field("Number of Mechanics", numMechanicsSpinner), 0, r);
        form.add(field("Number of Washers",   numWashersSpinner),   1, r++);

// --- Section: Customer Probabilities ---
        form.add(sectionHeader("Customer Probabilities"), 0, r++, 2, 1);
        form.add(fieldWithValue("P(Needs Mechanic)", probNeedsMechanicSlider, "%.0f%%", 100), 0, r);
        form.add(fieldWithValue("P(Needs Wash)",     probNeedsWashSlider,     "%.0f%%", 100), 1, r++);

        form.add(fieldWithValue("Wash: Exterior", probWashExteriorSlider, "%.0f%%", 100), 0, r);
        form.add(fieldWithValue("Wash: Interior", probWashInteriorSlider, "%.0f%%", 100), 1, r++);
        form.add(fieldWithValue("Wash: Both",     probWashBothSlider,     "%.0f%%", 100), 0, r);
        form.add(field("Wash split sum", washSplitSumLabel),             1, r++);

// --- Section: Distribution Options ---
        form.add(sectionHeader("Distribution Options"), 0, r++, 2, 1);
        form.add(field("Inter-arrival",       editInterBtn),     0, r);
        form.add(field("Reception service",   editReceptionBtn), 1, r++);
        form.add(field("Mechanic service",    editMechanicBtn),  0, r);
        form.add(field("Wash service",        editWashBtn),      1, r++);

// --- Section: Service Point Speed Factors ---
        form.add(sectionHeader("Service Point Speed Factors"), 0, r++, 2, 1);
        form.add(labeledBox("Mechanic speeds (0.5x–2.0x)", mechanicSpeedsBox), 0, r++, 2, 1);
        form.add(labeledBox("Washer speeds (0.5x–2.0x)",   washerSpeedsBox),   0, r++, 2, 1);

// --- Start button ---
        Button startButton = new Button("Start Simulation");
        startButton.setPrefWidth(220);
        startButton.getStyleClass().add("primary");
        startButton.setOnAction(e -> {
            Stage visualStage = new Stage();
            visualStage.setTitle("Simulation Visualization");

            VBox visualRoot = new VBox(20);  // 20px spacing between elements
            visualRoot.setAlignment(Pos.CENTER);
            visualRoot.setPadding(new Insets(20));
            visualRoot.setStyle("-fx-background-color: #2B2B2B;");

            // Add visualization
            visualRoot.getChildren().addAll(visualisation);

            Scene visualScene = new Scene(visualRoot, 1200, 900);
            visualScene.getStylesheets().add("styles.css");
            visualStage.setScene(visualScene);
            visualStage.show();

            // Start the simulation
            controller.startSimulation();
        });

        HBox startRow = new HBox(startButton);
        startRow.setAlignment(Pos.CENTER);
        startRow.setPadding(new Insets(8, 0, 0, 0));
        form.add(startRow, 0, r, 2, 1);
        GridPane.setValignment(startRow, VPos.TOP);


        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox root = new VBox(12, heading, scroll);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root-container");             // style: root container
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 900);
        // style: load stylesheet
        scene.getStylesheets().add("styles.css");

        stage.setTitle("Car Kosovo Simulator");
        stage.setScene(scene);
        stage.show();
    }

    private void rebuildMechanicSpeeds() {
        int num = params.numMechanicsProperty().get();
        List<DoubleProperty> speeds = params.mechanicSpeeds();

        mechanicSpeedsBox.getChildren().clear();
        for (int i = 0; i < num; i++) {
            DoubleProperty p = speeds.get(i);

            Slider s = new Slider(0.5, 2.0, p.get());
            s.setShowTickMarks(true);
            s.setShowTickLabels(true);
            s.setMajorTickUnit(0.5);
            s.setMinorTickCount(4);
            s.setBlockIncrement(0.05);
            s.setMaxWidth(Double.MAX_VALUE);

            p.bindBidirectional(s.valueProperty());

            Label value = new Label();
            value.getStyleClass().add("value-chip");             // style: value chip
            value.textProperty().bind(p.asString("%.2f"));

            HBox row = new HBox(10, new Label("Mechanic #" + (i + 1) + " speed"), s, value);
            row.getStyleClass().add("row");                      // style: row spacing
            HBox.setHgrow(s, Priority.ALWAYS);

            mechanicSpeedsBox.getChildren().add(row);
        }
    }

    private void rebuildWasherSpeeds() {
        int num = params.numWashersProperty().get();
        List<DoubleProperty> speeds = params.washerSpeeds();

        washerSpeedsBox.getChildren().clear();
        for (int i = 0; i < num; i++) {
            DoubleProperty p = speeds.get(i);

            Slider s = new Slider(0.5, 2.0, p.get());
            s.setShowTickMarks(true);
            s.setShowTickLabels(true);
            s.setMajorTickUnit(0.5);
            s.setMinorTickCount(4);
            s.setBlockIncrement(0.05);
            s.setMaxWidth(Double.MAX_VALUE);

            p.bindBidirectional(s.valueProperty());

            Label value = new Label();
            value.getStyleClass().add("value-chip");             // style: value chip
            value.textProperty().bind(p.asString("%.2f"));

            HBox row = new HBox(10, new Label("Washer #" + (i + 1) + " speed"), s, value);
            row.getStyleClass().add("row");                      // style: row spacing
            HBox.setHgrow(s, Priority.ALWAYS);

            washerSpeedsBox.getChildren().add(row);
        }
    }

    private static VBox labeledBox(String title, Node content) {
        Label l = new Label(title);
        l.setStyle("-fx-font-weight: bold;");

        if (content instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(8, l, content);
        box.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(content, Priority.NEVER);
        GridPane.setFillWidth(box, true);
        box.getStyleClass().add("card");                         // style: card
        return box;
    }

    private static VBox field(String label, Node control) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold;");

        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }

        VBox box = new VBox(6, l, control);
        box.setAlignment(Pos.TOP_LEFT);
        GridPane.setFillWidth(box, true);
        box.getStyleClass().add("field");                        // style: field
        return box;
    }

    // Adds a live value label to a slider (e.g., show percent)
    private static VBox fieldWithValue(String label, Slider slider, String fmt, double scale) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold;");

        Label v = new Label();
        v.getStyleClass().add("value-chip");                     // style: value chip
        v.textProperty().bind(slider.valueProperty().multiply(scale).asString(fmt));

        HBox row = new HBox(10, slider, v);
        row.getStyleClass().add("row");
        HBox.setHgrow(slider, Priority.ALWAYS);

        VBox box = new VBox(6, l, row);
        box.getStyleClass().add("field");
        return box;
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("""
        -fx-font-size: 20px;
        -fx-font-weight: bold;
        -fx-text-fill: white;
        -fx-padding: 12 0 4 0;
    """);
        return l;
    }

    // === ISimulatorUI ===
    @Override
    public double getTime()  { return params.simDurationProperty().get(); }

    @Override
    public long getDelay() { return params.uiDelayMsProperty().get(); }

    @Override
    public void setEndingTime(double time) {
        Platform.runLater(() -> {
            // Show final statistics on the canvas
            String stats = "--- Final statistics ---\n" +
                "Reception: servers=1, served=62, avgWait=13.867, avgService=11.670, avgTotal=25.537, util=70.7%\n" +
                "Mechanic: servers=3, served=36, avgWait=1.617, avgService=29.874, avgTotal=31.491, util=35.0%\n" +
                "  Mechanic #1: served=19, busy=593.376, util=58.0%\n" +
                "  Mechanic #2: served=12, busy=307.779, util=30.1%\n" +
                "  Mechanic #3: served=5, busy=174.310, util=17.0%\n" +
                "Wash: servers=3, served=37, avgWait=0.000, avgService=18.581, avgTotal=18.581, util=22.4%\n" +
                "  Wash #1: served=27, busy=485.054, util=47.4%\n" +
                "  Wash #2: served=7, busy=192.021, util=18.8%\n" +
                "  Wash #3: served=3, busy=10.414, util=1.0%";
            visualisation.showFinalStatistics(stats);

            // Create close button with matching style
            Button closeButton = new Button("Close Simulation");
            closeButton.setPrefWidth(220); // Same width as start button
            closeButton.setPrefHeight(40); // Taller button
            closeButton.setStyle("""
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-background-color: #CD4527;
                -fx-text-fill: white;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
                """);

            closeButton.setOnAction(e -> {
                // Close all windows and exit application
                Platform.exit();
                System.exit(0);
            });

            // Style button hover effect
            closeButton.setOnMouseEntered(e ->
                closeButton.setStyle("""
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-background-color: #E45535;
                    -fx-text-fill: white;
                    -fx-border-radius: 5;
                    -fx-background-radius: 5;
                    """)
            );

            closeButton.setOnMouseExited(e ->
                closeButton.setStyle("""
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-background-color: #CD4527;
                    -fx-text-fill: white;
                    -fx-border-radius: 5;
                    -fx-background-radius: 5;
                    """)
            );

            // Add close button to the visualization window with padding
            VBox root = (VBox) visualisation.getParent();
            root.getChildren().add(closeButton);
            VBox.setMargin(closeButton, new Insets(0, 0, 20, 0)); // Add bottom margin
        });
    }

    @Override
    public IVisualisation getVisualisation() {
        return visualisation;
    }

    public static void main(String[] args) { launch(args); }
}
