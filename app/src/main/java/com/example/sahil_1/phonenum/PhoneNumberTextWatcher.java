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

/*
    TODO -
        Create a gradle package for the same


 */

public final class PhoneNumberTextWatcher implements TextWatcher {
    // editView is the View that is being used for the template.
    private EditText editView;

    private static final String PLACEHOLDER_TEXT = "+1 (XXX) XXX-XXXX";
    private static final String EMPTY_CHARACTER_PLACEHOLDER = "X";

    // This is length() + 1 because we are keeping one index as buffer
    // to accommodate any new character that is entered. Later we again
    // bring the string to length() by deleting one character from the
    // string.
    public static final int LENGTH_OF_STRING = PLACEHOLDER_TEXT.length()+1;


    // Last pointer corresponds to the pointer location just before
    // some text was changed. ArrayIdx is the pointer to the elements
    // in the idxArray which is the array for keeping the mutable indexes
    // in the template, i.e, the fields where user can WRITE.
    private int mLastPointer = 0;
    private int mArrayIdx = 0;


    private static final int FIRST_GROUP_START_INDEX = 4;
    private static final int FIRST_GROUP_END_INDEX = 7;
    private static final int SECOND_GROUP_START_INDEX = 9;
    private static final int SECOND_GROUP_END_INDEX = 12;
    private static final int THIRD_GROUP_START_INDEX = 13;
    private static final int THIRD_GROUP_END_INDEX = 17;


    // idxArray - the array that is used to store the mutable indexes.
    // These are the indexes of the actual digits of the phone number
    // except the country code/
    private static final int[] sIndexArray = new int[] { 4,5,6,9,10,11,13,14,15,16};

    // Span for accumulating the former string or the string that has
    // just been changed. This helps to restore the earlier string in
    // case user tries to mis-align the template or types where he/she
    // should not.
    private RelativeSizeSpan span;
    private SpannableString spannable;

    public PhoneNumberTextWatcher (EditText v) {
        this.editView = v;

        editView.setText(PLACEHOLDER_TEXT);

        // Set the initial pointer to the first digit in the phone number.
        Selection.setSelection(editView.getText(), sIndexArray[0]);
    }

    private int search(int needle) {
        return Arrays.binarySearch(sIndexArray, 0, sIndexArray.length, needle);
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
        mLastPointer = Selection.getSelectionStart(s);
    }

