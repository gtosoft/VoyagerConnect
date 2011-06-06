
/**
 * This class will define the service aspect of VoyagerConnect.
 */
package com.gtosoft.voyager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.gtosoft.libvoyager.android.ServiceHelper;
import com.gtosoft.libvoyager.autosession.AutoSessionAdapter;
import com.gtosoft.libvoyager.db.DashDB;
import com.gtosoft.libvoyager.session.HybridSession;
import com.gtosoft.libvoyager.util.EasyTime;
import com.gtosoft.libvoyager.util.EventCallback;
import com.gtosoft.libvoyager.util.GeneralStats;

/**
 * @author Brad Hein / GTOSoft LLC. 
 */
public class VoyagerService extends Service {
	
	AutoSessionAdapter mSessionAdapter;
	
	boolean mThreadsOn = true;
	
	DashDB ddb;
//	HybridSession hs;
	GeneralStats mgStats;
	
	Thread mtDataCollector = null;
	String mBTPeerMAC;
	ServiceHelper msHelper;
	
	// for posting stuff back to the main service thread. 
	Handler mHandler = new Handler();

	// stuff for notificaitons. 
	NotificationManager mNotificationManager = null;
	private static final int VOYAGER_NOTIFY_ID_OBD = 1234;
	private static final int VOYAGER_NOTIFY_ID_CHECKENG = 2345;

	Notification mNotificationGeneral = null;
	Notification mNotificationCheckEngine = null;

	// if true, lots of debug messages may be produced for development and testing purposes. 
	private boolean DEBUG = true;


	/**
	 * Constructor.
	 */
	public VoyagerService() {
		// TODO Auto-generated constructor stub
	}


	/**
	 * Create method. Executed when the service is created but not yet started. 
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mgStats = new GeneralStats();
	}
	
	/**
	 * defines what action will be taken any time we receive an oob message. 
	 */
	EventCallback mOOBDataHandler = new EventCallback () {
		@Override
		public void onOOBDataArrived(String dataName, String dataValue) {
			msg ("OOB Data Received: " + dataName + "=" + dataValue);
			
			// Put state messages into the notification. 
			if (dataName.contains("state"))
				updateOBDNotification(dataValue);
		};
	};
	
	/**
	 * This method is executed once the service is "running". This is where magic happens. 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		setCurrentStateMessage("Adapter init...");
		mSessionAdapter = new AutoSessionAdapter(VoyagerService.this, BluetoothAdapter.getDefaultAdapter(), mOOBDataHandler);
		setCurrentStateMessage("Adapter is up");
		
		// just prints stats. 
		startDataCollectorLoop();
		
		// ask that the system re-deliver the start request with intent if we die.
		return START_REDELIVER_INTENT;
	}

	private void setCurrentStateMessage (String m) {
		mgStats.setStat("state", m);
		sendOOBMessage("autosessionadapter.state", m);
	}

	/**
	 * Sends a message through the OOB pipe. 
	 * @param dataName
	 * @param dataValue
	 */
	private void sendOOBMessage (String dataName, String dataValue) {
		if (mOOBDataHandler == null)
			return;
		mOOBDataHandler.onOOBDataArrived(dataName, dataValue);
	}

