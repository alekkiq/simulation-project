package simu.view;

// File: DistributionOptionsDialog.java
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import simu.config.DistributionOptions;
import simu.config.DistributionOptions.DistributionType;

import java.util.Optional;

public final class DistributionOptionsDialog extends Dialog<DistributionOptions> {

    private final ComboBox<DistributionType> typeBox = new ComboBox<>();
    private final Spinner<Double> meanSp  = dSpinner(0.0001, Double.MAX_VALUE, 10.0, 0.1);
    private final Spinner<Double> stdSp   = dSpinner(0.0001, Double.MAX_VALUE, 5.0, 0.1);
    private final Spinner<Double> minSp   = dSpinner(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0.1);
    private final Spinner<Double> maxSp   = dSpinner(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.1);

    private final Label err = new Label();

    public DistributionOptionsDialog(String title, DistributionOptions initial) {
        setTitle(title);
        setHeaderText(null);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
            @Override public DistributionType fromString(String s) { return DistributionType.valueOf(s.toUpperCase()); }
        });

        // layout
        GridPane g = new GridPane();
        getDialogPane().setPrefSize(300, 250);
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(16));

        int r = 0;
        g.add(new Label("Type"), 0, r); g.add(typeBox, 1, r++);

        g.add(new Label("Mean"), 0, r); g.add(meanSp, 1, r++);
        g.add(new Label("Std dev"), 0, r); g.add(stdSp, 1, r++);
        g.add(new Label("Min"), 0, r); g.add(minSp, 1, r++);
        g.add(new Label("Max"), 0, r); g.add(maxSp, 1, r++);

        err.setStyle("-fx-text-fill: red;");
        HBox errRow = new HBox(err);
        errRow.setAlignment(Pos.CENTER_LEFT);
        g.add(errRow, 0, r, 2, 1);

        getDialogPane().setContent(g);

        // init from existing
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

        // show/hide fields by type
        typeBox.valueProperty().addListener((o, old, t) -> updateVisibility(g));
        updateVisibility(g);

        // disable OK when invalid + set result
        Node okBtn = getDialogPane().lookupButton(ButtonType.OK);
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

    private static Spinner<Double> dSpinner(double min, double max, double init, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        return sp;
    }

    private void updateVisibility(GridPane g) {
        DistributionType t = typeBox.getValue();
        // rows: Mean (row1), Std (row2), Min (row3), Max (row4)
        boolean showMean = (t == DistributionType.NEGEXP || t == DistributionType.NORMAL);
        boolean showStd  = (t == DistributionType.NORMAL);
        boolean showMin  = (t == DistributionType.UNIFORM);
        boolean showMax  = (t == DistributionType.UNIFORM);

        setRowVisible(g, 1, showMean); // Mean
        setRowVisible(g, 2, showStd);  // Std dev
        setRowVisible(g, 3, showMin);  // Min
        setRowVisible(g, 4, showMax);  // Max
    }

    private void setRowVisible(GridPane g, int rowIndex, boolean visible) {
        g.getChildren().stream()
                .filter(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == rowIndex)
                .forEach(n -> n.setVisible(visible));
        // also manage managed state so layout collapses
        g.getChildren().stream()
                .filter(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == rowIndex)
                .forEach(n -> n.setManaged(visible));
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

