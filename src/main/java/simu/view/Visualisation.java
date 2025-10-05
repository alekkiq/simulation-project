package simu.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.*;

public class Visualisation extends Canvas implements IVisualisation {
    private final GraphicsContext gc;
    private final Map<Integer, Customer> customers = new HashMap<>();
    private int nextCustomerId = 0;

    // Visual constants
    private static final int CUSTOMER_SIZE = 20;
    private static final int STATION_SIZE = 100;
    private static final double HALF_STATION = STATION_SIZE / 2.0;
    private static final double HALF_CUSTOMER = CUSTOMER_SIZE / 2.0;
    private static final double QUEUE_SPACING = 30;  // Increased spacing between customers

    // Colors
    private static final Color BACKGROUND_COLOR = Color.web("#f0f0f0");
    private static final Color RECEPTION_COLOR = Color.web("#64B5F6");  // Material Blue
    private static final Color MECHANIC_COLOR = Color.web("#81C784");   // Material Green
    private static final Color WASHER_COLOR = Color.web("#E57373");     // Material Red
    private static final Color EXIT_COLOR = Color.web("#90A4AE");       // Material Gray
    private static final Color CUSTOMER_COLOR = Color.web("#FFA000");   // Material Amber
    private static final Color TEXT_COLOR = Color.web("#37474F");       // Dark Gray
    private static final Color LINE_COLOR = Color.web("#BDBDBD");       // Light Gray

    // Service point positions and queues
    private final double receptionX = 200;  // Moved reception point right to make room for queue
    private final double receptionY = 250;
    private final List<Customer> receptionQueue = new ArrayList<>();

    private final List<Position> mechanicPositions = new ArrayList<>();
    private final Map<Integer, List<Customer>> mechanicQueues = new HashMap<>();

    private final List<Position> washerPositions = new ArrayList<>();
    private final Map<Integer, List<Customer>> washerQueues = new HashMap<>();

    private final double exitX = 1000;      // Moved exit point right to make room
    private final double exitY = 250;

    // Position helper class
    private static class Position {
        final double x;
        final double y;
        Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private String finalStatsText = null;

    public Visualisation(int w, int h) {
        super(w, h);
        gc = this.getGraphicsContext2D();
        clearDisplay();
    }

    public void reset() {
        customers.clear();
        receptionQueue.clear();
        mechanicQueues.values().forEach(List::clear);
        washerQueues.values().forEach(List::clear);
        finalStatsText = null;
        clearDisplay();
    }

    private void setupCanvas() {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(2);
    }

    @Override
    public void newCustomer(int id) {
        Customer customer = new Customer(id, receptionX, receptionY);
        customers.put(id, customer);
        receptionQueue.add(customer);
        clearAndRedraw();
    }

    private boolean isShowingStats() {
        return finalStatsText != null && !finalStatsText.isEmpty();
    }

    @Override
    public void clearDisplay() {
        setupCanvas();
        if (isShowingStats()) {
            drawFinalStatistics();
            return;
        }
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, this.getWidth(), this.getHeight());
        drawServicePoints();
        drawQueueInfo();
    }

    private void drawServicePoints() {
        // Draw connecting lines first (background)
        drawConnectingLines();

        // Draw service points with shadows and gradients
        drawServicePoint(receptionX, receptionY, RECEPTION_COLOR, "Reception");
        drawQueue(receptionQueue, receptionX, receptionY);

        // Draw Mechanics
        for (int i = 0; i < mechanicPositions.size(); i++) {
            Position pos = mechanicPositions.get(i);
            drawServicePoint(pos.x, pos.y, MECHANIC_COLOR, "Mechanic " + (i + 1));
            drawQueue(mechanicQueues.get(i), pos.x, pos.y);
        }

        // Draw Washers
        for (int i = 0; i < washerPositions.size(); i++) {
            Position pos = washerPositions.get(i);
            drawServicePoint(pos.x, pos.y, WASHER_COLOR, "Washer " + (i + 1));
            drawQueue(washerQueues.get(i), pos.x, pos.y);
        }

        // Draw Exit
        drawServicePoint(exitX, exitY, EXIT_COLOR, "Exit");
    }

    private void drawQueue(List<Customer> queue, double servicePointX, double servicePointY) {
        if (queue == null) return;

        // Position queue to the left of the service point
        double queueStartX = servicePointX - (STATION_SIZE / 2.0 + QUEUE_SPACING);
        double queueY = servicePointY; // Same Y level as service point

        // Draw customers in queue
        for (int i = 0; i < queue.size(); i++) {
            Customer customer = queue.get(i);
            double x = queueStartX - (i * QUEUE_SPACING);
            customer.setPosition(x, queueY);
            drawCustomer(customer);
        }
    }

