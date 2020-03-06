package com.crazydev.net;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

public class NetWorker {

    public static final String ERROR_WHILE_SEND_REQUEST  = "errorwhilesendrequest";
    public static final String ERROR_WHILE_RETRIEVE_DATA = "errorwhileretrievedata";
    public static final String ERROR_WHILE_PASRING_DATA  = "errorwhileparsedata";

    public static final int TYPE_ERROR       = -1;
    public static final int TYPE_GROUPS      =  0;
    public static final int TYPE_SHEDULE     =  1;
    public static final int TYPE_POST_STATUS =  2;

    private final OnDataCameListener onDataCameListener;
    private static final String URL = "https://profkomstud.khai.edu/schedule/group/post";

    private Vector<NetThread> threads = new Vector<NetThread>();

    public NetWorker(OnDataCameListener onDataCameListener) {
        this.onDataCameListener = onDataCameListener;
    }

    public void sendRequest() {
        this.sendRequest("", TYPE_GROUPS);
    }

    public void sendRequest(final String group, final int whatToSend) {


        if (whatToSend == TYPE_SHEDULE) {
            for (int i = 0; i < threads.size(); i ++) {
                if (threads.get(i).whatToS == TYPE_SHEDULE) {
                    threads.get(i).whatToS = -1;
                    threads.get(i).interrupt();
                }
            }
        }

        if (whatToSend == TYPE_GROUPS) {
            for (int i = 0; i < threads.size(); i ++) {
                if (threads.get(i).whatToS == TYPE_GROUPS) {
                    threads.get(i).whatToS = -1;
                    threads.get(i).interrupt();
                }
            }
        }


        NetThread th = new NetThread() {

            @Override
            public void run() {
                HttpURLConnection conn = null;

                this.whatToS = whatToSend;

                try {
                    URL urlObj = new URL(NetWorker.URL);
                    conn = (HttpURLConnection) urlObj.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    conn.setReadTimeout(40000);
                    conn.setConnectTimeout(30000);

                    conn.connect();

                    String paramsString = "group=" + URLEncoder.encode(group, "UTF-8");

                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(paramsString);
                    wr.flush();

                    wr.close();

                } catch (IOException e) {
                    if (this.whatToS != - 1) {
                        NetWorker.this.onDataCameListener.onDataCame(NetWorker.ERROR_WHILE_SEND_REQUEST, TYPE_ERROR, null);
                        e.printStackTrace();
                    }

                }


                try {

                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                    StringBuilder result = new StringBuilder();

                    int counter = 0;

                    byte[] data = new byte[16];
                    int bytesRead = 0;

                    while ((bytesRead = in.read(data)) > 0) {

                        result.append(new String(data, 0, bytesRead, "UTF-8"));

                        counter ++;

                        if (this.whatToS == NetWorker.TYPE_SHEDULE) {
                      //      NetWorker.this.onDataCameListener.onDataCame(counter + "", NetWorker.TYPE_POST_STATUS, null);
                        }

                        if (this.whatToS == -1) {
                            break;
                        }

                    }

                    Document doc = Jsoup.parse(result.toString());

                    switch(this.whatToS) {
                        case NetWorker.TYPE_GROUPS:
                            Element rootElement = doc.select("select.mdb-select").first();
                            Elements groupList = rootElement.select("select > option");

                            NetWorker.this.onDataCameListener.onDataCame(groupList, NetWorker.TYPE_GROUPS, null);
                            break;

                        case NetWorker.TYPE_SHEDULE:

                            Element rootElementShedule = doc.select("div.table-responsive").first();
                            Elements shedulelist = rootElementShedule.select("tr");

                            for (int i = 0; i < shedulelist.size(); i++) {
                                Element element = shedulelist.get(i);

                                if (element.getAllElements().size() <= 1) {
                                    shedulelist.remove(element);
                                    i = - 1;
                                    continue;
                                }
                            }


                            NetWorker.this.onDataCameListener.onDataCame(shedulelist, NetWorker.TYPE_SHEDULE, group);

                            break;
                    }

                } catch (IOException e) {

                    if (this.whatToS != - 1) {
                        NetWorker.this.onDataCameListener.onDataCame(NetWorker.ERROR_WHILE_RETRIEVE_DATA, TYPE_ERROR, null);
                        e.printStackTrace();
                    }


                } catch (NullPointerException e) {
                    if (this.whatToS != - 1) {
                        NetWorker.this.onDataCameListener.onDataCame(NetWorker.ERROR_WHILE_PASRING_DATA, TYPE_ERROR, null);
                        e.printStackTrace();
                    }

                } finally {

                    NetWorker.this.threads.remove(this);

                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }


        };

        th.start();

        this.threads.add(th);

    }

}