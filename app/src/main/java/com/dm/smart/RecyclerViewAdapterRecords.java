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

    static final int RECORD_SHOW_IMAGE = Menu.FIRST + 1;
    static final int RECORD_SHOW_FOLDER = Menu.FIRST + 2;
    static final int RECORD_DELETE = Menu.FIRST + 3;
    private final List<Record> mRecords;
    private final LayoutInflater mInflater;
    private final Context mContext;
    public int selectedRecordPosition = 0;
    private ItemClickListener mClickListener;


    // data is passed into the constructor
    RecyclerViewAdapterRecords(Context context, List<Record> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mRecords = data;
        this.mContext = context;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Record record = mRecords.get(position);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(record.getTimestamp());
        SimpleDateFormat formatter =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateString = formatter.format(cal.getTime());
        holder.myTextView.setText(String.format("%s %s", dateString, record.getId()));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    // allows clicks events to be caught
    @SuppressWarnings("unused")
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public Record getItem(int position) {
        return mRecords.get(position);
    }


    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnCreateContextMenuListener {
        final TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.patient_name);
            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnCreateContextMenuListener(this);
        }


        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            selectedRecordPosition = getBindingAdapterPosition();
            int record_id = mRecords.get(selectedRecordPosition).getId();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mRecords.get(selectedRecordPosition).getTimestamp());
            SimpleDateFormat formatter =
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateString = formatter.format(cal.getTime());
            contextMenu.setHeaderTitle("Record: " + record_id + ",\n created: " + dateString);
            contextMenu.add(0, RECORD_SHOW_IMAGE, Menu.NONE,
                    mContext.getString(R.string.menu_show_image));
            contextMenu.add(1, RECORD_SHOW_FOLDER, Menu.NONE,
                    mContext.getString(R.string.menu_show_record_folder));
            contextMenu.add(2, RECORD_DELETE, Menu.NONE,
                    mContext.getString(R.string.menu_remove_record));
        }
    }
}