	/**
	 * Shutdown sequence. Close out any open connections. 
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdown();
	}

//	/** 
//     * libVoyager can do the BT discovery and device choosing for you. When it finds/chooses a device  it runs the device chosen callback.
//     * This method defines what to do when a new device is found.  
//     */
//    private EventCallback chosenCallback = new EventCallback () {
//
//    	@Override
//    	public void onELMDeviceChosen(String MAC) {
//    		mBTPeerMAC = MAC;
//    	}
//    	
//    };// end of eventcallback definition. 

//    /**
//     * Blocking method, sets up the session and returns only after it is ready or we gave up trying for another reason. 
//     * @param deviceMACAddress
//     * @return - true if setup was 100% successful. False otherwise. 
//     */
//    private synchronized boolean setupSession (String deviceMACAddress) {
//    	mBTPeerMAC = deviceMACAddress;
//    	
//    	mgStats.incrementStat ("sessionSetups");
//    	
//    	// Make sure we aren't threading out into more than one device. we can't presently handle multiple OBD devices at once. 
//    	if (hs != null) {
//    		msg ("Multiple OBD devices detected. throwing out " + deviceMACAddress);
//    		return false;
//    	}
//
//    	// instantiate dashDB if necessary.
//    	if (ddb == null) {
//    		msg  ("Spinning up DB...");
//    		ddb = new DashDB(this);
//    		msg  ("DB Ready.");
//    	}
//
//    	// mStatusBox.setStatusLevel("Connecting to " + deviceMACAddress, 2);
//    	hs = new HybridSession (BluetoothAdapter.getDefaultAdapter(), deviceMACAddress, ddb, ecbOOBMessageHandler);
//    	// register a method to be called when new data arrives. 
//    	hs.registerDPArrivedCallback(ecbDPNArrivedHandler);
//
//    	// detect hardware and network. Retry forever until we get it or are disconnected. 
//    	while (mThreadsOn == true && hs.getEBT().isConnected() == true && hs.runSessionDetection() != true) {
//    		mgStats.incrementStat("hsDetectTries");
//    		msg ("Running session detection. EBT.connected=" + hs.getEBT().isConnected() + " hsdetection.valid=" + hs.isDetectionValid());
//    	}
//
//    	// If the above while loop broke out because of a failure, then bust out of the session setup process. 
//    	if (hs.runSessionDetection() != true || hs.getEBT().isConnected() != true) 
//    		return false;
//    	
//    	// Hopefully it's ok if the loop was already running 'cause if setupSession gets called twice in the app's lifetime, it will get called twice. 
//    	startDataCollectorLoop();
//    	
//    	return true;
//    }

    private boolean startDataCollectorLoop () {
    	if (mtDataCollector != null) {
    		return false;
    	}
    	
    	// Define the thread. 
    	mtDataCollector = new Thread() {
    		public void run () {
    			boolean hardwareDetected = false;
    			while (mThreadsOn == true) {
    				mgStats.incrementStat("dataCollectorLoops");
    				// moved this to the top of the loop so that we can run a "continue" and not cause a tight loop. 
    				EasyTime.safeSleep(10000);
    				msg ("Loop Top.");
    				
    				if (mSessionAdapter != null) {
    					// svc.hs.rScan.loops=1465
    					updateOBDNotification("svc.hs.rScan.loops=" + getStats().getStat("svc.hs.rScan.loops"));
    					dumpStatsToScreen();
    				}
    				
    			}// end of main while loop. 
    			msg ("Data collector loop finished.");
    		}// end of run().
    	};// end of thread definition. 
    	
    	// kick off the thread.
    	mtDataCollector.start();
    	
    	return true;
    }

    /**
     * write allDPNsAsString to the screen. 
     */
	private void dumpAllDPNsToScreen() {
		try {
			msg(mSessionAdapter.getHybridSession().getPIDDecoder().getAllDataPointsAsString());
		} catch (Exception e) {
		}
	}

	private void dumpStatsToScreen  () {
		final String _stats = getStats().getAllStats();
		msg (_stats);
	}

	public GeneralStats getStats () {
		if (mSessionAdapter != null) mgStats.merge("svc", mSessionAdapter.getStats());
		
		return mgStats;
	}

	
//	/**
//	 * Do whatever necessary to detect hardware type and get it into sniff mode. 
//	 * We only try to detect the hardware once, if it fails, we return false. 
//	 * So if you get a false reading from this method, make another call. repeat as necessary.  
//	 */
//    private boolean detectSessionAndStartSniffing() {
//		msg ("Running session detection...");
//		boolean result = hs.runSessionDetection();
//		msg ("Session detection result was " + result);
//		if (result == true) {
//			// session detection was successful, move to next phase. 
//			msg ("Session detection succeeded, Switching to moni session... If possible. ");
//			if (hs.isHardwareSniffable()) {
//				msg ("Hardware IS sniffable, so switching to moni");
//				hs.setActiveSession(HybridSession.SESSION_TYPE_MONITOR);
//				msg ("After switch to moni.");
//			} else {
//				msg ("Hardware does not support sniff.");
//				return true;
//			}
//		} else {
//			// return value of the session deteciton was false so return false. 
//			return false;
//		}
//		
//		return true;
//    }

    
    // Defines the logic to take place when an out of band message is generated by the hybrid session layer. 
	EventCallback ecbOOBMessageHandler = new EventCallback () {
		@Override
		public void onOOBDataArrived(String dataName, String dataValue) {
			
			if (mThreadsOn != true) {
				msg ("Ignoring OOB message out of scope. Threads are off. " + dataName + "=" + dataValue);
				return;
			}
			
			msg ("OOB Data: " + dataName + "=" + dataValue);
			
			// state change?
			if (dataName.equals(HybridSession.OOBMessageTypes.IO_STATE_CHANGE)) {
				int newState = 0;
				try {
					newState = Integer.valueOf(dataValue);
					msg ("IO State changed to " + newState);
				} catch (NumberFormatException e) {
					msg ("ERROR: Could not interpret new state as string: " + dataValue);
				}
				
			}// end of "if this was a io state change". 
			
				
			}// end of session state change handler. 
		};// end of override.

    
	EventCallback ecbDPNArrivedHandler = new EventCallback () {
		@Override
		public void onDPArrived(String DPN, String sDecodedData, int iDecodedData) {
			msg ("DPN Arrived: " + DPN + "=" + sDecodedData);
		}// end of onDPArrived. 
	};// end of eventcallback def. 

    


