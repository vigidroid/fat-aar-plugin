package me.vigi.fataar.demo.lib;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RequestQueue.RequestFilter;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by Vigi on 2017/1/14.
 */

public class LibActivity extends Activity {

    Button mButton;
    TextView mContentText;

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lib);
        mButton = (Button) findViewById(R.id.lib_bt);
        mContentText = (TextView) findViewById(R.id.lib_tv);

        mRequestQueue = Volley.newRequestQueue(this);
    }

    public void onButtonClick(View view) {
        mRequestQueue.add(new StringRequest("https://api.github.com/users/vigi0303",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                mContentText.setText(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                mContentText.setText(error.getMessage());
                            }
                        })
        );
    }

    @Override
    protected void onDestroy() {
        mRequestQueue.cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        super.onDestroy();
    }
}