    private void drawConnectingLines() {
        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(3);
        gc.setLineDashes(5);  // Single value creates uniform dashed line

        // Draw lines from reception to mechanics and washers
        for (Position mechanic : mechanicPositions) {
            drawArrowLine(receptionX + HALF_STATION, receptionY,
                         mechanic.x - HALF_STATION, mechanic.y);
        }

        for (Position washer : washerPositions) {
            drawArrowLine(receptionX + HALF_STATION, receptionY,
                         washer.x - HALF_STATION, washer.y);
        }

        // Draw lines to exit
        for (Position mechanic : mechanicPositions) {
            drawArrowLine(mechanic.x + HALF_STATION, mechanic.y,
                         exitX - HALF_STATION, exitY);
        }

        for (Position washer : washerPositions) {
            drawArrowLine(washer.x + HALF_STATION, washer.y,
                         exitX - HALF_STATION, exitY);
        }

        gc.setLineDashes(0);  // Reset to solid line
    }

    private void drawArrowLine(double startX, double startY, double endX, double endY) {
        double arrowLength = 15;

        // Calculate the angle of the line
        double angle = Math.atan2(endY - startY, endX - startX);

        // Draw the main line
        gc.strokeLine(startX, startY, endX, endY);

        // Draw the arrow head
        double x1 = endX - arrowLength * Math.cos(angle - Math.PI/6);
        double y1 = endY - arrowLength * Math.sin(angle - Math.PI/6);
        double x2 = endX - arrowLength * Math.cos(angle + Math.PI/6);
        double y2 = endY - arrowLength * Math.sin(angle + Math.PI/6);

        gc.strokeLine(endX, endY, x1, y1);
        gc.strokeLine(endX, endY, x2, y2);
    }

    private void drawServicePoint(double x, double y, Color baseColor, String label) {
        // Create gradient for 3D effect
        Stop[] stops = new Stop[]{
            new Stop(0, baseColor.brighter()),
            new Stop(1, baseColor.darker())
        };
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops
        );

        // Draw shadow
        gc.save();
        gc.setFill(Color.color(0, 0, 0, 0.3));
        gc.fillRoundRect(x - HALF_STATION + 4, y - HALF_STATION + 4,
                        STATION_SIZE, STATION_SIZE, 15, 15);
        gc.restore();

        // Draw service point
        gc.setFill(gradient);
        gc.fillRoundRect(x - HALF_STATION, y - HALF_STATION,
                        STATION_SIZE, STATION_SIZE, 15, 15);

        // Draw border
        gc.setStroke(baseColor.darker());
        gc.setLineWidth(2);
        gc.strokeRoundRect(x - HALF_STATION, y - HALF_STATION,
                          STATION_SIZE, STATION_SIZE, 15, 15);

