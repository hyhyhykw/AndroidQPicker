package com.hy.picker;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.Toast;

import com.hy.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created time : 2019-10-20 09:47.
 *
 * @author HY
 */
public class PickerResultActivity extends AppCompatActivity implements PickerConstants, EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private CheckBox mChbNotShowing;
    private AlertDialog mHintDialog;

    private boolean crop;
    private boolean cropCircle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        video = data.getBooleanExtra(PICK_VIDEO, false);
        number = data.getIntExtra(PICK_NUMBER, 1);
        crop = data.getBooleanExtra(CROP, false);
        cropCircle = data.getBooleanExtra(CROP_CIRCLE, false);
        if (!video) {
            gif = data.getBooleanExtra(PICK_GIF, true);
            gifOnly = data.getBooleanExtra(GIF_ONLY, false);
        }
        prePick();

    }


    //开始之前 请求权限
    private void prePick() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            start();
        } else {
            List<String> permissionNames = Permission.transformText(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            String message = getString(R.string.picker_message_permission_rationale, TextUtils.join("\n", permissionNames));
            EasyPermissions.requestPermissions(
                    this,
                    message,
                    PICKER_PERMISSION_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }

    }

    private int number;
    private boolean gif;
    private boolean gifOnly;
    private boolean video;

    //开始选取 弹出提示框等
    private void start() {
        if (number == 1) {
            pick();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(PICKER_SP_NAME, MODE_PRIVATE);

        boolean isShow = sharedPreferences.getBoolean(PICKER_SHOW_HINT_DLG, true);

        if (isShow) {
            if (null == mHintDialog) {
                mHintDialog = new AlertDialog.Builder(this)
                        .setView(R.layout.picker_dialog_hint)
                        .setMessage(getString(R.string.picker_str_note_content, number))
                        .setTitle(R.string.picker_str_note)
                        .setPositiveButton(R.string.picker_str_sure, (dialog, which) -> {
                            pick();
                            dialog.cancel();
                        })
                        .setCancelable(false)
                        .setOnCancelListener(dialog -> {
                            if (null != mChbNotShowing) {
                                sharedPreferences.edit()
                                        .putBoolean(PICKER_SHOW_HINT_DLG, !mChbNotShowing.isChecked())
                                        .apply();
                            }
                        })
                        .create();
            }

            mHintDialog.show();
            //自定义的布局需要在显示之后调用findViewById
            if (null == mChbNotShowing) {
                mChbNotShowing = mHintDialog.findViewById(R.id.picker_dlg_chb);
            }
        } else {
            pick();
        }


    }

    //选取图片
    private void pick() {
        Intent intent = new Intent()
                .setAction(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*");

        String[] mimeTypes;

        if (video) {
            mimeTypes = ONLY_VIDEO;
        } else {
            if (gifOnly) {
                mimeTypes = ONLY_GIF_MIME;
            } else {
                if (gif) {
                    mimeTypes = GIF_MIME;
                } else {
                    mimeTypes = IMAGE_MIME;
                }
            }

        }

        if (number > 1) {//多选
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        //设置类型
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_PICK);
    }

    private AlertDialog mProgressDialog;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //手动开启权限
        if (requestCode == PICKER_PERMISSION_STORAGE) {
            if (EasyPermissions.hasPermissions(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
                start();
            } else {
                Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        if (requestCode == PICKER_CROP) {
            //直接返回
            setResult(resultCode, data);
            finish();
            return;
        }

        if (mProgressDialog == null) {
            mProgressDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setView(R.layout.picker_dialog_progress)
                    .create();
        }

        if (number != 1 || video || !crop) {
            mProgressDialog.show();
        }
        ArrayList<Uri> uris = new ArrayList<>();
        if (resultCode == RESULT_OK && data != null) {
            String dataString = data.getDataString();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    uris.add(item.getUri());
                }
            } else if (dataString != null) {
                uris.add(Uri.parse(dataString));
            }
        }

        if (uris.size() == 1 && !video && crop) {
            AndroidQPicker.uris2photos(this, uris, number, false, photos -> {
                mProgressDialog.setOnCancelListener(dialog -> {

                    Intent intent = new Intent(this, PickerCropActivity.class)
                            .putExtra(CROP_CIRCLE, cropCircle)
                            .putExtra(ORIGINAL, photos.get(0).getPath());
                    startActivityForResult(intent,PICKER_CROP);
                });
                AppUtils.postDelay(() -> mProgressDialog.cancel(), 200);
            });


        } else {
            AndroidQPicker.uris2photos(this, uris, number, video, photos -> {
                mProgressDialog.setOnCancelListener(dialog -> {
                    setResult(RESULT_OK, new Intent()
                            .putParcelableArrayListExtra(PHOTO_RESULT, photos));
                    finish();
                });
                AppUtils.postDelay(() -> mProgressDialog.cancel(), 200);
            });
        }

    }

    //获取到权限
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        start();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {

            List<String> permissionNames = Permission.transformText(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            String message = getString(R.string.picker_message_permission_always_failed, TextUtils.join("\n", permissionNames));

            new AppSettingsDialog.Builder(this)
                    .setRationale(message)
                    .setRequestCode(requestCode)
                    .build()
                    .show();
        } else {
            Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICKER_PERMISSION_STORAGE) {
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        //再次请求权限被允许
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Toast.makeText(this, R.string.picker_str_permission_denied, Toast.LENGTH_SHORT).show();
        finish();
    }
}
