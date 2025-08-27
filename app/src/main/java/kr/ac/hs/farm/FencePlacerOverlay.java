package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 설치 오버레이: 드래그(가로/세로) 선택, 상단 버튼 터치 통과 */
public class FencePlacerOverlay extends View {

    public interface CommitListener {
        void onCommit(Map<Point, Integer> maskByCell);
    }

    private final Paint previewPaint = new Paint();
    private final Paint gatePaint = new Paint();   // ★ 게이트 프리뷰용
    private final int grid;
    private final CommitListener commitListener;

    private final Set<Point> selectedCells = new HashSet<>();
    private final Set<Point> gateCells = new HashSet<>();    // ★ 게이트 셀
    private Point hoverCell = null;
    private Point lastCell  = null;

    private boolean axisLocked = false;
    private boolean lockHorizontal = true;
    private float startX, startY;

    private View[] exclusionViews = new View[0];
    private final Rect tmpRect = new Rect();

    // ★ 게이트 모드 토글
    private boolean gateMode = false;

    public FencePlacerOverlay(Context ctx, int gridSizePx, CommitListener listener) {
        super(ctx);
        this.grid = Math.max(8, gridSizePx);
        this.commitListener = listener;

        setClickable(true);
        setFocusable(true);

        // 기본 선택 프리뷰(반투명 초록)
        previewPaint.setAntiAlias(true);
        previewPaint.setStyle(Paint.Style.FILL);
        previewPaint.setColor(0x6633CC66); // ← 검정 그림자 대신 컬러 명시
        previewPaint.setAlpha(110);

        // 게이트 프리뷰(반투명 주황)
        gatePaint.setAntiAlias(true);
        gatePaint.setStyle(Paint.Style.FILL);
        gatePaint.setColor(0x66FFAA00);
        gatePaint.setAlpha(170);
    }

    // === 기존 시그니처 유지(카메라값 자체는 여기서 보관하지 않아도 됨) ===
    public void setCamera(int l, int t) { invalidate(); }
    public void setExclusionViews(View... views) { this.exclusionViews = views != null ? views : new View[0]; }

    // ★ 게이트 모드/데이터 접근자 (기존 로직과 독립)
    public void setGateMode(boolean on) { this.gateMode = on; invalidate(); }
    public boolean isGateMode() { return gateMode; }
    public Set<Point> getGateCells() { return gateCells; }
    public void clearGateCells() { gateCells.clear(); invalidate(); }

    private boolean isInExcluded(MotionEvent e) {
        final int rx = (int) e.getRawX();
        final int ry = (int) e.getRawY();
        for (View v : exclusionViews) {
            if (v == null || v.getVisibility() != VISIBLE) continue;
            v.getGlobalVisibleRect(tmpRect);
            if (tmpRect.contains(rx, ry)) return true;
        }
        return false;
    }

    private Point snapByAxis(float x, float y) {
        int cx = Math.round(x / grid);
        int cy = Math.round(y / grid);
        if (axisLocked) {
            if (lockHorizontal) cy = Math.round(startY / grid);
            else cx = Math.round(startX / grid);
        }
        return new Point(cx, cy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 기존 선택 프리뷰
        for (Point p : selectedCells) {
            float l = p.x * grid, t = p.y * grid;
            canvas.drawRect(new RectF(l, t, l + grid, t + grid), previewPaint);
        }
        if (hoverCell != null) {
            float l = hoverCell.x * grid, t = hoverCell.y * grid;
            canvas.drawRect(new RectF(l, t, l + grid, t + grid), previewPaint);
        }

        // ★ 게이트 선택 프리뷰
        for (Point g : gateCells) {
            float l = g.x * grid, t = g.y * grid;
            canvas.drawRect(new RectF(l, t, l + grid, t + grid), gatePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isInExcluded(e)) return false;

        // ★ 게이트 모드: 탭으로 해당 셀 토글만 수행 (드래그 라인은 그대로)
        if (gateMode) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    Point c = snapByAxis(e.getX(), e.getY());
                    Point key = new Point(c);
                    if (gateCells.contains(key)) gateCells.remove(key);
                    else gateCells.add(key);
                    invalidate();
                    return true;
                }
            }
            return true;
        }

        // ====== 기존 드래그 라인 선택 로직 ======
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                startX = e.getX(); startY = e.getY();
                axisLocked = false; lockHorizontal = true;

                Point c = snapByAxis(startX, startY);
                selectedCells.clear();
                selectedCells.add(c);
                hoverCell = c;
                lastCell  = c;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - startX, dy = e.getY() - startY;
                if (!axisLocked && (Math.abs(dx) > 6 || Math.abs(dy) > 6)) {
                    axisLocked = true;
                    lockHorizontal = Math.abs(dx) >= Math.abs(dy);
                }
                Point c = snapByAxis(e.getX(), e.getY());
                hoverCell = c;
                if (lastCell == null || !lastCell.equals(c)) {
                    fillLineAxis(lastCell, c);
                    lastCell = c;
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                Map<Point,Integer> out = new HashMap<>();
                for (Point p : selectedCells) out.put(new Point(p), 0); // mask는 메인에서 재계산
                if (commitListener != null && !out.isEmpty()) commitListener.onCommit(out);
                selectedCells.clear(); hoverCell=null; lastCell=null; axisLocked=false;
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    private void fillLineAxis(Point a, Point b) {
        if (a == null || b == null) { if (b!=null) selectedCells.add(new Point(b)); return; }
        int x0=a.x,y0=a.y,x1=b.x,y1=b.y;
        if (x0==x1 || y0==y1) { lineAdd(x0,y0,x1,y1); return; }
        if (axisLocked) { if (lockHorizontal) y1=y0; else x1=x0; lineAdd(x0,y0,x1,y1); }
        else { if (Math.abs(x1-x0)>=Math.abs(y1-y0)) y1=y0; else x1=x0; lineAdd(x0,y0,x1,y1); }
    }

    private void lineAdd(int x0,int y0,int x1,int y1){
        int dx=Math.abs(x1-x0), sx=x0<x1?1:-1;
        int dy=Math.abs(y1-y0), sy=y0<y1?1:-1;
        int x=x0,y=y0;
        if(dy<=dx){ while(true){ selectedCells.add(new Point(x,y)); if(x==x1) break; x+=sx; } }
        else{ while(true){ selectedCells.add(new Point(x,y)); if(y==y1) break; y+=sy; } }
    }
}
