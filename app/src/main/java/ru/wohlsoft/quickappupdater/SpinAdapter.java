package ru.wohlsoft.quickappupdater;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;

public class SpinAdapter extends ArrayAdapter<DownloadAppEntry>
{
    private final List<DownloadAppEntry> values;

    public SpinAdapter(Context context, int textViewResourceId,
                       List<DownloadAppEntry> values)
    {
        super(context, textViewResourceId, values);
        this.values = values;
    }

    public void clear()
    {
        values.clear();
    }

    @Override
    public int getCount()
    {
        return values.size();
    }

    @Override
    public DownloadAppEntry getItem(int position)
    {
        return values.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(values.get(position).name);
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent)
    {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setText(values.get(position).name);
        return label;
    }
}
