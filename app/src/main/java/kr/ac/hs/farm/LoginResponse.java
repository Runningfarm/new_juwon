package kr.ac.hs.farm;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private float weight;
    private String name;

    private long totalRunTime;

    private double totalDistance;
    private int totalCalories;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public float getWeight() {
        return weight;
    }

    public String getName() {
        return name;
    }

    public long getTotalRunTime() {
        return totalRunTime;
    }

    public double getTotalDistance() { return totalDistance; }

    public int getTotalCalories() { return totalCalories; }

}
