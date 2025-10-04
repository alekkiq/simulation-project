package simu.view;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import simu.model.ISnapshotListener;
import simu.model.SimulationSnapshot;

public class Visualisation extends Canvas implements IVisualisation, ISnapshotListener {

	private final GraphicsContext gc;
	private volatile SimulationSnapshot last;

	private static final double PADDING = 12;
	private static final double SERVER_W = 36;
	private static final double SERVER_H = 40;
	private static final double SERVER_ROW_GAP = 8;
	private static final double SERVER_Q_GAP = 12;
	private static final double QUEUE_ITEM_R = 6;
	private static final double QUEUE_ITEM_GAP = 6;
	private static final double SPACING_AFTER_STATS = 14;

	// Color palette (centralized)
	private static final Color COLOR_BG = Color.WHITE;
	private static final Color COLOR_TEXT = Color.BLACK;
	private static final Color COLOR_SERVER_BUSY = Color.LIMEGREEN;
	private static final Color COLOR_SERVER_IDLE = Color.LIGHTGRAY;
	private static final Color COLOR_QUEUE = Color.CRIMSON;
	private static final Color COLOR_LABEL = Color.DIMGRAY;
	private static final Color COLOR_STATS_ALL = Color.DIMGRAY;
	private static final Color COLOR_STATS_PER = Color.GRAY;

	public Visualisation(int w, int h) {
		super(w, h);
		gc = getGraphicsContext2D();
		clearDisplay();
	}

	@Override
	public void clearDisplay() {
		gc.setFill(Color.rgb(245,245,245));
		gc.fillRect(0,0,getWidth(),getHeight());
		gc.setFill(Color.GRAY);
		gc.fillText("Waiting for simulation...", PADDING, PADDING + 12);
	}

	@Override public void newCustomer() {}

	@Override
	public void onSnapshot(SimulationSnapshot snapshot) {
		last = snapshot;
		Platform.runLater(this::render);
	}

	private void render() {
		SimulationSnapshot snap = last;
		if (snap == null) return;

		gc.setFill(COLOR_BG);
		gc.fillRect(0,0,getWidth(),getHeight());

		double y = PADDING;
		gc.setFill(COLOR_TEXT);
		gc.fillText(String.format("t=%.2f  Arrived=%d  Departed=%d",
				snap.clock, snap.totalArrived, snap.totalServed), PADDING, y + 12);
		y += 28;

		for (SimulationSnapshot.ServicePointState sp : snap.servicePoints) {
			y = drawServicePoint(sp, y) + SPACING_AFTER_STATS;
		}
	}

	private double drawServicePoint(SimulationSnapshot.ServicePointState sp, double yTop) {
		double x = PADDING;
		gc.setFill(COLOR_TEXT);
		gc.fillText(sp.name, x, yTop + 12);
		yTop += 18;

		// Single-server case
		if (sp.totalServers <= 1 || sp.perServerQueueLengths == null) {
			boolean busy = sp.busyServers > 0;
			gc.setFill(busy ? COLOR_SERVER_BUSY : COLOR_SERVER_IDLE);
			gc.fillRoundRect(x, yTop, SERVER_W, SERVER_H, 6, 6);
			gc.setStroke(Color.DARKGRAY);
			gc.strokeRoundRect(x, yTop, SERVER_W, SERVER_H, 6, 6);

			// Queue dots: previously inherited server color -> now explicit
			gc.setFill(COLOR_QUEUE);
			double qxStart = x + SERVER_W + SERVER_Q_GAP;
			drawQueueDots(qxStart, yTop + SERVER_H / 2.0 - QUEUE_ITEM_R, sp.queueLength);

			double statsY = yTop + SERVER_H + 14;
			drawAggregateStats(sp, x, statsY);
			return statsY;
		}

		// Multi-server vertical stack
		for (int i = 0; i < sp.totalServers; i++) {
			boolean busy = sp.perServerBusy != null && i < sp.perServerBusy.length && sp.perServerBusy[i];
			gc.setFill(busy ? COLOR_SERVER_BUSY : COLOR_SERVER_IDLE);
			double sy = yTop + i * (SERVER_H + SERVER_ROW_GAP);
			gc.fillRoundRect(x, sy, SERVER_W, SERVER_H, 6, 6);
			gc.setStroke(Color.DARKGRAY);
			gc.strokeRoundRect(x, sy, SERVER_W, SERVER_H, 6, 6);

			gc.setFill(COLOR_LABEL);
			gc.fillText("#" + (i + 1), x + 8, sy + SERVER_H / 2.0 + 4);

			int qlen = (sp.perServerQueueLengths != null && i < sp.perServerQueueLengths.length)
					? sp.perServerQueueLengths[i] : 0;
			gc.setFill(COLOR_QUEUE);
			double qxStart = x + SERVER_W + SERVER_Q_GAP;
			double qy = sy + SERVER_H / 2.0 - QUEUE_ITEM_R;
			drawQueueDots(qxStart, qy, qlen);
		}

		double blockHeight = sp.totalServers * SERVER_H + (sp.totalServers - 1) * SERVER_ROW_GAP;
		double statsY = yTop + blockHeight + 10;
		statsY = drawAggregateStats(sp, x, statsY) + 4;
		statsY = drawPerServerStats(sp, x, statsY);
		return statsY;
	}

	private double drawAggregateStats(SimulationSnapshot.ServicePointState sp, double x, double y) {
		gc.setFill(COLOR_STATS_ALL);
		gc.fillText(String.format("All: Served=%d  AvgWait=%.2f  AvgServ=%.2f  Util=%.0f%%",
				sp.served, sp.avgWait, sp.avgService, sp.utilization * 100.0), x, y);
		return y + 14;
	}

	private double drawPerServerStats(SimulationSnapshot.ServicePointState sp, double x, double y) {
		if (sp.perServerServed == null || sp.perServerBusyTimes == null) return y;
		for (int i = 0; i < sp.totalServers; i++) {
			int served = i < sp.perServerServed.length ? sp.perServerServed[i] : 0;
			double util = (i < sp.perServerUtilization.length ? sp.perServerUtilization[i] : 0.0) * 100.0;
			double avgServ = i < sp.perServerAvgService.length ? sp.perServerAvgService[i] : 0.0;
			gc.setFill(COLOR_STATS_PER);
			gc.fillText(String.format("  #%d: Served=%d  AvgServ=%.2f  Util=%.0f%%",
					i + 1, served, avgServ, util), x, y);
			y += 14;
		}
		return y;
	}

	private void drawQueueDots(double startX, double centerY, int count) {
		double x = startX;
		for (int i = 0; i < count; i++) {
			if (x + QUEUE_ITEM_R * 2 > getWidth() - PADDING) break;
			gc.fillOval(x, centerY, QUEUE_ITEM_R * 2, QUEUE_ITEM_R * 2);
			x += QUEUE_ITEM_R * 2 + QUEUE_ITEM_GAP;
		}
	}
}
