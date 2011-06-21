package com.gtosoft.voyager;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.gtosoft.libvoyager.android.ELMBT;
import com.gtosoft.libvoyager.svip.SVIPTCPClient;

public class ConnectUI extends Activity {
	
	ELMBT ebt;
	SVIPTCPClient svipClient;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Create Intent...");
        Intent svc=new Intent ("com.gtosoft.voyager.service.start"); 
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Run intent to start service...");
        startService(svc);
        
        svipClient = new SVIPTCPClient();
    }
    
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (svipClient != null) {
    		svipClient.shutdown();
    	}
    }
}