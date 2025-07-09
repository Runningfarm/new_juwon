package kr.ac.hs.farm;

public class Item {
    public String name;
    public String category;  // "먹이" or "농장"
    public int count;
    public int imageRes;
    public boolean obtained;

    // 생성자 순서: name, category, count, imageRes, obtained
    public Item(String name, String category, int count, int imageRes, boolean obtained) {
        this.name = name;
        this.category = category;
        this.count = count;
        this.imageRes = imageRes;
        this.obtained = obtained;
    }
}
