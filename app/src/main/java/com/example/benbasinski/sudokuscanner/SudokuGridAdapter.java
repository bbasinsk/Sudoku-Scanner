package com.example.benbasinski.sudokuscanner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by benbasinski on 9/9/17.
 */

public class SudokuGridAdapter extends BaseAdapter {
    private Context mContext;
    private int[] digits;

    public SudokuGridAdapter(Context c, int[] digits) {
        this.mContext = c;
        this.digits = digits;
    }

    @Override
    public int getCount() {
        return digits.length;
    }

    @Override
    public Object getItem(int i) {
        return digits[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView digitTextView = new TextView(mContext);
        digitTextView.setText(String.valueOf(digits[i]));
        return digitTextView;
    }
}
