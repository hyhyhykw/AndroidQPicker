package com.hy.picker;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * Created time : 2019-10-21 11:09.
 * 将uri转为photo对象 包含一些信息
 * 实现Parcelable接口 方便传递数据
 *
 * @author HY
 */
public class PhotoEntry implements Parcelable {

    //宽
    private int width;
    //高
    private int height;
    //文件大小
    private long size;
    //路径
    private String path;
    //类型
    private String mimeType;
    //文件名
    private String filename;

    //视频封面（仅视频有）
    private String cover;

    //原路径 裁剪之后会有
    private String original;

    public PhotoEntry() {
    }

    public PhotoEntry(int width,
                      int height,
                      long size,
                      String path,
                      String mimeType,
                      String filename,
                      String cover,
                      String original) {
        this.width = width;
        this.height = height;
        this.size = size;
        this.path = path;
        this.mimeType = mimeType;
        this.filename = filename;
        this.cover = cover;
        this.original = original;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoEntry that = (PhotoEntry) o;
        return width == that.width &&
                height == that.height &&
                size == that.size &&
                Objects.equals(path, that.path) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, size, path, mimeType, filename);
    }

    @Override
    public @NonNull
    String toString() {
        return "PhotoEntry{" +
                "width=" + width +
                ", height=" + height +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", filename='" + filename + '\'' +
                ", cover='" + cover + '\'' +
                ", original='" + original + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeLong(this.size);
        dest.writeString(this.path);
        dest.writeString(this.mimeType);
        dest.writeString(this.filename);
        dest.writeString(this.cover);
        dest.writeString(this.original);
    }

    protected PhotoEntry(Parcel in) {
        this.width = in.readInt();
        this.height = in.readInt();
        this.size = in.readLong();
        this.path = in.readString();
        this.mimeType = in.readString();
        this.filename = in.readString();
        this.cover = in.readString();
        this.original = in.readString();
    }

    public static final Parcelable.Creator<PhotoEntry> CREATOR = new Parcelable.Creator<PhotoEntry>() {
        @Override
        public PhotoEntry createFromParcel(Parcel source) {
            return new PhotoEntry(source);
        }

        @Override
        public PhotoEntry[] newArray(int size) {
            return new PhotoEntry[size];
        }
    };
}
