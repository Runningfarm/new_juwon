package kr.ac.hs.farm;

public class Item {
    public String name;
    public String category;  // "먹이", "울타리", "목장_구조물", "건축물" 등
    public int count;
    public int imageRes;
    public boolean obtained;

    public Item(String name, String category, int count, int imageRes, boolean obtained) {
        this.name = name;
        this.category = category;
        this.count = count;
        this.imageRes = imageRes;
        this.obtained = obtained;
    }
}