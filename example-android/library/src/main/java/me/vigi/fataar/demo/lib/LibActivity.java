package me.vigi.fataar.demo.lib;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

import pl.droidsonroids.gif.GifDrawable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Vigi on 2017/1/14.
 */

public class LibActivity extends Activity {

    Button mButton;
    TextView mContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lib);
        mButton = (Button) findViewById(R.id.lib_bt);
        mContentText = (TextView) findViewById(R.id.lib_tv);

        // use android-gif-drawable
        try {
            new GifDrawable("test");
        } catch (IOException ignored) {}
        mContentText.append("Have android-gif-drawable\n");

        // use guava
        Lists.newArrayList();
        mContentText.append("Have guava\n");

        // use java-lib
//        mContentText.append("Have java-lib(project) and output=" + JavaLibClass.plus(1, 2) + "\n");

        // use rxandroid and rxjava
        Observable.just("Have", "rxandroid", "and", "rxjava")
                .toList()
                .map(new Func1<List<String>, String>() {
                    @Override
                    public String call(List<String> strings) {
                        return Joiner.on(' ').join(strings);
                    }
                })
                .subscribeOn(Schedulers.immediate())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mContentText.append(s);
                    }
                });

        // use commons-lang
        String[] strArray = new String[]{"Have", "commons-lang(jar file)"};
        mContentText.append(StringUtils.join(strArray, ' ') + '\n');
    }

    public void onButtonClick(View view) {
        // use aar-lib
//        mContentText.append("Have aar-lib(project)\n");
//        startActivity(new Intent(this, AarLibActivity.class));
    }
}
