package com.hy.androidqpicker;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * Created time : 2019-10-19 22:03.
 *
 * @author HY
 */
public class App  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
