package simu.view;

import java.text.DecimalFormat;
import java.util.List;

import javafx.application.Application;
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
    private final Spinner<Double>  simulationDurationSpinner = new Spinner<>(); // simulation length
    private final Spinner<Integer> uiDelaySpinner            = new Spinner<>(); // UI delay (ms)

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
    private final VBox mechanicSpeedsBox = new VBox(10);
    private final VBox washerSpeedsBox   = new VBox(10);

    // --- Model ---
    private final SimParameters params = new SimParameters();

    // --- Controller ---
    private IControllerVtoM controller;

    // -------- helpers --------
    private static Slider makeProbSlider(double def) {
        Slider s = new Slider(0, 1, def);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.setMajorTickUnit(0.25);
        s.setMinorTickCount(4);
        s.setBlockIncrement(0.01);
        s.setSnapToTicks(true);
        s.setMaxWidth(Double.MAX_VALUE); // let grid stretch it
        return s;
    }

    @Override
    public void init() {
        Trace.setTraceLevel(Level.INFO);
        controller = new Controller(this, params); // ensure such ctor exists
    }

    @Override
    public void start(Stage stage) {
        // --- Spinners with explicit factories (type-safe) ---
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
                        0, 50, params.numMechanicsProperty().get(), 1
                )
        );
        numWashersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 50, params.numWashersProperty().get(), 1
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
        editInterBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Inter-arrival", params.interArrivalProperty().get())
                        .ifPresent(params.interArrivalProperty()::set)
        );
        Button editReceptionBtn = new Button("Edit Reception…");
        editReceptionBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Reception service", params.receptionServiceProperty().get())
                        .ifPresent(params.receptionServiceProperty()::set)
        );
        Button editMechanicBtn = new Button("Edit Mechanic…");
        editMechanicBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Mechanic service", params.mechanicServiceProperty().get())
                        .ifPresent(params.mechanicServiceProperty()::set)
        );
        Button editWashBtn = new Button("Edit Wash…");
        editWashBtn.setOnAction(e ->
                DistributionOptionsDialog.show("Wash service", params.washServiceProperty().get())
                        .ifPresent(params.washServiceProperty()::set)
        );

        // --- Layout ---
        Label heading = new Label("Car Kosovo Simulator");
        heading.setStyle("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-padding: 0 0 10 0;
        """);

        GridPane form = new GridPane();
        form.setHgap(24);
        form.setVgap(16);
        form.setPadding(new Insets(16));

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
        form.add(field("Simulation duration", simulationDurationSpinner), 0, r);
        form.add(field("UI delay (ms)", uiDelaySpinner),                 1, r++);

        form.add(field("Number of Mechanics", numMechanicsSpinner), 0, r);
        form.add(field("Number of Washers",   numWashersSpinner),   1, r++);

        form.add(field("P(Needs Mechanic)", probNeedsMechanicSlider), 0, r);
        form.add(field("P(Needs Wash)",     probNeedsWashSlider),     1, r++);

        form.add(field("Wash: Exterior", probWashExteriorSlider), 0, r);
        form.add(field("Wash: Interior", probWashInteriorSlider), 1, r++);
        form.add(field("Wash: Both",     probWashBothSlider),     0, r);
        form.add(field("Wash split sum", washSplitSumLabel),      1, r++);

        form.add(field("Inter-arrival",       editInterBtn),     0, r);
        form.add(field("Reception service",   editReceptionBtn), 1, r++);
        form.add(field("Mechanic service",    editMechanicBtn),  0, r);
        form.add(field("Wash service",        editWashBtn),      1, r++);

        // Dynamic sections spanning both columns
        form.add(labeledBox("Mechanic speeds (0.5x–2.0x)", mechanicSpeedsBox), 0, r++, 2, 1);
        form.add(labeledBox("Washer speeds (0.5x–2.0x)",   washerSpeedsBox),   0, r++, 2, 1);

        // Start button row
        Button startButton = new Button("Start Simulation");
        startButton.setPrefWidth(220);

        startButton.setOnAction(e -> {
            System.out.println(params);
        });

        HBox startRow = new HBox(startButton);
        startRow.setAlignment(Pos.CENTER);
        startRow.setPadding(new Insets(8, 0, 0, 0));

        form.add(startRow, 0, r, 2, 1);
        GridPane.setValignment(startRow, VPos.TOP);

        // single scroll container around the form
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox root = new VBox(12, heading, scroll);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(20));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        stage.setTitle("Car Kosovo Simulator");
        stage.setScene(new Scene(root, 900, 900));
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

            // bidirectional: slider <-> property
            p.bindBidirectional(s.valueProperty());

            Label value = new Label();
            value.textProperty().bind(p.asString("%.2f"));

            HBox row = new HBox(10, new Label("Mechanic #" + (i + 1) + " speed"), s, value);
            row.setAlignment(Pos.CENTER_LEFT);
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
            value.textProperty().bind(p.asString("%.2f"));

            HBox row = new HBox(10, new Label("Washer #" + (i + 1) + " speed"), s, value);
            row.setAlignment(Pos.CENTER_LEFT);
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
        return box;
    }

    private static VBox field(String label, Node control) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold;");

        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE); // stretch within grid cell
        }

        VBox box = new VBox(6, l, control);
        box.setAlignment(Pos.TOP_LEFT);
        GridPane.setFillWidth(box, true);
        return box;
    }

    // === ISimulatorUI ===
    @Override
    public double getTime()  { return params.simDurationProperty().get(); }

    @Override
    public long getDelay() { return params.uiDelayMsProperty().get(); }

    @Override
    public void setEndingTime(double time) {
        DecimalFormat formatter = new DecimalFormat("#0.00");
        // e.g., resultsLabel.setText(formatter.format(time));
    }

    @Override
    public IVisualisation getVisualisation() { return null; }

    public static void main(String[] args) { launch(args); }
}
