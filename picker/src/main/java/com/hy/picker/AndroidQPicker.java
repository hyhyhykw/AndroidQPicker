package com.hy.picker;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.hy.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Created time : 2019-10-20 09:19.
 *
 * @author HY
 */
@SuppressWarnings("WeakerAccess")
public final class AndroidQPicker implements PickerConstants {

    //数量
    private final int number;
    //是否选择gif
    private final boolean gif;
    //是否只选择gif
    private final boolean onlyGif;
    //是否选择video
    private final boolean video;
    //是否裁剪
    private final boolean crop;
    //是否裁剪
    private final boolean cropCircle;

    private AndroidQPicker(int number,
                           boolean gif,
                           boolean onlyGif,
                           boolean video,
                           boolean crop, boolean cropCircle) {
        this.number = number;
        this.gif = gif;
        this.onlyGif = onlyGif;
        this.video = video;
        this.crop = crop;

        this.cropCircle = cropCircle;
    }

    public AndroidQPicker(Builder builder) {
        this(builder.number,
                builder.gif,
                builder.onlyGif,
                builder.video,
                builder.crop,
                builder.cropCircle);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void pick(Activity activity) {
        activity.startActivityForResult(
                new Intent(activity, PickerResultActivity.class)
                        .putExtra(PICK_NUMBER, number)
                        .putExtra(PICK_GIF, gif)
                        .putExtra(GIF_ONLY, onlyGif)
                        .putExtra(CROP, crop)
                        .putExtra(CROP_CIRCLE, cropCircle)
                        .putExtra(PICK_VIDEO, video),
                REQUEST_PICK
        );
    }


    public void openCamera(Activity activity) {

        activity.startActivityForResult(
                new Intent(activity, OpenCameraResultActivity.class)
                        .putExtra(PICK_VIDEO, video)
                        .putExtra(CROP_CIRCLE, cropCircle)
                        .putExtra(CROP, crop),
                REQUEST_CAMERA
        );
    }


    @SuppressWarnings("unused")
    public static final class Builder {
        private boolean cropCircle = false;
        private int number;
        private boolean gif = true;
        private boolean onlyGif = false;
        private boolean video = false;
        private boolean crop = false;

        private Builder() {
        }

        public Builder number(@IntRange(from = 1) int number) {
            this.number = number;
            return this;
        }

        public Builder gif(boolean gif) {
            this.gif = gif;
            return this;
        }

        public Builder crop() {
            this.crop = true;
            return this;
        }

        public Builder cropCircle() {
            this.cropCircle = true;
            return this;
        }

        public Builder onlyGif() {
            this.onlyGif = true;
            return this;
        }

        public Builder video() {
            this.video = true;
            return this;
        }


        public AndroidQPicker build() {
            return new AndroidQPicker(this);
        }

        public void pick(Activity activity) {
            build().pick(activity);
        }

        public void openCamera(Activity activity) {
            build().openCamera(activity);
        }
    }


    /////////////////////////////////--处理数据--////////////////////////////////////

    /**
     * 获取选择的文件 uri集合
     */
    @NonNull
    public static ArrayList<PhotoEntry> obtainResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return new ArrayList<>();
        }

        ArrayList<PhotoEntry> result = data.getParcelableArrayListExtra(PHOTO_RESULT);

