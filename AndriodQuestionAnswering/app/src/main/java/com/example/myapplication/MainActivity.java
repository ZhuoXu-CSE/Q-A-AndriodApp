package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.amadeus.resources.FlightOfferSearch;
import com.amadeus.resources.FlightPrice;
import com.amadeus.resources.HotelOffer;
import com.amadeus.resources.PointOfInterest;
import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.language_translator.v3.LanguageTranslator;
import com.ibm.watson.language_translator.v3.model.Languages;
import com.ibm.watson.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.language_translator.v3.model.TranslationResult;

import com.amadeus.Amadeus;
import com.amadeus.Params;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.referenceData.Locations;
import com.amadeus.resources.Location;
import com.qweather.sdk.bean.base.Code;
import com.qweather.sdk.bean.base.Lang;
import com.qweather.sdk.bean.base.Unit;
import com.qweather.sdk.bean.weather.WeatherNowBean;
import com.qweather.sdk.view.HeConfig;
import com.qweather.sdk.view.QWeather;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity {
    private Button sendButton;
    private EditText sendEditor;
    private ListView chatListView;
    private List<ChatEntity> entityList;
    private ChatAdapter chatAdapter;
    private String textIn;






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);






        //强制主线程联网更新
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        //IBM translate test
        //IamAuthenticator authenticator = new IamAuthenticator("r2oVSotNYWmO9z_h2zRBHeXNcL44mZ1sDiNf4HefUcsn");
        //final LanguageTranslator languageTranslator = new LanguageTranslator("2018-05-01", authenticator);
        //languageTranslator.setServiceUrl("https://api.us-south.language-translator.watson.cloud.ibm.com/instances/df9964dd-e28a-4608-925e-fadcdf839f78");

        //IBM assistant
        IamAuthenticator authenticator = new IamAuthenticator("wlvJHqhAkKJBzdXcBTTkxgRkIHOnCKlCtPyBjrMMenUP");
        final Assistant assistant = new Assistant("2020-09-24", authenticator);
        assistant.setServiceUrl("https://api.us-south.assistant.watson.cloud.ibm.com/instances/fa8ca58d-917c-4467-b9cb-d1331e8d9f83");
        CreateSessionOptions options = new CreateSessionOptions.Builder("1870ff0f-bfb0-45ff-9857-909fa6e960bb").build();

        final SessionResponse session_response = assistant.createSession(options).execute().getResult();
        System.out.println(session_response);



        //set up
        sendEditor = (EditText) this.findViewById(R.id.editor);
        sendButton = (Button) this.findViewById(R.id.send_btn);
        chatListView = (ListView) this.findViewById(R.id.list);

        //list of message
        entityList = new ArrayList<ChatEntity>();

        ChatEntity connect = new ChatEntity(0,"connection complete");
        entityList.add(connect);


        //adapter set
        chatAdapter = new ChatAdapter(entityList,this);
        chatListView.setAdapter(chatAdapter);




        //IBM assistant 发一条空的触发对话
        MessageInput input = new MessageInput.Builder()
                .messageType("text")
                .text("")
                .build();

        MessageOptions options2 = new MessageOptions.Builder("1870ff0f-bfb0-45ff-9857-909fa6e960bb", session_response.getSessionId())
                .input(input)
                .build();

        MessageResponse response = assistant.message(options2).execute().getResult();

        System.out.println(response);

        final String responseText=response.getOutput().getGeneric().get(0).text();
        ChatEntity reply = new ChatEntity(0,responseText);
        entityList.add(reply);






        //天气v1