	/**
	 * Required when defining a service. We won't be using IPC/binding at this point so it will go unused for now. 
	 */
	@Override
	public IBinder onBind(Intent intent) {
		msg ("Warning: onBind() executed but we don't support binding at this time.");
		return null;
	}
	
	private void msg (String m) {
		Log.d("VoyagerService",m);
	}

	
	
	/**
	 * Wrapper function that can be called from any thread, runs the updater in the main thread. 
	 * @param newText - the text we shall post to the notification. 
	 * @return - returns true on success, false on failure.
	 */
	private boolean updateOBDNotification (String newText) {
		
		final String txt = newText;

		//vdb.logDBMessage("updateOBDNotification(): New Notification Message: " + newText);
		
		mHandler.post(new Runnable () {
			public void run () {
				updateOBDNotification_ui(txt);
			} // end of run() definition. 
		}// end of handler post. 
		); // end of mhandler.post call. 
		
		return true;		
	}
	
	/**
	 * Provide a means for member functions to update the system notifcation text. 
	 * @param newText
	 * @return
	 */
	private boolean updateOBDNotification_ui (String newText) {

		// initialize stuff if necessary. 
		if (mNotificationManager == null) {
			String ns = Context.NOTIFICATION_SERVICE;
			mNotificationManager = (NotificationManager) getSystemService(ns);
		}
		
		if (mNotificationGeneral == null) {
			CharSequence tickerText = "Voyager Connect";
			
			long when = System.currentTimeMillis();
			
			mNotificationGeneral = new Notification (R.drawable.voyagercarlogo,tickerText, when);
			mNotificationGeneral.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		}
			
		Intent i = new Intent (this,ConnectUI.class);
		PendingIntent pi = PendingIntent.getActivity (this,0,i,0);
	
		mNotificationGeneral.setLatestEventInfo(getApplicationContext(),getString(R.string.app_name), newText, pi);		
		mNotificationManager.notify (VOYAGER_NOTIFY_ID_OBD,mNotificationGeneral);
		
		return true;
	}
	
	/**
	 * Put up a notification that there are DTC's available. 
	 * @return
	 */
	private boolean notifyCheckEngine (String infoText) {
		
		// initial config stuff. 
		if (mNotificationManager == null) {
			String ns = Context.NOTIFICATION_SERVICE;
			mNotificationManager = (NotificationManager) getSystemService(ns);
		}
		
		if (mNotificationCheckEngine == null) {
			CharSequence tickerText = "Check Engine (click for details)";
			
			long when = System.currentTimeMillis();
			mNotificationCheckEngine = new Notification (R.drawable.checkengine2,tickerText, when);
			mNotificationCheckEngine.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		}
		
//		Intent i = new Intent (this,TroubleActivity.class);
//		PendingIntent pi = PendingIntent.getActivity (this,0,i,0);
//		mNotificationCheckEngine.setLatestEventInfo(getApplicationContext(),"Check Engine", infoText, pi);
		mNotificationManager.notify (VOYAGER_NOTIFY_ID_CHECKENG,mNotificationCheckEngine);

		
		return true;
	}

		
	/**
	 * call this method to shut down. 
	 */
	private void shutdown () {
		mThreadsOn = false;
		// shut down the hybridsession chain.
//		if (hs != null) hs.shutdown();
		if (mSessionAdapter != null) mSessionAdapter.shutdown();
		// interrupt the data collection thread. 
		if (mtDataCollector != null) mtDataCollector.interrupt();
		stopSelf();
	}
	
}