        return result == null ? new ArrayList<>() : result;
    }


    static void uris2photos(Context context,
                            ArrayList<Uri> uris,
                            int number,
                            boolean video,
                            OnParseFinishListener listener) {
        Application applicationContext = (Application) context.getApplicationContext();

        new Uri2PathTask(applicationContext,
                uris,
                number,
                video,
                listener)
                .execute();
    }


    public interface OnParseFinishListener {
        void onFinish(ArrayList<PhotoEntry> photos);
    }


    static final class ThumbEntry {
        private final String path;
        private final int width;
        private final int height;

        public ThumbEntry(String path, int width, int height) {
            this.path = path;
            this.width = width;
            this.height = height;
        }

        public String getPath() {
            return path;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }


    /**
     * 删除图片缓存
     */
    public static void clearCache(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }

        File copy = new File(cacheDir, COPY_DIR);
        File[] files = copy.listFiles();
        delete(files);

        File thumbDir = new File(cacheDir, VIDEO_THUMB);
        File[] thumbFiles = thumbDir.listFiles();
        delete(thumbFiles);


        File movieDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (movieDir == null) {
            movieDir = new File(context.getFilesDir().getAbsolutePath()
                    + File.separator
                    + Environment.DIRECTORY_MOVIES);
        }

        File picFiles = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picFiles == null) {
            picFiles = new File(context.getFilesDir().getAbsolutePath()
                    + File.separator
                    + Environment.DIRECTORY_PICTURES);
        }

        File[] vdoFiles = movieDir.listFiles((dir, name) -> name.startsWith(VDO));
        File[] imgFiles = picFiles.listFiles((dir, name) -> name.startsWith(IMG));

        delete(vdoFiles);
        delete(imgFiles);

    }

    private static void delete(File[] files) {
        if (null != files) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static ThumbEntry video2thumb(Context context, String path, String name) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();//获取视频第一帧
        media.setDataSource(path);

        Bitmap bitmap = media.getFrameAtTime();

        String orientation = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);//视频旋转方向
        String height = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // 视频高度
        String width = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // 视频宽度

        int vdoWidth;
        int vdoHeight;

        if (orientation.equals("90") || orientation.equals("270")) {
            vdoWidth = (int) Float.parseFloat(height);
            vdoHeight = (int) Float.parseFloat(width);
        } else {
            vdoWidth = (int) Float.parseFloat(width);
            vdoHeight = (int) Float.parseFloat(height);
        }
        FileOutputStream fos = null;

        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }

        String thumbPath = cacheDir.getAbsolutePath()
                + File.separator
                + VIDEO_THUMB
                + File.separator
                + name + "_thumb_" + System.currentTimeMillis() + ".jpg";

        File file = new File(thumbPath);
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            file.createNewFile();
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos);
        } catch (Exception e) {
            Logger.e(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new ThumbEntry(thumbPath, vdoWidth, vdoHeight);
    }


    private static final class Uri2PathTask extends AsyncTask<String, Void, ArrayList<PhotoEntry>> {
        private final Application context;
        private final ArrayList<Uri> uris;
        private final int number;
        private final boolean video;
        private final OnParseFinishListener listener;


        Uri2PathTask(Application context,
                     ArrayList<Uri> uris,
                     int number,
                     boolean video,
                     OnParseFinishListener listener) {
            this.context = context;
            this.uris = uris;
            this.number = number;
            this.video = video;
            this.listener = listener;
        }

        @Override
        protected ArrayList<PhotoEntry> doInBackground(String... strings) {
            ContentResolver contentResolver = context.getContentResolver();

            ArrayList<PhotoEntry> photos = new ArrayList<>();

            for (Uri uri : uris) {
                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);

                    Cursor returnCursor =
                            contentResolver.query(uri, null, null, null, null);

                    String extension;
                    if (returnCursor != null) {
                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        returnCursor.moveToFirst();

                        long size = returnCursor.getInt(returnCursor.getColumnIndex(OpenableColumns.SIZE));
                        if (size < 1) continue;

                        String name = returnCursor.getString(nameIndex);
                        String[] split = name.split("\\.", 2);
                        if (split[1].isEmpty()) {
                            extension = video ? ".mp4" : ".jpg";
                        } else {
                            extension = "." + split[1];
                        }

                        String filename = md5(uri.toString()) + extension;
                        String path = getPathFromInputStreamUri(context, inputStream, filename);

                        String mimeType = returnCursor.getString(returnCursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));

                        returnCursor.close();

                        if (video) {

                            ThumbEntry thumbEntry = video2thumb(context, path, split[0]);

                            PhotoEntry photoEntry = new PhotoEntry(
                                    thumbEntry.getWidth(),
                                    thumbEntry.getHeight(),
                                    size,
                                    path,
                                    mimeType,
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
                                    size,
                                    path,
                                    mimeType,
                                    filename,
                                    null,
                                    null
                            );
                            photos.add(photoEntry);
                        }


                    }

                } catch (FileNotFoundException e) {
                    Logger.e(e.getMessage(), e);
                }
                if (photos.size() == number) break;
            }

            return photos;
        }


        @Override
        protected void onPostExecute(ArrayList<PhotoEntry> photos) {
            super.onPostExecute(photos);
            listener.onFinish(photos);
        }
    }


    /**
     * 用流拷贝文件一份到自己APP目录下
     */
    private static String getPathFromInputStreamUri(Context context, InputStream inputStream, String fileName) {
        String filePath = null;

        try {
            File file = createTemporalFileFrom(context, inputStream, fileName);
            filePath = file.getPath();

        } catch (Exception e) {
            Logger.e(e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Logger.e(e);
            }
        }

        return filePath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createTemporalFileFrom(Context context, InputStream inputStream, String fileName) {
        File targetFile = null;

        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[1024 * 1024];
            //自己定义拷贝文件路径

            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir == null) {
                externalCacheDir = context.getCacheDir();
            }

            File imageCopy = new File(externalCacheDir, COPY_DIR);
            if (!imageCopy.exists()) {
                imageCopy.mkdirs();
            } else if (!imageCopy.isDirectory()) {
                imageCopy.delete();
                imageCopy.mkdirs();
            }
            targetFile = new File(imageCopy, fileName);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            try {
                OutputStream outputStream = new FileOutputStream(targetFile);

                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();

                outputStream.close();
            } catch (IOException e) {
                Logger.e(e.getMessage(), e);
            }

        }

        return targetFile;
    }


    /**
     * Encodes a string 2 MD5
     *
     * @param str String to encode
     * @return Encoded String
     */
    private static String md5(String str) {
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] hash = md.digest(str.getBytes());

            for (byte b : hash) {
                String tmp = Integer.toHexString(0xFF & b);
                if (tmp.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(tmp);
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.e(e.getMessage(), e);
        }
        return hexString.toString();
    }


}
