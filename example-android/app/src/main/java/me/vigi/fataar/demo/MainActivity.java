package me.vigi.fataar.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import me.vigi.fataar.demo.lib.LibActivity;

/**
 * Created by Vigi on 2017/1/14.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onJumpBtnClick(View view) {
        startActivity(new Intent(this, LibActivity.class));
    }
}
