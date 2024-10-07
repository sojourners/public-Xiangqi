package com.sojourners.chess.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public interface User32Extra extends User32 {

    User32Extra INSTANCE = Native.load("user32", User32Extra.class);

    WinDef.BOOL SetSystemCursor(WinDef.HCURSOR hcur, WinDef.DWORD id);
    WinDef.BOOL SystemParametersInfoA(int uiAction, int uiParam, int pvParam, int fWinIni);
    WinDef.HCURSOR LoadCursorFromFileA(String lpFileName);

    int GetDpiForSystem();
    int GetDpiForWindow(WinDef.HWND hwnd);

    boolean GetCursorPos(long[] lpPoint); //use macros POINT_X() and POINT_Y() on long lpPoint[0]
    HWND WindowFromPoint(long point);
}