        // Draw label
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        gc.setFill(TEXT_COLOR);
        double textWidth = gc.getFont().getSize() * label.length() * 0.5;
        gc.fillText(label, x - textWidth/2, y + HALF_STATION + 20);
    }

    @Override
    public void updateQueueLengths(int receptionQueue, int[] mechanicQueues, int[] washerQueues) {
        // Just trigger a redraw since we're using actual queue sizes
        clearAndRedraw();
    }

    @Override
    public void updateServicePoints(int numMechanics, int numWashers) {
        mechanicPositions.clear();
        washerPositions.clear();
        mechanicQueues.clear();
        washerQueues.clear();

        double startX = 300;
        double spacing = Math.max(220, (getWidth() - startX - 100) / Math.max(numMechanics, numWashers)); // Increased min spacing
        double offset = 40; // Amount to move the first mechanic/washer to the right

        // Position mechanics in top row
        for (int i = 0; i < numMechanics; i++) {
            double x = startX + (i * spacing);
            if (i == 0) x += offset; // Move first mechanic to the right
            mechanicPositions.add(new Position(x, 150));
            mechanicQueues.put(i, new ArrayList<>());
        }

        // Position washers in bottom row
        for (int i = 0; i < numWashers; i++) {
            double x = startX + (i * spacing);
            if (i == 0) x += offset; // Move first washer to the right
            washerPositions.add(new Position(x, 350));
            washerQueues.put(i, new ArrayList<>());
        }

        clearDisplay();
    }

    private void drawFinalStatistics() {
        if (finalStatsText != null && !finalStatsText.isEmpty()) {
            double width = getWidth();
            double height = getHeight();
            gc.setFill(Color.web("#2B2B2B"));
            gc.fillRect(0, 0, width, height);

            // Card background
            double cardPadX = 60;
            double cardPadY = 60;
            double cardWidth = width - 2 * cardPadX;
            double cardHeight = height - 2 * cardPadY;
            gc.setFill(Color.rgb(40,40,40,0.92));
            gc.fillRoundRect(cardPadX, cardPadY, cardWidth, cardHeight, 40, 40);
            gc.setStroke(Color.web("#CD4527"));
            gc.setLineWidth(8);
            gc.strokeRoundRect(cardPadX, cardPadY, cardWidth, cardHeight, 40, 40);

            // Prepare text
            String[] lines = finalStatsText.split("\\n");
            double maxTextWidth = cardWidth - 40;
            double maxTextHeight = cardHeight - 40;
            double fontSize = 32;
            Font statsFont;
            boolean fits = false;
            while (fontSize > 14 && !fits) {
                statsFont = Font.font("System", FontWeight.BOLD, fontSize);
                double widest = 0;
                for (String line : lines) {
                    Text textNode = new Text(line);
                    textNode.setFont(statsFont);
                    widest = Math.max(widest, textNode.getLayoutBounds().getWidth());
                }
                double totalHeight = lines.length * (fontSize + 12);
                if (widest <= maxTextWidth && totalHeight <= maxTextHeight) {
                    fits = true;
                } else {
                    fontSize -= 2;
                }
            }
            statsFont = Font.font("System", FontWeight.BOLD, fontSize);
            gc.setFont(statsFont);
            gc.setFill(Color.WHITE);
            double lineHeight = fontSize + 12;
            double totalHeight = lines.length * lineHeight;
            double startY = cardPadY + (cardHeight - totalHeight) / 2 + lineHeight;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Text textNode = new Text(line);
                textNode.setFont(statsFont);
                double textWidth = textNode.getLayoutBounds().getWidth();
                double x = cardPadX + (cardWidth - textWidth) / 2;
                double y = startY + i * lineHeight;
                gc.fillText(line, x, y);
            }
        }
    }

    private void drawQueueInfo() {
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setFill(TEXT_COLOR);

        // Reception queue - use actual queue size
        String receptionText = String.format("Queue: %d", receptionQueue.size());
        gc.fillText(receptionText, receptionX - 60, receptionY - HALF_STATION - 10);

        // Mechanic queues - use actual queue sizes
        for (int i = 0; i < mechanicPositions.size(); i++) {
            Position pos = mechanicPositions.get(i);
            String queueText = String.format("Queue: %d", mechanicQueues.get(i).size());
            gc.fillText(queueText, pos.x - 60, pos.y - HALF_STATION - 10);
        }

        // Washer queues - use actual queue sizes
        for (int i = 0; i < washerPositions.size(); i++) {
            Position pos = washerPositions.get(i);
            String queueText = String.format("Queue: %d", washerQueues.get(i).size());
            gc.fillText(queueText, pos.x - 60, pos.y - HALF_STATION - 10);
        }
    }

    private void drawCustomer(Customer customer) {
        double x = customer.getX();
        double y = customer.getY();

        // Draw shadow
        gc.setFill(Color.color(0, 0, 0, 0.3));
        gc.fillOval(x - HALF_CUSTOMER + 2, y - HALF_CUSTOMER + 2,
                    CUSTOMER_SIZE, CUSTOMER_SIZE);

        // Create gradient for customer
        Stop[] stops = new Stop[]{
            new Stop(0, CUSTOMER_COLOR.brighter()),
            new Stop(1, CUSTOMER_COLOR.darker())
        };
        RadialGradient gradient = new RadialGradient(
            0, 0, x, y, HALF_CUSTOMER,
            false, CycleMethod.NO_CYCLE, stops
        );

        // Draw customer
        gc.setFill(gradient);
        gc.fillOval(x - HALF_CUSTOMER, y - HALF_CUSTOMER,
                    CUSTOMER_SIZE, CUSTOMER_SIZE);

        // Draw border
        gc.setStroke(CUSTOMER_COLOR.darker());
        gc.setLineWidth(1.5);
        gc.strokeOval(x - HALF_CUSTOMER, y - HALF_CUSTOMER,
                      CUSTOMER_SIZE, CUSTOMER_SIZE);
    }

    private void clearAndRedraw() {
        if (isShowingStats()) {
            clearDisplay(); // Only draw stats, nothing else
            return;
        }
        clearDisplay();
        // Draw all customers in their current positions
        for (Customer customer : customers.values()) {
            drawCustomer(customer);
        }
    }

    public void moveCustomerToMechanic(int id, int mechanicId) {
        if (mechanicId >= 0 && mechanicId < mechanicPositions.size()) {
            Customer customer = customers.get(id);
            if (customer != null) {
                receptionQueue.remove(customer);
                for (List<Customer> queue : mechanicQueues.values()) {
                    queue.remove(customer);
                }

                Position pos = mechanicPositions.get(mechanicId);
                customer.setPosition(pos.x, pos.y);
                mechanicQueues.get(mechanicId).add(customer);
                clearAndRedraw();
            }
        }
    }

    public void moveCustomerToWasher(int id, int washerId) {
        if (washerId >= 0 && washerId < washerPositions.size()) {
            Customer customer = customers.get(id);
            if (customer != null) {
                receptionQueue.remove(customer);
                mechanicQueues.values().forEach(queue -> queue.remove(customer));

                Position pos = washerPositions.get(washerId);
                customer.setPosition(pos.x, pos.y);
                washerQueues.get(washerId).add(customer);
                clearAndRedraw();
            }
        }
    }

    public void customerExit(int id) {
        Customer customer = customers.get(id);
        if (customer != null) {
            receptionQueue.remove(customer);
            mechanicQueues.values().forEach(queue -> queue.remove(customer));
            washerQueues.values().forEach(queue -> queue.remove(customer));

            customer.setPosition(exitX, exitY);
            clearAndRedraw();
            customers.remove(id);
        }
    }
}
