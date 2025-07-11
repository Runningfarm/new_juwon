package kr.ac.hs.farm;

public class EditProfileRequest {
    private String id;
    private String name;
    private String password;
    private float weight;

    public EditProfileRequest(String id, String name, String password, float weight) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.weight = weight;
    }
}
