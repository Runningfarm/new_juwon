package kr.ac.hs.farm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

public class PhotoPreviewActivity extends AppCompatActivity {

    private ImageView previewImageView;  // 촬영한 사진 미리보기
    private Button btnClaimReward;       // 사진 확인 후 보상 받기 버튼
    private Uri photoUri;                // ★ CHANGED: Bitmap 대신 Uri로 수신
    private int questNumber;             // 현재 퀘스트 번호 (24, 25)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ★ CHANGED: XML 파일명은 질문에 주신 대로 사용 (PhotoPreview.xml)
        setContentView(R.layout.activity_photopreview);

        // XML 뷰 초기화
        previewImageView = findViewById(R.id.previewImageView);
        btnClaimReward = findViewById(R.id.btnClaimReward);

        // Intent에서 전달된 사진과 퀘스트 번호 받기
        Intent intent = getIntent();
        photoUri = intent.getParcelableExtra("photoUri"); // ★ CHANGED
        questNumber = intent.getIntExtra("questNumber", -1);

        // 사진 미리보기 설정
        if (photoUri != null) {
            previewImageView.setImageURI(photoUri);

            if (questNumber == 24) {
                // ★ CHANGED: 24번은 "촬영만 하면" 보상 가능
                btnClaimReward.setEnabled(true);
            } else if (questNumber == 25) {
                // ★ CHANGED: 25번은 ML Kit 라벨링 후 식물/나무일 때 활성화
                btnClaimReward.setEnabled(false);
                analyzePhotoForQuest25(photoUri);
            }
        } else {
            Toast.makeText(this, "사진 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // 보상 버튼 클릭 이벤트
        btnClaimReward.setOnClickListener(v -> {
            // 이 화면에서는 보상 UI를 건드리지 않고 결과만 반환
            Intent resultIntent = new Intent();
            resultIntent.putExtra("questNumber", questNumber);
            resultIntent.putExtra("rewardResult", "success");
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    // ★ CHANGED: 25번 퀘스트 - ML Kit 라벨링 연동 지점
    // 실제 구현 시: ML Kit Image Labeling을 사용하여 "tree", "plant", "leaf", "flower" 등 라벨이 포함되면 활성화
    // 이 샘플은 컴파일 오류 없이 동작하도록 비동기 모사만 넣었습니다.
    private void analyzePhotoForQuest25(Uri uri) {
        try {
            // 1) InputImage 객체 생성
            InputImage image = InputImage.fromFilePath(this, uri);

            // 2) 기본 이미지 라벨러 옵션 (사물/식물/동물 등 자동 라벨링)
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            // 3) 라벨링 실행
            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        boolean foundPlant = false;
                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            // "plant" 또는 "flower" 라벨 감지 시 성공 처리
                            if (text.equalsIgnoreCase("plant") || text.equalsIgnoreCase("flower")) {
                                foundPlant = true;
                                break;
                            }
                        }

                        if (foundPlant) {
                            btnClaimReward.setEnabled(true);
                            Toast.makeText(this, "식물 라벨 감지! 보상 버튼이 활성화되었습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "식물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "이미지 분석 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "분석 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}