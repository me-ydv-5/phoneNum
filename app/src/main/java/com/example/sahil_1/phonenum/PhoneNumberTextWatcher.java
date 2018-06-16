package com.example.sahil_1.phonenum;

import android.text.Editable;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.EditText;

import java.util.Arrays;

public final class PhoneNumberTextWatcher implements TextWatcher {

    // editView is the View that is being used for the template.
    private EditText editView;


    // idxArray - the array that is used to store the mutable indexes.
    // These are the indexes of the actual digits of the phone number
    // except the country code/
    private final int[] idxArray = new int[]{ 4,5,6,9,10,11,13,14,15,16 };

    // Last pointer corresponds to the pointer location just before
    // some text was changed. ArrayIdx is the pointer to the elements
    // in the idxArray which is the array for keeping the mutable indexes
    // in the template, i.e, the fields where user can WRITE.
    private Integer last_pointer = 0, arrayIdx = 0;

    // This is length() + 1 because we are keeping one index as buffer
    // to accommodate any new character that is entered. Later we again
    // bring the string to length() by deleting one character from the
    // string.
    private final Integer lengthOfString;

    private final Integer FIRST_GROUP_START_INDEX = 4;
    private final Integer FIRST_GROUP_END_INDEX = 7;

    private final Integer SECOND_GROUP_START_INDEX = 9;
    private final Integer SECOND_GROUP_END_INDEX = 12;

    private final Integer THIRD_GROUP_START_INDEX = 13;
    private final Integer THIRD_GROUP_END_INDEX = 17;

    // Span for accumulating the former string or the string that has
    // just been changed. This helps to restore the earlier string in
    // case user tries to mis-align the template or types where he/she
    // should not.
    private RelativeSizeSpan span;
    private SpannableString spannable;

    public PhoneNumberTextWatcher (EditText v) {
        this.editView = v;

        editView.setText("+1 (XXX) XXX-XXXX");

        // Set the initial pointer to the first digit in the phone number.
        Selection.setSelection(editView.getText(), idxArray[0]);

        lengthOfString = editView.getText().length() + 1;
    }

