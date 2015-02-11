/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.crosswalk.engine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;

import org.apache.cordova.CordovaBridge;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkView;
/*
 * This class is our web view.
 *
 * @see <a href="http://developer.android.com/guide/webapps/webView.html">WebView guide</a>
 * @see <a href="http://developer.android.com/reference/android/webkit/WebView.html">WebView</a>
 */
public class XWalkWebViewEngine implements CordovaWebViewEngine {

    public static final String TAG = "XWalkWebViewEngine";

    protected XWalkCordovaView webView;
    protected XWalkCordovaCookieManager cookieManager;
    protected CordovaInterface cordova;
    protected CordovaBridge bridge;
    protected PluginManager pluginManager;
    protected CordovaResourceApi resourceApi;
    protected CordovaPreferences preferences;
    protected Client client;
    String loadedUrl;

    public XWalkWebViewEngine(Context context) {
        this(new XWalkCordovaView(context, null));
    }

    public XWalkWebViewEngine(XWalkCordovaView webView) {
        this.webView = webView;
        cookieManager = new XWalkCordovaCookieManager();
    }

    // Use two-phase init so that the control will work with XML layouts.

    @Override
    public void init(CordovaInterface cordova, Client client,
                     CordovaPreferences preferences, CordovaResourceApi resourceApi,
                     PluginManager pluginManager, NativeToJsMessageQueue nativeToJsMessageQueue) {
        if (this.cordova != null) {
            throw new IllegalStateException();
        }
        this.cordova = cordova;
        this.client = client;
        this.preferences = preferences;
        this.resourceApi = resourceApi;
        this.pluginManager = pluginManager;
        webView.init(this);

        initWebViewSettings();

        nativeToJsMessageQueue.addBridgeMode(new NativeToJsMessageQueue.OnlineEventsBridgeMode(new NativeToJsMessageQueue.OnlineEventsBridgeMode.OnlineEventsBridgeModeDelegate() {
            @Override
            public void setNetworkAvailable(boolean value) {
                webView.setNetworkAvailable(value);
            }
            @Override
            public void runOnUiThread(Runnable r) {
                XWalkWebViewEngine.this.cordova.getActivity().runOnUiThread(r);
            }
        }));
        bridge = new CordovaBridge(pluginManager, nativeToJsMessageQueue, this.cordova.getActivity().getPackageName());
        exposeJsInterface(webView, bridge);
    }

    private void initWebViewSettings() {
        webView.setVerticalScrollBarEnabled(false);
    }

    private static void exposeJsInterface(XWalkView webView, CordovaBridge bridge) {
        XWalkExposedJsApi exposedJsApi = new XWalkExposedJsApi(bridge);
        webView.addJavascriptInterface(exposedJsApi, "_cordovaNative");
    }

    // TODO(ningxin): XWalkViewUIClient should provide onScrollChanged callback
    /*
    public void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        //We should post a message that the scroll changed
        ScrollEvent myEvent = new ScrollEvent(l, t, oldl, oldt, this);
        this.postMessage("onScrollChanged", myEvent);
    }
    */

    @Override
    public boolean canGoBack() {
        return this.webView.getNavigationHistory().canGoBack();
    }

    @Override
    public boolean goBack() {
        if (this.webView.getNavigationHistory().canGoBack()) {
            this.webView.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            return true;
        }
        return false;
    }

    @Override
    public void setPaused(boolean value) {
        if (value) {
            // TODO: I think this has been fixed upstream and we don't need to override pauseTimers() anymore.
            webView.pauseTimersForReal();
        } else {
            webView.resumeTimers();
        }
    }

    @Override
    public void destroy() {
        webView.onDestroy();
    }

    @Override
    public void clearHistory() {
    	this.webView.getNavigationHistory().clear();
    }

	@Override
    public View getView() {
    	return this.webView;
    }

    @Override
    public void stopLoading() {
        this.webView.stopLoading();
    }

    @Override
    public void clearCache() {
        webView.clearCache(true);
    }

    @Override
    public String getUrl() {
        return this.webView.getUrl();
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        return cookieManager;
    }

    @Override
    public void loadUrl(String url, boolean clearNavigationStack) {
        webView.load(url, null);
    }
}
