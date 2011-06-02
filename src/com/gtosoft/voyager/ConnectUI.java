package com.gtosoft.voyager;

import com.gtosoft.voyager.service.VoyagerService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConnectUI extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Create Intent...");
        Intent svc=new Intent (ConnectUI.this,VoyagerService.class);
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Run intent to start service...");
        startService(svc);
        
    }
}