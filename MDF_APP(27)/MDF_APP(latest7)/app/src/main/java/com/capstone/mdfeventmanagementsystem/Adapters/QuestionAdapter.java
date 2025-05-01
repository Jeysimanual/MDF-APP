package com.capstone.mdfeventmanagementsystem.Adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Models.Question;
import com.capstone.mdfeventmanagementsystem.Models.QuestionResponse;

import java.util.ArrayList;
import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "QuestionAdapter";
    private static final String RESPONSE_TAG = "responseTest";
    private static final int VIEW_TYPE_LIKERT = 0;
    private static final int VIEW_TYPE_COMMENT = 1;

    private List<Question> questionList;
    private List<QuestionResponse> responseList = new ArrayList<>();

    public QuestionAdapter(List<Question> questionList) {
        this.questionList = questionList;
        Log.d(RESPONSE_TAG, "QuestionAdapter created with " + questionList.size() + " questions");

        // Initialize response list with empty responses
        responseList.clear(); // Ensure we start with an empty list
        for (Question question : questionList) {
            responseList.add(new QuestionResponse(question.getId(), ""));
            Log.d(RESPONSE_TAG, "Added empty response for question: " + question.getId());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LIKERT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_likert_question, parent, false);
            Log.d(RESPONSE_TAG, "Created LikertViewHolder");
            return new LikertViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment_question, parent, false);
            Log.d(RESPONSE_TAG, "Created CommentViewHolder");
            return new CommentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            // Use final int pos to avoid issues with lambdas
            final int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                Log.w(RESPONSE_TAG, "No adapter position for holder at position " + position);
                return;
            }

            Question question = questionList.get(pos);
            String questionText = question.getText();

            if (question.isRequired()) {
                questionText += " *";
            }

            Log.d(RESPONSE_TAG, "Binding question at position " + pos +
                    ": id=" + question.getId() +
                    ", type=" + question.getType() +
                    ", required=" + question.isRequired());

            // Ensure responseList has an entry for this position
            while (responseList.size() <= pos) {
                responseList.add(new QuestionResponse(question.getId(), ""));
                Log.d(RESPONSE_TAG, "Added missing response at position " + (responseList.size() - 1));
            }

            if (holder instanceof LikertViewHolder) {
                LikertViewHolder likertHolder = (LikertViewHolder) holder;
                likertHolder.questionText.setText(questionText);

                // Important: Remove listener before setting value to avoid triggering it during binding
                likertHolder.radioGroup.setOnCheckedChangeListener(null);

                // Set the current value if there is one
                String answer = responseList.get(pos).getAnswer();
                Log.d(RESPONSE_TAG, "Binding Likert question at pos " + pos + " with answer: " + answer);

                switch (answer) {
                    case "1":
                        likertHolder.radioGroup.check(R.id.radio_1);
                        break;
                    case "2":
                        likertHolder.radioGroup.check(R.id.radio_2);
                        break;
                    case "3":
                        likertHolder.radioGroup.check(R.id.radio_3);
                        break;
                    case "4":
                        likertHolder.radioGroup.check(R.id.radio_4);
                        break;
                    case "5":
                        likertHolder.radioGroup.check(R.id.radio_5);
                        break;
                    default:
                        likertHolder.radioGroup.clearCheck();
                        break;
                }

                // Now add the listener back
                likertHolder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    String value = "";
                    if (checkedId == R.id.radio_1) value = "1";
                    else if (checkedId == R.id.radio_2) value = "2";
                    else if (checkedId == R.id.radio_3) value = "3";
                    else if (checkedId == R.id.radio_4) value = "4";
                    else if (checkedId == R.id.radio_5) value = "5";

                    // Update response in the list
                    if (pos < responseList.size()) {
                        Log.d(RESPONSE_TAG, "Selected Likert value for pos " + pos + ", question " +
                                question.getId() + ": " + value);
                        responseList.set(pos, new QuestionResponse(question.getId(), value));

                        // Debug log after updating
                        logAllResponses("After Likert update");
                    } else {
                        Log.e(RESPONSE_TAG, "Position out of bounds: " + pos + ", responseList size: " + responseList.size());
                    }
                });

            } else if (holder instanceof CommentViewHolder) {
                CommentViewHolder commentHolder = (CommentViewHolder) holder;
                commentHolder.questionText.setText(questionText);

                // Remove existing text watcher if any
                if (commentHolder.textWatcher != null) {
                    commentHolder.commentEditText.removeTextChangedListener(commentHolder.textWatcher);
                }

                // Set the current value if there is one
                String answer = responseList.get(pos).getAnswer();
                commentHolder.commentEditText.setText(answer);
                Log.d(RESPONSE_TAG, "Binding Comment question at pos " + pos + " with text: " + answer);

                // Create and set a new text watcher
                commentHolder.textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // Not needed
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Not needed
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (pos < responseList.size()) {
                            String text = s.toString().trim();
                            Log.d(RESPONSE_TAG, "Saving comment response at pos " + pos +
                                    ", question " + question.getId() + ": " + text);
                            responseList.set(pos, new QuestionResponse(question.getId(), text));

                            // Debug log after updating
                            logAllResponses("After Comment update");
                        }
                    }
                };

                commentHolder.commentEditText.addTextChangedListener(commentHolder.textWatcher);
            }
        } catch (Exception e) {
            Log.e(RESPONSE_TAG, "Error in onBindViewHolder at position " + position, e);
        }
    }

    private void logAllResponses(String context) {
        Log.d(RESPONSE_TAG, "=== CURRENT RESPONSES (" + context + ") ===");
        for (int i = 0; i < responseList.size(); i++) {
            QuestionResponse response = responseList.get(i);
            if (response != null) {
                Log.d(RESPONSE_TAG, "  Response " + i + ": id=" + response.getQuestionId() +
                        ", answer='" + response.getAnswer() + "'");
            } else {
                Log.d(RESPONSE_TAG, "  Response " + i + ": NULL");
            }
        }
        Log.d(RESPONSE_TAG, "==============================");
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    @Override
    public int getItemViewType(int position) {
        String type = questionList.get(position).getType();
        Log.d(RESPONSE_TAG, "Getting view type for position " + position + ": " + type);
        return ("default".equals(type) || "likert".equals(type)) ? VIEW_TYPE_LIKERT : VIEW_TYPE_COMMENT;
    }

    // Modified getResponses method in QuestionAdapter.java
    public List<QuestionResponse> getResponses() {
        // Log each response to verify data
        Log.d(RESPONSE_TAG, "=== GETTING RESPONSES FOR SUBMISSION ===");
        Log.d(RESPONSE_TAG, "Total responses in list: " + responseList.size());

        // Clean up responses and log all responses
        List<QuestionResponse> validResponses = new ArrayList<>();
        for (int i = 0; i < responseList.size(); i++) {
            QuestionResponse response = responseList.get(i);
            if (response != null && response.getQuestionId() != null) {
                // Keep the response as is - we'll handle N/A conversion in the submission process
                Log.d(RESPONSE_TAG, "Valid response " + i + ": id=" + response.getQuestionId() +
                        ", answer='" + response.getAnswer() + "'");
                validResponses.add(response);
            } else {
                Log.w(RESPONSE_TAG, "Invalid response at position " + i +
                        (response == null ? ": null" : ": id=" + response.getQuestionId()));
            }
        }

        Log.d(RESPONSE_TAG, "Returning " + validResponses.size() + " valid responses");

        // Double check that we have responses for each question
        for (Question question : questionList) {
            boolean found = false;
            for (QuestionResponse response : validResponses) {
                if (question.getId().equals(response.getQuestionId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.w(RESPONSE_TAG, "WARNING: No response found for question: " + question.getId() +
                        ", adding empty response");
                // Add an empty response to ensure all questions are represented
                validResponses.add(new QuestionResponse(question.getId(), ""));
            }
        }

        return validResponses;
    }

    static class LikertViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        RadioGroup radioGroup;
        RadioButton radio1, radio2, radio3, radio4, radio5;

        LikertViewHolder(View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.question_text);
            radioGroup = itemView.findViewById(R.id.radio_group);
            radio1 = itemView.findViewById(R.id.radio_1);
            radio2 = itemView.findViewById(R.id.radio_2);
            radio3 = itemView.findViewById(R.id.radio_3);
            radio4 = itemView.findViewById(R.id.radio_4);
            radio5 = itemView.findViewById(R.id.radio_5);
        }
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        EditText commentEditText;
        TextWatcher textWatcher;  // Store the TextWatcher to remove it when recycling

        CommentViewHolder(View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.question_text);
            commentEditText = itemView.findViewById(R.id.comment_edit_text);
        }
    }
}