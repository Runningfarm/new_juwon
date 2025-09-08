package kr.ac.hs.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import android.widget.ImageButton;


public class Tab3Activity extends BaseActivity {

    private ProgressBar[] progressBars = new ProgressBar[23];
    private Button[] claimButtons = new Button[23];
    private ProgressBar progressQuestP1, progressQuestP2;
    private ImageView boxRewardP1, boxRewardP2;
    private Button btnClaimP1, btnClaimP2;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<Intent> previewLauncher;
    private Uri photoURI;
    private File photoFile;
    private int currentPhotoQuestNumber = -1; // P1 또는 P2


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
        progressBars[17] = findViewById(R.id.progressQuest18);
        progressBars[18] = findViewById(R.id.progressQuest19);
        progressBars[19] = findViewById(R.id.progressQuest20);
        progressBars[20] = findViewById(R.id.progressQuest21);
        progressBars[21] = findViewById(R.id.progressQuest22);
        progressBars[22] = findViewById(R.id.progressQuest23);

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
        claimButtons[17] = findViewById(R.id.btnClaim18);
        claimButtons[18] = findViewById(R.id.btnClaim19);
        claimButtons[19] = findViewById(R.id.btnClaim20);
        claimButtons[20] = findViewById(R.id.btnClaim21);
        claimButtons[21] = findViewById(R.id.btnClaim22);
        claimButtons[22] = findViewById(R.id.btnClaim23);

        //  카메라 퀘스트(P1, P2) 뷰 바인딩
        progressQuestP1 = findViewById(R.id.progressQuestP1);
        progressQuestP2 = findViewById(R.id.progressQuestP2);
        boxRewardP1 = findViewById(R.id.boxRewardP1);
        boxRewardP2 = findViewById(R.id.boxRewardP2);
        btnClaimP1 = findViewById(R.id.btnClaimP1);
        btnClaimP2 = findViewById(R.id.btnClaimP2);

        double lastRunDistance = getIntent().getDoubleExtra("lastRunDistance", 0.0);
        // Activity Result 런처 초기화
        setupActivityResultLaunchers();

        // 전달받은 러닝 거리를 1km 조건 체크에 활용
        btnClaimP1.setEnabled(lastRunDistance >= 1.0);
        btnClaimP2.setEnabled(true); // 항상 가능

        // p1번 버튼 클릭 → 권한 → 촬영 → 미리보기
        btnClaimP1.setOnClickListener(v -> {
            currentPhotoQuestNumber = 101;
            if (lastRunDistance < 1.0) {
                Toast.makeText(this, "1km 이상 러닝 시 활성화됩니다.", Toast.LENGTH_LONG).show();
                return;
            }
            ensureCameraPermissionThenCapture();
        });

        //p2번 버튼 클릭 → 권한 → 촬영 → 미리보기
        btnClaimP2.setOnClickListener(v -> {
            currentPhotoQuestNumber = 102;
            ensureCameraPermissionThenCapture();
        });

        for (int i = 0; i < claimButtons.length; i++) {
            final int index = i;
            claimButtons[i].setOnClickListener(v -> {
                claimQuest(index + 1);
            });
        }

        ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭(MainActivity = tab3)을 강조
        updateBottomBarUI(R.id.tab3Button);


        findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(this, Tab6Activity.class)));

        loadQuestProgressFromServer();
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
        for (int i = 0; i < Math.min(quests.size(), 23); i++) {
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

    // ====== 카메라 퀘스트 ======
    private void setupActivityResultLaunchers() {
        // 카메라 권한 요청
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        capturePhoto();
                    } else {
                        Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 사진 촬영 (URI로 저장)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoURI != null) {
                        // 촬영 성공 → 미리보기 화면으로 이동
                        Intent intent = new Intent(this, PhotoPreviewActivity.class);
                        intent.putExtra("photoUri", photoURI);
                        intent.putExtra("questNumber", currentPhotoQuestNumber);
                        previewLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // PhotoPreviewActivity 결과 수신 (보상 성공 여부)
        previewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String status = data.getStringExtra("rewardResult");
                        int qn = data.getIntExtra("questNumber", -1);
                        if ("success".equals(status) && (qn == 101 || qn == 102)) {
                            // UI 업데이트 (상자 열림 + 진행바 100% + 버튼 비활성화)
                            handleCameraQuestRewardUI(qn);

                            // 서버에 보상 수령 요청 (기존 메서드 재사용)
                            claimQuest(qn);
                        }
                    }
                }
        );
    }

    //카메라 권한 확인 후 촬영 실행
    private void ensureCameraPermissionThenCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            capturePhoto();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // 사진 촬영 실행 (ACTION_IMAGE_CAPTURE + FileProvider)
    private void capturePhoto() {
        try {
            photoFile = createImageFile();
            photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(photoURI);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "사진 파일 생성 실패", Toast.LENGTH_SHORT).show();
        }
    }

    // 이미지 파일 생성 (기존 로직 유지)
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    //보상 성공 시 UI 업데이트 (신규)
    private void handleCameraQuestRewardUI(int questNumber) {
        try {
            int boxId;
            int progressId;
            int btnId;

            // P1(24), P2(25) 매핑
            if (questNumber == 101) {
                boxId = R.id.boxRewardP1;
                progressId = R.id.progressQuestP1;
                btnId = R.id.btnClaimP1;
            } else {
                boxId = R.id.boxRewardP2;
                progressId = R.id.progressQuestP2;
                btnId = R.id.btnClaimP2;
            }

            ImageView box = findViewById(boxId);
            if (box != null) {
                box.setImageResource(R.drawable.box_opened);
                Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_open);
                box.startAnimation(fadeIn);
            }

            ProgressBar pb = findViewById(progressId);
            if (pb != null) pb.setProgress(100);

            Button btn = findViewById(btnId);
            if (btn != null) {
                btn.setEnabled(false);
                btn.setText("완료");
            }

            Toast.makeText(this, "퀘스트 " + questNumber + " 보상을 받았습니다!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CameraQuestUI", "UI update failed: " + e.getMessage());
        }
    }
}