package orbital.com.menusnap.Views;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.google.firebase.crash.FirebaseCrash;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import orbital.com.menusnap.Models.OcrPOJO.Line;
import orbital.com.menusnap.R;

/**
 * Extended ImageView to draw the bounding boxes. Overrides onDraw method and it has
 * a transparent background so as to overlay over another view.
 */
public class DrawableView extends FrameLayout {
    private View mRootView = null;

    private List<Rect> mRects = null;
    private List<String> mLineTexts = null;

    private int originalWidth = 0;
    private int originalHeight = 0;
    private Matrix mMatrix = new Matrix();
    private Float mAngle = 0f;
    private float xScale = 0f;
    private float yScale = 0f;

    private Paint greenPaint = null;
    private Paint redPaint = null;

    private Boolean multRect = false;
    private ArrayList<Integer> selectedRects = new ArrayList<>();

    public DrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRects = new ArrayList<>();
        mLineTexts = new ArrayList<>();
        setupPaints(context);
    }

    /**
     * onDraw is called when view is created/refreshed or when invalidate is called.
     * We draw all the drawables in the list of drawables here.
     * Canvas is rotated to make for the angle. (Using centerX and centerY as pivot is
     * not 100% accurate but it looks close enough for now)
     *
     * @param canvas
     */
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // To draw all the rects
        if (mRects != null && !mRects.isEmpty()) {
            canvas.drawColor(Color.TRANSPARENT);
            canvas.save();
            canvas.rotate(mAngle, getPivotX(), getPivotY());
            for (int i = 0; i < mRects.size(); i++) {
                Rect rect = mRects.get(i);
                if (selectedRects.contains(i)) {
                    canvas.drawRect(rect, redPaint);
                    if (!multRect) {
                        selectedRects.clear();
                    }
                } else {
                    canvas.drawRect(rect, greenPaint);
                }
            }
            canvas.restore();
        }

    }

    /**
     * Draws boxes on drawableView with
     *
     * @param rootView  rootView holding this view
     * @param imageUri Image uri for the compressed image file
     * @param lines     list of line to be drawn
     * @param angle     textAngle as received from bing api
     */
    public void drawBoxes(Context context, View rootView, Uri imageUri, List<Line> lines,
                          Float angle, String lang) {
        if (lines == null || lines.isEmpty()) {
            Snackbar.make(this, R.string.no_text_found, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (angle != null) {
            mAngle = angle;
        }
        if (mMatrix == null) {
            mMatrix = new Matrix();
        } else {
            mMatrix.reset();
        }
        mRootView = rootView;
        try {
            getOriginalDimen(context, imageUri);
        } catch (FileNotFoundException e) {
            FirebaseCrash.report(e);
            return;
        }
        addLinesForDraw(lines, lang);
        invalidate();
    }

    public boolean chooseRect(int selectedIndex, boolean multRect) {
        if (selectedRects.contains(selectedIndex)) {
            selectedRects.remove((Integer) selectedIndex);
            this.invalidate();
            return false;
        } else {
            selectedRects.add(selectedIndex);
            this.multRect = multRect;
            this.invalidate();
            return true;
        }
    }

    public int getSelectedCount() {
        if (selectedRects == null) {
            return 0;
        } else {
            return selectedRects.size();
        }
    }

    public void clearSelectedRects() {
        selectedRects.clear();
        //invalidate();
    }

    /**
     * This private method adds the drawables in the list by parsing the boundary
     * parameters and then scaling it and setting them as the drawables' bounds.
     *
     * @param lines List of line to convert into drawables
     */
    private void addLinesForDraw(List<Line> lines, String lang) {
        mLineTexts.clear();
        mRects.clear();
        mMatrix.setScale(findXScale(), findYScale());
        for (Line line : lines) {
            String text = line.getText(lang);
            if (text.trim().isEmpty()) {
                continue;
            }
            mLineTexts.add(text);
            String[] bounds = line.getBoundsArray();
            int x = Integer.parseInt(bounds[0]);
            int y = Integer.parseInt(bounds[1]);
            int width = Integer.parseInt(bounds[2]);
            int height = Integer.parseInt(bounds[3]);
            // Scale using matrix. Rotation can still be improved using matrix transform
            Rect drawRect = new Rect(x, y, x + width, y + height);
            RectF rectF = new RectF(x, y, x + width, y + height);
            scaleRect(drawRect, rectF);
            mRects.add(drawRect);
        }
    }

    private void getOriginalDimen(Context context, Uri imageUri) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(imageStream, null, options);
        originalHeight = options.outHeight;
        originalWidth = options.outWidth;
        try {
            imageStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method sets scale on matrix to findXScale and findYScale then
     * maps it to rectF which is then rounded into outputRect
     *
     * @param outputRect The rect to be used as output for the scaling
     * @param rectF      The input rectF to be mapped
     */
    private void scaleRect(Rect outputRect, RectF rectF) {
        mMatrix.mapRect(rectF);
        rectF.round(outputRect);
    }

    private float findXScale() {
        DrawableView drawableView = (DrawableView) mRootView.findViewById(R.id.drawable_view);
        xScale = (float) drawableView.getWidth() /
                (float) originalWidth;
        return xScale;
    }

    private float findYScale() {
        DrawableView drawableView = (DrawableView) mRootView.findViewById(R.id.drawable_view);
        yScale = (float) drawableView.getHeight() /
                (float) originalHeight;
        return yScale;
    }

    private void setupPaints(Context context) {
        greenPaint = new Paint();
        greenPaint.setStyle(Paint.Style.STROKE);
        greenPaint.setColor(ContextCompat.getColor(context, R.color.basePaintColor));
        greenPaint.setStrokeWidth(4);
        redPaint = new Paint(greenPaint);
        redPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
    }


    public List<Rect> getmRects() {
        return mRects;
    }

    public List<String> getmLineTexts() {
        return mLineTexts;
    }
}
