package kr.ac.hs.farm;

public class RunResultRequest {
    private String id;
    private double distance;
    private int time; // 초
    private int kcal;
    private double pace;

    public RunResultRequest(String id, double distance, int time, int kcal, double pace) {
        this.id = id;
        this.distance = distance;
        this.time = time;
        this.kcal = kcal;
        this.pace = pace;
    }

}
