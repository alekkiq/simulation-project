package simu.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Visualisation2 extends Canvas implements IVisualisation {
    private GraphicsContext gc;

    public Visualisation2(int width, int height) {
        super(width, height);
        gc = this.getGraphicsContext2D();
        clear();
    }

    @Override
    public void clearDisplay() {
        clear();
    }

    private void clear() {
        gc.setFill(Color.BEIGE);
        gc.fillRect(0, 0, this.getWidth(), this.getHeight());
    }

    @Override
    public void newCustomer() {
        // Not used in this visualization
    }

    @Override
    public void updateServicePoints(int numMechanics, int numWashers) {
        // Not used in this visualization
    }

    @Override
    public void updateQueueLengths(int receptionQueue, int[] mechanicQueues, int[] washerQueues) {
        // Not used in this visualization
    }

    @Override
    public void moveCustomerToMechanic(int id, int mechanicId) {
        // Not used in this visualization
    }

    @Override
    public void moveCustomerToWasher(int id, int washerId) {
        // Not used in this visualization
    }

    @Override
    public void customerExit(int id) {
        // Not used in this visualization
    }
}
