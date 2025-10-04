package simu.view;

// File: DistributionOptionsDialog.java
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import simu.config.DistributionOptions;
import simu.config.DistributionOptions.DistributionType;

import java.util.Optional;
import java.util.regex.Pattern;

public final class DistributionOptionsDialog extends Dialog<DistributionOptions> {

    private final ComboBox<DistributionType> typeBox = new ComboBox<>();

    // Use large finite bounds; NOT ±Infinity
    private static final double MIN_BOUND = -1_000_000.0;
    private static final double MAX_BOUND =  1_000_000.0;

    private final Spinner<Double> meanSp = dSpinner(0.0001, MAX_BOUND, 10.0, 0.1);
    private final Spinner<Double> stdSp  = dSpinner(0.0001, MAX_BOUND, 5.0, 0.1);
    private final Spinner<Double> minSp  = dSpinner(MIN_BOUND, MAX_BOUND, 0.0, 0.1);
    private final Spinner<Double> maxSp  = dSpinner(MIN_BOUND, MAX_BOUND, 1.0, 0.1);

    private final Label err = new Label();

    public DistributionOptionsDialog(String title, DistributionOptions initial) {
        setTitle(title);
        setHeaderText(null);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Load app stylesheet so dialog matches the app theme
        getDialogPane().getStylesheets().add("styles.css");

        // Combo items + user-friendly labels
        typeBox.getItems().addAll(DistributionType.values());
        typeBox.setConverter(new StringConverter<>() {
            @Override public String toString(DistributionType t) {
                if (t == null) return "";
                switch (t) {
                    case NEGEXP: return "Negative Exponential";
                    case NORMAL: return "Normal";
                    case UNIFORM: return "Uniform";
                    default: return t.name();
                }
            }
            @Override public DistributionType fromString(String s) {
                return DistributionType.valueOf(s.toUpperCase());
            }
        });

        // Grid
        GridPane g = new GridPane();
        g.getStyleClass().addAll("dialog-grid");
        g.setPadding(Insets.EMPTY);

        int r = 0;
        g.add(styledLabel("Type"), 0, r); g.add(typeBox, 1, r++);

        g.add(styledLabel("Mean"), 0, r); g.add(meanSp, 1, r++);
        g.add(styledLabel("Std dev"), 0, r); g.add(stdSp, 1, r++);
        g.add(styledLabel("Min"), 0, r); g.add(minSp, 1, r++);
        g.add(styledLabel("Max"), 0, r); g.add(maxSp, 1, r++);

        err.getStyleClass().add("error-text");
        HBox errRow = new HBox(err);
        errRow.setAlignment(Pos.CENTER_LEFT);
        g.add(errRow, 0, r, 2, 1);

        // Header + card wrapper
        Label header = new Label("Distribution Options");
        header.getStyleClass().add("dialog-heading");

        VBox card = new VBox(g);
        card.getStyleClass().add("dialog-card");

        VBox body = new VBox(12, header, card);
        body.getStyleClass().add("dialog-body");

        getDialogPane().setContent(body);
        getDialogPane().setPrefSize(480, 360);

        // Init from existing
        if (initial != null) {
            typeBox.getSelectionModel().select(initial.getType());
            switch (initial.getType()) {
                case NEGEXP:
                    meanSp.getValueFactory().setValue(initial.getMean());
                    break;
                case NORMAL:
                    meanSp.getValueFactory().setValue(initial.getMean());
                    stdSp.getValueFactory().setValue(initial.getStdDev());
                    break;
                case UNIFORM:
                    minSp.getValueFactory().setValue(initial.getMin());
                    maxSp.getValueFactory().setValue(initial.getMax());
                    break;
            }
        } else {
            typeBox.getSelectionModel().select(DistributionType.NEGEXP);
        }

        // Visibility by type + sanitize editors on UNIFORM
        typeBox.valueProperty().addListener((o, old, t) -> updateVisibility(g));
        updateVisibility(g);

        // Disable OK when invalid + build result
        final Node okBtn = getDialogPane().lookupButton(ButtonType.OK);
        okBtn.getStyleClass().add("primary");
        okBtn.disableProperty().bind(new javafx.beans.binding.BooleanBinding() {
            { super.bind(typeBox.valueProperty(),
                    meanSp.valueProperty(), stdSp.valueProperty(),
                    minSp.valueProperty(), maxSp.valueProperty()); }
            @Override protected boolean computeValue() { return validate(false) != null; }
        });

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String errMsg = validate(true);
            if (errMsg != null) return null;
            DistributionType t = typeBox.getValue();
            switch (t) {
                case NEGEXP: return DistributionOptions.negExp(meanSp.getValue());
                case NORMAL: return DistributionOptions.normal(meanSp.getValue(), stdSp.getValue());
                case UNIFORM: return DistributionOptions.uniform(minSp.getValue(), maxSp.getValue());
                default: throw new IllegalStateException("Unhandled type " + t);
            }
        });
    }

    /** Spinner with safe parsing and editor guard */
    private static Spinner<Double> dSpinner(double min, double max, double init, double step) {
        Spinner<Double> sp = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory vf =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, init, step);

        // Robust converter (supports "," as decimal, avoids Infinity/NaN text)
        vf.setConverter(new StringConverter<>() {
            @Override public String toString(Double d) {
                if (d == null || d.isNaN() || d.isInfinite()) return "";
                return String.valueOf(d);
            }
            @Override public Double fromString(String s) {
                if (s == null) return vf.getValue();
                String t = s.trim().replace(',', '.');
                if (t.isEmpty() || "-".equals(t) || ".".equals(t)) return vf.getValue();
                return Double.parseDouble(t);
            }
        });

        sp.setValueFactory(vf);
        sp.setEditable(true);

        // Allow valid double patterns; allow transient edits
        Pattern valid = Pattern.compile("-?((\\d+\\.?\\d*)|(\\.\\d+))([eE][-+]?\\d+)?");
        TextFormatter<Double> tf = new TextFormatter<>(vf.getConverter(), vf.getValue(), change -> {
            String newText = change.getControlNewText().trim().replace(',', '.');
            if (newText.isEmpty() || "-".equals(newText) || ".".equals(newText)) return change;
            return valid.matcher(newText).matches() ? change : null;
        });

        sp.getEditor().setTextFormatter(tf);
        vf.valueProperty().bindBidirectional(tf.valueProperty());

        return sp;
    }

    private static Label styledLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("dialog-label");
        return l;
    }

    private void updateVisibility(GridPane g) {
        DistributionType t = typeBox.getValue();

        boolean showMean = (t == DistributionType.NEGEXP || t == DistributionType.NORMAL);
        boolean showStd  = (t == DistributionType.NORMAL);
        boolean showMin  = (t == DistributionType.UNIFORM);
        boolean showMax  = (t == DistributionType.UNIFORM);

        setRowVisible(g, 1, showMean); // Mean
        setRowVisible(g, 2, showStd);  // Std dev
        setRowVisible(g, 3, showMin);  // Min
        setRowVisible(g, 4, showMax);  // Max

        if (t == DistributionType.UNIFORM) {
            // Ensure editor text is a clean, parsable number to avoid NFE when clicking arrows
            syncEditor(minSp);
            syncEditor(maxSp);
        }
    }

    private void setRowVisible(GridPane g, int rowIndex, boolean visible) {
        g.getChildren().stream()
                .filter(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == rowIndex)
                .forEach(n -> { n.setVisible(visible); n.setManaged(visible); });
    }

    private static void syncEditor(Spinner<Double> sp) {
        SpinnerValueFactory.DoubleSpinnerValueFactory vf =
                (SpinnerValueFactory.DoubleSpinnerValueFactory) sp.getValueFactory();
        sp.getEditor().setText(vf.getConverter().toString(vf.getValue()));
    }

    /** @return null if valid, otherwise error message; optionally writes to label */
    private String validate(boolean write) {
        err.setText("");
        DistributionType t = typeBox.getValue();
        switch (t) {
            case NEGEXP:
                if (meanSp.getValue() <= 0) return write("Mean must be > 0 for NegExp");
                break;
            case NORMAL:
                if (meanSp.getValue() <= 0) return write("Mean must be > 0 for Normal");
                if (stdSp.getValue()  <= 0) return write("Std dev must be > 0 for Normal");
                break;
            case UNIFORM:
                if (minSp.getValue() >= maxSp.getValue()) return write("Min must be < Max for Uniform");
                break;
        }
        return null;
    }

    private String write(String m) { err.setText(m); return m; }

    // Convenience API
    public static Optional<DistributionOptions> show(String title, DistributionOptions initial) {
        DistributionOptionsDialog dlg = new DistributionOptionsDialog(title, initial);
        return dlg.showAndWait();
    }
}
