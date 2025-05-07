package comp4342.totp_manager.utils;

import java.util.ArrayList;
import java.util.List;

public class ChartData {
    private String name;
    private String key;
    private long time;
    private long cdid;
    private String comment;
    private boolean isDeleted;

    // Constructor
    public ChartData(String name, String key, long time,long cdid, String comment) {
        this.name = name;
        this.key = key;
        this.time = time;
        this.cdid = cdid;
        this.comment = comment;
        this.isDeleted = false;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void delete(){
        this.isDeleted = true;
        this.key = "";
        this.name = "";
        this.time = System.currentTimeMillis();
    }

    public boolean is_deleted(){
        return this.isDeleted;
    }



    // Merge two lists of ChartData objects without duplicates based on name, keeping the one with the latest time
    public static List<ChartData> mergeLists(List<ChartData> list1, List<ChartData> list2) {
        if (list1 == null && list2 == null) {
            return new ArrayList<>();
        }
        if (list1 == null || list1.isEmpty()){
            return list2;
        }else if (list2 == null || list2.isEmpty()){
            return list1;
        }

        List<ChartData> mergedList = new ArrayList<>();

        // Add all elements from the first list
        for (ChartData data : list1) {
            mergedList.add(data);
        }
        // Merge elements from the second list
        for (ChartData data : list2) {
            boolean exists = false;
            // Check if the name already exists in the merged list
            for (int i = 0; i < mergedList.size(); i++) {
                ChartData existingData = mergedList.get(i);
                if (existingData.getCdid()==data.getCdid()) {
                    // If the name exists, compare the time, and keep the one with the latest time
                    if (existingData.getTime() < data.getTime() ) {
                        mergedList.set(i, data); // Replace with the latest one
                    }
                    exists = true;
                    break;
                }
            }
            // If the name doesn't exist, add the new element
            if (!exists) {
                mergedList.add(data);
            }
        }
        return mergedList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public static String chartDataListToString(List<ChartData> chartDataList) {
        if (chartDataList == null || chartDataList.isEmpty()) {
            return "ChartData list is empty.";
        }

        StringBuilder result = new StringBuilder();
        for (ChartData chartData : chartDataList) {
            result.append(chartData.toString()) // Assuming ChartData has a meaningful toString() implementation
                    .append("\n"); // Add a newline or any delimiter
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "Name: " + name +
                ", Key: " + key +
                ", Time: " + time +
                ", CDID: " + cdid +
                ", Comment: " + comment;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTime() {
        return time;
    }

    public long getCdid() {
        return cdid;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
