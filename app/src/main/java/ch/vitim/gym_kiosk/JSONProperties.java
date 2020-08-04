package ch.vitim.gym_kiosk;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;

public class JSONProperties
    //Base class to hold information about the property
 {

    //property basics
    private String date;
    private String result;
    private LineData lineData;

    //constructor
    public JSONProperties(String date, String result, LineData lineData){

        this.date = date;
        this.result = result;
        this.lineData = lineData;
    }

    //getters
//    public int getStreetNumber() { return streetNumber; }
    public String getDate() {return date;}
    public String getResult() {return result;}
    public LineData getLineData() {return lineData;}
}
