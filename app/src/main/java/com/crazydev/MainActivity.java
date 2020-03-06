package com.crazydev;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.crazydev.net.DataHolder;
import com.crazydev.net.NetWorker;
import com.crazydev.net.OnDataCameListener;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements OnDataCameListener {

    private Drawer.Result drawerResult;
    private NetWorker netWorker;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private RelativeLayout constraintLayout;
    private LinearLayout main_shedule_layout;

    private static final String GROUPS_KEY = "groups_key";
    private static final String SHEDULE_KEY = "shedule_key";
    private static final String LAST_GROUP_KEY = "last_group_key";

    private Elements groupElements;
    private TextView slectedGroup;
    private EditText typeGroup;
    private ImageView update;
    private ScrollView scrollView;
    private InflateManager inflateManager;

    private String currentGroup = "";
    private int weekType;

    private ArrayList<PrimaryDrawerItem> items = new ArrayList<PrimaryDrawerItem>();
    private ArrayList<DividerDrawerItem> itemsDeviders = new ArrayList<DividerDrawerItem>();
    private int indexItem = 0;

    private TextView labelTextView;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.MyTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.createItems();
        this.labelTextView = (TextView) findViewById(R.id.label_text_view);
        this.progressBar   = (ProgressBar) findViewById(R.id.progress_bar);

        MainActivity.this.inflateManager = new InflateManager(MainActivity.this.getApplicationContext());

        String format = "yyyyMMdd";
        SimpleDateFormat df = new SimpleDateFormat(format);

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int month = cal.get(Calendar.MONTH);
        int y     = cal.get(Calendar.YEAR);

        int year = month > 8 ? y : (y - 1);

        String input  =  year + "0901";

        try {

            int diff = cal.get(Calendar.WEEK_OF_YEAR);

            //     Log.d("dateeee", "diff before = " + diff);

            Date date2 = df.parse(input);
            cal.setTime(date2);

            diff = diff - cal.get(Calendar.WEEK_OF_YEAR) ;
            diff+= 1;


            this.weekType = diff % 2;

            //  Log.d("dateeee", "diff = " + diff);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView weekTypeTv = (TextView) findViewById(R.id.type);

        weekTypeTv.setText("зараз " + (weekType == 0 ? "чисельник" : "знаменник"));
        //

        this.sharedPreferences = this.getSharedPreferences("sheduler", MODE_PRIVATE);

        this.drawerResult = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withActionBarDrawerToggle(true)
                .withHeader(R.layout.drawer_header).withOnDrawerListener(new Drawer.OnDrawerListener() {

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        MainActivity.this.hideKeyboard();

                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {

                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                        MainActivity.this.hideKeyboard();
                    }


                })
                .build();

        this.drawerResult.setOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {

                if (drawerItem == null) {
                    return;
                }

                MainActivity.this.scrollView.setScrollY(0);
                MainActivity.this.update.setVisibility(View.VISIBLE);
                MainActivity.this.outputShedule(((PrimaryDrawerItem) drawerItem).getName());

            }

        });

        constraintLayout = (RelativeLayout) findViewById(R.id.constraintlayout);

        this.netWorker = new NetWorker(this);

        this.handler = new Handler() {

            public void handleMessage(android.os.Message msg) {

                MainActivity.this.loadData(msg.obj, msg.what);

            }
        };

        this.typeGroup = (EditText) findViewById(R.id.type_group);
        this.update = (ImageView) findViewById(R.id.update);

        this.typeGroup.setFilters(new InputFilter[] { this.filter, new InputFilter.LengthFilter(10)} );
        this.typeGroup.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }


            @Override
            public void onTextChanged(CharSequence charSequence, int ii, int i1, int i2) {

                String text = typeGroup.getText().toString();

                MainActivity.this.drawerResult.removeAllItems();
                MainActivity.this.drawerResult.setSelection(-1);

                MainActivity.this.reset();

                ArrayList<IDrawerItem> ims = new  ArrayList<IDrawerItem>();

                if (groupElements == null) {
                    return;
                }

                int size = groupElements.size();
                for (int j = 0; j < size; j ++) {
                    Element element = groupElements.get(j);
                    String group = element.attr("value");

                    if (group.startsWith(text) &&  group != "") {
                        ims.add(MainActivity.this.getDrawerItem().withName(group));
                        ims.add(MainActivity.this.getDeviderItem());
                    }
                }

                size = ims.size();
                IDrawerItem[] hh = new IDrawerItem[ims.size()];

                for (int j = 0; j < size; j ++) {
                    hh[j] = ims.get(j);
                }

                MainActivity.this.drawerResult.addItems(hh);
                MainActivity.this.drawerResult.removeItem(MainActivity.this.drawerResult.getDrawerItems().size() - 1);

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        this.main_shedule_layout = (LinearLayout) findViewById(R.id.main_shedule_layout);
        this.slectedGroup        = (TextView)     findViewById(R.id.selected_group);
        this.scrollView          = (ScrollView)   findViewById(R.id.scroll_view);

        // condition

        MainActivity.this.makeWaitingScreen("Виберіть вашу групу");

        outputGroups();

        String group = this.sharedPreferences.getString(LAST_GROUP_KEY, "");

        if (group != "") {
            this.update.setVisibility(View.VISIBLE);
            outputShedule(group);
        }

    }

    private String blockCharacterSet = " ";

    private InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }

            return null;
        }
    };

    private void loadData(Object o, int type) {

        switch (type) {

            case NetWorker.TYPE_GROUPS:

                String groups = this.sharedPreferences.getString(GROUPS_KEY, "");

                if (groups == "") {
                    this.makeWaitingScreen("Виберіть вашу групу");
                }

                Elements elementsGroups = (Elements) ((DataHolder) o).obj;

                SharedPreferences.Editor editor1 = this.sharedPreferences.edit();
                editor1.putString(GROUPS_KEY, elementsGroups.toString());
                editor1.commit();

                outputGroups(elementsGroups);

                break;
            case NetWorker.TYPE_SHEDULE:

                Elements elementsShedule = (Elements) ((DataHolder) o).obj;
                String group = ((DataHolder) o).group;

                SharedPreferences.Editor editor2 = this.sharedPreferences.edit();

                String text = "<table>";
                for (Element e : elementsShedule) {
                    text += e.toString();
                    //    Log.d("taggg", "inside = " + e.wholeText());
                }

                text += "</table>";

                editor2.putString(SHEDULE_KEY + group, text);
                editor2.commit();

                this.update.setEnabled(true);
                outputShedule(elementsShedule);


                break;
            case NetWorker.TYPE_ERROR:

                this.update.setEnabled(true);
                String error = (String) ((DataHolder) o).obj;

                switch(error) {
                    case NetWorker.ERROR_WHILE_SEND_REQUEST:
                    case NetWorker.ERROR_WHILE_RETRIEVE_DATA:
                        makeWaitingScreen("Перевірте підключення до інтернету");
                        break;

                    case NetWorker.ERROR_WHILE_PASRING_DATA:
                        makeWaitingScreen("Помилка. Оновіть список груп");
                        break;
                }

                break;

            case NetWorker.TYPE_POST_STATUS:
                TextView tv = (TextView) findViewById(R.id.label_text_view);

                if (tv != null) {
                    tv.setText((String) ((DataHolder) o).obj);
                }

                break;
        }
    }

    private void outputGroups(Elements elements) {

        this.groupElements = elements;
        this.drawerResult.removeAllItems();



        this.reset();

        for (Element element : elements) {
            String group = element.attr("value");

            if (group == "") {
                continue;
            }

            this.drawerResult.addItems(
                    this.getDrawerItem().withName(group),
                    this.getDeviderItem());

        }

        this.drawerResult.removeItem(this.drawerResult.getDrawerItems().size() - 1);

    }

    private void outputGroups() {
        String groups = this.sharedPreferences.getString(GROUPS_KEY, "");

        if (groups == "") {
            this.netWorker.sendRequest();
            Snackbar.make(this.constraintLayout, "Оновлення груп...", Snackbar.LENGTH_LONG).show();
            return;
        }


        Document doc = Jsoup.parse(groups);
        Elements groupElemnts = doc.getAllElements();

        this.outputGroups(groupElemnts);

    }

    private void outputShedule(Elements elements) {

        this.progressBar.setVisibility(View.GONE);
        this.labelTextView.setVisibility(View.GONE);
        this.main_shedule_layout.removeAllViewsInLayout();
        this.inflateManager.reset();

        int len = elements.size();
        for (int i = 0; i < len; i ++) {

            Element element = elements.get(i);
            Elements tds = element.select("td");

            switch(tds.size()) {
                case 1:

                    // error message, interrupt
                    break;
                case 2:
                    if (tds.get(0).attr("rowspan") == "" && tds.get(1).attr("rowspan") == "" ) {

                        String day = tds.get(1).text();

                        TextView tv = this.inflateManager.getNextDay();
                        tv.setText(day);

                        //     tv.setBackgroundColor(this.getResources().getColor(R.color.default_color_surface));

                        MainActivity.this.main_shedule_layout.addView(tv);

                    }

                    if (tds.get(0).attr("rowspan") != "" && tds.get(1).attr("rowspan") != "" ) {
                        //      Log.d("logtagg", tds.get(1).text());

                        String time   = tds.get(0).text();
                        time = time.replace("-", "");
                        String lesson = tds.get(1).text();


                        LinearLayout linlay = this.inflateManager.getNextLesson1();
                        TextView timeTV     = linlay.findViewById(R.id.time);
                        TextView lessonTV   = linlay.findViewById(R.id.lesson);


                        timeTV.setText(time);
                        lessonTV.setText(lesson);


                        //     timeTV.setBackgroundColor(this.getResources().getColor(R.color.time_background));
                        //    lessonTV.setBackgroundColor(this.getResources().getColor(R.color.default_color_surface));

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            timeTV.setForeground(this.getResources().getDrawable(R.drawable.text_view_border));
                            lessonTV.setForeground(this.getResources().getDrawable(R.drawable.text_view_border));
                        }

                        MainActivity.this.main_shedule_layout.addView(linlay);

                    }

                    if (tds.get(0).attr("rowspan") != "" && tds.get(1).attr("rowspan") == "" ) {

                        String time = tds.get(0).text();
                        time = time.replace("-", "");
                        String lesson1 = tds.get(1).text();

                        //     Log.d("logtagg", "time = " + time);
                        //     Log.d("logtagg", "lesson 1 = " + lesson1);

                        Element el = elements.get(++ i);

                        Elements td_s = el.select("td");

                        if (td_s.size() == 1) {
                            String lesson2 = td_s.get(0).text();

                            LinearLayout linlay  = this.inflateManager.getNextLesson2();
                            TextView timeTV      = linlay.findViewById(R.id.time);
                            TextView lesson1TV   = linlay.findViewById(R.id.lesson1);
                            TextView lesson2TV   = linlay.findViewById(R.id.lesson2);

                            timeTV.setText(time);
                            lesson1TV.setText(lesson1);
                            lesson2TV.setText(lesson2);

                            //    timeTV.setBackgroundColor(this.getResources().getColor(R.color.time_background));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                lesson1TV.setBackground(this.getResources().getDrawable(R.drawable.text_view_border_lesson_unselected));
                                lesson2TV.setBackground(this.getResources().getDrawable(R.drawable.text_view_border_lesson_unselected));
                            }
                            //     lesson2TV.setBackgroundColor(this.getResources().getColor(R.color.default_color_surface));

                            if (this.weekType == 0) {
                                lesson1TV.setBackgroundColor(this.getResources().getColor(R.color.selected_lesson));
                            } else {
                                lesson2TV.setBackgroundColor(this.getResources().getColor(R.color.selected_lesson));
                            }

                            MainActivity.this.main_shedule_layout.addView(linlay);


                        } else {

                            // error message, interrupt
                        }


                    }


                    break;

                default:
                    // message about parsing error, interrupt
            }
        }

    }

    private void outputShedule(String group) {

        this.slectedGroup.setText(group);

        String groupShedule = this.sharedPreferences.getString(SHEDULE_KEY + group, "");

        SharedPreferences.Editor editor1 = this.sharedPreferences.edit();
        editor1.putString(LAST_GROUP_KEY, group);
        editor1.commit();

        if (groupShedule == "") {
            this.currentGroup = group;
            this.makeWaitingScreen("loading");
            this.netWorker.sendRequest(group, NetWorker.TYPE_SHEDULE);
            return;
        }

        Document doc = Jsoup.parse(groupShedule);
        Elements sheduleElms = doc.select("tr");

        this.outputShedule(sheduleElms);

        this.currentGroup = group;
    }


    //  private View messageView = null;

    private void makeWaitingScreen(String label) {

        this.main_shedule_layout.removeAllViewsInLayout();

        if (label == "loading") {
            labelTextView.setVisibility(View.GONE);
            this.progressBar.setVisibility(View.VISIBLE);
        } else {
            this.progressBar.setVisibility(View.GONE);
            labelTextView.setVisibility(View.VISIBLE);
            labelTextView.setText(label);
        }

    }

    @Override
    public void onDataCame(Object data, int type, String group) {
        Message message = new Message();

        DataHolder dataHolder = new DataHolder();
        dataHolder.obj   = data;
        dataHolder.group = group;

        message.obj = dataHolder;
        message.what = type;

        this.handler.sendMessage(message);
    }

    private void hideKeyboard() {

        View v  = this.getCurrentFocus();

        if (v != null) {
            IBinder binder = v.getWindowToken();
            if (binder != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(binder, 0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.item1:
                this.netWorker.sendRequest();
                this.typeGroup.setText("");
                Snackbar.make(this.constraintLayout, "Оновлення груп...", Snackbar.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onUpdateGroup(View v) {
        this.makeWaitingScreen("loading");
        this.update.setEnabled(false);
        this.netWorker.sendRequest(currentGroup, NetWorker.TYPE_SHEDULE);
    }

    private void createItems() {
        for (int i = 0; i < 1100; i ++) {
            this.items.add(new PrimaryDrawerItem());
            this.itemsDeviders.add(new DividerDrawerItem());
        }
    }

    private PrimaryDrawerItem getDrawerItem() {
        return this.items.get(this.indexItem);
    }

    private DividerDrawerItem getDeviderItem() {
        return this.itemsDeviders.get(this.indexItem ++);
    }

    private void reset() {
        this.indexItem = 0;
    }

    @Override
    public void onBackPressed(){
        if (drawerResult.isDrawerOpen()){
            drawerResult.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

}