//        final OpenWeatherMapHelper helper = new OpenWeatherMapHelper("9aa3c7f3271dc1c92eb3dfbd6f879978");
//        helper.setUnits(Units.IMPERIAL);
//        helper.getCurrentWeatherByCityName("HangZhou", new CurrentWeatherCallback() {
//            @Override
//            public void onSuccess(CurrentWeather currentWeather) {
//                System.out.println(currentWeather);
//                ChatEntity reply = new ChatEntity(0,currentWeather.getWeather().get(0).getDescription());
//                entityList.add(reply);
//            }
//
//            @Override
//            public void onFailure(Throwable throwable) {
//                System.out.println("xx");
//                ChatEntity reply = new ChatEntity(0,"failed");
//                entityList.add(reply);
//            }
//        });








        //Amadeus 初始化
        final Amadeus amadeus = Amadeus
                .builder("TWaUVVSwG47cGnOpAvD1uZU8vmfAANmv","tFrslCrNxIP5gfLs")
                .build();






        //input click
        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (sendEditor.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "Please enter something", Toast.LENGTH_SHORT).show();
                }else {
                    //用户输入
                    textIn=sendEditor.getText().toString();
                    sendEditor.setText("");
                    //System.out.println("1");
                    ChatEntity sent = new ChatEntity(1,textIn);
                    chatAdapter.add(sent);





                    //IBM assistant
                    MessageInput input = new MessageInput.Builder()
                            .messageType("text")
                            .text(textIn)
                            .build();

                    MessageOptions options = new MessageOptions.Builder("1870ff0f-bfb0-45ff-9857-909fa6e960bb", session_response.getSessionId())
                            .input(input)
                            .build();

                    MessageResponse response = assistant.message(options).execute().getResult();
                    System.out.println(response);
                    String responseText=response.getOutput().getGeneric().get(0).text();





                    //实时搜索 仅限hangzhou wuhan
                    if(responseText.contains("!R")) {//restaurant
                        //没做完
                        //从IBM接受city名
                        String city=responseText.substring(3);


                        try {
                            //测试hangzhou wuhan搜索两个地方
                            PointOfInterest[] pointsOfInterest;
                            if(city.equals("Hangzhou")){
                                pointsOfInterest = amadeus.referenceData.locations.pointsOfInterest.get(Params
                                        .with("latitude", "41.39715")
                                        .and("longitude", "2.160873"));
                            }else{
                                pointsOfInterest = amadeus.referenceData.locations.pointsOfInterest.get(Params
                                        .with("latitude", "40.792")
                                        .and("longitude", "-74.058"));
                            }

                            if(pointsOfInterest.length!=0){
                                //只显示餐厅
                                int i=0;
                                while(!pointsOfInterest[i].getCategory().equals("RESTAURANT")){
                                    i++;
                                }
                                //System.out.println(pointsOfInterest[0]);
                                ChatEntity reply = new ChatEntity(0, "You can try "+pointsOfInterest[i].getName());
                                entityList.add(reply);
                            }else{
                                ChatEntity reply = new ChatEntity(0, "There's no result");
                                entityList.add(reply);
                            }

                        } catch (ResponseException e) {
                            e.printStackTrace();
                        }







                    }else if(responseText.contains("!H")){//hotel
                        //从IBM接受city名
                        String city=responseText.substring(3);
                        try {
                            //测试hangzhou wuhan搜索两个地方
                            HotelOffer[] offers;
                            if(city.equals("Hangzhou")){
                                offers = amadeus.shopping.hotelOffers.get(Params
                                        .with("latitude", "41.39715")
                                        .and("longitude", "2.160873"));
                            }else{
                                offers = amadeus.shopping.hotelOffers.get(Params
                                        .with("latitude", "40.792")
                                        .and("longitude", "-74.058"));
                            }


                            //界面回复
                            if(offers.length!=0) {
                                ChatEntity reply = new ChatEntity(0, "You can stay in  "+offers[0].getHotel().getName()+"\nThe contact phone is "+offers[0].getHotel().getContact().getPhone());
                                entityList.add(reply);
                            }else{
                                ChatEntity reply = new ChatEntity(0, "There's no result");
                                entityList.add(reply);
                            }

                        } catch (ResponseException e) {
                            e.printStackTrace();
                        }







                    }else if(responseText.contains("!W")){//weather
                        //从IBM接受city名
                        String cityName=responseText.substring(3);
                        String city="101010100";//默认北京
                        if(cityName.contains("Shanghai")){
                            city="101020100";
                        }else if(cityName.contains("Guangzhou")){
                            city="101280101";
                        }else if(cityName.contains("Chongqing")){
                            city="101040100";
                        }else if(cityName.contains("Chengdu")){
                            city="101270101";
                        }else if(cityName.contains("Hangzhou")){
                            city="101210101";
                        }else if(cityName.contains("Wuhan")){
                            city="101200101";
                        }

                        //API请求
                        String API = "https://devapi.qweather.com/v7/weather/now?key=d6fb495196774fabb1d62927864afd0f&location=";
                        String lang="&lang=en";
                        try {
                            URL url = new URL(API+city+lang);
                            HttpURLConnection httpURLConnection= (HttpURLConnection) url.openConnection();
                            InputStream inputStream=httpURLConnection.getInputStream();
                            InputStreamReader inputStreamReader=new InputStreamReader(inputStream,"UTF-8");
                            BufferedReader bufferedReader=new BufferedReader(inputStreamReader);
                            StringBuffer stringBuffer=new StringBuffer();
                            String temp=null;

                            while ((temp=bufferedReader.readLine())!=null){
                                stringBuffer.append(temp);
                            }

                            bufferedReader.close();
                            inputStreamReader.close();
                            inputStream.close();
                            System.out.println(stringBuffer);

                            //内容解析
                            try {
                                JSONObject jsonObject = new JSONObject(stringBuffer.toString());
                                String now = jsonObject.getString("now");
                                System.out.println(now);
                                JSONObject now2=new JSONObject(now);

                                String feelsLike = now2.getString("feelsLike");
                                String Temp = now2.getString("temp");
                                String text = now2.getString("text");
                                String windDir = now2.getString("windDir");
                                //界面回复
                                ChatEntity reply = new ChatEntity(0, "Weather in "+cityName+" is "+text+"  Temperature: "+Temp+"  Wind: "+windDir);
                                entityList.add(reply);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }







                    }else if(responseText.contains("!F")){//flight  目前只有beijing飞shanghai
                        String departureDate=responseText.substring(3);

                        FlightOfferSearch[] flightOffersSearches;
                        try {
                            flightOffersSearches = amadeus.shopping.flightOffersSearch.get(
                                    Params.with("originLocationCode", "SYD")
                                            .and("destinationLocationCode", "BKK")
                                            .and("departureDate", departureDate)
                                            .and("adults", 1)
                                            .and("max", 2));
                            if(flightOffersSearches.length==0){
                                ChatEntity reply2 = new ChatEntity(0, "No flight was found");
                                entityList.add(reply2);
                            }else{
                                int bookable=flightOffersSearches[0].getNumberOfBookableSeats();
                                String duration=flightOffersSearches[0].getItineraries()[0].getDuration();
                                String carrier=flightOffersSearches[0].getItineraries()[0].getSegments()[0].getCarrierCode();
                                String number=flightOffersSearches[0].getItineraries()[0].getSegments()[0].getNumber();
                                String dep=flightOffersSearches[0].getItineraries()[0].getSegments()[0].getDeparture().getAt();
                                String aircraft=flightOffersSearches[0].getItineraries()[0].getSegments()[0].getAircraft().getCode();
                                int numberOfStops=flightOffersSearches[0].getItineraries()[0].getSegments()[0].getNumberOfStops();

                                ChatEntity reply2 = new ChatEntity(0, "Flight: "+carrier+number+"  Departure: "+dep+"  By "+aircraft+"\nDuration: "+duration+"  Number of Stops: "+numberOfStops+"  Bookable seats: "+bookable);
                                entityList.add(reply2);
                            }

                        } catch (ResponseException e) {
                            e.printStackTrace();
                            ChatEntity reply2 = new ChatEntity(0, e.getMessage());
                            entityList.add(reply2);
                        }







                    }else if(responseText.contains("!S")){//Site
                        //从IBM接受city名
                        String city=responseText.substring(3);
                        try {
                            //测试hangzhou wuhan搜索两个地方
                            PointOfInterest[] pointsOfInterest;
                            if(city.equals("Hangzhou")){
                                pointsOfInterest = amadeus.referenceData.locations.pointsOfInterest.get(Params
                                        .with("latitude", "41.39715")
                                        .and("longitude", "2.160873"));
                            }else{
                                pointsOfInterest = amadeus.referenceData.locations.pointsOfInterest.get(Params
                                        .with("latitude", "40.792")
                                        .and("longitude", "-74.058"));
                            }


                            //只显示景点
                            if(pointsOfInterest.length!=0){
                                int i=0;
                                while(!pointsOfInterest[i].getCategory().equals("SIGHTS")){
                                    i++;
                                }
                                System.out.println(pointsOfInterest[i]);
                                //界面回复
                                ChatEntity reply = new ChatEntity(0, "You can visit "+pointsOfInterest[i].getName()+"\nTag: "+pointsOfInterest[i].getTags()[0]+","+pointsOfInterest[i].getTags()[1]);
                                entityList.add(reply);
                            }else{
                                ChatEntity reply = new ChatEntity(0, "There's no result");
                                entityList.add(reply);
                            }


                        } catch (ResponseException e) {
                            e.printStackTrace();
                        }
                    }else{
                        ChatEntity reply = new ChatEntity(0,responseText);
                        entityList.add(reply);
                    }



                }
            }
        });

    }




}