package com.dm.smart;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Record;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterRecords extends RecyclerView.Adapter<RecyclerViewAdapterRecords.ViewHolder> {

    static final int RECORD_SHOW_IMAGE = Menu.FIRST + 2;
    static final int RECORD_SHARE = Menu.FIRST + 3;
    static final int RECORD_DELETE = Menu.FIRST + 4;
    private final List<Record> mRecords;
    private final LayoutInflater mInflater;
    private final Context mContext;
    public int selectedRecordPosition = 0;

    RecyclerViewAdapterRecords(Context context, List<Record> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mRecords = data;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // Bind the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Record record = mRecords.get(position);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(record.getTimestamp());
        SimpleDateFormat formatter =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateString = formatter.format(cal.getTime());
        holder.myTextView.setText(String.format("%s | Nr. %s", dateString, record.getN()));
    }

    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    public Record getItem(int position) {
        return mRecords.get(position);
    }

    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnCreateContextMenuListener {
        final TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.subject_name);
            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View view) {
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            selectedRecordPosition = getBindingAdapterPosition();
            int N = mRecords.get(selectedRecordPosition).getN();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mRecords.get(selectedRecordPosition).getTimestamp());
            contextMenu.setHeaderTitle(mContext.getString(R.string.menu_record) + ": " + N + ", "
                    + mContext.getString(R.string.menu_config) + ": " + mRecords.get(selectedRecordPosition).getConfig());
            contextMenu.add(0, RECORD_SHOW_IMAGE, Menu.NONE,
                    mContext.getString(R.string.menu_show_image));
            contextMenu.add(1, RECORD_SHARE, Menu.NONE,
                    mContext.getString(R.string.menu_share));
            contextMenu.add(2, RECORD_DELETE, Menu.NONE,
                    mContext.getString(R.string.menu_remove_record));
        }
    }
}