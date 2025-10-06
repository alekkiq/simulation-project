package simu.view;

import javafx.scene.image.Image;

public class Customer {
    private int id;
    private double x;
    private double y;
    boolean visitedMechanic = false;
    boolean afterMechanicInWasher = false;
    private Image image;

    public Customer(int id, double x, double y, Image image) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Image getImage() {
        return image;
    }
}
