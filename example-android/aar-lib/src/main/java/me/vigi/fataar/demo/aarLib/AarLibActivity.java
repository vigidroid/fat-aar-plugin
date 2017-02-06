package me.vigi.fataar.demo.aarLib;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by Vigi on 2017/1/14.
 */

public class AarLibActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aar_lib);
    }

    public void onFinishClick(View view) {
        finish();
    }
}
