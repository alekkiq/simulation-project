package simu.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.util.*;

public class Visualisation extends Canvas implements IVisualisation {
    private final GraphicsContext gc;
    private final Map<Integer, Customer> customers = new HashMap<>();

    // Visual constants
    private static final int CUSTOMER_SIZE = 26;
    private static final double CAR_DRAW_WIDTH = 34;
    private static final double CAR_DRAW_HEIGHT = 28;
    private static final double IN_SERVICE_SCALE = 1.5; // Make in-service cars larger
    private static final int STATION_SIZE = 80;
    private static final double HALF_STATION = STATION_SIZE / 2.0;
    private static final double HALF_CUSTOMER = CUSTOMER_SIZE / 2.0;
    private static final double QUEUE_SPACING = CAR_DRAW_WIDTH + 12;  // Increased spacing between customers
    private static final int MAX_VISIBLE_QUEUE = 4; // Max customers to show in queue -> the rest are shown in a counter
    private static final double QUEUE_OVERFLOW_PADDING = 6;

    // Colors
    private static final Color BACKGROUND_COLOR = Color.web("#0e1116");
    private static final Color RECEPTION_COLOR = Color.web("#3a3f44");
    private static final Color MECHANIC_COLOR = Color.web("#CD4527");
    private static final Color WASHER_COLOR = Color.web("#1E8074");
    private static final Color EXIT_COLOR = Color.web("#151a20");
    private static final Color CUSTOMER_COLOR = Color.web("#e6eaf0");
    private static final Color CUSTOMER_POST_MECH_COLOR = Color.web("#CD4527");
    private static final Color TEXT_COLOR = Color.web("#e6eaf0");
    private static final Color LINE_COLOR = Color.web("#262c36");
    private static final Color OVERFLOW_BG = Color.web("#151a20");
    private static final Color OVERFLOW_BORDER = Color.web("#262c36");

    // Car sprite fields
    private static final String[] CAR_SPRITES = {
        "img/coupe_blue.png",
        "img/sedan_green.png",
        "img/sport_red.png",
        "img/van_white.png"
    };
    private final List<Image> carImages = new ArrayList<>();
    private final Random rng = new Random();


    // Service point positions and queues
    private static final double CANVAS_SHIFT_X = 80;
    private double receptionX = 200 + CANVAS_SHIFT_X;  // Moved reception point right to make room for queue
    private final double receptionY = 250;
    private double exitX = 1000  + CANVAS_SHIFT_X;      // Moved exit point right to make room
    private final double exitY = 250;

    // General layout constants
    private static final double BASE_EDGE_GAP = 160;
    private static final double LEFT_MARGIN = 100;
    private static final double RIGHT_MARGIN = 10;

    private final List<Customer> receptionQueue = new ArrayList<>();
    private final List<Customer> checkoutQueue = new ArrayList<>();

    private final List<Position> mechanicPositions = new ArrayList<>();
    private final Map<Integer, List<Customer>> mechanicQueues = new HashMap<>();

    private final List<Position> washerPositions = new ArrayList<>();
    private final Map<Integer, List<Customer>> washerQueues = new HashMap<>();


    // Position helper class
    private static class Position {
        final double x;
        final double y;
        Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public Visualisation(int w, int h) {
        super(w, h);
        gc = this.getGraphicsContext2D();
        loadCarSprites();
        clearDisplay();
    }

    public void reset() {
        customers.clear();
        receptionQueue.clear();
        mechanicQueues.values().forEach(List::clear);
        washerQueues.values().forEach(List::clear);
        checkoutQueue.clear();
        clearDisplay();
    }

    private void setupCanvas() {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(2);
    }

    private void loadCarSprites() {
        for (String path : CAR_SPRITES) {
            try (InputStream is = getClass().getResourceAsStream("/" + path)) {
                if (is != null) {
                    carImages.add(new Image(is, CAR_DRAW_WIDTH, CAR_DRAW_HEIGHT, true, true));
                }
            } catch (Exception e) {
                System.err.println("Failed to load sprite: " + path + " -> " + e.getMessage());
            }
        }
    }

    @Override
    public void newCustomer(int id) {
        Image sprite = carImages.isEmpty() ? null : carImages.get(rng.nextInt(carImages.size()));
        Customer customer = new Customer(id, receptionX, receptionY, sprite);
        customers.put(id, customer);
        receptionQueue.add(customer);
        clearAndRedraw();
    }

    @Override
    public void clearDisplay() {
        setupCanvas();
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, this.getWidth(), this.getHeight());
        drawServicePoints();
        drawQueueInfo();
    }

