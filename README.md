>>> 이전 수정 사항 관련 내용은 모두 juwonkim 레포 참고해주세요.

## <9/8 수정사항>

1. 하단바 수정
2. 사용자 레벨별 아이템 해금 흐름 로직 추가
3. 집 커스텀 삭제 및 인벤토리 아이템 수정
4. 메인 화면에 닭, 소 바로 추가되게끔 설정
5. 사용자별 누적 거리, 시간, 칼로리 저장
6. 사용자별 레벨 저장

<기존 수정 파일>
```
activity_main.xml
activity_mypage.xml
activity_tab2.xml
activity_tab3.xml
activity_tab4.xml
activity_tab6.xml
AndroidManifest.xml
```

```
EditProfileActivity.java
ItemAdapter.java
MainActivity.java
MypageAcitivty.java
SpriteView.java
Tab2Activity.java
Tab3Activity.java
Tab4Activity.java
Tab6Activity.java
```

<새로운 파일>
```
BaseActivity.java
HeroOverlayView.java
tab_click.xml
tab_glow_yellow.xml
```

```
feed_item.npg
ic_home.png
ic_inventory.png
ic_mypage.png
ic_quest.png
ic_running.png
```
-> 기존에 있던 png들을 지우고 새로운 png들을 다운받아서 drawable에 넣어주세요


## 수정 내용
>>> 겹치는 파일 없으신 분들은 복붙해도 상관 없습니다.

>>> 수정 파일이 매우 많아서 다른 분들 레포의 수정 사항과 겹치지 않는 파일은 내용 적지 않았습니다. 안 되는 부분은 > 9/8 수정 사항 < 클릭해서 Commit diff 참고해주세요.

1. AndroidManifest.xml

```
<!-- 시작 액티비티 -->
        <activity
            android:name=".MainActivity"
```
바로 아래에

```
android:launchMode="singleTop"
```
추가


2. activity_tab3.xml

맨 아래에 '하단 탭바' 부분을 아래 코드로 통째로 교체
```
<!-- 하단 탭바 -->
    <LinearLayout
        android:id="@+id/bottomBar"
        android:layout_width="match_parent"
        android:layout_height="70dp"
        android:layout_alignParentBottom="true"
        android:orientation="horizontal"
        android:background="#FFFFFF"
        android:elevation="8dp"
        android:layout_margin="8dp"
        android:padding="6dp"
        android:backgroundTint="#FFFFFF"
        android:backgroundTintMode="src_in"
        android:clipToPadding="false"
        android:gravity="center">

        <!-- 탭 버튼들 (생략 없이 5개 모두 구성) -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">
            <ImageButton
                android:id="@+id/tab1Button"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:src="@drawable/ic_home"
                android:background="@drawable/bottom_tab_selector"
                android:scaleType="centerInside"
                android:contentDescription="홈"
                android:elevation="2dp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">
            <ImageButton
                android:id="@+id/tab2Button"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:src="@drawable/ic_running"
                android:background="@drawable/bottom_tab_selector"
                android:scaleType="centerInside"
                android:contentDescription="러닝"
                android:elevation="2dp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">
            <ImageButton
                android:id="@+id/tab3Button"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:src="@drawable/ic_quest"
                android:background="@drawable/bottom_tab_selector"
                android:scaleType="centerInside"
                android:contentDescription="퀘스트"
                android:elevation="2dp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">
            <ImageButton
                android:id="@+id/tab4Button"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:src="@drawable/ic_inventory"
                android:background="@drawable/bottom_tab_selector"
                android:scaleType="centerInside"
                android:contentDescription="인벤토리"
                android:elevation="2dp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">
            <ImageButton
                android:id="@+id/tab6Button"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:src="@drawable/ic_mypage"
                android:background="@drawable/bottom_tab_selector"
                android:scaleType="centerInside"
                android:contentDescription="마이페이지"
                android:elevation="2dp" />
        </LinearLayout>
    </LinearLayout>
```

