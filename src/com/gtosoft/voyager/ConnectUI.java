package com.gtosoft.voyager;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.gtosoft.libvoyager.android.ELMBT;
import com.gtosoft.libvoyager.svip.SVIPTCPClient;
import com.gtosoft.libvoyager.util.EasyTime;
import com.gtosoft.libvoyager.util.EventCallback;
import com.gtosoft.libvoyager.util.OOBMessageTypes;

public class ConnectUI extends Activity {
	boolean 	DEBUG 			= true;
	ELMBT		ebt;
	SVIPTCPClient svipClient;
	TextView 	mtvMain;
	Button 		mbtnStopService;
	Handler 	muiHandler 		= new Handler();
	Thread 		stupidThread 	= null;
	boolean 	mThreadsOn 		= true;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mtvMain = (TextView) findViewById(R.id.tvMain);
        mbtnStopService = (Button) findViewById(R.id.btnStopService);
        setButtonEventHandlers();       
        
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Create Intent...");
        Intent svc=new Intent ("com.gtosoft.voyager.service.start"); 
        
        Log.d("ConnectUI.svcEnable.setOnclickListener()","Run intent to start service...");
        startService(svc);
        
        svipClient = new SVIPTCPClient();
        // Registers to handle OOB, DPN arrived, etc. 
        registerSVIPCallbacks();
        // DPN subscriptions will automatically be made upon SVIP connect. 
        
        startStupidThread();
    }

    private boolean setButtonEventHandlers () {
    	
    	mbtnStopService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				msg ("Stopping service.");
				Intent svc = new Intent ("com.gtosoft.voyager.service.start");
				stopService(svc);
			}
		});
    	
    	return true;
    }
    

    EventCallback mLocalECBDPArrived = new EventCallback () {
    	public void onDPArrived(String DPN, String sDecodedData, int iDecodedData) {
    		msg ("(ui)DP Arrived: " + DPN + "=" + sDecodedData);
    	}
    };
    
    EventCallback mLocalECBOOBArrived = new EventCallback () {
    	public void onOOBDataArrived(String dataName, String dataValue) {
    		msg ("(ui)OOB Arrived: " + dataName + "=" + dataValue);
    		
    		if (dataName.equals(OOBMessageTypes.SVIP_CLIENT_JUST_CONNECTED)) {
    			// SVIP connected... Pass our data subscriptions to the server.
    			if (DEBUG) msg ("ConnectUI OOB event handler: SVIP Connected, registering subscriptions.");
    	        svipClient.subscribe ("VOLTS");
    	        svipClient.subscribe ("SPEED");
    	        svipClient.subscribe ("RPM");
    		}
    		
    	}    	
    };

    private void registerSVIPCallbacks() {
		if (svipClient == null)
			return;
		
		svipClient.registerDPArrivedHandler(mLocalECBDPArrived);
		svipClient.registerOOBArrivedHandler(mLocalECBOOBArrived);
	}

    private boolean startStupidThread () {
    	if (stupidThread != null) return false;
    	
    	stupidThread = new Thread() {
    		public void run () {
    		
    			while (mThreadsOn == true) {
    				if (svipClient != null && svipClient.connected() == true) {
    					if (DEBUG) msg ("Server PING response time: " + svipClient.pingServer());
    				}
    				
    				EasyTime.safeSleep(1000);
    			}
    			
    			if (DEBUG) msg ("End of connectUI thread run.");
    		}// end of run()
    	};
    	stupidThread.start();
    	
    	
    	return true;
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
    	mThreadsOn = false;
    	if (stupidThread != null) stupidThread.interrupt();
    }
}