package id.depthlock.hios;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad * 2, pad, pad);
        tv.setTextSize(16f);
        tv.setText(getString(R.string.main_info));
        setContentView(tv);
    }
}
