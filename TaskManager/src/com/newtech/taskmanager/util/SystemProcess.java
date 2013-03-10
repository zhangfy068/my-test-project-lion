/**
 * CopyRight (C) 2013 NewTech CORP LTD.
 * @file SystemProcess.java
 */

package com.newtech.taskmanager.util;

import java.util.ArrayList;

public class SystemProcess {
    
    private static ArrayList<String> sSystemList;

    public static boolean isSystemList(String applicationName) {
        if (sSystemList == null) {
            initList();
        }
        if (sSystemList.contains(applicationName)) {
            return true;
        }
        if (applicationName.contains("home")
                || applicationName.contains("inputmethod")
                || applicationName.contains("keyboard")) {
            return true;
        }
        return false;
    }

    private static void initList() {
        sSystemList = new ArrayList<String>();
        sSystemList.add("com.android.phone");
        sSystemList.add("system");
        sSystemList.add("android.process.acore");
//        sSystemList.add("com.android.settings");
        sSystemList.add("com.android.systemui");
        sSystemList.add("android.process.media");
//        sSystemList.add("com.android.mms");
        sSystemList.add("com.android.voicedialer");
        sSystemList.add("com.android.nfc");
        sSystemList.add("com.android.nfc3");
//        sSystemList.add("com.android.alarmclock");
//        sSystemList.add("com.android.deskclock");
        sSystemList.add("com.google.android.deskclock");
        sSystemList.add("com.htc.android.worldclock");
        sSystemList.add("com.htc.widget.clockwidget");
//        sSystemList.add("com.android.htccontacts");
//        sSystemList.add("com.htc.messagecs");
        sSystemList.add("zte.com.cn.alarmclock");
        sSystemList.add("com.sonyericsson.eventstream.calllogplugin");
        sSystemList.add("com.sonyericsson.customization");
        sSystemList.add("com.sonyericsson.alarm");
        sSystemList.add("com.samsung.inputmethod");
        sSystemList.add("com.sec.android.app.controlpanel");
        sSystemList.add("com.motorola.numberlocation");
        sSystemList.add("com.sec.android.app.FileTransferManager");
        sSystemList.add("com.motorola.process.system");
//        sSystemList.add("com.motorola.blur.conversations");
        sSystemList.add("com.motorola.blur.alarmclock");
        sSystemList.add("com.motorola.blur.home.clock");
        sSystemList.add("com.motorola.widgetapp.worldclock");
//        sSystemList.add("com.motorola.contacts");
        sSystemList.add("com.motorola.blur.contacts.data");
//        sSystemList.add("com.motorola.blur.contacts");
        sSystemList.add("com.huawei.android.gpms");
        sSystemList.add("com.lge.simcontacts");
        sSystemList.add("com.android.SmsService");
        sSystemList.add("com.sonyericsson.secureclockservice");
        sSystemList.add("com.sonyericsson.widget.digitalclock");
        sSystemList.add("com.sonyericsson.digitalclockwidget");
        sSystemList.add("com.huawei.widget.localcityweatherclock");
        sSystemList.add("com.sec.android.widgetapp.stockclock");
        sSystemList.add("com.sec.android.widgetapp.weatherclock");
        sSystemList.add("com.sec.android.app.clockpackage");
        sSystemList.add("com.motorola.usb");
        sSystemList.add("com.motorola.blur.friendfeed");
        sSystemList.add("com.motorola.android.phoneportal.androidui");
        sSystemList.add("com.android.stk");
        sSystemList.add("com.svox.pico");
        sSystemList.add("android.tts");
        sSystemList.add("com.google.android.gsf");
        sSystemList.add("com.google.android.backup");
        sSystemList.add("com.android.keychain");
        sSystemList.add("com.google.android.inputmethod.latin.dictionarypack");
        sSystemList.add("com.android.providers.downloads");
        sSystemList.add("com.android.providers.drm");
        sSystemList.add("com.android.vending.updater");
//        sSystemList.add("com.android.mms");
        sSystemList.add("com.android.smspush");
        sSystemList.add("com.android.bluetooth");
        sSystemList.add("com.android.certinstaller");
        sSystemList.add("com.android.alarmclock");
        sSystemList.add("com.google.android.providers.subsriber");
        sSystemList.add("com.google.android.apps.uploader");
        sSystemList.add("com.google.android.systemupdater");
        sSystemList.add("com.android.providers.subscribedfeeds");
        sSystemList.add("com.google.android.syncadapters.contacts");
        sSystemList.add("com.google.android.location");
        sSystemList.add("com.google.android.apps.gtalkservice");
        sSystemList.add("com.youlu");
        sSystemList.add("com.htc.quicklaunchwidget");
        sSystemList.add("com.kunpeng.hipb");
//        sSystemList.add("com.jbapps.contactpro");
        sSystemList.add("com.towalds.android");
        sSystemList.add("com.baidu.input");
        sSystemList.add("com.cootek.smartinputv5");
        sSystemList.add("com.sohu.inputmethod.sogou");
        sSystemList.add("com.jb.goime");
        sSystemList.add("com.jb.gokeyboard");
        sSystemList.add("com.tencent.qqpinyin");
        sSystemList.add("com.google.android.inputmethod.pinyin");
        sSystemList.add("com.iflytek.inputmethod");
        sSystemList.add("com.bitfire.development.calendarsnooze");
        sSystemList.add("com.koushikdutta.klaxon");
        sSystemList.add("com.alarmclock.xtreme");
        sSystemList.add("com.zdworks.android.zdclock");
        sSystemList.add("org.woodroid.alarmbird");
        sSystemList.add("org.woodroid.alarmlady");
        sSystemList.add("com.splunchy.android.alarmclock");
        sSystemList.add("com.huawei.accountagent");
        sSystemList.add("com.motorola.blur.news");
        //sonyericsson's chinese input method
        sSystemList.add("com.sonyericsson.textinput.chinese");
        //CT ID Provider for Sony's phone
        sSystemList.add("com.sonymobile.providers.ctlandownerprovider");
        //HomeScreen for Sony Phone
        sSystemList.add("com.sonyericsson.home");
        //USB connection
        sSystemList.add("com.sonyericsson.usbux");
        //NFC for Sony Phone
        sSystemList.add("com.sonyericsson.nfc");
        //Heat Controller 
        sSystemList.add("com.sonyericsson.psm.sysmonservice");
        //Application package access helper
        sSystemList.add("com.android.defcontainer");
        //Wakeup for Sony Phone
        sSystemList.add("com.sonyericsson.android.wakeup");
        sSystemList.add("com.google.android.gsf");
        //DLAN for Sony Phone
        sSystemList.add("com.sonyericsson.dlna");
        //Google Service Framework
        sSystemList.add("com.google.process.gapps");
        //old home screen for sonyericsson phone
        sSystemList.add("com.sonyericsson.homescreen");
        //seems samsungs's phone homescreen
        sSystemList.add("com.sec.android.app.twlauncher");
        sSystemList.add("com.android.launcher");
        sSystemList.add("com.google.android.gsf.login");
        sSystemList.add("com.sonymobile.providers.ctlandownerprovider");
        sSystemList.add("com.sonyericsson.textinput.uxp");
    }
}
