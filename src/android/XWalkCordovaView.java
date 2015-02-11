package org.crosswalk.engine;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class XWalkCordovaView extends XWalkView {
    protected XWalkCordovaResourceClient resourceClient;
    protected XWalkCordovaUiClient uiClient;
    protected XWalkWebViewEngine parentEngine;

    private static boolean hasSetStaticPref;
    // This needs to run before the super's constructor.
    private static Context setGlobalPrefs(Context context) {
        if (!hasSetStaticPref) {
            hasSetStaticPref = true;
            ApplicationInfo ai = null;
            try {
                ai = context.getPackageManager().getApplicationInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (ai.metaData.getBoolean("CrosswalkAnimatable")) {
                // Slows it down a bit, but allows for it to be animated by Android View properties.
                XWalkPreferences.setValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW, true);
            }
            if ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
            }
            XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);
            XWalkPreferences.setValue(XWalkPreferences.ALLOW_UNIVERSAL_ACCESS_FROM_FILE, true);
        }
        return context;
    }

    public XWalkCordovaView(Context context, AttributeSet attrs) {
        super(setGlobalPrefs(context), attrs);
    }

    void init(XWalkWebViewEngine parentEngine) {
        this.parentEngine = parentEngine;
        if (resourceClient == null) {
            setResourceClient(new XWalkCordovaResourceClient(parentEngine));
        }
        if (uiClient == null) {
            setUIClient(new XWalkCordovaUiClient(parentEngine));
        }
    }

    @Override
    public void setResourceClient(XWalkResourceClient client) {
        // XWalk calls this method from its constructor.
        if (client instanceof XWalkCordovaResourceClient) {
            this.resourceClient = (XWalkCordovaResourceClient)client;
        }
        super.setResourceClient(client);
    }

    @Override
    public void setUIClient(XWalkUIClient client) {
        // XWalk calls this method from its constructor.
        if (client instanceof XWalkCordovaUiClient) {
            this.uiClient = (XWalkCordovaUiClient)client;
        }
        super.setUIClient(client);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Boolean ret = parentEngine.client.onKeyDown(keyCode, event);
            if (ret != null) {
                return ret.booleanValue();
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            Boolean ret = parentEngine.client.onKeyUp(keyCode, event);
            if (ret != null) {
                return ret.booleanValue();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void pauseTimers() {
        // This is called by XWalkViewInternal.onActivityStateChange().
        // We don't want them paused by default though.
    }

    public void pauseTimersForReal() {
        super.pauseTimers();
    }
}
