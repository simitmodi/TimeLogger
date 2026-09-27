using System;
using System.Drawing;
using System.Threading;
using System.Windows.Forms;

public class WindowsNotifier {
    [STAThread]
    public static void Main(string[] args) {
        string title = args.Length > 0 ? args[0] : "TimeLogger Alert";
        string msg = args.Length > 1 ? args[1] : "App closed.";
        string type = args.Length > 2 ? args[2].ToLower() : "info";

        using (NotifyIcon icon = new NotifyIcon()) {
            if (type == "warning" || type == "error") {
                icon.Icon = SystemIcons.Warning;
            } else {
                icon.Icon = SystemIcons.Information;
            }
            icon.Visible = true;

            ToolTipIcon tipIcon = ToolTipIcon.Info;
            if (type == "warning") tipIcon = ToolTipIcon.Warning;
            else if (type == "error") tipIcon = ToolTipIcon.Error;

            icon.ShowBalloonTip(4500, title, msg, tipIcon);
            Thread.Sleep(5000);
            icon.Visible = false;
        }
    }
}
