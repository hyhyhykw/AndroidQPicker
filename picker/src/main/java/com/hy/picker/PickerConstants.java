package com.hy.picker;

/**
 * Created time : 2019-10-20 10:29.
 *
 * @author HY
 */
public interface PickerConstants {
    //单选
    int REQUEST_PICK = 0x01;

    //单选
    int REQUEST_CAMERA = 0x02;
    //存储权限
    int PICKER_PERMISSION_STORAGE = 0x03;

    //相机权限
    int PICKER_PERMISSION_CAMERA = 0x04;

    //裁剪
    int PICKER_CROP = 0x05;

    //裁剪action
    String ACTION_CROP = "com.android.camera.action.CROP";
    //数量
    String PICK_NUMBER = "com.hy.picker.PICK_NUMBER";

    //是否选择gif
    String PICK_GIF = "com.hy.picker.PICK_GIF";
    //是否只选择gif
    String GIF_ONLY = "com.hy.picker.ONLY_PICK_GIF";
    //是否选择视频
    String PICK_VIDEO = "com.hy.picker.PICK_VIDEO";

    //是否裁剪
    String CROP = "com.hy.picker.CROP";

    //原路径
    String ORIGINAL = "com.hy.picker.ORIGINAL";

    //是否圆形裁剪 裁剪为true才有效
    String CROP_CIRCLE = "com.hy.picker.CROP_CIRCLE";

    //结果
    String PHOTO_RESULT = "com.hy.picker.result.PHOTO";

    //SharedPreference
    String PICKER_SP_NAME = "picker_config_sp_hy";
    //是否显示提示弹窗
    String PICKER_SHOW_HINT_DLG = "picker_show_hint_dlg";


    //文件拷贝目录
    String COPY_DIR = "pickerImageCopy";

    //视频缩略图目录
    String VIDEO_THUMB = "VideoThumb";

    //拍摄视频前缀
    String VDO = "VDO-";

    //拍摄照片前缀
    String IMG = "IMG-";

    //只选择视频
    String[] ONLY_VIDEO = {
            "video/mp4"
    };

    //只选择gif
    String[] ONLY_GIF_MIME = {
            "image/gif"
    };

    //图片带gif
    String[] GIF_MIME = {
            "image/png", "image/jpeg", "image/gif"
    };

    //图片不带gif
    String[] IMAGE_MIME = {
            "image/png", "image/jpeg"
    };

}
