package com.example.pookies;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private EditText usernameEditText, emailEditText, passwordEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize the EditText fields
        usernameEditText = view.findViewById(R.id.username);
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);  // For showing password as asterisks

        // Fetch the current user from Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // If the user is logged in, set their email, username, and dummy password
        if (currentUser != null) {
            // Set the email
            emailEditText.setText(currentUser.getEmail());

            // Set the display name (username)
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                usernameEditText.setText(displayName);
            } else {
                // If display name is not set, show a default placeholder
                usernameEditText.setHint("No username set");
            }

            // Set a dummy password as asterisks (cannot retrieve the actual password)
            passwordEditText.setText("********"); // Show 8 asterisks as a placeholder for password
        }

        return view;
    }
}