package kr.ac.hs.farm;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Color; // 6/30
import com.google.android.gms.maps.GoogleMap; // 6/30
import com.google.android.gms.maps.OnMapReadyCallback; // 6/30
import com.google.android.gms.maps.SupportMapFragment; // 6/30
import com.google.android.gms.maps.CameraUpdateFactory; // 6/30
import com.google.android.gms.maps.model.LatLng; // 6/30
import com.google.android.gms.maps.model.PolylineOptions; // 6/30
import com.google.android.gms.maps.model.Polyline; // 6/30
import java.util.ArrayList;  // 6/30

public class RunningResult extends AppCompatActivity implements OnMapReadyCallback {    // 6/30

    private TextView tvTime, tvDistance, tvKcal, tvPace;
    private ArrayList<LatLng> runPath; // 6/30

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runningresult);

        // TextView 연결
        tvTime = findViewById(R.id.tvTime);
        tvDistance = findViewById(R.id.tvDistance);
        tvKcal = findViewById(R.id.tvKcal);
        tvPace = findViewById(R.id.tvPace);
        runPath = getIntent().getParcelableArrayListExtra("path");

        // Intent에서 데이터 받아오기
        Intent intent = getIntent();
        String time = intent.getStringExtra("time");
        String distance = intent.getStringExtra("distance");
        String kcal = intent.getStringExtra("kcal");
        String pace = intent.getStringExtra("pace");

        Log.d("RunningResult", "받은 time=" + time);
        Log.d("RunningResult", "받은 distance=" + distance);
        Log.d("RunningResult", "받은 kcal=" + kcal);
        Log.d("RunningResult", "받은 pace=" + pace);

        // 받아온 데이터 화면에 표시
        if (time != null) tvTime.setText(time);
        if (distance != null) tvDistance.setText(distance);
        if (kcal != null) tvKcal.setText(kcal);
        if (pace != null) tvPace.setText(pace);

        // 지도 연결
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.resultMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 버튼 연결 및 클릭시 토스트 메시지 띄우고 Tab3Activity로 이동
        Button btnQuestReward = findViewById(R.id.btnQuestReward);
        btnQuestReward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(RunningResult.this, "퀘스트화면으로 이동!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RunningResult.this, Tab3Activity.class);
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }
        });

    }
    // 6/30
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (runPath != null && !runPath.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(runPath)
                    .width(10f)
                    .color(Color.GREEN)
                    .geodesic(true);

            googleMap.addPolyline(polylineOptions);
            // 시작 지점으로 카메라 이동
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(runPath.get(0), 16));
        }
    }
}