    private Integer doBinarySearch(Integer idxToBeFound){
        return Arrays.binarySearch(idxArray, 0, idxArray.length, idxToBeFound);
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
    public void onTextChanged(CharSequence s, int i, int i1, int i2) {
        // Remove the listener to avoid infinite loop or crash in memory location.
        editView.removeTextChangedListener(this);

        // As soon as the length increases from its normal length, accommodate the new
        // entered character.
        if (s.length() == lengthOfString)
        {
            // If the number entered is among the mutable index, accommodate it.
            if (doBinarySearch(last_pointer) >= 0) {

                // Construct a new string of length = lengthOfString-1
                // finalString = substring from zeroth index to the last pointer's location
                // + character at the last pointer's loc (because this is the newly entered char)
                // + substring after the last pointer's loc except it's first element (because
                //   this element is now replaced by the newly entered element)
                CharSequence newPhoneNumber = s.subSequence(0, last_pointer) +
                        String.valueOf(s.toString().charAt(last_pointer))
                        + s.subSequence(last_pointer +2, s.length());

                // Set this string as the view.
                editView.setText(newPhoneNumber);

                // Move the arrayIdx to the next index.
                arrayIdx = doBinarySearch(last_pointer)+1;

                // If end of the idxArray is not reached, set the pointer to arrayIdx.
                // Else  log the output or do anything, reset the index
                // to the first mutable index and the pointer as well.

                // NOTE: This has a potential downfall when the user might want to change
                // the last mutable index because the pointer will be shifted to the first
                // mutable index. However, they can always change the last mutable index by
                // manually dropping it at the last index.

                if(arrayIdx < idxArray.length)
                    Selection.setSelection(editView.getText(), idxArray[arrayIdx]);
                else
                {
                    Log.d("Reached the end","Reached the end");
                    arrayIdx = 0;
                    Selection.setSelection(editView.getText(), idxArray[arrayIdx]);
                }
            }
            // If the last_pointer is at the end of a three digit group then the newly entered
            // digit should go to the next group.
            else if(last_pointer.equals(FIRST_GROUP_END_INDEX)
                    || last_pointer.equals(SECOND_GROUP_END_INDEX)){

                // Change the mutable index to the start index of the next group.
                arrayIdx =  doBinarySearch(last_pointer-1)+1;

                // Accommodate the newly entered character in the next group of digits
                // New string = old string up to the next group's opening parenthesis
                // + the digit entered by the user
                // + remaining string of old string except the first element of the next group
                //   which is to be replaced.
                CharSequence newNumber = spannable.toString().subSequence(0, idxArray[arrayIdx])
                        + String.valueOf(s.toString().charAt(last_pointer))
                        + spannable.toString().subSequence(idxArray[arrayIdx]+1, lengthOfString-1);

                // Set this string as the view.
                editView.setText(newNumber);

                // Point the mutable index to next digit and set selection there.
                ++arrayIdx;

                Selection.setSelection(editView.getText(), idxArray[arrayIdx]);
            }

            // If the changed index is not mutable then restore the earlier string.
            // This will also put the pointer to the first mutable index. wherever
            // the earlier pointer was.
            else{
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(), idxArray[0]);
            }

        }
        // Add the textWatcher again.
        editView.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {

        editView.removeTextChangedListener(this);

        // If user tries to select the string and delete all at once, nothing would
        // happen because the string has been restored and the pointer is again at
        // first mutable index.
        if(s.length() < lengthOfString-2)
        {
            arrayIdx = 0;

            editView.setText(spannable.toString());
            Selection.setSelection(editView.getText(), idxArray[0]);
        }

        // If only one character has been deleted, replace the character with a whitespace
        // if the character was a mutable one, else restore the ealier string, i.e, do
        // nothing.
        if (s.length() < lengthOfString-1)
        {
            // If the last pointer was a mutable index, change the index to a whitespace.
            if((last_pointer >= FIRST_GROUP_START_INDEX+1 && last_pointer <= FIRST_GROUP_END_INDEX)
            || (last_pointer >= SECOND_GROUP_START_INDEX+1 && last_pointer <= SECOND_GROUP_END_INDEX)
            || (last_pointer >= THIRD_GROUP_START_INDEX+1 && last_pointer <= THIRD_GROUP_END_INDEX))
            {
                // NOTE: last_pointer is changed here to the current pointer. Beware!
                last_pointer = Selection.getSelectionStart(s);

                CharSequence newNumber = s.subSequence(0, last_pointer) +
                        String.valueOf('X')
                        + s.subSequence(last_pointer, s.length());

                editView.setText(newNumber);
                Selection.setSelection(editView.getText(), last_pointer);
                // Update the array index to the pointer where last pointer is currently.
                arrayIdx = doBinarySearch(last_pointer);
            }
            // Corner case when the user presses backspace from one bracket group's start position
            // In this case, bring the pointer to the last position in the earlier bracket group.
            else if(last_pointer.equals(SECOND_GROUP_START_INDEX)
                || last_pointer.equals(THIRD_GROUP_START_INDEX)) {

                editView.setText(spannable.toString());

                arrayIdx = doBinarySearch(last_pointer)-1;

                CharSequence newNumber = editView.getText().toString().substring(0, idxArray[arrayIdx])
                        + String.valueOf('X')
                        + editView.getText().toString().substring(idxArray[arrayIdx] + 1);

                editView.setText(newNumber);
                Selection.setSelection(editView.getText(), idxArray[arrayIdx]);
            }

            // Do not change the string at all. Keep the pointer intact (Don't move it to the
            // first mutable index).
            else
            {
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(), last_pointer);
            }
        }

        // Remove the span applied in the beforeTextChanged callback. Even I don't know
        // why I did this. Let me know if you get some explanation about this.
        spannable.removeSpan(span);
        span = null;
        spannable = null;

        editView.addTextChangedListener(this);
    }
}