    /**
     * Helper to check if a customer is in any queue
     * Fixes a bug where a customer could be drawn multiple times
     * @param c Customer to check
     * @return true if in any queue, false otherwise
     */
    private boolean isInAnyQueue(Customer c) {
        if (receptionQueue.contains(c)) return true;
        if (checkoutQueue.contains(c)) return true;
        for (List<Customer> q : mechanicQueues.values()) if (q.contains(c)) return true;
        for (List<Customer> q : washerQueues.values()) if (q.contains(c)) return true;
        return false;
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
        drawServicePoint(exitX, exitY, EXIT_COLOR, "Checkout");
        drawQueue(checkoutQueue, exitX, exitY);
    }

    private void drawQueue(List<Customer> queue, double servicePointX, double servicePointY) {
        if (queue == null || queue.isEmpty()) return;

        // Customer at index 0 = currently in service (draw centered on station)
        Customer inService = queue.get(0);
        inService.setPosition(servicePointX, servicePointY);
        drawCustomer(inService, true);

        int waiting = queue.size() - 1;
        if (waiting <= 0) return;

        int visibleWaiting = Math.min(waiting, MAX_VISIBLE_QUEUE);
        double queueStartX = servicePointX - (STATION_SIZE / 2.0 + QUEUE_SPACING);

        // Draw waiting customers to the left
        for (int i = 0; i < visibleWaiting; i++) {
            Customer c = queue.get(i + 1);
            double x = queueStartX - (i * QUEUE_SPACING);
            c.setPosition(x, servicePointY);
            drawCustomer(c, false);
        }

        // Overflow badge if more are hidden
        if (waiting > MAX_VISIBLE_QUEUE) {
            int hidden = waiting - MAX_VISIBLE_QUEUE;
            double nextSlotCenter = queueStartX - (visibleWaiting * QUEUE_SPACING);
            double badgeX = nextSlotCenter - 8;
            drawOverflowBadge(badgeX, servicePointY, hidden);
        }
    }

