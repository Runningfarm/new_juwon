package kr.ac.hs.farm;

public class DuplicateCheckRequest {
    private String id;

    public DuplicateCheckRequest(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