    @Override
    public void onTextChanged(CharSequence s, int i, int i1, int i2) {
        // Remove the listener to avoid infinite loop or crash in memory location.
        editView.removeTextChangedListener(this);

        // As soon as the length increases from its normal length, accommodate the new
        // entered character.
        if (s.length() == LENGTH_OF_STRING) {
            // Add the textWatcher again.
            // If the number entered is among the mutable index, accommodate it.
            if (search(mLastPointer) >= 0) {

                // Construct a new string of length = lengthOfString-1
                // finalString = substring from zeroth index to the last pointer's location
                // + character at the last pointer's loc (because this is the newly entered char)
                // + substring after the last pointer's loc except it's first element (because
                //   this element is now replaced by the newly entered element)
                CharSequence newPhoneNumber = s.subSequence(0, mLastPointer) +
                        String.valueOf(s.toString().charAt(mLastPointer))
                        + s.subSequence(mLastPointer +2, s.length());

                // Set this string as the view.
                editView.setText(newPhoneNumber);

                // Move the arrayIdx to the next index.
                mArrayIdx = search(mLastPointer)+1;

                // If end of the idxArray is not reached, set the pointer to arrayIdx.
                // Else  log the output or do anything, reset the index
                // to the first mutable index and the pointer as well.

                // NOTE: This has a potential downfall when the user might want to change
                // the last mutable index because the pointer will be shifted to the first
                // mutable index. However, they can always change the last mutable index by
                // manually dropping it at the last index.

                if(mArrayIdx < sIndexArray.length) {
                    Selection.setSelection(editView.getText(), sIndexArray[mArrayIdx]);
                } else {
                    Log.d("Reached the end","Reached the end");
                    mArrayIdx = 0;
                    Selection.setSelection(editView.getText(), sIndexArray[mArrayIdx]);
                }
            }
            // If the last_pointer is at the end of a three digit group then the newly entered
            // digit should go to the next group.
            else if(mLastPointer == FIRST_GROUP_END_INDEX
                    || mLastPointer == SECOND_GROUP_END_INDEX) {

                // Change the mutable index to the start index of the next group.
                mArrayIdx =  search(mLastPointer-1)+1;

                // Accommodate the newly entered character in the next group of digits
                // New string = old string up to the next group's opening parenthesis
                // + the digit entered by the user
                // + remaining string of old string except the first element of the next group
                //   which is to be replaced.
                CharSequence newNumber = spannable.toString().subSequence(0, sIndexArray[mArrayIdx])
                        + String.valueOf(s.toString().charAt(mLastPointer))
                        + spannable.toString().subSequence(sIndexArray[mArrayIdx]+1, LENGTH_OF_STRING-1);

                // Set this string as the view.
                editView.setText(newNumber);

                // Point the mutable index to next digit and set selection there.
                ++mArrayIdx;

                Selection.setSelection(editView.getText(), sIndexArray[mArrayIdx]);
            }

            // If the changed index is not mutable then restore the earlier string.
            // This will also put the pointer to the first mutable index. wherever
            // the earlier pointer was.
            else{
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(), sIndexArray[0]);
            }

        }
        editView.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {

        editView.removeTextChangedListener(this);

        // If user tries to select the string and delete all at once, nothing would
        // happen because the string has been restored and the pointer is again at
        // first mutable index.
        // Example: If user 'SELECT ALL' the string and hit backspace, more than two
        // characters are being deleted at that moment, which it avoid by this.
        if(s.length() < LENGTH_OF_STRING-2) {
            // Sets the pointer index in sIndexArray to be the zeroth index.
            mArrayIdx = 0;

            // Restores the string to earlier one
            editView.setText(spannable.toString());

            // Sets the selection to first mutable index (because we set the pointer)
            // to zeroth index above.
            Selection.setSelection(editView.getText(), sIndexArray[0]);
        }

        // If only one character has been deleted, replace the character with an
        // EMPTY_CHARACTER_PLACEHOLDER if the character was a mutable one,
        // else restore the earlier string, i.e, do nothing.
        //
        // Example (| denotes the selection pointer):
        // Earlier string: +1 (123) 456-78|90   => Current Pointer index = 15.
        // After deletion: +1 (123) 456-7|X90   => mLastPointer will be taken as 15
        //                                         because mLastPointer is the one being
        //                                         assigned beforeTextChanged (L82).
        // (This is the reason it is being called as mLastPointer, it is NOT current!)
        //
        // else if the pointer is at the start of a group, then bring it in the previous
        // group.
        //
        // Example:
        // Earlier String: +1 (123) |456-7890   => Current pointer index = 9.
        // After Deletion: +1 (12|X) 456-7890   => mLastPointer will be taken as 9
        //                                         as explained above.
        //
        // else leave everything as it is. (the selection is at any other place than the
        // above two cases)
        //
        // Example:
        // Earlier String: +1| (123) 456-7890   => Trying to delete "1" in country code.
        // After Deletion: +1| (123) 456-7890   => Selection is at the same place, no changes.

        if (s.length() < LENGTH_OF_STRING-1) {
            // If the last pointer was a mutable index, change the index to a whitespace.
            if(isLastPointerMutable()) {
                int mCurPointer = Selection.getSelectionStart(s);

                CharSequence newNumber = s.subSequence(0, mCurPointer) +
                        EMPTY_CHARACTER_PLACEHOLDER
                        + s.subSequence(mCurPointer, s.length());

                editView.setText(newNumber);
                Selection.setSelection(editView.getText(), mCurPointer);
                // Update the array index to the pointer where last pointer is currently.
                mArrayIdx = search(mCurPointer);
            }
            // Corner case when the user presses backspace from one bracket group's start position
            // In this case, bring the pointer to the last position in the earlier bracket group.
            else if(mLastPointer ==SECOND_GROUP_START_INDEX
                || mLastPointer == THIRD_GROUP_START_INDEX) {

                editView.setText(spannable.toString());

                mArrayIdx = search(mLastPointer)-1;

                CharSequence newNumber = editView.getText()
                        .toString().substring(0, sIndexArray[mArrayIdx])
                        + EMPTY_CHARACTER_PLACEHOLDER
                        + editView.getText().toString().substring(sIndexArray[mArrayIdx] + 1);

                editView.setText(newNumber);
                Selection.setSelection(editView.getText(), sIndexArray[mArrayIdx]);
            }

            // Do not change the string at all. Keep the pointer intact (Don't move it to the
            // first mutable index).
            else {
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(), mLastPointer);
            }
        }

        // Remove the span applied in the beforeTextChanged callback. Even I don't know
        // why I did this. Let me know if you get some explanation about this.
        spannable.removeSpan(span);
        span = null;
        spannable = null;

        editView.addTextChangedListener(this);
    }

    // In a phone number, if the selection pointer is at the following "|" places in (I),
    // then hitting up a backspace should delete the character and put
    // EMPTY_CHARACTER_PLACEHOLDER in the place.
    // (I) -- +1 (1|2|3|) 4|5|6|-7|8|9|0|
    //
    // (II)-- +1 (*123*) *456*-*7890*
    //            ^   ^  ^   ^ ^    ^
    // Each pair of these caret in (II) represents the start and end index of a group.
    // Thus, the following function returns true if the selection pointer is at
    // any of the above "|" places listed in (I).
    private boolean isLastPointerMutable() {
        return  (  mLastPointer >= FIRST_GROUP_START_INDEX + 1
                &&   mLastPointer <= FIRST_GROUP_END_INDEX ) ||
                ( mLastPointer >= SECOND_GROUP_START_INDEX + 1 &&
                        mLastPointer <= SECOND_GROUP_END_INDEX ) ||
                 (mLastPointer >= THIRD_GROUP_START_INDEX + 1 &&
                         mLastPointer <= THIRD_GROUP_END_INDEX);
    }
}
