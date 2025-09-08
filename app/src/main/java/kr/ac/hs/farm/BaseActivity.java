package kr.ac.hs.farm;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {

    // 하단 탭 아이콘들 모음
    protected final List<ImageButton> bottomTabs = new ArrayList<>();

    /** 각 액티비티 onCreate()에서 호출 */
    protected void initBottomTabs(List<ImageButton> tabs) {
        bottomTabs.clear();
        if (tabs != null) bottomTabs.addAll(tabs);

        // 글로우가 밖으로 새도록 부모가 잘리지 않게
        for (ImageButton btn : bottomTabs) {
            ViewGroup parent = (ViewGroup) btn.getParent();
            if (parent != null) {
                parent.setClipToPadding(false);
                parent.setClipToOutline(false);
            }
        }
    }

    /** 선택/비선택 UI 갱신: 선택 탭은 노란 글로우 유지 */
    protected void updateBottomBarUI(int selectedId) {
        for (ImageButton btn : bottomTabs) {
            boolean selected = (btn.getId() == selectedId);

            // 크기/투명도
            btn.setAlpha(selected ? 1.0f : 0.30f);
            btn.setScaleX(selected ? 1.08f : 0.93f);
            btn.setScaleY(selected ? 1.08f : 0.93f);
            btn.setSelected(selected);

            // 글로우는 '부모 컨테이너' 배경으로 준다 (아이콘 색은 그대로!)
            ViewGroup parent = (ViewGroup) btn.getParent();
            if (parent != null) {
                if (selected) {
                    parent.setBackgroundResource(R.drawable.tab_glow_yellow);
                    parent.setAlpha(0.85f);            // 글로우 강도 (0.75~1.0 사이 취향대로)
                    parent.setTranslationZ(dp(2));     // 살짝 떠보이게
                    btn.setTranslationZ(dp(3));
                } else {
                    parent.setBackground(null);
                    parent.setAlpha(1f);
                    parent.setTranslationZ(0f);
                    btn.setTranslationZ(0f);
                }
            }
        }
    }

    /** 탭 버튼 클릭 리스너: 클릭 순간 팝 애니 + 글로우 반짝 후 전환(한 번만) */
    protected void setTabButton(int buttonId, Class<?> targetActivity) {
        ImageButton tab = findViewById(buttonId);
        if (tab == null) return;

        tab.setOnClickListener(v -> {
            // 같은 화면이면 전환 없이 ‘팝’ 연출만
            boolean same = this.getClass().equals(targetActivity);

            // 클릭 탭 부모에 글로우를 잠깐 강하게 (반짝)
            ViewGroup parent = (ViewGroup) v.getParent();
            if (parent != null) {
                parent.setBackgroundResource(R.drawable.tab_glow_yellow);
                parent.setAlpha(1f); // 순간 더 진하게
            }

            // 팝 애니메이션 (res/anim/tab_click.xml)
            Animation clickAnim = AnimationUtils.loadAnimation(this, R.anim.tab_click);
            v.startAnimation(clickAnim);

            // 애니 끝나면:
            clickAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    // 글로우를 정상 강도로 낮추고, 선택상태 갱신
                    updateBottomBarUI(buttonId);
                    if (!same) {
                        // 전환은 ‘한 번만’ 실행
                        startActivity(new Intent(BaseActivity.this, targetActivity));
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                }
            });
        });
    }

    // ────────── 유틸 ──────────
    protected float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
