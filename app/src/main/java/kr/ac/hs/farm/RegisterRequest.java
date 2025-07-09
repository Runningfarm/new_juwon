package kr.ac.hs.farm;

public class RegisterRequest {
    private String id;
    private String password;
    private float weight;

    public RegisterRequest(String id, String password, float weight) {
        this.id = id;
        this.password = password;
        this.weight = weight;
    }
}
