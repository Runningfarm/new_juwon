package kr.ac.hs.farm;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private ArrayList<LatLng> runPath = new ArrayList<>();
    private double totalDistance = 0.0;
    private Location lastLocation = null;
    private float weight = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab2);

        View rootView = findViewById(R.id.root_running);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemInsets.top, 0, systemInsets.bottom);
            return insets;
        });

        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        weight = pref.getFloat("weight", 0f);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);

            // fragment에도 insets 적용
            if (mapFragment.getView() != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mapFragment.getView(), (v, insets) -> {
                    Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, systemInsets.top, 0, systemInsets.bottom);
                    return insets;
                });
            }
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

        timerRunnable = () -> {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimeText();
                updateRunStats();
                handler.postDelayed(timerRunnable, 1000);
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

                        long prev = pref.getLong("total_run_time_seconds", 0L);
                        long add = elapsedTime / 1000L;   // 이번 러닝 소요 시간(초)
                        pref.edit().putLong("total_run_time_seconds", prev + add).apply();

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

        findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(Tab2Activity.this, Tab6Activity.class)));

        loadQuestProgressFromServer();
    }

    private void sendRunResultToServer() {
        // 앱 터짐 방지
        if (polylineOptions == null) {
            polylineOptions = new PolylineOptions();
        }
        runPath = new ArrayList<>(polylineOptions.getPoints());
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String id = pref.getString("id", null);
        String token = pref.getString("token", null);
        float weight = pref.getFloat("weight", 0f);

        if (id == null || token == null) {
            Toast.makeText(this, "로그인 정보 없음!", Toast.LENGTH_SHORT).show();
            return;
        }

        double distance = totalDistance;
        int time = (int)(elapsedTime / 1000);
        double pace = time > 0 ? distance / (time / 3600.0) : 0.0;
        double MET = getMetsByPace(pace);
        int kcal = (int) Math.round(MET * weight * (time / 3600.0));

        RunResultRequest request = new RunResultRequest(id, distance, time, kcal, pace);

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<RunResultResponse> call = api.runResult(request, "Bearer " + token);
        call.enqueue(new Callback<RunResultResponse>() {
            @Override
            public void onResponse(Call<RunResultResponse> call, Response<RunResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Intent intent = new Intent(Tab2Activity.this, RunningResult.class);
                    intent.putExtra("time", timeTextView.getText().toString());
                    intent.putExtra("distance", tvDistance.getText().toString());
                    intent.putExtra("kcal", tvKcal.getText().toString());
                    intent.putExtra("pace", tvPace.getText().toString());
                    intent.putParcelableArrayListExtra("path", runPath);
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

    private double getMetsByPace(double pace) {
        if (pace < 6) return 3.5;
        if (pace < 8) return 6.0;
        if (pace < 10) return 8.3;
        if (pace < 12) return 10.5;
        if (pace < 14) return 12.8;
        return 15.0;
    }

    private void loadQuestProgressFromServer() {
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String id = pref.getString("id", null);

        if (id == null) return;

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        String token = pref.getString("token", null); // 7/30
        Call<QuestProgressResponse> call = api.getQuestProgress("Bearer " + token); // 7/30
        call.enqueue(new Callback<QuestProgressResponse>() {
            @Override
            public void onResponse(Call<QuestProgressResponse> call, Response<QuestProgressResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<QuestProgressResponse.Quest> quests = response.body().getQuests();
                    if (quests != null) {
                        int completed = 0;
                        int total = quests.size();
                        for (QuestProgressResponse.Quest q : quests) {
                            if (q.isCompleted()) completed++;
                        }
                        ProgressBar questBar = findViewById(R.id.quest_progress_bar);
                        questBar.setProgress((int) (100.0 * completed / total));
                        TextView progressText = findViewById(R.id.quest_progress_text);
                        progressText.setText(completed + " / " + total + " 완료");
                    }
                }
            }

            @Override
            public void onFailure(Call<QuestProgressResponse> call, Throwable t) {
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        mMap.setMyLocationEnabled(true);

        polylineOptions = new PolylineOptions().width(12f).color(Color.parseColor("#2E7D32")).geodesic(true);
        polyline = mMap.addPolyline(polylineOptions);

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000);

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
                polylineOptions.add(latLng);
                polyline.setPoints(polylineOptions.getPoints());

                if (lastLocation != null) {
                    float distance = lastLocation.distanceTo(location);
                    if (distance > 1.0) {
                        totalDistance += (distance / 1000.0);
                    }
                }
                lastLocation = location;

                updateRunStats();

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
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
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
        long seconds = elapsedTime / 1000;
        double hours = seconds / 3600.0;
        double pace = hours > 0 ? (totalDistance / hours) : 0.0;
        double MET = getMetsByPace(pace);
        int kcal = (int) Math.round(MET * weight * hours);

        tvDistance.setText(String.format("%.2f km", totalDistance));
        tvKcal.setText(String.format("%d kcal", kcal));
        tvPace.setText(String.format("%.2f km/h", pace));
    }
}