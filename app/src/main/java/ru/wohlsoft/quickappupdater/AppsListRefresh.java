package ru.wohlsoft.quickappupdater;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;


public class AppsListRefresh extends AsyncTask<String, Void, Void>
{
    public AppsListRefresh(@NonNull MainActivity activity)
    {
        /* application context. */
        this.dialog = new ProgressDialog(activity);
        this.activity = new WeakReference<>(activity);
    }

    /**
     * progress dialog to show user that the backup is processing.
     */
    private final ProgressDialog dialog;
    private final WeakReference<MainActivity> activity;
    private Boolean errorOccurred = false;
    private String errorString = "";

    InputStream inputStream = null;
    String result = "";

    protected void onPreExecute()
    {
        this.errorOccurred = false;
        this.errorString = "";
        this.dialog.setMessage("Refreshing applications list...");
        this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            public void onCancel(DialogInterface arg0)
            {
                AppsListRefresh.this.cancel(true);
            }
        });
        this.dialog.show();
    }

    @Override
    protected Void doInBackground(String... params)
    {
        String url_select = activity.get().getRepoFileUrl();
        HttpURLConnection urlConnection;

        try
        {
            URL url = new URL(url_select);
            urlConnection = (HttpURLConnection) url.openConnection();
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
        }
        catch (UnsupportedEncodingException e1)
        {
            Log.e("UnsupportedEncoding", e1.toString());
            e1.printStackTrace();
            errorOccurred = true;
            errorString = "Got an exception: " + e1.toString();
            return null;
        }
        catch (IllegalStateException e3)
        {
            Log.e("IllegalStateException", e3.toString());
            e3.printStackTrace();
            errorOccurred = true;
            errorString = "Got an exception: " + e3.toString();
            return null;
        }
        catch (IOException e4)
        {
            Log.e("IOException", e4.toString());
            e4.printStackTrace();
            errorOccurred = true;
            errorString = "Got an exception: " + e4.toString();
            return null;
        }
        catch (IllegalArgumentException e5)
        {
            Log.e("IllegalArgument", e5.toString());
            e5.printStackTrace();
            errorOccurred = true;
            errorString = "Got an exception: " + e5.toString();
            return null;
        }

        // Convert response to string using String Builder
        try
        {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
            StringBuilder sBuilder = new StringBuilder();

            String line;
            while ((line = bReader.readLine()) != null)
                sBuilder.append(line).append("\n");

            inputStream.close();
            result = sBuilder.toString();
            urlConnection.disconnect();

            String downloadPath = activity.get().getFilesDir().getPath() + "/apps.json";
            try (PrintWriter out = new PrintWriter(downloadPath))
            {
                out.println(result);
            }
        }
        catch (Exception e)
        {
            Log.e("String/Buffered", "Error converting result " + e.toString());
            errorOccurred = true;
            errorString = "Got an exception: " + e.toString();
        }

        return null;
    }

    protected void onPostExecute(Void v)
    {
        if(errorOccurred)
        {
            activity.get().clearAppsList();
            activity.get().setShownText(errorString);
        }
        else
            activity.get().reloadAppsList();

        this.dialog.dismiss();
    }
}
