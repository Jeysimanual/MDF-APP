package com.capstone.mdfeventmanagementsystem.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Models.Question;
import com.capstone.mdfeventmanagementsystem.Models.QuestionResponse;
import com.capstone.mdfeventmanagementsystem.R;

import java.util.List;

public class ResponseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LIKERT = 1;
    private static final int VIEW_TYPE_COMMENT = 2;
    private static final int VIEW_TYPE_UNSUPPORTED = 3;

    private final List<QuestionResponse> responseList;

    public ResponseAdapter(List<QuestionResponse> responseList) {
        this.responseList = responseList;
    }

    @Override
    public int getItemViewType(int position) {
        QuestionResponse response = responseList.get(position);
        Question question = response.getQuestion();

        if (question == null) {
            return VIEW_TYPE_UNSUPPORTED;
        }

        // Modified to use likert layout for both "likert" and "default" types
        switch (question.getType()) {
            case "likert":
            case "default":  // Added default case to use likert layout
                return VIEW_TYPE_LIKERT;
            case "comment":
                return VIEW_TYPE_COMMENT;
            default:
                return VIEW_TYPE_LIKERT;  // Changed to use likert layout as default
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_LIKERT:
                // Use a view layout specifically for displaying likert responses
                View likertView = inflater.inflate(R.layout.item_likert_response, parent, false);
                return new LikertViewHolder(likertView);
            case VIEW_TYPE_COMMENT:
                // Use a view layout specifically for displaying comment responses
                View commentView = inflater.inflate(R.layout.item_comment_response, parent, false);
                return new CommentViewHolder(commentView);
            default:
                // Changed to use likert layout as the default fallback
                View defaultView = inflater.inflate(R.layout.item_likert_response, parent, false);
                return new LikertViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        QuestionResponse response = responseList.get(position);
        Question question = response.getQuestion();

        if (question == null) {
            return;
        }

        // Add required indicator if needed
        String questionText = question.getText();
        if (question.isRequired()) {
            questionText = questionText + " *";
        }

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_LIKERT:
                LikertViewHolder likertHolder = (LikertViewHolder) holder;
                likertHolder.questionText.setText(questionText);

                try {
                    // Parse the answer as an integer from 1-5
                    int rating = Integer.parseInt(response.getAnswer());

                    // Set the appropriate radio button based on the rating
                    switch (rating) {
                        case 1:
                            likertHolder.radio1.setChecked(true);
                            break;
                        case 2:
                            likertHolder.radio2.setChecked(true);
                            break;
                        case 3:
                            likertHolder.radio3.setChecked(true);
                            break;
                        case 4:
                            likertHolder.radio4.setChecked(true);
                            break;
                        case 5:
                            likertHolder.radio5.setChecked(true);
                            break;
                        default:
                            // Clear selection if invalid rating
                            likertHolder.radioGroup.clearCheck();
                            break;
                    }
                } catch (NumberFormatException e) {
                    // Clear selection if parsing failed
                    likertHolder.radioGroup.clearCheck();
                }

                // Ensure radio buttons are not clickable in display mode
                likertHolder.radio1.setEnabled(false);
                likertHolder.radio2.setEnabled(false);
                likertHolder.radio3.setEnabled(false);
                likertHolder.radio4.setEnabled(false);
                likertHolder.radio5.setEnabled(false);
                break;

            case VIEW_TYPE_COMMENT:
                CommentViewHolder commentHolder = (CommentViewHolder) holder;
                commentHolder.questionText.setText(questionText);
                commentHolder.answerText.setText(response.getAnswer());
                break;

            default:
                // Default now handles as likert type
                if (holder instanceof LikertViewHolder) {
                    LikertViewHolder defaultHolder = (LikertViewHolder) holder;
                    defaultHolder.questionText.setText(questionText);

                    try {
                        int rating = Integer.parseInt(response.getAnswer());
                        switch (rating) {
                            case 1:
                                defaultHolder.radio1.setChecked(true);
                                break;
                            case 2:
                                defaultHolder.radio2.setChecked(true);
                                break;
                            case 3:
                                defaultHolder.radio3.setChecked(true);
                                break;
                            case 4:
                                defaultHolder.radio4.setChecked(true);
                                break;
                            case 5:
                                defaultHolder.radio5.setChecked(true);
                                break;
                            default:
                                defaultHolder.radioGroup.clearCheck();
                                break;
                        }
                    } catch (NumberFormatException e) {
                        defaultHolder.radioGroup.clearCheck();
                    }

                    defaultHolder.radio1.setEnabled(false);
                    defaultHolder.radio2.setEnabled(false);
                    defaultHolder.radio3.setEnabled(false);
                    defaultHolder.radio4.setEnabled(false);
                    defaultHolder.radio5.setEnabled(false);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return responseList.size();
    }

    // ViewHolder for Likert-scale questions with RadioGroup
    static class LikertViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        RadioGroup radioGroup;
        RadioButton radio1, radio2, radio3, radio4, radio5;

        LikertViewHolder(View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.questionText);
            radioGroup = itemView.findViewById(R.id.radio_group_response);
            radio1 = itemView.findViewById(R.id.radio_1);
            radio2 = itemView.findViewById(R.id.radio_2);
            radio3 = itemView.findViewById(R.id.radio_3);
            radio4 = itemView.findViewById(R.id.radio_4);
            radio5 = itemView.findViewById(R.id.radio_5);
        }
    }

    // ViewHolder for comment questions
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        TextView answerText;

        CommentViewHolder(View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.questionText);
            answerText = itemView.findViewById(R.id.answerText);
        }
    }
}