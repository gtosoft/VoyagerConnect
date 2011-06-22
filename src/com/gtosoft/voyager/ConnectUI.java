package com.gtosoft.voyager;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.gtosoft.libvoyager.android.ELMBT;
import com.gtosoft.libvoyager.svip.SVIPTCPClient;
import com.gtosoft.libvoyager.util.EventCallback;

public class ConnectUI extends Activity {
	
	ELMBT ebt;
	SVIPTCPClient svipClient;
	TextView mtvMain;
	Handler muiHandler = new Handler();
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mtvMain = (TextView) findViewById(R.id.tvMain);
        
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Create Intent...");
        Intent svc=new Intent ("com.gtosoft.voyager.service.start"); 
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Run intent to start service...");
        startService(svc);
        
        svipClient = new SVIPTCPClient();
        registerSVIPCallbacks();
    }

    

    EventCallback mECBDPArrived = new EventCallback () {
    	public void onDPArrived(String DPN, String sDecodedData, int iDecodedData) {
    		msg ("(ui)DP Arrived: " + DPN + "=" + sDecodedData);
    	}
    };
    
    EventCallback mECBOOBArrived = new EventCallback () {
    	public void onOOBDataArrived(String dataName, String dataValue) {
    		msg ("(ui)OOB Arrived: " + dataName + "=" + dataValue);
    	}    	
    };

    private void registerSVIPCallbacks() {
		if (svipClient == null)
			return;
		
		svipClient.registerDPArrivedHandler(mECBDPArrived);
		svipClient.registerOOBArrivedHandler(mECBOOBArrived);
	}



	/**
     * 
     * @param m
     */
    private void msg (final String m) {
    	Log.d("ConnectUI",m);
    	muiHandler.post(new Runnable () {
    		public void run () {
    			mtvMain.append(m + "\n");
    		}// end of run
    	});// end of posting
    }// end of msg()
    
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (svipClient != null) svipClient.shutdown();
    }
}