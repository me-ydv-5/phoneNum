package com.example.sahil_1.phonenum;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    // m1 is the View that is being used for the template.
    EditText m1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m1 = findViewById(R.id.m1);

        // This is the main part of this class which manipulates the string.
        m1.addTextChangedListener(new PhoneNumberTextWatcher(m1));
    }

}

// Known bug: The cursor doesn't stay on the last mutable index but just goes to the
// zeroth index.