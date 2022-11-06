package com.sojourners.chess.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;

public interface User32Extra extends User32 {

    User32Extra INSTANCE = Native.load("user32", User32Extra.class);

    BOOL SetSystemCursor(HCURSOR hcur, DWORD id);
    BOOL SystemParametersInfoA(int uiAction, int uiParam, int pvParam, int fWinIni);
    HCURSOR LoadCursorFromFileA(String lpFileName);
}
