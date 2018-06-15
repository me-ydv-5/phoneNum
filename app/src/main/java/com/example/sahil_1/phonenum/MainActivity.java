package com.example.sahil_1.phonenum;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.EditText;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    //TODO: Create a separate TextWatcher class called PhoneNumberTextWatcher.
    //TODO: The paramters to this textwatcher class would be the edit text
    //TODO: I'd like to use it as m1.addTextChangeListener(new PhoneNumberTextWatcher(m1))
    //TODO: Figureout how to keep the inter-character spacing as constant (this could be done from the layout file itself)

    // m1 is the View that is being used for the template.
    EditText m1;

    // Last pointer corresponds to the pointer location just before
    // some text was changed. ArrayIdx is the pointer to the elements
    // in the idxArray which is the array for keeping the mutable indexes
    // in the template, i.e, the fields where user can WRITE.
    Integer last_pointer = 0, arrayIdx = 0;

    // Span for accumulating the former string or the string that has
    // just been changed. This helps to restore the earlier string in
    // case user tries to misalign the template or types where he/she
    // should not.
    RelativeSizeSpan span;
    SpannableString spannable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m1 = findViewById(R.id.m1);

        // The following is the Canadian format for the phone numbers.
        // countryCode-(threeDigits)-threeDigits-fourDigits
        m1.setText(getString(R.string.phone_format));

        // This is length() + 1 because we are keeping one index as buffer
        // to accomodate any new character that is entered. Later we again
        // bring the string to length() by deleting one character from the
        // string.
        final Integer lengthOfString = m1.getText().length() + 1;
        Log.d("lengthOfString",Integer.toString(lengthOfString));
        m1.setFilters(new InputFilter[] { new InputFilter.LengthFilter(lengthOfString)} );

        // idxArray - the array that is used to store the mutable indexes.
        // These are the indexes of the actual digits of the phone number
        // except the country code/
        final int[] idxArray = new int[]{ 4,5,6,9,10,11,13,14,15,16 };//TODO: this should go inside the textwatcher class

        // Set the initial pointer to the first digit in the phone number.
        Selection.setSelection(m1.getText(), idxArray[0]);//TODO: this may also go inside the textwathcer class?


        // This is the main part of this class which manipulates the string.
        m1.addTextChangedListener(new TextWatcher() {

            // onTextChanged listener deals on-the-fly changes in the string.
            // Whenever a user enters a new character, this callback is called.
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove the listener to avoid infinite loop or crash in memory location.
                m1.removeTextChangedListener(this);

                // As soon as the length increases from its normal length, accomodate the new
                // entered character.
                if (s.length() == lengthOfString)
                {
                    // If the number entered is among the mutable index, accomodate it.
                    if (Arrays.binarySearch(idxArray, 0, idxArray.length, last_pointer ) >= 0) {//TODO: put this in a separate function (improves readability)

                        // Construct a new string of length = lengthOfString-1
                        // finalString = substring from zeroth index to the last pointer's location +
                        // character at the last pointer's loc (because this is the newly entered char) +
                        // substring after the last pointer's loc except it's first element (because
                        // this element is now replaced by the newly entered element)
                        //TODO: Use a different variable name as compared to "t" (somthing that makes sense)
                        CharSequence t = s.subSequence(0, last_pointer) +
                                String.valueOf(s.toString().charAt(last_pointer))
                                + s.subSequence(last_pointer +2, s.length());
                        // Set this string as the view.
                        m1.setText(t);

                        // Move the arrayIdx to the next index.
                        //TODO: you may want to use this from the previously declared function (reusability)
                        arrayIdx = Arrays.binarySearch(idxArray,0, idxArray.length, last_pointer)+1;

                        // If end of the idxArray is not reached, set the pointer to arrayIdx.
                        // Else  log the output or do anything, reset the index
                        // to the first mutable index and the pointer as well.

                        // NOTE: This has a potential downfall when the user might want to change
                        // the last mutable index because the pointer will be shifted to the first
                        // mutable index. However, they can always change the last mutable index by
                        // manually dropping it at the last index.

                        if(arrayIdx < idxArray.length)
                            Selection.setSelection(m1.getText(), idxArray[arrayIdx]);
                        else
                        {
                            Log.d("Reached the end","Reached the end");
                            arrayIdx = 0;
                            Selection.setSelection(m1.getText(), idxArray[arrayIdx]);
                        }
                    }
                    // If the last_pointer is at the end of a three digit group then the newly entered digit
                    // should go to the next group.
                    else if(last_pointer == 7 || last_pointer == 12) { //TODO: never use integer constants. Instead, use a named constant like START_BRACKET_INDEX (declared somewhere as 7)
                        // Change the mutable index to the start index of the next group.
                        arrayIdx =  Arrays.binarySearch(idxArray,0, idxArray.length, last_pointer-1)+1;

                        // Accomodate the newly entered character in the next group of digits
                        // New string = old string upto the next group's opening parenthesis +
                        // the digit entered by the user +
                        // remaining string of old string except the first element of the next group
                        // which is to be replaced.
                        //TODO: Change variable name
                        CharSequence t = spannable.toString().subSequence(0, idxArray[arrayIdx])
                                + String.valueOf(s.toString().charAt(last_pointer))
                                + spannable.toString().subSequence(idxArray[arrayIdx]+1, spannable.toString().length());

                        // Set this string as the view.
                        m1.setText(t);
                        // Point the mutable index to next digit and set selection there.
                        arrayIdx++;//TODO: Thought it is not much significant here, ++variable is actually faster than variable++
                        Selection.setSelection(m1.getText(), idxArray[arrayIdx]);
                    }
                    // If the changed index is not mutable then restore the earlier string.
                    // This will also put the pointer to the first mutable index. wherever
                    // the earlier pointer was.
                    else{
                        m1.setText(spannable.toString());
                        Selection.setSelection(m1.getText(), idxArray[0]);
                    }

                }
                // Add the textwatcher again.
                m1.addTextChangedListener(this);
            }



            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // RelativeSizeSpan does not change any size of the text, hence it does
                // nothing except initializing a new span obj.
                span = new RelativeSizeSpan(1.0f);

                // Set the span on the string. This will keep the copy of the earlier string
                // so that in any case which needs the string to be reverted, we can just
                // pass the string obtained from this span.
                spannable = new SpannableString(s);
                spannable.setSpan(span, start, start + count, Spanned.SPAN_COMPOSING);

                // This is the origin of the last pointer's location discussed above.
                last_pointer = Selection.getSelectionStart(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                m1.removeTextChangedListener(this);
                // If user tries to select the string and delete all at once, nothing would
                // happen because the string has been restored and the pointer is again at
                // first mutable index.
                if(s.length() < lengthOfString-2)
                {
                    arrayIdx = 0;
                    m1.setText(spannable.toString());
                    Selection.setSelection(m1.getText(), idxArray[0]);
                }

                // If only one character has been deleted, replace the character with a whitespace
                // if the character was a mutable one, else restore the ealier string, i.e, do
                // nothing.
                if (s.length() < lengthOfString-1)
                {
                    // If the last pointer was a mutable index, change the index to a whitespace.
                    if((last_pointer >= 5 && last_pointer <= 7) || (last_pointer >= 10 && last_pointer <= 12)
                        || (last_pointer >= 14 && last_pointer <= 17)){
                        // NOTE: last_pointer is chanegd here to the current pointer. Beware!
                        last_pointer = Selection.getSelectionStart(s);
                        CharSequence t = s.subSequence(0, last_pointer) +
                                String.valueOf(' ')
                                + s.subSequence(last_pointer, s.length());
                        m1.setText(t);
                        Selection.setSelection(m1.getText(), last_pointer);
                        // Update the array index to the pointer where last pointer is currently.
                        arrayIdx = Arrays.binarySearch(idxArray,0, idxArray.length, last_pointer);//TODO - separate function
                    }
                    // Corner case when the user presses backspace from one bracket group's start position
                    // In this case, bring the pointer to the last position in the earlier bracket group.
                    else if(last_pointer == 9 || last_pointer == 13 ) {
                        m1.setText(spannable.toString());
                        arrayIdx = Arrays.binarySearch(idxArray,0, idxArray.length, last_pointer)-1;
                        CharSequence t = m1.getText().toString().substring(0, idxArray[arrayIdx]) +
                                String.valueOf(' ')
                                + m1.getText().toString().substring(idxArray[arrayIdx] + 1);
                        m1.setText(t);
                        Selection.setSelection(m1.getText(), idxArray[arrayIdx]);
                    }
                    // Do not change the string at all. Keep the pointer intact (Don't move it to the
                    // first mutable index.
                    else
                    {
                        m1.setText(spannable.toString());
                        Selection.setSelection(m1.getText(), last_pointer);
                    }
                }

                // Remove the span applied in the beforeTextChanged callback. Even I don't know
                // why I did this. Let me know if you get some explanation about this.
                spannable.removeSpan(span);
                span = null;
                spannable = null;

                m1.addTextChangedListener(this);
            }
        });
    }

}

// Known bug: The cursor doesn't stay on the last mutable index but just goes to the
// zeroth index.