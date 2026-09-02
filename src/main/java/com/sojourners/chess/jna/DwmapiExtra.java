package com.sojourners.chess.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

/**
 * dwmapi.dll 扩展接口（JNA 自带库未包含）。
 * 用于获取窗口可见边框矩形（不含 Win10/11 隐形缩放边框），
 * 借鉴 VinXiangQi 的 DWMWA_EXTENDED_FRAME_BOUNDS 窗口适配。
 */
public interface DwmapiExtra extends StdCallLibrary {

    DwmapiExtra INSTANCE = Native.load("dwmapi", DwmapiExtra.class);

    /** 窗口可见扩展边框 */
    int DWMWA_EXTENDED_FRAME_BOUNDS = 9;

    /**
     * 获取窗口属性。
     *
     * @param hwnd          窗口句柄
     * @param dwAttribute   属性编号
     * @param pvAttribute   输出结构指针
     * @param cbAttribute   输出结构大小
     * @return 0 表示成功（S_OK）
     */
    int DwmGetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, WinDef.RECT pvAttribute, int cbAttribute);
}
