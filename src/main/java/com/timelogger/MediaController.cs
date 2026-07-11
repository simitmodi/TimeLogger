using System;
using System.Runtime.InteropServices;

public class MediaController {
    [DllImport("user32.dll")]
    private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo);

    public static void Main(string[] args) {
        if (args.Length == 0) return;
        byte vk = 0;
        if (args[0] == "play") vk = 0xB3;
        else if (args[0] == "next") vk = 0xB0;
        else if (args[0] == "prev") vk = 0xB1;
        
        if (vk != 0) {
            keybd_event(vk, 0, 0, 0); // press
            keybd_event(vk, 0, 2, 0); // release
        }
    }
}
