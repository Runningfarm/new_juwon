package kr.ac.hs.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ProgressBar;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

public class Tab3Activity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100; //추가
    private ProgressBar[] progressBars = new ProgressBar[17];
    private Button[] claimButtons = new Button[17];
    private ImageView imagePreview;
    private Location startLocation; // 러닝 시작 지점 저장용
    private Uri photoURI;
    private File photoFile;
    private Button buttonTakePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab3);
        Log.d("퀘스트응답", "Tab3Activity onCreate 진입!");

        View rootView = findViewById(R.id.root_quest);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBars[0] = findViewById(R.id.progressQuest1);
        progressBars[1] = findViewById(R.id.progressQuest2);
        progressBars[2] = findViewById(R.id.progressQuest3);
        progressBars[3] = findViewById(R.id.progressQuest4);
        progressBars[4] = findViewById(R.id.progressQuest5);
        progressBars[5] = findViewById(R.id.progressQuest6);
        progressBars[6] = findViewById(R.id.progressQuest7);
        progressBars[7] = findViewById(R.id.progressQuest8);
        progressBars[8] = findViewById(R.id.progressQuest9);
        progressBars[9] = findViewById(R.id.progressQuest10);
        progressBars[10] = findViewById(R.id.progressQuest11);
        progressBars[11] = findViewById(R.id.progressQuest12);
        progressBars[12] = findViewById(R.id.progressQuest13);
        progressBars[13] = findViewById(R.id.progressQuest14);
        progressBars[14] = findViewById(R.id.progressQuest15);
        progressBars[15] = findViewById(R.id.progressQuest16);
        progressBars[16] = findViewById(R.id.progressQuest17);

        claimButtons[0] = findViewById(R.id.btnClaim1);
        claimButtons[1] = findViewById(R.id.btnClaim2);
        claimButtons[2] = findViewById(R.id.btnClaim3);
        claimButtons[3] = findViewById(R.id.btnClaim4);
        claimButtons[4] = findViewById(R.id.btnClaim5);
        claimButtons[5] = findViewById(R.id.btnClaim6);
        claimButtons[6] = findViewById(R.id.btnClaim7);
        claimButtons[7] = findViewById(R.id.btnClaim8);
        claimButtons[8] = findViewById(R.id.btnClaim9);
        claimButtons[9] = findViewById(R.id.btnClaim10);
        claimButtons[10] = findViewById(R.id.btnClaim11);
        claimButtons[11] = findViewById(R.id.btnClaim12);
        claimButtons[12] = findViewById(R.id.btnClaim13);
        claimButtons[13] = findViewById(R.id.btnClaim14);
        claimButtons[14] = findViewById(R.id.btnClaim15);
        claimButtons[15] = findViewById(R.id.btnClaim16);
        claimButtons[16] = findViewById(R.id.btnClaim17);

        imagePreview = findViewById(R.id.imagePreview);
        Button buttonTakePhoto = findViewById(R.id.buttonTakePhoto);

        getStartLocation(); //러닝 시작 위치 설정

        //사진 찍기 버튼 동작 정의
        buttonTakePhoto.setOnClickListener(v -> {
            if(checkLocationDistance()){
                requestCameraPermission();
            } else{
                Toast.makeText(this,"2km 이상 이동해야 퀘스트를 수행할 수 있습니다.", Toast.LENGTH_LONG).show();
            }
        });

        for (int i = 0; i < claimButtons.length; i++) {
            final int index = i;
            claimButtons[i].setOnClickListener(v -> {
                claimQuest(index + 1);
            });
        }

        findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(this, Tab6Activity.class)));

        loadQuestProgressFromServer();
    }

    // 임시 러닝 시작 지점 설정
    private void getStartLocation() {
        startLocation = new Location("start");
        startLocation.setLatitude(37.5665);
        startLocation.setLongitude(126.9780);
    }

    //현재 위치와 시작 위치 거리 비교
    private boolean checkLocationDistance() {
        Location currentLocation = getCurrentLocation();
        if (currentLocation != null && startLocation != null) {
            float distance = startLocation.distanceTo(currentLocation);
            return distance >= 2000;
        }
        return false;
    }

    //임시 현재 위치 설정
    private Location getCurrentLocation() {
        Location location = new Location("current");
        location.setLatitude(37.5765);
        location.setLongitude(126.9880);
        return location;
    }

    //카메라 권한 요청
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //카메라 열기 및 파일 저장 위치 지정
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //이미지 파일 미리보기 생성
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    //사진 촬영 후 보상 처리 및 버튼 설정
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imagePreview.setImageURI(photoURI);
            buttonTakePhoto.setText("보상 받기");
            buttonTakePhoto.setOnClickListener(v -> {
                Toast.makeText(this, "보상을 받았습니다!", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("questResult", "success");
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }
    }

    private void loadQuestProgressFromServer() {
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String token = pref.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<QuestProgressResponse> call = api.getQuestProgress("Bearer " + token);
        call.enqueue(new Callback<QuestProgressResponse>() {
            @Override
            public void onResponse(Call<QuestProgressResponse> call, Response<QuestProgressResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateQuestUI(response.body().getQuests());
                } else {
                    Toast.makeText(Tab3Activity.this, "퀘스트 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QuestProgressResponse> call, Throwable t) {
                Toast.makeText(Tab3Activity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void claimQuest(int questNumber) {
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String token = pref.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        ClaimQuestRequest request = new ClaimQuestRequest(questNumber);
        Call<RunResultResponse> call = api.claimQuest("Bearer " + token, request);

        call.enqueue(new Callback<RunResultResponse>() {
            @Override
            public void onResponse(Call<RunResultResponse> call, Response<RunResultResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RunResultResponse result = response.body();
                    if (result.isSuccess()) {
                        int reward = result.getReward();
                        Toast.makeText(Tab3Activity.this, "보상으로 먹이 " + reward + "개를 받았습니다!", Toast.LENGTH_SHORT).show();

                        // 상자 열기 애니메이션 적용
                        int boxId = getResources().getIdentifier("boxReward" + questNumber, "id", getPackageName());
                        ImageView box = findViewById(boxId);
                        if (box != null) {
                            box.setImageResource(R.drawable.box_opened);
                            Animation fadeIn = AnimationUtils.loadAnimation(Tab3Activity.this, R.anim.fade_open);
                            box.startAnimation(fadeIn);
                        }

                        Intent intent = new Intent(Tab3Activity.this, MainActivity.class);
                        intent.putExtra("reward", reward);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(Tab3Activity.this, "보상을 받을 수 없는 상태입니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Tab3Activity.this, "퀘스트 중복 보상 받기 불가", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RunResultResponse> call, Throwable t) {
                Toast.makeText(Tab3Activity.this, "서버 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuestUI(List<QuestProgressResponse.Quest> quests) {
        for (int i = 0; i < Math.min(quests.size(), 17); i++) {
            QuestProgressResponse.Quest q = quests.get(i);
            double target = q.getTarget();
            int percent = (target > 0) ? (int) ((q.getProgress() / target) * 100) : 0;
            progressBars[i].setProgress(percent);
            claimButtons[i].setEnabled(q.isCompleted());
        }

        for (QuestProgressResponse.Quest q : quests) {
            if ("kcal".equals(q.getType())) {
                int progress = (int) q.getProgress();
                if (progress >= 100) {
                    progressBars[6].setProgress(100);
                    claimButtons[6].setEnabled(true);
                }
                if (progress >= 200) {
                    progressBars[7].setProgress(100);
                    claimButtons[7].setEnabled(true);
                }
                if (progress >= 400) {
                    progressBars[8].setProgress(100);
                    claimButtons[8].setEnabled(true);
                }
            }
        }
    }
}
