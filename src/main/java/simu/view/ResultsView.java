package simu.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import simu.model.SimulationData;

public class ResultsView {

    private final Stage stage;

    public ResultsView(Stage owner, SimulationData data, Runnable onClose) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Simulation Results");
        stage.setResizable(false);
        stage.setScene(buildScene(data, onClose));
    }

    public void show() { stage.show(); }

    private Scene buildScene(SimulationData data, Runnable onClose) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        root.getStyleClass().add("results-root");

        Label heading = new Label("Simulation Results");
        heading.setFont(Font.font("System", FontWeight.BOLD, 28));
        heading.getStyleClass().add("results-heading");

        GridPane summary = new GridPane();
        summary.setHgap(30);
        summary.setVgap(8);

        addSummaryBlock(summary, 0, "Reception",
                data.getReceptionServers(), data.getReceptionServed(),
                data.getReceptionAvgWait(), data.getReceptionAvgService(),
                data.getReceptionAvgTotal(), data.getReceptionUtil());

        addSummaryBlock(summary, 1, "Mechanic",
                data.getMechanicServers(), data.getMechanicServed(),
                data.getMechanicAvgWait(), data.getMechanicAvgService(),
                data.getMechanicAvgTotal(), data.getMechanicUtil());

        addSummaryBlock(summary, 2, "Wash",
                data.getWashServers(), data.getWashServed(),
                data.getWashAvgWait(), data.getWashAvgService(),
                data.getWashAvgTotal(), data.getWashUtil());

        TableView<Row> table = new TableView<>();
        table.getStyleClass().add("results-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("")); // no 'No content...' label

        TableColumn<Row,String> cName = new TableColumn<>("Point");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Row,Integer> cServ = new TableColumn<>("Served");
        cServ.setCellValueFactory(new PropertyValueFactory<>("served"));
        cServ.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Row,Integer> cSrv = new TableColumn<>("Srv");
        cSrv.setCellValueFactory(new PropertyValueFactory<>("servers"));
        cSrv.setStyle("-fx-alignment: CENTER;");

        TableColumn<Row,String> cWait = new TableColumn<>("Avg Wait");
        cWait.setCellValueFactory(new PropertyValueFactory<>("avgWait"));
        cWait.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Row,String> cSvc = new TableColumn<>("Avg Service");
        cSvc.setCellValueFactory(new PropertyValueFactory<>("avgService"));
        cSvc.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Row,String> cTot = new TableColumn<>("Avg Total");
        cTot.setCellValueFactory(new PropertyValueFactory<>("avgTotal"));
        cTot.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Row,String> cUtil = new TableColumn<>("Util %");
        cUtil.setCellValueFactory(new PropertyValueFactory<>("utilPct"));
        cUtil.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(cName,cServ,cSrv,cWait,cSvc,cTot,cUtil);

        // Aggregated rows (flag aggregate=true)
        table.getItems().add(new Row("Reception", data.getReceptionServed(), data.getReceptionServers(),
                data.getReceptionAvgWait(), data.getReceptionAvgService(), data.getReceptionAvgTotal(), data.getReceptionUtil(), true));

        table.getItems().add(new Row("Mechanic (all)", data.getMechanicServed(), data.getMechanicServers(),
                data.getMechanicAvgWait(), data.getMechanicAvgService(), data.getMechanicAvgTotal(), data.getMechanicUtil(), true));

        table.getItems().add(new Row("Wash (all)", data.getWashServed(), data.getWashServers(),
                data.getWashAvgWait(), data.getWashAvgService(), data.getWashAvgTotal(), data.getWashUtil(), true));

        // Per-server mechanic rows
        int[] mServed = data.getMechanicServedPerServer();
        double[] mUtil = data.getMechanicUtilPerServer();
        for (int i = 0; i < mServed.length; i++) {
            table.getItems().add(new Row("Mechanic #" + (i + 1),
                    mServed[i], 1,
                    data.getMechanicAvgWait(), data.getMechanicAvgService(), data.getMechanicAvgTotal(),
                    mUtil[i], false));
        }

        // Per-server wash rows
        int[] wServed = data.getWashServedPerServer();
        double[] wUtil = data.getWashUtilPerServer();
        for (int i = 0; i < wServed.length; i++) {
            table.getItems().add(new Row("Wash #" + (i + 1),
                    wServed[i], 1,
                    data.getWashAvgWait(), data.getWashAvgService(), data.getWashAvgTotal(),
                    wUtil[i], false));
        }

        // Row styling (bold aggregate)
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Row item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isAggregate()) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #40464b;");
                } else {
                    setStyle("");
                }
            }
        });

        // Remove extra blank visual rows: fix cell size & bind height to item count
        double cellH = 28;
        table.setFixedCellSize(cellH);
        table.prefHeightProperty().bind(
                Bindings.size(table.getItems()).multiply(table.getFixedCellSize())
                        .add(table.getFixedCellSize() + 6) // header + padding
        );
        table.minHeightProperty().bind(table.prefHeightProperty());
        table.maxHeightProperty().bind(table.prefHeightProperty());

        Button closeBtn = new Button("Close Results");
        closeBtn.getStyleClass().add("results-close-btn");
        closeBtn.setOnAction(e -> {
            stage.close();
            if (onClose != null) onClose.run();
        });

        root.getChildren().addAll(heading, summary, new Separator(), table, closeBtn);

        Scene sc = new Scene(root, 920, 640);
        sc.getStylesheets().add("results.css");
        return sc;
    }

    private void addSummaryBlock(GridPane gp, int col, String title,
                                 int servers, int served,
                                 double avgWait, double avgService,
                                 double avgTotal, double util) {
        VBox box = new VBox(4);
        box.getStyleClass().add("results-block");
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 16));
        t.getStyleClass().add("results-block-title");
        Label l1 = labelKV("Servers", String.valueOf(servers));
        Label l2 = labelKV("Served", String.valueOf(served));
        Label l3 = labelKV("Avg Wait", fmt(avgWait));
        Label l4 = labelKV("Avg Service", fmt(avgService));
        Label l5 = labelKV("Avg Total time", fmt(avgTotal));
        Label l6 = labelKV("Utilization %", pct(util));
        box.getChildren().addAll(t,l1,l2,l3,l4,l5,l6);
        gp.add(box, col, 0);
    }

    private Label labelKV(String k, String v) {
        Label l = new Label(k + ": " + v);
        l.getStyleClass().add("results-metric");
        return l;
    }

    private static String fmt(double v) { return String.format("%.3f", v); }
    private static String pct(double v) { return String.format("%.1f", v * 100.0); }

    public static class Row {
        private final String name;
        private final int served;
        private final int servers;
        private final String avgWait;
        private final String avgService;
        private final String avgTotal;
        private final String utilPct;
        private final boolean aggregate;

        public Row(String name, int served, int servers,
                   double avgWait, double avgService, double avgTotal, double util,
                   boolean aggregate) {
            this.name = name;
            this.served = served;
            this.servers = servers;
            this.avgWait = fmt(avgWait);
            this.avgService = fmt(avgService);
            this.avgTotal = fmt(avgTotal);
            this.utilPct = pct(util);
            this.aggregate = aggregate;
        }

        public String getName() { return name; }
        public int getServed() { return served; }
        public int getServers() { return servers; }
        public String getAvgWait() { return avgWait; }
        public String getAvgService() { return avgService; }
        public String getAvgTotal() { return avgTotal; }
        public String getUtilPct() { return utilPct; }
        public boolean isAggregate() { return aggregate; }
    }
}
