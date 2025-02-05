package ru.wohlsoft.quickappupdater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{
    Button doDownloadButton;
    Button doRefreshList;
    Spinner appListComboBox;
    TextView appPathLabel;
    EditText urlRepoEdit;
    AppsListRefresh appsListRefreshing;
    SpinAdapter adapter;

    DownloadManager manager;
    private long downloadReference;
    private String downloadPath;
    private String downloadedAppName;
    private String urlDownloadSelected;
    private String urlRepositoryFile;
    private int urlSelectedItem = 0;

    public SharedPreferences m_setup = null;

    public String getRepoFileUrl()
    {
        return urlRepositoryFile;
    }

    public void setShownText(String s)
    {
        appPathLabel.setText(s);
    }

    public void clearAppsList()
    {
        if(adapter != null)
        {
            adapter.clear();
            appListComboBox.setAdapter(adapter);
        }
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (downloadReference == referenceId)
            {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(referenceId);
                Cursor c = manager.query(q);

                String apkName = downloadedAppName;
                if (c.moveToFirst())
                {
                    int status = c.getInt(Math.max(c.getColumnIndex(DownloadManager.COLUMN_STATUS), 0));
                    Log.i("BroadcastReceiver()", "Reason: " + c.getInt(Math.max(c.getColumnIndex(DownloadManager.COLUMN_REASON), 0)));
                    if (status == DownloadManager.STATUS_SUCCESSFUL)
                    {
                        // process download
                        apkName = c.getString(Math.max(c.getColumnIndex(DownloadManager.COLUMN_TITLE), 0));
                        // get other required data by changing the constant passed to getColumnIndex
                    }
                }

                File file = new File(downloadPath, apkName);
                if(file.exists())
                    startInstallerIntent(new File(downloadPath + File.separator + apkName), manager.getMimeTypeForDownloadedFile(referenceId));
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

    public void reloadAppsList()
    {
        String appsListFile = getFilesDir().getPath() + "/apps.json";
        File fl = new File(appsListFile);
        if(!fl.exists())
        {
            clearAppsList();
            appPathLabel.setText(getResources().getString(R.string.app_refresh_no_json));
            return; // File doesn't exists
        }

        FileInputStream fin = null;
        String content = null;

        try
        {
            fin = new FileInputStream(fl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            content = sb.toString();
            reader.close();
            //Make sure you close all streams.
            fin.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            clearAppsList();
            appPathLabel.setText("Got an exception: " + e.toString());
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            clearAppsList();
            appPathLabel.setText("Got an exception: " + e.toString());
            return;
        }

        try
        {
            List<DownloadAppEntry> spinnerArray = new ArrayList<>();

            JSONArray jArray = new JSONArray(content);
            for(int i = 0; i < jArray.length(); i++)
            {
                JSONObject jObject = jArray.getJSONObject(i);

                DownloadAppEntry a = new DownloadAppEntry();
                a.name = jObject.getString("name");
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    a.url = jObject.getString("url");
                else
                    a.url = jObject.getString("url-http");
                spinnerArray.add(a);
            } // End Loop

            adapter = new SpinAdapter(this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            appListComboBox.setAdapter(adapter);
            appListComboBox.setSelection(urlSelectedItem);

            if(!urlRepositoryFile.equals(m_setup.getString("repo-url", urlRepositoryFile)))
            {
                m_setup.edit()
                        .putString("repo-url", urlRepositoryFile)
                        .apply();
            }
        }
        catch (JSONException e)
        {
            Log.e("JSONException", "Error: " + e.toString());
            clearAppsList();
            appPathLabel.setText("Got an exception: " + e.toString());
        } // catch (JSONException e)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            urlRepositoryFile = "https://builds.wohlsoft.ru/android/repo.json";
        else
            urlRepositoryFile = "http://builds.wohlsoft.ru/android/repo.json";

        m_setup = getPreferences(Context.MODE_PRIVATE);
        urlSelectedItem = m_setup.getInt("selected-item", 0);
        urlDownloadSelected = m_setup.getString("selected-url", "");
        urlRepositoryFile = m_setup.getString("repo-url", urlRepositoryFile);

        File baseCache = getExternalCacheDir();

        downloadPath = baseCache.getPath();
        appPathLabel = findViewById(R.id.appPath);

        urlRepoEdit = findViewById(R.id.repoUrl);
        urlRepoEdit.setText(urlRepositoryFile);
        urlRepoEdit.addTextChangedListener(new TextWatcher()
        {
            // ...
            @Override
            public void onTextChanged(CharSequence text, int start, int count, int after)
            {
                urlRepositoryFile = text.toString();
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                // Nothing
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // Nothing
            }
        });

        appListComboBox = findViewById(R.id.appToUpdate);
        appListComboBox.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id)
            {
                DownloadAppEntry app = adapter.getItem(position);
                if(app == null)
                    return;

                urlDownloadSelected = app.url;
                urlSelectedItem = position;
                appPathLabel.setText(urlDownloadSelected);
                m_setup.edit()
                        .putInt("selected-item", urlSelectedItem)
                        .putString("selected-url", urlDownloadSelected)
                        .apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapter) {  }
        });

        doRefreshList = findViewById(R.id.refreshList);
        doRefreshList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(appsListRefreshing != null)
                    appsListRefreshing.cancel(true);
                urlDownloadSelected = "";
                appPathLabel.setText(getResources().getString(R.string.app_is_not_selected));
                appsListRefreshing = new AppsListRefresh(MainActivity.this);
                appsListRefreshing.execute();
            }
        });

        doDownloadButton = findViewById(R.id.doDownload);
        doDownloadButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(urlDownloadSelected == null || urlDownloadSelected.equals(""))
                    return;

                downloadedAppName = "app-update.apk";
                doDownloadApp(urlDownloadSelected, Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? "https" : "http");
            }
        });

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(downloadReceiver, filter);

        reloadAppsList();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // Cancel running task(s) to avoid memory leaks
        if(appsListRefreshing != null)
            appsListRefreshing.cancel(true);
    }

    @SuppressLint("SetTextI18n")
    private void doDownloadApp(String appUrl, String proto)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls())
        {
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
