package com.hy.picker;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import com.hy.utils.AppUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.hy.picker.AndroidQPicker.video2thumb;

/**
 * Created time : 2018/8/23 10:56.
 *
 * @author HY
 */
public class OpenCameraResultActivity extends AppCompatActivity implements PickerConstants, EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {
    //    public static final int REQUEST_EDIT = 0x753;
    private boolean video;
    private boolean crop;
    private boolean cropCircle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        video = intent.getBooleanExtra(PICK_VIDEO, false);
        crop = intent.getBooleanExtra(CROP, false);
        cropCircle = intent.getBooleanExtra(CROP_CIRCLE, false);
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestCamera();
        } else {
            List<String> permissionNames = Permission.transformText(this, Manifest.permission.CAMERA);
            String message = getString(R.string.picker_message_permission_rationale, TextUtils.join("\n", permissionNames));
            EasyPermissions.requestPermissions(
                    this,
                    message,
                    PICKER_PERMISSION_CAMERA, Manifest.permission.CAMERA);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_PERMISSION_CAMERA) {
            if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
                requestCamera();
            } else {
                Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        if (resultCode == RESULT_OK) {
            if (requestCode == PICKER_CROP) {
                //直接返回
                setResult(resultCode, data);
                finish();
                return;
            }

            if (requestCode == REQUEST_CAMERA) {
                File file = new File(picturePath);

                if (!video && crop) {//裁剪
                    Intent intent = new Intent(this, PickerCropActivity.class)
                            .putExtra(CROP_CIRCLE, cropCircle)
                            .putExtra(ORIGINAL, file.getAbsolutePath());
                    startActivityForResult(intent,PICKER_CROP);
                    return;
                }
                getPhoto(file);
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            requestCamera();
        } else {
            Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            List<String> permissionNames = Permission.transformText(this, Manifest.permission.CAMERA);
            String message = getString(R.string.picker_message_permission_always_failed, TextUtils.join("\n", permissionNames));

            new AppSettingsDialog.Builder(this)
                    .setRationale(message)
                    .setRequestCode(requestCode)
                    .build()
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
        finish();
    }

    private AlertDialog mProgressDialog;

    //讲媒体文件转为PhotoEntry对象
    private void getPhoto(File file) {

        AppUtils.post(() -> {
            if (mProgressDialog == null) {
                mProgressDialog = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setView(R.layout.picker_dialog_progress)
                        .create();
            }

            mProgressDialog.show();
        });


        new Thread(() -> {
            ArrayList<PhotoEntry> photos = new ArrayList<>();
            String path = file.getAbsolutePath();

            String filename = file.getName();
            String[] split = filename.split("\\.", 2);
            if (video) {
                AndroidQPicker.ThumbEntry thumbEntry = video2thumb(getApplicationContext(), path, split[0]);

                PhotoEntry photoEntry = new PhotoEntry(
                        thumbEntry.getWidth(),
                        thumbEntry.getHeight(),
                        file.length(),
                        path,
                        "video/mp4",
                        filename,
                        thumbEntry.getPath(),
                        null
                );
                photos.add(photoEntry);

            } else {
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, op);

                PhotoEntry photoEntry = new PhotoEntry(
                        op.outWidth,
                        op.outHeight,
                        file.length(),
                        path,
                        "image/jpeg",
                        filename,
                        null,
                        null
                );
                photos.add(photoEntry);
            }

            runOnUiThread(() -> {
                mProgressDialog.setOnCancelListener(dialog -> {
                    setResult(RESULT_OK, new Intent()
                            .putParcelableArrayListExtra(PHOTO_RESULT, photos));
                    finish();
                });
                AppUtils.postDelay(() -> mProgressDialog.cancel(), 200);
            });
        }).start();
    }


    private String picturePath;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void requestCamera() {
        //检测是否可以拍照
        Intent intent;
        if (video) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        } else {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.size() <= 0) {
            Toast.makeText(this, R.string.picker_voip_cpu_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //文件
        String type;
        String filename;
        if (video) {
            type = Environment.DIRECTORY_MOVIES;
            filename = VDO + format(new Date()) + ".mp4";
        } else {
            type = Environment.DIRECTORY_PICTURES;
            filename = IMG + format(new Date()) + ".jpg";
        }

        //创建文件夹
        File filesDir = getExternalFilesDir(type);
        if (filesDir == null) {
            filesDir = new File(getFilesDir().getAbsolutePath()
                    + File.separator
                    + type);
        }

        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        File file = new File(filesDir, filename);
        picturePath = file.getAbsolutePath();

        //uri处理
        Uri takePictureUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            takePictureUri = MyFileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".picker.file_provider", file);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            takePictureUri = Uri.fromFile(file);
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, takePictureUri)
                .addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    /**
     * 功能描述：格式化输出日期
     *
     * @param date Date 日期
     * @return 返回字符型日期
     */
    private static String format(Date date) {
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getInstance();
        sdf.applyPattern("yyyy-MM-dd-HHmmss");
        return sdf.format(date);
    }
}