package com.pushsl.pushsl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pushsl.pushsl.Objects.Leg;
import com.pushsl.pushsl.Objects.RealTimeBusesAndMetros;
import com.pushsl.pushsl.Objects.SiteInfo;
import com.pushsl.pushsl.Objects.Trip;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Component
public class APIData {

    public List<SiteInfo> getSiteInfo(String searchString) {
        String format = "json";
        String key = System.getenv("SiteInfoKey");
        boolean stationsOnly = true;
        int maxResults = 10;

        String urlString = "http://api.sl.se/api2/typeahead." + format
                + "?key=" + key
                + "&searchstring=" + searchString
                + "&stationsonly=" + stationsOnly
                + "&maxresults=" + maxResults;
        urlString = urlString.replaceAll(" ", "%20");
        String result = fetch(urlString);
        System.out.println(urlString);

        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        JsonArray siteInfoArray = jsonObject.get("ResponseData").getAsJsonArray();
        List<SiteInfo> siteInfoList = new ArrayList<>();

        Gson gson = new Gson();
        for (int i = 0; i < siteInfoArray.size(); i++) {
            siteInfoList.add(gson.fromJson(siteInfoArray.get(i), SiteInfo.class));
        }

        return siteInfoList;

    }

    public List<RealTimeBusesAndMetros> getRealTimeInfo(String siteId, String timewindow) {
        String format = "json";
        String key = System.getenv("RealTimeKey");

        String urlString = "http://api.sl.se/api2/realtimedeparturesV4." + format
                + "?key=" + key
                + "&siteid=" + siteId
                + "&timewindow=" + timewindow;

        String result = fetch(urlString);

        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        JsonArray metroArray = jsonObject.get("ResponseData").getAsJsonObject().get("Metros").getAsJsonArray();
        List<RealTimeBusesAndMetros> realTimeList = new ArrayList<>();

        Gson gson = new Gson();
        for (int i = 0; i < metroArray.size(); i++) {
            realTimeList.add(gson.fromJson(metroArray.get(i), RealTimeBusesAndMetros.class));
        }

        JsonArray busArray = jsonObject.get("ResponseData").getAsJsonObject().get("Buses").getAsJsonArray();
        for (int i = 0; i < busArray.size(); i++) {
            realTimeList.add(gson.fromJson(busArray.get(i), RealTimeBusesAndMetros.class));
        }
        return realTimeList;
    }

    public List<Trip> tripInfo(String originId, String destId, String date, String time) {
        String format = "json";
        String key = System.getenv("PlannerKey");
        String urlString = "http://api.sl.se/api2/TravelplannerV3/trip." + format
                + "?key=" + key
                + "&originId=" + originId
                + "&destId=" + destId
                + "&date=" + date
                + "&time=" + time;

        String result = fetch(urlString);

        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        JsonArray tripArray = jsonObject.get("Trip").getAsJsonArray();
        List<Trip> tripInfo = new ArrayList<>();

        Gson gson = new Gson();
        for (int i = 0; i < tripArray.size(); i++) {
            tripInfo.add(new Trip());
            tripInfo.get(i).originId = originId;
            tripInfo.get(i).duration = tripArray.get(i).getAsJsonObject().get("duration").toString();
            JsonArray tripPartArray = tripArray.get(i).getAsJsonObject().get("LegList").getAsJsonObject().get("Leg").getAsJsonArray();
            for (int j = 0; j < tripPartArray.size(); j++) {
                JsonObject json = tripPartArray.get(j).getAsJsonObject();
                tripInfo.get(i).legList.add(gson.fromJson(json, Leg.class));
            }
            tripInfo.get(i).setInfo();
        }
        System.out.println("Heeej");
        return tripInfo;
    }

    public String getRemainingTime(Trip trip) {

        List<RealTimeBusesAndMetros> realTimeList = getRealTimeInfo(trip.originId, "30");

        for(RealTimeBusesAndMetros rt : realTimeList) {
            if(rt.JourneyNumber.equals(trip.legList.get(0).number)) {
                return rt.ExpectedDateTime;
            }
        }
        return null;
    }

    private String fetch(String urlString) {
        String result = "";
        try {
            URL url = new URL(urlString);
            Scanner sc = new Scanner(url.openStream());
            result = sc.nextLine();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