3. Tab2Activity.java

(1)
```
private float weight = 0f;
```
바로 밑에

```
private SharedPreferences statsPrefs() {
        return getSharedPreferences("run_stats", MODE_PRIVATE);
    }
    private String scopedKey(String base) {
        SharedPreferences login = getSharedPreferences("login", MODE_PRIVATE);
        String uid = login.getString("id", null);
        return base + "_" + (uid != null && !uid.trim().isEmpty() ? uid : "guest");
    }
```
추가

(2)
```
.setPositiveButton("예", (dialog, which) -> {
                        pauseRunning();
                        stopRunning();

                        long prev = pref.getLong("total_run_time_seconds", 0L);
                        long add = elapsedTime / 1000L;   // 이번 러닝 소요 시간(초)
                        pref.edit().putLong("total_run_time_seconds", prev + add).apply();

                        Log.d("러닝", "time=" + timeTextView.getText().toString());
                        Log.d("러닝", "distance=" + tvDistance.getText().toString());
                        Log.d("러닝", "kcal=" + tvKcal.getText().toString());
                        Log.d("러닝", "pace=" + tvPace.getText().toString());

                        // Tab3로 이번 러닝 거리 전달
                        Intent intent = new Intent(Tab2Activity.this, Tab3Activity.class);
                        double distanceKm = totalDistance; // totalDistance는 km 단위
                        intent.putExtra("lastRunDistance", distanceKm);
                        startActivity(intent);

                        sendRunResultToServer();
                    })
```
이 부분을 아래로 통째로 교체

```
.setPositiveButton("예", (dialog, which) -> {
                        pauseRunning();
                        stopRunning();

                        SharedPreferences stats = statsPrefs();

                        // 시간 누적
                        long prevSecs = stats.getLong(scopedKey("total_run_time_seconds"), 0L);
                        long addSecs  = elapsedTime / 1000L;
                        long newSecs  = prevSecs + addSecs;

                        // 거리 누적 (double→longBits)
                        long prevDistBits = stats.getLong(scopedKey("total_distance_km"),
                                Double.doubleToRawLongBits(0.0));
                        double prevDistKm = Double.longBitsToDouble(prevDistBits);
                        double newDistKm  = prevDistKm + totalDistance;

                        // 칼로리 누적
                        SharedPreferences login = getSharedPreferences("login", MODE_PRIVATE);
                        float weight = login.getFloat("weight", 0f);
                        long seconds = elapsedTime / 1000;
                        double hours = seconds / 3600.0;
                        double pace  = hours > 0 ? (totalDistance / hours) : 0.0;
                        double MET   = getMetsByPace(pace);
                        int addKcal  = (int) Math.round(MET * weight * hours);
                        int prevKcal = stats.getInt(scopedKey("total_kcal"), 0);
                        int newKcal  = prevKcal + addKcal;

                        stats.edit()
                                .putLong(scopedKey("total_run_time_seconds"), newSecs)
                                .putLong(scopedKey("total_distance_km"), Double.doubleToRawLongBits(newDistKm))
                                .putInt (scopedKey("total_kcal"), newKcal)
                                .apply();

                        // 이후 기존 동작
                        Intent intent = new Intent(Tab2Activity.this, Tab3Activity.class);
                        intent.putExtra("lastRunDistance", totalDistance);
                        startActivity(intent);

                        sendRunResultToServer();
                    })
```

(3)
```
findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab6Activity.class)));
```
바로 위에

```
ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭(MainActivity = tab2)을 강조
        updateBottomBarUI(R.id.tab2Button);
```
추가


4. Tab3Activity.java

(1)
```
import android.view.View;
```
아래에

```
import android.widget.ImageButton;
```
추가

(2)
```
findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(this, Tab6Activity.class)));
```
바로 위에

```
ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭(MainActivity = tab3)을 강조
        updateBottomBarUI(R.id.tab3Button);
```
추가
