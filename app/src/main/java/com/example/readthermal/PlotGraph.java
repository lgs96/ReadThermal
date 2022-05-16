package com.example.readthermal;


import android.graphics.Color;

import com.example.readthermal.utils.RTLog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class PlotGraph {
    private String TAG = "PlotGrapht";
    public LineChart mChart;

    public PlotGraph (LineChart chart, int index){
        mChart = chart;
        setupChart();
        setupAxes(index);
        setupData();
        setLegend();
    }

    public void run(List<String> values, List<String> labels, int index) {
        addEntry(values, labels, index);
    }

    private void setupChart() {
        // disable description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // enable scaling
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        // set an alternative background color
        mChart.setBackgroundColor(Color.DKGRAY);
    }

    private void setupAxes(int index) {
        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        if (index == 0) {
            leftAxis.setAxisMaximum(80f);
            leftAxis.setAxisMinimum(25f);
        }
        if (index == 1) {
            leftAxis.setAxisMaximum(3f);
            leftAxis.setAxisMinimum(0f);
        }
        if (index == 2) {
            leftAxis.setAxisMaximum(3000f);
            leftAxis.setAxisMinimum(500f);
        }
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupData() {
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);
        mChart.highlightValue(null);

    }

    private void setLegend() {
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        //l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextColor(Color.WHITE);
    }

    private LineDataSet createSet(String label, int index) {
        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[index+1]);
        //set.setCircleColor(index);
        set.setLineWidth(1f);
        set.setCircleRadius(0f);
        //set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(0f);
        // To show values of each point
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.enableDashedLine(index*2, index*2, index*2);

        return set;
    }

    private void addEntry(List<String> values, List<String> labels, int index) {
        LineData data = mChart.getData();

        if (data != null) {
            ILineDataSet [] datasets = new ILineDataSet[values.size()];
            for (int i = 0; i < values.size(); i++){
                datasets[i] = data.getDataSetByIndex(i);
                if (datasets[i] == null){
                    datasets[i] = createSet(labels.get(i), i);
                    data.addDataSet(datasets[i]);
                }
                if (index == 0 || index == 2) {
                    data.addEntry(new Entry(datasets[i].getEntryCount(), Float.parseFloat(values.get(i)) / 1000.0f), i);
                }
                else{
                    data.addEntry(new Entry(datasets[i].getEntryCount(), Float.parseFloat(values.get(i))), i);
                }

                RTLog.d(TAG, "Float value: " + Float.parseFloat(values.get(i)));
            }


            // let the chart know it's data has changed
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(500);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());
        }
    }
}