package com.example.sahil_1.phonenum;

import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public final class PhoneNumberTextWatcher implements TextWatcher {
    // editView is the View that is being used for the template.
    private EditText editView;

    private static final String PLACEHOLDER_TEXT = "(123) 456-7890";

    // This is length() + 1 because we are keeping one index as buffer
    // to accommodate any new character that is entered. Later we again
    // bring the string to length() by deleting one character from the
    // string.
    public static final int LENGTH_OF_STRING = PLACEHOLDER_TEXT.length();
    public int mCurrentLength = 0;

    // Last pointer corresponds to the pointer location just before
    // some text was changed. ArrayIdx is the pointer to the elements
    // in the idxArray which is the array for keeping the mutable indexes
    // in the template, i.e, the fields where user can WRITE.
    private int mLastPointer = 0;
    private String mAddBlock;


    private static final int FIRST_GROUP_START_INDEX = 1;
    private static final int FIRST_GROUP_END_INDEX = 4;
    private static final int SECOND_GROUP_START_INDEX = 6;
    private static final int SECOND_GROUP_END_INDEX = 9;
    private static final int THIRD_GROUP_START_INDEX = 10;
    private static final int THIRD_GROUP_END_INDEX = 14;


    // idxArray - the array that is used to store the mutable indexes.
    // These are the indexes of the actual digits of the phone number
    // except the country code/
    private static final int[] sIndexArray = new int[] { 1,2,3,6,7,8,10,11,12,13 };

    // Span for accumulating the former string or the string that has
    // just been changed. This helps to restore the earlier string in
    // case user tries to mis-align the template or types where he/she
    // should not.
    private RelativeSizeSpan span;
    private SpannableString spannable;

    public PhoneNumberTextWatcher (EditText v) {
        this.editView = v;

        editView.setHint(PLACEHOLDER_TEXT);
    }

    private int search(int needle) {
        return Arrays.binarySearch(sIndexArray, 0, sIndexArray.length, needle);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // This is the origin of the last pointer's location discussed above.
        mLastPointer = Selection.getSelectionStart(s);
        if(s.length() > 0) {
            // RelativeSizeSpan does not change any size of the text, hence it does
            // nothing except initializing a new span obj.
            span = new RelativeSizeSpan(1.0f);

            // Set the span on the string. This will keep the copy of the earlier string
            // so that in any case which needs the string to be reverted, we can just
            // pass the string obtained from this span.
            spannable = new SpannableString(s);
            spannable.setSpan(span, start, start + count, Spanned.SPAN_COMPOSING);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int i, int i1, int i2) {
        // Remove the listener to avoid infinite loop or crash in memory location.
        editView.removeTextChangedListener(this);
        if(s.length() > mCurrentLength) {

            if(mLastPointer == FIRST_GROUP_START_INDEX-1)
                mAddBlock = "(";
            else if (mLastPointer == FIRST_GROUP_END_INDEX)
                mAddBlock = ") ";
            else if(mLastPointer == SECOND_GROUP_END_INDEX)
                mAddBlock = "-";
            else
                mAddBlock = "";

            if(mLastPointer == mCurrentLength && mCurrentLength < LENGTH_OF_STRING)
                appendString(s.toString(), mAddBlock);
            else if(mLastPointer == mCurrentLength && mCurrentLength == LENGTH_OF_STRING){
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(), mCurrentLength);
            }
            else reconstructString(s.toString(), mAddBlock);
        }

        editView.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {

        editView.removeTextChangedListener(this);

        int mCurrentPointer = Selection.getSelectionStart(s);
        if (mCurrentPointer <= mLastPointer) {
            if(isLastPointerDeletable()) {
            String unformattedString = s.toString().replaceAll("[^0-9]", "");
            StringBuilder newPhoneNumber = new StringBuilder();
            int pivot = 0;
            for(int i = 0; i < unformattedString.length(); i++) {
                if(pivot == FIRST_GROUP_START_INDEX-1)
                    mAddBlock = "(";
                else if (pivot == FIRST_GROUP_END_INDEX)
                    mAddBlock = ") ";
                else if(pivot == SECOND_GROUP_END_INDEX)
                    mAddBlock = "-";
                else
                    mAddBlock = "";
                newPhoneNumber.append(mAddBlock);
                newPhoneNumber.append(unformattedString.charAt(i));
                pivot += mAddBlock.length()+1;
            }
            mCurrentLength = newPhoneNumber.length();
            editView.setText(newPhoneNumber);

            if(mCurrentLength >= mCurrentPointer)
                Selection.setSelection(editView.getText(),mCurrentPointer);
            else Selection.setSelection(editView.getText(),mCurrentLength);
            }
            else {
                editView.setText(spannable.toString());
                Selection.setSelection(editView.getText(),mCurrentPointer);
            }
            // Remove the span applied in the beforeTextChanged callback. Even I don't know
            // why I did this. Let me know if you get some explanation about this.
            spannable.removeSpan(span);
            span = null;
            spannable = null;
        }

        editView.addTextChangedListener(this);
    }

    private boolean isLastPointerDeletable() {
        return  (  mLastPointer >= FIRST_GROUP_START_INDEX + 1
                &&   mLastPointer <= FIRST_GROUP_END_INDEX ) ||
                ( mLastPointer >= SECOND_GROUP_START_INDEX + 1 &&
                        mLastPointer <= SECOND_GROUP_END_INDEX ) ||
                 (mLastPointer >= THIRD_GROUP_START_INDEX + 1 &&
                         mLastPointer <= THIRD_GROUP_END_INDEX);
    }
    private boolean isLastPointerMutable() {
       return (search(mLastPointer) >= 0
                || mLastPointer == FIRST_GROUP_START_INDEX-1
                || mLastPointer == FIRST_GROUP_END_INDEX
                || mLastPointer == SECOND_GROUP_END_INDEX);
    }

    private void appendString(CharSequence s, String mAddBlock){
        CharSequence newPhoneNumber = s.toString().substring(0, mLastPointer)
                + mAddBlock + s.toString().charAt(mLastPointer);
        mCurrentLength += mAddBlock.length() + 1;
        editView.setText(newPhoneNumber);
        Selection.setSelection(editView.getText(), mCurrentLength);
    }

    private  void reconstructString(CharSequence s, String mAddBlock){
        String mEarlierString = spannable.toString();
        if(isLastPointerMutable()) {

            CharSequence newPhoneNumber =
                            mEarlierString.substring(0, mLastPointer + mAddBlock.length())
                            + s.toString().charAt(mLastPointer)
                            + mEarlierString.substring(mLastPointer + mAddBlock.length() + 1);

            editView.setText(newPhoneNumber);
            Selection.setSelection(editView.getText(), mLastPointer + mAddBlock.length() + 1);
        }
        else {
            editView.setText(mEarlierString);
            Selection.setSelection(editView.getText(), FIRST_GROUP_START_INDEX);
        }
    }
}
