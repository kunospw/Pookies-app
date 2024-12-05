package com.example.pookies;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FeedbackFragment extends Fragment {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RadioGroup radioGroup;
    private EditText editTextFeedback;
    private TextView errorTextView;
    private Button submitButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // UI element binding
        radioGroup = view.findViewById(R.id.feedbackType);
        editTextFeedback = view.findViewById(R.id.feedbackText);
        errorTextView = view.findViewById(R.id.errorTextView);
        submitButton = view.findViewById(R.id.submitButton);

        // Set button listener
        submitButton.setOnClickListener(v -> submitFeedback());

        return view;
    }

    private void submitFeedback() {
        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();

        // Validate feedback type selection
        if (selectedRadioButtonId == -1) {
            showError("Please select a feedback type.");
            return;
        }

        // Validate feedback description
        String feedbackDescription = editTextFeedback.getText().toString().trim();
        if (!isValidFeedback(feedbackDescription)) {
            return; // Error is already displayed in isValidFeedback
        }

        // Hide error messages on valid input
        errorTextView.setVisibility(View.GONE);

        // Get feedback type
        RadioButton selectedRadioButton = radioGroup.findViewById(selectedRadioButtonId);
        String feedbackType = selectedRadioButton.getText().toString();

        // Fetch user details from Firebase Authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String username = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "No Email";
            String currentTime = getCurrentTime();

            // Create feedback object
            Feedback feedback = new Feedback(userId, username, email, feedbackType, feedbackDescription, currentTime);

            // Save feedback to the user's node only
            mDatabase.child("feedback").child(userId).push().setValue(feedback)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showSuccess("Feedback submitted successfully!");
                            clearForm();
                        } else {
                            showError("Failed to submit feedback. Please try again.");
                        }
                    });
        } else {
            showError("You must be logged in to submit feedback.");
        }
    }

    private boolean isValidFeedback(String feedbackDescription) {
        if (feedbackDescription.isEmpty()) {
            showError("Feedback description cannot be empty.");
            return false;
        }
        if (feedbackDescription.split("\\s+").length < 5) {
            showError("Please put at least 5 words in the feedback.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    private void showSuccess(String message) {
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    private String getCurrentTime() {
        // Get the current time in a specific format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void clearForm() {
        // Clear the form fields after submission
        radioGroup.clearCheck();
        editTextFeedback.setText("");
    }
}