    private void drawOverflowBadge(double centerX, double centerY, int count) {
        String text = "(+" + count + ")";
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));

        double textWidth = gc.getFont().getSize() * text.length() * 0.52;
        double textHeight = gc.getFont().getSize() + 4;

        double w = textWidth + QUEUE_OVERFLOW_PADDING * 2;
        double h = textHeight + 2;
        double x = centerX - w / 2;
        double y = centerY - h / 2;

        gc.setFill(OVERFLOW_BG);
        gc.fillRoundRect(x, y, w, h, 10, 10);

        gc.setStroke(OVERFLOW_BORDER);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, 10, 10);

        // Text
        gc.setFill(TEXT_COLOR);
        gc.fillText(text, centerX - textWidth / 2, centerY + (gc.getFont().getSize() / 3.0));
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

        // Mechanics -> Washers
        for (Position mechanic : mechanicPositions) {
            for (Position washer : washerPositions) {
                drawArrowLine(
                        mechanic.x, mechanic.y + HALF_STATION,
                        washer.x, washer.y - HALF_STATION
                );
            }
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

        int maxServers = Math.max(numMechanics, numWashers);

        // Keep reception fully visible (left margin for its full queue footprint)
        double neededLeft = leftQueueFootprint() + LEFT_MARGIN;
        if (receptionX < neededLeft) receptionX = neededLeft;

        // Internal spacing between service points (large enough so their left queues don't overlap)
        double minSpacing = computeMinCenterSpacing();
        double spacing = (maxServers <= 1) ? 0 : minSpacing;

        // Small fixed symmetric edge gap (Reception -> first, last -> Exit)
        double startX = receptionX + BASE_EDGE_GAP;

        // Mechanics
        for (int i = 0; i < numMechanics; i++) {
            double x = startX + i * spacing;
            mechanicPositions.add(new Position(x, 150));
            mechanicQueues.put(i, new ArrayList<>());
        }

        // Washers
        for (int i = 0; i < numWashers; i++) {
            double x = startX + i * spacing;
            washerPositions.add(new Position(x, 350));
            washerQueues.put(i, new ArrayList<>());
        }

        double lastServerX = (maxServers > 0) ? (startX + (maxServers - 1) * spacing) : startX;

        // Symmetric small gap to exit
        exitX = lastServerX + BASE_EDGE_GAP;

        // Ensure canvas wide enough so exit queue (including overflow) is visible
        double exitRightFootprint = HALF_STATION + leftQueueFootprint();
        double requiredWidth = Math.max(
                exitX + exitRightFootprint + RIGHT_MARGIN,
                receptionX + HALF_STATION + RIGHT_MARGIN
        );
        if (getWidth() < requiredWidth) setWidth(requiredWidth);

        clearDisplay();
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

        // Checkout / Exit queue
        String checkoutText = String.format("Queue: %d", checkoutQueue.size());
        gc.fillText(checkoutText, exitX - 60, exitY - HALF_STATION - 10);
    }

    private void drawCustomer(Customer customer, boolean cInService) {
        double x = customer.getX();
        double y = customer.getY();
        Image sprite = customer.getImage();
        double scale = cInService ? IN_SERVICE_SCALE : 1.0;

        if (sprite != null) {
            double w = CAR_DRAW_WIDTH * scale;
            double h = CAR_DRAW_HEIGHT * scale;

            // Shadow
            gc.setFill(Color.color(0, 0, 0, 0.35));
            gc.fillOval(x - w / 2, y - h / 2 + 4, w, h);

            // Car sprite
            gc.drawImage(sprite, x - w / 2, y - h / 2, w, h);

            // Accent border ONLY if mechanic -> washer path
            if (customer.afterMechanicInWasher) {
                gc.setStroke(CUSTOMER_POST_MECH_COLOR);
                gc.setLineWidth(3);
                gc.strokeRoundRect(
                    x - w / 2 - 2,
                    y - h / 2 - 2,
                    w + 4,
                    h + 4,
                    10 * scale,
                    10 * scale
                );
            }
            return;
        }

        // Fallback circle
        double r = (CUSTOMER_SIZE * scale) / 2.0;
        gc.setFill(Color.color(0, 0, 0, 0.3));
        gc.fillOval(x - r + 2, y - r + 2, r * 2, r * 2);

        Color base = customer.afterMechanicInWasher ? CUSTOMER_POST_MECH_COLOR : CUSTOMER_COLOR;
        Stop[] stops = new Stop[]{
                new Stop(0, base.brighter()),
                new Stop(1, base.darker())
        };
        RadialGradient gradient = new RadialGradient(
                0, 0, x, y, r,
                false, CycleMethod.NO_CYCLE, stops
        );
        gc.setFill(gradient);
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        gc.setStroke(base.darker());
        gc.setLineWidth(1.5);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);
    }

    private double computeMinCenterSpacing() {
        // Queue extends left: QUEUE_SPACING * MAX_VISIBLE_QUEUE + half car width
        double queueExtent = (QUEUE_SPACING * MAX_VISIBLE_QUEUE) + (CAR_DRAW_WIDTH / 2.0);
        double margin = 20;
        double minSpacing = STATION_SIZE + queueExtent + margin; // ensures no overlap
        return Math.max(minSpacing, 320); // keep a visual floor
    }

    private double leftQueueFootprint() {
        // Station half + one in-service (center) not needed (we start from center),
        // visible queue cars: MAX_VISIBLE_QUEUE each shifted QUEUE_SPACING starting one slot left of station edge,
        // plus one extra slot for overflow badge, plus half car for badge width approximation.
        return (STATION_SIZE / 2.0)
                + (QUEUE_SPACING * MAX_VISIBLE_QUEUE)
                + QUEUE_SPACING
                + (CAR_DRAW_WIDTH / 2.0)
                + 8;
    }

    private void clearAndRedraw() {
        clearDisplay(); // draws service points + queues (which already draw queued customers)
        for (Customer c : customers.values()) {
            if (!isInAnyQueue(c)) {
                // e.g. car at exit (just before removal) or any future free-floating states
                drawCustomer(c, false);
            }
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

                customer.visitedMechanic = true;
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

                if (customer.visitedMechanic) {
                    customer.afterMechanicInWasher = true;
                }

                washerQueues.get(washerId).add(customer);
                clearAndRedraw();
            }
        }
    }

    public void moveCustomerToCheckout(int id) {
        Customer c = customers.get(id);

        if (c == null) return;

        receptionQueue.remove(c);
        mechanicQueues.values().forEach(q -> q.remove(c));
        washerQueues.values().forEach(q -> q.remove(c));

        if (!checkoutQueue.contains(c)) checkoutQueue.add(c);

        clearAndRedraw();
    }

    public void customerExit(int id) {
        Customer customer = customers.get(id);
        if (customer != null) {
            receptionQueue.remove(customer);
            mechanicQueues.values().forEach(queue -> queue.remove(customer));
            washerQueues.values().forEach(queue -> queue.remove(customer));
            checkoutQueue.remove(customer);

            customer.setPosition(exitX, exitY);
            clearAndRedraw();
            customers.remove(id);
        }
    }
}
