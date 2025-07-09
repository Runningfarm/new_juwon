package kr.ac.hs.farm;

import java.util.List;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.SharedPreferences;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList; // 6/30


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Tab2Activity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageButton playButton, pauseButton, endButton;
    private TextView timeTextView, tvDistance, tvKcal, tvPace;
    private boolean isRunning = false;
    private long startTime = 0L;
    private long elapsedTime = 0L;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private LinearLayout Questbar;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private boolean isFirstLocation = true;
    private ArrayList<LatLng> runPath = new ArrayList<>(); // 6/30

    // 거리 누적, 위치 기억용
    private double totalDistance = 0.0;
    private Location lastLocation = null;
    private float weight = 0f;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab2);

        // 지도 프래그먼트 준비 (추가)
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        playButton = findViewById(R.id.play_button);
        pauseButton = findViewById(R.id.pause_button);
        endButton = findViewById(R.id.end_button);
        timeTextView = findViewById(R.id.time);
        tvDistance = findViewById(R.id.tvDistance);
        tvKcal = findViewById(R.id.tvKcal);
        tvPace = findViewById(R.id.tvPace);
        Questbar = findViewById(R.id.quest_progress_container);

        Questbar.setOnClickListener(view -> {
            Intent intent = new Intent(Tab2Activity.this, Tab3Activity.class);
            startActivity(intent);
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    updateTimeText();
                    updateRunStats();
                    handler.postDelayed(this, 1000);
                }
            }
        };

        playButton.setOnClickListener(view -> {
            if (!isRunning) startRunning();
        });

        pauseButton.setOnClickListener(view -> {
            if (isRunning) pauseRunning();
        });

        endButton.setOnClickListener(view -> {

            new AlertDialog.Builder(Tab2Activity.this)
                    .setTitle("러닝 종료")
                    .setMessage("러닝을 종료하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        pauseRunning();
                        stopRunning();
                        Log.d("러닝", "time=" + timeTextView.getText().toString());
                        Log.d("러닝", "distance=" + tvDistance.getText().toString());
                        Log.d("러닝", "kcal=" + tvKcal.getText().toString());
                        Log.d("러닝", "pace=" + tvPace.getText().toString());

                        sendRunResultToServer();
                    })
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        });

        // 하단 탭바 이동 로직
        findViewById(R.id.tab1Button).setOnClickListener(view ->
                startActivity(new Intent(Tab2Activity.this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view ->
                startActivity(new Intent(Tab2Activity.this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view ->
                startActivity(new Intent(Tab2Activity.this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view ->
                startActivity(new Intent(Tab2Activity.this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view ->
                startActivity(new Intent(Tab2Activity.this, Tab6Activity.class)));

        loadQuestProgressFromServer();

    }

    private double getMetsByPace(double pace) {
        if (pace < 6) return 3.5;
        if (pace < 8) return 6.0;
        if (pace < 10) return 8.3;
        if (pace < 12) return 10.5;
        if (pace < 14) return 12.8;
        return 15.0;
    }
    private void sendRunResultToServer() {
        runPath = new ArrayList<>(polylineOptions.getPoints()); // 6/30

        // id 불러오는 코드
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String id = pref.getString("id", null);
        String token = pref.getString("token", null);
        float weight = pref.getFloat("weight", 0f);

        // 만약 id가 없으면(로그인 안됨) 그냥 종료
        if (id == null || token == null) {
            Toast.makeText(this, "로그인 정보 없음!", Toast.LENGTH_SHORT).show();
            return;
        }
        double distance = totalDistance; // km
        int time = (int)(elapsedTime / 1000); // 초
        double pace = time > 0 ? distance / (time / 3600.0) : 0.0; // km/h
        double MET = getMetsByPace(pace);
        int kcal = (int) Math.round(MET * weight * (time / 3600.0));

        RunResultRequest request = new RunResultRequest(id, distance, time, kcal, pace);

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<RunResultResponse> call = api.runResult(request, "Bearer " + token);
        call.enqueue(new Callback<RunResultResponse>() {
            @Override
            public void onResponse(Call<RunResultResponse> call, Response<RunResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 여기서만 화면 이동
                    Intent intent = new Intent(Tab2Activity.this, RunningResult.class);
                    intent.putExtra("time", timeTextView.getText().toString());
                    intent.putExtra("distance", tvDistance.getText().toString());
                    intent.putExtra("kcal", tvKcal.getText().toString());
                    intent.putExtra("pace", tvPace.getText().toString());
                    intent.putParcelableArrayListExtra("path", runPath); // 6/30
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Tab2Activity.this, "러닝 결과 전송 실패!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RunResultResponse> call, Throwable t) {
                Toast.makeText(Tab2Activity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestProgressFromServer() {
        //  SharedPreferences에서 id 꺼내오기
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String id = pref.getString("id", null);

        if (id == null) {
            // 로그인 안 된 경우 처리(예: 토스트 메시지 등)
            return;
        }

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<QuestProgressResponse> call = api.getQuestProgress(id);
        call.enqueue(new Callback<QuestProgressResponse>() {
            @Override
            public void onResponse(Call<QuestProgressResponse> call, Response<QuestProgressResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 퀘스트 배열을 받아온다!
                    List<QuestProgressResponse.Quest> quests = response.body().getQuests();
                    if (quests != null) {
                        int completed = 0;
                        int total = quests.size(); // 보통 6개
                        // 완료된 퀘스트 개수 세기
                        for (QuestProgressResponse.Quest q : quests) {
                            if (q.isCompleted()) completed++;
                        }
                        // 진행률 표시
                        ProgressBar questBar = findViewById(R.id.quest_progress_bar);
                        questBar.setProgress((int) (100.0 * completed / total));
                        TextView progressText = findViewById(R.id.quest_progress_text);
                        progressText.setText(completed + " / " + total + " 완료");
                    }
                }
            }
            @Override
            public void onFailure(Call<QuestProgressResponse> call, Throwable t) {
                // 실패시 처리
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Polyline 초기화
        polylineOptions = new PolylineOptions()
                .width(12f)
                .color(Color.BLUE)
                .geodesic(true);
        polyline = mMap.addPolyline(polylineOptions);

        startLocationUpdates(); // 위치 업데이트 시작
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000); // 2초마다 위치

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;
            for (Location location : locationResult.getLocations()) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Polyline에 위치 추가
                polylineOptions.add(latLng);
                polyline.setPoints(polylineOptions.getPoints());

                // 여기서부터 거리, 칼로리, 페이스 계산 코드 추가
                if (lastLocation != null) {
                    float distance = lastLocation.distanceTo(location); // 미터(m)
                    if (distance > 1.0) { // 너무 작은 오차는 무시
                        totalDistance += (distance / 1000.0); // km 단위로 누적
                    }
                }
                lastLocation = location;

                updateRunStats();

                // 처음 위치만 카메라 이동
                if (isFirstLocation) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                    isFirstLocation = false;
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                        startLocationUpdates();
                    }
                }
            }
        }
    }


    private void startRunning() {
        isRunning = true;
        startTime = System.currentTimeMillis() - elapsedTime;
        handler.post(timerRunnable);
    }

    private void pauseRunning() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
    }

    private void stopRunning() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        updateTimeText();
    }

    private void updateTimeText() {
        long seconds = elapsedTime / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        String timeFormatted = String.format("%02d:%02d:%02d", h, m, s);
        timeTextView.setText(timeFormatted);
    }

    private void updateRunStats() {
        // 경과시간(초)
        long seconds = elapsedTime / 1000;
        double hours = seconds / 3600.0;
        // 칼로리, 페이스 계산
        double pace = hours > 0 ? (totalDistance / hours) : 0.0; // km/h
        double MET = getMetsByPace(pace);
        int kcal = (int) Math.round(MET * weight * hours);

        tvDistance.setText(String.format("%.2f km", totalDistance));
        tvKcal.setText(String.format("%d kcal", kcal));
        tvPace.setText(String.format("%.2f km/h", pace));
    }

}