package net.mooctest;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private int nodeId;
    private boolean isObstacle;
    private List<Edge> neighbors; // Adjacent nodes and distances
    private String roadType; // Type of road (e.g., highway, regular road, toll road)
    private boolean tollRoad; // Indicates if it is a toll road
    private boolean restrictedForHeavyVehicles; // Indicates if it restricts heavy vehicles
    private boolean highRiskArea; // Indicates if it is a high-risk area
    private double costPerKm; // Fuel or energy cost per kilometer
    private int openTime;  // Road open start time (in hours)
    private int closeTime; // Road close time

    public Node(int nodeId, boolean isObstacle, String roadType, boolean tollRoad,
                boolean restrictedForHeavyVehicles, boolean highRiskArea,
                double costPerKm, int openTime, int closeTime) {
        this.nodeId = nodeId;
        this.isObstacle = isObstacle;
        this.roadType = roadType;
        this.tollRoad = tollRoad;
        this.restrictedForHeavyVehicles = restrictedForHeavyVehicles;
        this.highRiskArea = highRiskArea;
        this.costPerKm = costPerKm;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.neighbors = new ArrayList<>();
    }

    public int getNodeId() {
        return nodeId;
    }

    public boolean isObstacle() {
        return isObstacle;
    }

    public List<Edge> getNeighbors() {
        return neighbors;
    }

    public String getRoadType() {
        return roadType;
    }

    public boolean isTollRoad() {
        return tollRoad;
    }

    public boolean isRestrictedForHeavyVehicles() {
        return restrictedForHeavyVehicles;
    }

    public boolean isHighRiskArea() {
        return highRiskArea;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    public boolean isOpenAt(int currentTime) {
        return currentTime >= openTime && currentTime <= closeTime;
    }

    public void addNeighbor(Node neighbor, double distance) {
        if (!neighbor.isObstacle()) {
            this.neighbors.add(new Edge(neighbor, distance));
        }
    }
}
