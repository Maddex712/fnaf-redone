package dev.thor.fnaf;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

/** Windows' default timer quantum can turn a 16 ms Swing timer into ~31 ms. */
final class DesktopTiming implements AutoCloseable {
    interface WinMM extends StdCallLibrary {int timeBeginPeriod(int ms);int timeEndPeriod(int ms);}
    private WinMM api;private boolean active;
    DesktopTiming(){try{api=Native.load("winmm",WinMM.class);}catch(UnsatisfiedLinkError ignored){}}
    synchronized void active(boolean value){
        if(api==null||value==active)return;
        if(value)active=api.timeBeginPeriod(1)==0;
        else{api.timeEndPeriod(1);active=false;}
    }
    public void close(){active(false);}
}
