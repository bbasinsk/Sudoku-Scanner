//package com.example.benbasinski.sudokuscanner;
//
//import android.content.Context;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.TextView;
//
///**
// * Created by benbasinski on 9/9/17.
// */
//
//public class SudokuGridAdapter extends BaseAdapter {
//    private Context mContext;
//    private int[] digits;
//
//    public SudokuGridAdapter(Context c) {
//        mContext = c;
//    }
//
//    @Override
//    public int getCount() {
//        return digits.length;
//    }
//
//    @Override
//    public Object getItem(int i) {
//        return digits[i];
//    }
//
//    @Override
//    public long getItemId(int i) {
//        return 0;
//    }
//
//    @Override
//    public View getView(int i, View view, ViewGroup viewGroup) {
//        TextView textView = new TextView();
//
////        if (convertView == null) {
//            // if it's not recycled, initialize some attributes
////            imageView = new TextView(mContext);
////            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
////            imageView.setScaleType(TextView.ScaleType.CENTER_CROP);
////            imageView.setPadding(8, 8, 8, 8);
////        } else {
////            imageView = (ImageView) convertView;
////        }
//
////        imageView.setImageResource(mThumbIds[position]);
//        return textView;
//    }
//}
