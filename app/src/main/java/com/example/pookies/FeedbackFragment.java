package com.example.pookies;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FeedbackFragment extends Fragment {
    private RadioGroup radioGroup;
    private EditText editTextFeedback;
    private TextView errorTextView;
    private Button submitButton;
    private DBHelper dbHelper;
    private String userId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(getContext());

        // Get userId from SharedPreferences
        userId = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("user_id", "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        // UI element binding
        radioGroup = view.findViewById(R.id.feedbackType);
        editTextFeedback = view.findViewById(R.id.feedbackText);
        errorTextView = view.findViewById(R.id.errorTextView);
        submitButton = view.findViewById(R.id.submitButton);

        // Set button listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFeedback();
            }
        });

        return view;
    }

    private void submitFeedback() {
        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();

        // Check if a feedback type is selected
        if (selectedRadioButtonId == -1) {
            errorTextView.setVisibility(View.VISIBLE);
            errorTextView.setText("Please select a feedback type.");
            return;
        }

        // Get feedback text and check if it's at least 5 words
        String feedbackDescription = editTextFeedback.getText().toString().trim();
        if (feedbackDescription.split("\\s+").length < 5) {
            errorTextView.setVisibility(View.VISIBLE);
            errorTextView.setText("Please put at least 5 words.");
            return;
        }

        // If validation passes, hide error messages
        errorTextView.setVisibility(View.GONE);

        RadioButton selectedRadioButton = radioGroup.findViewById(selectedRadioButtonId);
        String feedbackType = selectedRadioButton.getText().toString();

        // Get user details from SQLite database
        User user = dbHelper.getUserById(userId);
        if (user != null) {
            String username = user.getName() != null ? user.getName() : "Anonymous";
            String email = user.getEmail() != null ? user.getEmail() : "No Email";
            String currentTime = getCurrentTime();

            Feedback feedback = new Feedback(
                    userId,
                    username,
                    email,
                    feedbackType,
                    feedbackDescription,
                    currentTime
            );

            // Save feedback to SQLite
            if (saveFeedbackToSQLite(feedback)) {
                Toast.makeText(getContext(), "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                clearForm();
            } else {
                Toast.makeText(getContext(), "Error submitting feedback", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveFeedbackToSQLite(Feedback feedback) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.COLUMN_USER_ID, feedback.getUserId());          // Using constant
            values.put(DBHelper.COLUMN_NAME, feedback.getUsername());           // Using constant
            values.put(DBHelper.COLUMN_EMAIL, feedback.getEmail());            // Using constant
            values.put(DBHelper.COLUMN_FEEDBACK_TYPE, feedback.getFeedbackType());
            values.put(DBHelper.COLUMN_DESCRIPTION, feedback.getDescription());
            values.put(DBHelper.COLUMN_FEEDBACK_TIME, feedback.getFeedbackTime());

            long newRowId = db.insert(DBHelper.TABLE_FEEDBACK, null, values);
            return newRowId != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void clearForm() {
        radioGroup.clearCheck();
        editTextFeedback.setText("");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}