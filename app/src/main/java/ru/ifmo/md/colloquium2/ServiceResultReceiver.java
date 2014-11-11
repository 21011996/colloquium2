package ru.ifmo.md.lesson7;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

public class ServiceResultReceiver extends ResultReceiver {
    Context context;

    public ServiceResultReceiver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    protected void onReceiveResult(int code, Bundle bundle) {
        String toShow = "";
        switch (code) {
            case LoadFeedService.ERROR_FEED_EXISTS:
                toShow = "Feed with this URL already exists";
                break;
            case LoadFeedService.ERROR_EXCEPTION:
                toShow = bundle.getString("error");
                break;
        }
        Toast.makeText(context, toShow, Toast.LENGTH_SHORT).show();
    }
}
