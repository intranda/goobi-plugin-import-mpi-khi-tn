package de.intranda.goobi.plugins;

public class LocationWithDist {
    private int x, y;
    private double distance;

    public LocationWithDist(int x, int y, double distance) {
        super();
        this.x = x;
        this.y = y;
        this.distance = distance;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getDistance() {
        return distance;
    }

}
