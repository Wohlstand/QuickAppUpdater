package ru.wohlsoft.quickappupdater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;


public class MainActivity extends AppCompatActivity
{
    Button doDownloadButton;
    Button doDownloadButtonHttp;
    TextView appPathLabel;

    DownloadManager manager;
    private long downloadReference;
    private String downloadPath;
    private String downloadedAppName;

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (downloadReference == referenceId) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(referenceId);
                Cursor c = manager.query(q);

                String apkName = downloadedAppName;
                if (c.moveToFirst()) {
                    int status = c.getInt(Math.max(c.getColumnIndex(DownloadManager.COLUMN_STATUS), 0));
                    Log.i("BroadcastReceiver()", "Reason: " + c.getInt(Math.max(c.getColumnIndex(DownloadManager.COLUMN_REASON), 0)));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // process download
                        apkName = c.getString(Math.max(c.getColumnIndex(DownloadManager.COLUMN_TITLE), 0));
                        // get other required data by changing the constant passed to getColumnIndex
                    }
                }

                File file = new File(downloadPath, apkName);
                if (file.exists()) {
                    startInstallerIntent(new File(downloadPath + File.separator + apkName), manager.getMimeTypeForDownloadedFile(referenceId));
                }
            }
        }
    };

    Uri uriFromFile(Context context, File file)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void startInstallerIntent(File file, String type)
    {
        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
        promptInstall.setDataAndType(uriFromFile(this, file), type);
        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        promptInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(promptInstall);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        downloadPath = getExternalCacheDir().getPath();
        appPathLabel = findViewById(R.id.appPath);

        doDownloadButton = findViewById(R.id.doDownload);
        doDownloadButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                downloadedAppName = "thextech-update-master.apk";
                doDownloadApp("https://builds.wohlsoft.ru/android/thextech-android-master.apk", "https");
            }
        });

        doDownloadButtonHttp = findViewById(R.id.doDownloadHttp);
        doDownloadButtonHttp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                downloadedAppName = "thextech-update-master.apk";
                doDownloadApp("http://builds.wohlsoft.ru/android/thextech-android-master.apk", "http");
            }
        });

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
    }

    private void doDownloadApp(String appUrl, String proto)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            String pName = getPackageName();
            Uri uri = Uri.fromParts("package", pName, null);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        String dstFilePath = downloadPath + File.separator + downloadedAppName;

        File dstFile = new File(dstFilePath);
        if (dstFile.exists())
            dstFile.delete();

        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(appUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationUri(Uri.fromFile(dstFile));
        request.setTitle(downloadedAppName);
        downloadReference = manager.enqueue(request);

        appPathLabel.setText(dstFilePath + " (" + proto + ")");
    }
}
