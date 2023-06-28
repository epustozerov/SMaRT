package com.dm.smart;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Subject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterSubjects extends RecyclerView.Adapter<RecyclerViewAdapterSubjects.ViewHolder> {

    static final int SUBJECT_DELETE = Menu.FIRST;
    private final List<Subject> mSubjects;
    private final LayoutInflater mInflater;
    private final Context mContext;
    public int selectedSubjectPosition = 0;
    private ItemClickListener mClickListener;

    private boolean show_names;

    private final String string_subject;

    // data is passed into the constructor
    RecyclerViewAdapterSubjects(Context context, List<Subject> subjects, boolean show_names) {
        this.mInflater = LayoutInflater.from(context);
        this.mSubjects = subjects;
        this.mContext = context;
        this.show_names = show_names;
        this.string_subject = context.getResources().getString(R.string.subject);
    }

    public void setShowNames(boolean show_names) {
        this.show_names = show_names;
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = mSubjects.get(position);
        if (show_names)
            holder.myTextView.setText(String.format("%s", subject.getName()));
        else
            holder.myTextView.setText(String.format(string_subject + " %s", subject.getId()));
        if (selectedSubjectPosition == position) {
            holder.myTextView.setTextColor(ContextCompat.getColor(mContext, R.color.gray_dark));
            holder.myTextView.setTypeface(null, Typeface.BOLD);
        } else {
            holder.myTextView.setTextColor(ContextCompat.getColor(mContext, R.color.gray_light));
            holder.myTextView.setTypeface(null, Typeface.NORMAL);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mSubjects.size();
    }

    // allows clicks events to be caught
    @SuppressWarnings("unused")
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public Subject getItem(int position) {
        return mSubjects.get(position);
    }


    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }

    // stores and recycles views as they are scrolled off screen
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
            if (mClickListener != null) {
                mClickListener.onItemClick(getBindingAdapterPosition());
                DBAdapter DBAdapter = new DBAdapter(mContext);
                DBAdapter.open();
                Cursor cursorSinglePatient =
                        DBAdapter.getSubjectById(mSubjects.get(getBindingAdapterPosition()).getId());
                cursorSinglePatient.moveToFirst();
                MainActivity.currentlySelectedSubject = SubjectFragment.extractSubjectFromTheDB(cursorSinglePatient);
                cursorSinglePatient.close();
                DBAdapter.close();
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            selectedSubjectPosition = getBindingAdapterPosition();
            String patient_name = mSubjects.get(selectedSubjectPosition).getName();
            int patient_gender = mSubjects.get(selectedSubjectPosition).getGender();
            String string_gender = mContext.getResources().getStringArray(R.array.genders)[patient_gender];
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mSubjects.get(selectedSubjectPosition).getTimestamp());
            SimpleDateFormat formatter =
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateString = formatter.format(cal.getTime());
            contextMenu.setHeaderTitle(patient_name + ", " + string_gender +
                    ",\n created: " + dateString);
            contextMenu.add(0, SUBJECT_DELETE, Menu.NONE,
                    mContext.getString(R.string.menu_remove_subject));
        }
    }
}