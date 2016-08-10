/*
 * Copyright (C) 2013 Yujin Robot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.android_remocons.common_tools.zeroconf;

import android.content.Context;

import com.github.rosjava.android_remocons.common_tools.R;
import com.github.rosjava.android_remocons.common_tools.data.Data;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;

import java.util.ArrayList;


public class DiscoveryAdapterNoui extends ArrayList<DiscoveredService> {


    private final Context context;
    private ArrayList<DiscoveredService> discoveredServices;
    private String targetServiceName;
    private int targetServiceDrawable;
    private int otherServicesDrawable;
    private Data data;

    public DiscoveryAdapterNoui(Context context, ArrayList<DiscoveredService> discoveredServices,
                            String targetServiceName, Data data) {
        this.context = context;
        this.data = data;

        this.discoveredServices = discoveredServices;  // keep a pointer locally so we can play with it
        this.targetServiceName     = targetServiceName;
     }

    public void notifyDataSetAdded()
    {
        android.util.Log.i("DiscoveryAdapterNoui", "notifyDataSetAdded() called");
        //DiscoveredService discovered_service = discoveredServices.get(0);

        data.setDiscoveredServices(discoveredServices);
        data.Notify();
        return;
    }

    public void notifyDataSetRemoved()
    {
        android.util.Log.i("DiscoveryAdapterNoui", "notifyDataSetRemoved() called");
        return;
    }
}
