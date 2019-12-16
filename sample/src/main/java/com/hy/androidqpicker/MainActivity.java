package com.hy.androidqpicker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.hy.picker.AndroidQPicker;
import com.hy.picker.PhotoEntry;
import com.hy.utils.Logger;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void onClick(View view) {
        AndroidQPicker.newBuilder()
                .number(1)
                .crop()
                .cropRadius(20)
                .openCamera(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ArrayList<PhotoEntry> entries = AndroidQPicker.obtainResult(resultCode, data);

        for (PhotoEntry entry : entries) {
            Logger.e("entry===" + entry);
        }
    }
}
