package net.mooctest;

public class Vehicle {
    private String vehicleType; // e.g., "Standard Vehicle", "Heavy Vehicle"
    private double maxLoad; // Maximum load capacity
    private boolean requiresTollFreeRoute; // Indicates if toll roads should be avoided
    private double fuelCapacity; // Capacity of the fuel tank or battery
    private double currentFuel; // Current fuel or battery level
    private double fuelConsumptionPerKm; // Fuel/electricity consumption per kilometer
    private double minFuelAtEnd; // Minimum remaining fuel level upon reaching the destination
    private boolean emergencyVehicle; // Indicates if it is an emergency vehicle

    public Vehicle(String vehicleType, double maxLoad, boolean requiresTollFreeRoute,
                   double fuelCapacity, double currentFuel, double fuelConsumptionPerKm,
                   double minFuelAtEnd, boolean emergencyVehicle) {
        this.vehicleType = vehicleType;
        this.maxLoad = maxLoad;
        this.requiresTollFreeRoute = requiresTollFreeRoute;
        this.fuelCapacity = fuelCapacity;
        this.currentFuel = currentFuel;
        this.fuelConsumptionPerKm = fuelConsumptionPerKm;
        this.minFuelAtEnd = minFuelAtEnd;
        this.emergencyVehicle = emergencyVehicle;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public double getMaxLoad() {
        return maxLoad;
    }

    public boolean requiresTollFreeRoute() {
        return requiresTollFreeRoute;
    }

    public double getFuelCapacity() {
        return fuelCapacity;
    }

    public double getCurrentFuel() {
        return currentFuel;
    }

    public double getFuelConsumptionPerKm() {
        return fuelConsumptionPerKm;
    }

    public double getMinFuelAtEnd() {
        return minFuelAtEnd;
    }

    public boolean isEmergencyVehicle() {
        return emergencyVehicle;
    }

    public void consumeFuel(double distance) {
        this.currentFuel -= distance * fuelConsumptionPerKm;
    }

    public boolean needsRefueling(double distanceToEnd) {
        return (currentFuel - distanceToEnd * fuelConsumptionPerKm) < minFuelAtEnd;
    }

    public void refuel(double amount) {
        this.currentFuel = Math.min(fuelCapacity, this.currentFuel + amount);
    }
}
