package com.hy.picker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;

import com.hy.picker.crop.PickerLikeQQCropView;
import com.hy.utils.AppUtils;
import com.hy.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created time : 2019-10-30 11:47.
 *
 * @author HY
 */
public class PickerCropActivity extends AppCompatActivity implements PickerConstants {


    private PickerLikeQQCropView likeView;

    private String filepath;

    private boolean cropCircle;
    private int cropRadius;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picker_activity_crop);
        likeView = findViewById(R.id.picker_like_view);

        Intent intent = getIntent();

        cropCircle = intent.getBooleanExtra(CROP_CIRCLE, false);
        filepath = intent.getStringExtra(ORIGINAL);
        cropRadius = intent.getIntExtra(PICKER_CROP_RADIUS, -1);

        findViewById(R.id.picker_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.picker_done).setOnClickListener(v -> {
            Bitmap bitmap = likeView.clip();
            saveBitmap(bitmap);
        });


        Looper.myQueue().addIdleHandler(() -> {
            AppUtils.post(this::init);
            return false;
        });
    }

    private void saveBitmap(Bitmap bitmap) {
        File filesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (filesDir == null) {
            filesDir = new File(getFilesDir().getAbsolutePath()
                    + File.separator
                    + Environment.DIRECTORY_PICTURES
            );
        }

        if (!filesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            filesDir.mkdirs();
        }


        String filename;

        if (cropCircle) {
            filename = "CROP-" + System.currentTimeMillis() + ".png";
        } else {
            filename = "CROP-" + System.currentTimeMillis() + ".jpg";
        }
        File file = new File(filesDir, filename);

        try {
            FileOutputStream out = new FileOutputStream(file);
            if (cropCircle) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
            out.flush();
            out.close();
            getPhoto(file);
            finish();
            return;
        } catch (IOException e) {
            Logger.e(e.getMessage(), e);
        }

        finish();
    }

    private void getPhoto(File file) {
        ArrayList<PhotoEntry> photos = new ArrayList<>();
        String filename = file.getName();
        int size = (int) likeView.getClipWidth();
        PhotoEntry photoEntry = new PhotoEntry(
                size,
                size,
                file.length(),
                file.getAbsolutePath(),
                cropCircle ? "image/png" : "image/jpeg",
                filename,
                null,
                filepath
        );
        photos.add(photoEntry);
        setResult(RESULT_OK, new Intent()
                .putParcelableArrayListExtra(PHOTO_RESULT, photos));

    }

    private void init() {
        likeView.setBitmapForWidth(filepath, 1080);
        float radius = likeView.getClipWidth() / 2;

        if (cropCircle) {
            likeView.setRadius(radius);
            return;
        }

        if (cropRadius == -1) {
            likeView.setRadius(0);
        } else {
//            int dp = SizeUtils.dp2px(this, cropRadius);
            likeView.setRadius(Math.min(radius,cropRadius));
        }

    }


}
