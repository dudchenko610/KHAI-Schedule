package com.crazydev;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class InflateManager {

    private ArrayList<TextView> days = new ArrayList<TextView>();
    private ArrayList<LinearLayout> lessons1 = new ArrayList<LinearLayout>();
    private ArrayList<LinearLayout> lessons2 = new ArrayList<LinearLayout>();
    private View view;

    private LayoutInflater inflater;
    private Context context;

    private int day = 0;
    private int lesson1 = 0;
    private int lesson2 = 0;

    public InflateManager(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (int i = 0; i < 30; i ++) {
            TextView day = (TextView) inflater.inflate(R.layout.item_day, null);
            days.add(day);

            LinearLayout linlay1 = (LinearLayout) inflater.inflate(R.layout.item_time_one_lesson, null);
            lessons1.add(linlay1);

            LinearLayout linlay2 = (LinearLayout) inflater.inflate(R.layout.item_time_two_lessons, null);
            lessons2.add(linlay2);
        }

     //   this.view = inflater.inflate(R.layout.label_text_view, null);

    }

    public LayoutInflater getInflater() {
        return this.inflater;
    }

    public View getWaitingView() {
        return this.view;
    }


    public TextView getNextDay() {
        return this.days.get(this.day ++);
    }

    public LinearLayout getNextLesson1() {
        return this.lessons1.get(this.lesson1 ++);
    }

    public LinearLayout getNextLesson2() {
        return this.lessons2.get(this.lesson2 ++);
    }

    public void reset() {
        this.day     = 0;
        this.lesson1 = 0;
        this.lesson2 = 0;

        for (int i = 0; i < 30; i ++) {
            lessons2.get(i).setBackgroundColor(this.context.getResources().getColor(R.color.default_color_surface));
        }
    }

}
