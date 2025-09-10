// Tab3Activity.java
package kr.ac.hs.farm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Tab3Activity extends BaseActivity {

    // 전체 퀘스트 수 9/5 기준(23개 + 카메라 2개(cameraQuestCount 검색해서 카메라 부분 퀘스트 늘어나면 숫자 올리기))
    // Tab2Activity.java도 똑같이 해줘야함
    private static final int QUEST_COUNT = 25;
    private ProgressBar[] progressBars;
    private Button[] claimButtons;
    private ImageView[] boxImages;

    private ProgressBar progressQuestP1, progressQuestP2;
    private ImageView boxRewardP1, boxRewardP2;
    private Button btnQuestP1, btnQuestP2;

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

        progressBars = new ProgressBar[QUEST_COUNT];
        claimButtons = new Button[QUEST_COUNT];
        boxImages   = new ImageView[QUEST_COUNT];

        initQuestViews();

        // 카메라 퀘스트(P1, P2) 뷰 바인딩
        progressQuestP1 = findViewById(R.id.progressQuestP1);
        progressQuestP2 = findViewById(R.id.progressQuestP2);
        boxRewardP1 = findViewById(R.id.boxRewardP1);
        boxRewardP2 = findViewById(R.id.boxRewardP2);
        btnQuestP1 = findViewById(R.id.btnQuestP1);
        btnQuestP2 = findViewById(R.id.btnQuestP2);

        double lastRunDistance = getIntent().getDoubleExtra("lastRunDistance", 0.0);

        // Activity Result 런처 초기화
        setupActivityResultLaunchers();

        // 전달받은 러닝 거리를 1km 조건 체크에 활용
        btnQuestP1.setEnabled(true);
        btnQuestP2.setEnabled(true);

        btnQuestP1.setOnClickListener(v -> {
            if (lastRunDistance >= 1.0) {
                currentPhotoQuestNumber = 101;
                ensureCameraPermissionThenCapture();
            } else {
                Toast.makeText(this, "1km 이상 러닝 시 촬영 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnQuestP2.setOnClickListener(v -> {
            currentPhotoQuestNumber = 102;
            ensureCameraPermissionThenCapture();
        });

        // 일반 퀘스트 버튼 리스너
        for (int i = 0; i < claimButtons.length; i++) {
            final int index = i;
            if (claimButtons[i] != null) {
                claimButtons[i].setOnClickListener(v -> claimQuest(index + 1));
            }
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

        // 하단 탭
        findViewById(R.id.tab1Button).setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(view -> startActivity(new Intent(this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(view -> startActivity(new Intent(this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(view -> startActivity(new Intent(this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(view -> startActivity(new Intent(this, Tab6Activity.class)));

        loadQuestProgressFromServer();
    }

    private void initQuestViews() {
        for (int i = 0; i < QUEST_COUNT; i++) {
            int idx = i + 1;

            int progId = getResIdByName("progressQuest" + idx);
            int btnId  = getResIdByName("btnClaim" + idx);
            int boxId  = getResIdByName("boxReward" + idx);

            if (progId != 0) progressBars[i] = findViewById(progId);
            if (btnId  != 0) claimButtons[i] = findViewById(btnId);
            if (boxId  != 0) boxImages[i]    = findViewById(boxId);
        }
    }

    private int getResIdByName(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
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

                        // 상자 열기 애니메이션 (동적 id 탐색)
                        int idx = questNumber - 1;
                        ImageView box = (idx >= 0 && idx < boxImages.length) ? boxImages[idx] : null;
                        if (box == null) {
                            int dynamicBoxId = getResIdByName("boxReward" + questNumber);
                            if (dynamicBoxId != 0) box = findViewById(dynamicBoxId);
                        }
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
        if (quests == null) return;

        int count = Math.min(quests.size(), QUEST_COUNT);
        for (int i = 0; i < count; i++) {
            QuestProgressResponse.Quest q = quests.get(i);
            ProgressBar bar = progressBars[i];
            Button btn = claimButtons[i];

            if (bar != null) {
                double target = q.getTarget();
                int percent = (target > 0) ? (int) ((q.getProgress() / target) * 100) : 0;
                if (percent < 0) percent = 0;
                if (percent > 100) percent = 100;
                bar.setProgress(percent);
            }
            if (btn != null) {
                btn.setEnabled(q.isCompleted());
            }
        }

        // kcal 누적형 보너스 퀘스트(예: 7,8,9번) 처리
        for (QuestProgressResponse.Quest q : quests) {
            if ("kcal".equals(q.getType())) {
                int progress = (int) q.getProgress();
                if (progress >= 100 && 6 < QUEST_COUNT) {
                    if (progressBars[6] != null) progressBars[6].setProgress(100);
                    if (claimButtons[6] != null) claimButtons[6].setEnabled(true);
                }
                if (progress >= 200 && 7 < QUEST_COUNT) {
                    if (progressBars[7] != null) progressBars[7].setProgress(100);
                    if (claimButtons[7] != null) claimButtons[7].setEnabled(true);
                }
                if (progress >= 400 && 8 < QUEST_COUNT) {
                    if (progressBars[8] != null) progressBars[8].setProgress(100);
                    if (claimButtons[8] != null) claimButtons[8].setEnabled(true);
                }
            }
        }

        // 카메라 퀘스트 자동 완료 처리 (예: P1, P2, P3...)
        int cameraQuestCount = 2; // 현재 카메라 퀘스트 개수(카메라 퀘스트 개수 늘어나면 늘려주기)
        SharedPreferences qp = getSharedPreferences("quest_progress", MODE_PRIVATE);
        for (int i = 1; i <= cameraQuestCount; i++) {
            boolean done = qp.getBoolean("quest_p" + i + "_done", false);
            if (done) {
                int boxId = getResIdByName("boxRewardP" + i);
                int progressId = getResIdByName("progressQuestP" + i);
                int btnId = getResIdByName("btnClaimP" + i);

                ImageView box = findViewById(boxId);
                if (box != null) box.setImageResource(R.drawable.box_opened);

                ProgressBar pb = findViewById(progressId);
                if (pb != null) pb.setProgress(100);

                Button btn = findViewById(btnId);
                if (btn != null) {
                    btn.setEnabled(false);
                    btn.setText("완료");
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

    // 카메라 권한 확인 후 촬영 실행
    private void ensureCameraPermissionThenCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            capturePhoto();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // 사진 촬영 실행 ( ACTION_IMAGE_CAPTURE + FileProvider)
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

    // 이미지 파일 생성
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // 보상 성공 시 UI 업데이트
    private void handleCameraQuestRewardUI(int questNumber) {
        try {
            int boxId,progressId,btnId;

            // P1(24), P2(25) 매핑
            if (questNumber == 101) {
                boxId = R.id.boxRewardP1;
                progressId = R.id.progressQuestP1;
                btnId = R.id.btnQuestP1;
            } else {
                boxId = R.id.boxRewardP2;
                progressId = R.id.progressQuestP2;
                btnId = R.id.btnQuestP2;
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

            Toast.makeText(this, "카메라 퀘스트 보상을 받았습니다!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CameraQuestUI", "UI update failed: " + e.getMessage());
        }
    }
}