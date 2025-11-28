package com.capstone.mdfeventmanagementsystem.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.R;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
    private final List<StudentItem> studentItems;
    private final OnStudentClickListener onStudentClickListener;

    public interface OnStudentClickListener {
        void onStudentClick(String studentId, String studentName);
    }

    public StudentAdapter(OnStudentClickListener listener) {
        this.studentItems = new ArrayList<>();
        this.onStudentClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentItem item = studentItems.get(position);
        if (item.isHeader()) {
            holder.gradeHeader.setText(item.getDisplayText());
            holder.gradeHeader.setVisibility(View.VISIBLE);
            holder.sectionHeader.setText(item.getSection());
            holder.sectionHeader.setVisibility(View.VISIBLE);
            holder.studentItemLayout.setVisibility(View.GONE);
        } else {
            holder.gradeHeader.setVisibility(View.GONE);
            holder.sectionHeader.setVisibility(View.GONE);
            holder.studentItemLayout.setVisibility(View.VISIBLE);
            holder.studentNameTextView.setText(item.getDisplayName());
            holder.addButton.setText("Add");
            holder.addButton.setOnClickListener(v -> {
                if (onStudentClickListener != null && item.getStudentId() != null) {
                    onStudentClickListener.onStudentClick(item.getStudentId(), item.getDisplayName());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return studentItems.size();
    }

    public void setData(List<StudentItem> newStudents) {
        this.studentItems.clear();
        this.studentItems.addAll(newStudents);
        notifyDataSetChanged();
    }

    public void clear() {
        this.studentItems.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView gradeHeader;
        TextView sectionHeader;
        LinearLayout studentItemLayout;
        TextView studentNameTextView;
        Button addButton;

        ViewHolder(View itemView) {
            super(itemView);
            gradeHeader = itemView.findViewById(R.id.gradeHeader);
            sectionHeader = itemView.findViewById(R.id.sectionHeader);
            studentItemLayout = itemView.findViewById(R.id.studentItemLayout);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }

    public static class StudentItem {
        private final String studentId; // Firebase key for the student
        private final String displayName; // Student name only (lastName, firstName, middle initial)
        private final String displayText; // For headers: "Grade X"
        private final String yearLevel; // Internal use for grouping/sorting
        private final String section; // For headers: section name
        private final boolean isHeader; // True for grade headers, false for students

        // Constructor for grade header
        public StudentItem(String gradeLevel, String section) {
            this.studentId = null;
            this.displayName = null;
            this.displayText = "Grade " + gradeLevel;
            this.yearLevel = gradeLevel;
            this.section = section;
            this.isHeader = true;
        }

        // Constructor for student item
        public StudentItem(String studentId, String studentName, String yearLevel) {
            this.studentId = studentId;
            this.displayName = studentName;
            this.displayText = null;
            this.yearLevel = yearLevel;
            this.section = null;
            this.isHeader = false;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getYearLevel() {
            return yearLevel;
        }

        public String getSection() {
            return section;
        }

        public boolean isHeader() {
            return isHeader;
        }
    }
}
