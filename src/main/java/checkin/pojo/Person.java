package checkin.pojo;

import java.util.Map;
import java.util.TreeMap;

public class Person {
    private String id;
    private String name;

    private Map<String, MonthlyCheckInRecord> monthlyCheckInRecords;


    public Person() {
        id = "";
        name = "";
        monthlyCheckInRecords = new TreeMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, MonthlyCheckInRecord> getMonthlyCheckInRecords() {
        return monthlyCheckInRecords;
    }

    public void clearMonthlyCheckInRecords() {
        this.monthlyCheckInRecords = new TreeMap<>();
    }

    public void addMonthlyCheckInRecords(MonthlyCheckInRecord monthlyCheckInRecord) {
        String key = getMonthlyCheckInRecordKey(monthlyCheckInRecord);
        monthlyCheckInRecords.put(key, monthlyCheckInRecord);
    }

    /**
     * 每个月的打卡记录的key的生成规则
     */
    public static String getMonthlyCheckInRecordKey(MonthlyCheckInRecord monthlyCheckInRecord) {
        String key = monthlyCheckInRecord.getCheckInRecords().iterator().next().getYear() + "-" + monthlyCheckInRecord.getCheckInRecords().iterator().next().getMonth();
        return key;
    }

    /**
     * 每个月的打卡记录的key的生成规则
     */
    public static String getMonthlyCheckInRecordKey(CheckInRecord checkInRecord) {
        String key = checkInRecord.getYear() + "-" + checkInRecord.getMonth();
        return key;
    }
}
