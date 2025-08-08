package kr.ac.hs.farm;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.widget.ProgressBar;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

public class Tab3Activity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ProgressBar[] progressBars = new ProgressBar[13];
    private Button[] claimButtons = new Button[13];
    private ImageView imagePreview;

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

        imagePreview = findViewById(R.id.imagePreview);
        Button buttonTakePhoto = findViewById(R.id.buttonTakePhoto);

        buttonTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
        for (int i = 0; i < Math.min(quests.size(), 13); i++) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(imageBitmap);
        }
    }
}