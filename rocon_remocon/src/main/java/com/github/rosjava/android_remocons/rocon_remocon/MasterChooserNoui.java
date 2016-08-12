/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * Copyright (c) 2013, Yujin Robot.
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_remocons.rocon_remocon;

import android.app.AlertDialog;
import android.app.Service;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import android.util.Log;


import com.github.rosjava.android_remocons.common_tools.master.MasterId;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;
import com.github.rosjava.android_remocons.common_tools.zeroconf.MasterSearcherNoui;
import com.github.rosjava.android_remocons.common_tools.data.Data;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;


import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A rewrite of ye olde RobotMasterChooser to work with rocon masters (i.e.
 * those that have rocon master info and an interactions manager present).
 */
public class MasterChooserNoui extends Service {
	public static final String TAG = "MasterChooserNoui";
	private static final int ADD_URI_DIALOG_ID = 0;
	private static final int ADD_DELETION_DIALOG_ID = 1;
	private static final int ADD_SEARCH_CONCERT_DIALOG_ID = 2;

    private static final int QR_CODE_SCAN_REQUEST_CODE = 101;
    private static final int NFC_TAG_SCAN_REQUEST_CODE = 102;

	private List<RoconDescription> masters;
	private boolean[] selections;
	private MasterSearcherNoui masterSearcher;

    private Yaml yaml = new Yaml();
	private ArrayList<DiscoveredService> discoveredMasters;
    private Data data;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate() executed");
		masters = new ArrayList<RoconDescription>();
		data = new Data();
		masterSearcher = new MasterSearcherNoui(this, discoveredMasters, "concert-master", data);
		new Thread(new Runnable() {
			@Override
			public void run() {
				int index = 0;
				Log.d(TAG, "onCreate() Begin search.");
				data.Wait();
				Log.d(TAG, "onCreate() Finish search.");
				for (int i = 0; i < data.getDiscoveredServices().size(); i++) {
					enterMasterInfo((DiscoveredService) data.getDiscoveredServices().get(i));
					//FIXME!
					if (data.getDiscoveredServices().get(i).ipv4_addresses.get(0).equals("192.168.2.191") )
						index = i;
				}
				//FIXME! I just choosed the first item.
				choose(index);
			}
		}).start();
	}

	public MasterChooserNoui() {
		masters = new ArrayList<RoconDescription>();
	}

	// broadcast a custom intent.
	private void broadcastIntent(RoconDescription concert){
		Intent intent = new Intent();
		intent.putExtra(RoconDescription.UNIQUE_KEY, concert);
		intent.setAction("com.github.rosjava.android_remocons.rocon_remocon.MasterChooserNouiBroadcast");
		sendBroadcast(intent);
	}


    /**
     * Called when the user clicks on one of the listed masters in master chooser
     * view. Should probably check the connection status before
     * proceeding here, but perhaps we can just rely on the user clicking
     * refresh so this process stays without any lag delay.
     *
     * @param position
     */
	private void choose(int position) {
		RoconDescription concert = masters.get(position);
		if (concert == null || concert.getConnectionStatus() == null
				|| concert.getConnectionStatus().equals(RoconDescription.ERROR)) {
			Log.i("MasterChooser", "Error! Failed: Cannot contact concert");
        } else if ( concert.getConnectionStatus().equals(RoconDescription.UNAVAILABLE) ) {
			Log.i("MasterChooser", "Master Unavailable! Currently busy serving another.");
        } else {
            //Intent resultIntent = new Intent();
            //resultIntent.putExtra(RoconDescription.UNIQUE_KEY, concert);
			broadcastIntent(concert);
		}
	}

	private void addMaster(MasterId masterId) {
		addMaster(masterId, false);
	}

	private void addMaster(MasterId masterId, boolean connectToDuplicates) {
		Log.i("MasterChooserActivity", "adding master to the concert master chooser [" + masterId.toString() + "]");
		if (masterId == null || masterId.getMasterUri() == null) {
		} else {
			for (int i = 0; i < masters.toArray().length; i++) {
				RoconDescription concert = masters.get(i);
				if (concert.getMasterId().equals(masterId)) {
					if (connectToDuplicates) {
						choose(i);
						return;
					} else {
						Log.i("MasterChooserActivity", "That concert is already listed.");
						return;
					}
				}
			}
			Log.i("MasterChooserActivity", "creating concert description: "
					+ masterId.toString());
			masters.add(RoconDescription.createUnknown(masterId));
			Log.i("MasterChooserActivity", "description created");
			onMastersChanged();
		}
	}

	private void onMastersChanged() {
		updateList();
	}

	private void updateList() {
		new MasterAdapterNoui(this, masters);
	}

	public void enterMasterInfo(DiscoveredService discovered_service) {
        /*
          This could be better - it should actually contact and check off each
          resolvable zeroconf address looking for the master. Instead, we just grab
          the first ipv4 address and totally ignore the possibility of an ipv6 master.
         */
        String newMasterUri = null;
        if ( discovered_service.ipv4_addresses.size() != 0 ) {
            newMasterUri = "http://" + discovered_service.ipv4_addresses.get(0) + ":"
                    + discovered_service.port + "/";
        }
        if (newMasterUri != null && newMasterUri.length() > 0) {
            android.util.Log.i("Remocon", newMasterUri);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("URL", newMasterUri);
            try {
                addMaster(new MasterId(data));
            } catch (Exception e) {
				android.util.Log.i("Remocon", "Invalid Parameters.");
            }
        } else {
			android.util.Log.i("Remocon", "No valid resolvable master URI.");
        }
	}

}
