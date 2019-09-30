package checkin.pojo;

import checkin.constants.CheckInRecordStatisticsConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MonthlyCheckInRecord implements Comparable {
    private Set<CheckInRecord> checkInRecords;

    private int entryDate;//入职时间，如果是0表示不是新来的

    private List<String> checkInRecordStatistics;//考勤统计条的数据，录入时没有做校验，所以获取是要检测数据是否合法

    private ConfirmForm confirmForm;//如果有通过本程序生成的上个月的考勤确认表，就把对应数据放到这里（没有则为null）

    public MonthlyCheckInRecord() {
        checkInRecords = new TreeSet<>();
        entryDate = 0;
        checkInRecordStatistics = new ArrayList<>(CheckInRecordStatisticsConstants.maxCol);
    }

    public Set<CheckInRecord> getCheckInRecords() {
        return checkInRecords;
    }

    /**
     * 设置时要做校验，以保证录入记录的年、月都是一致的
     *
     * @param checkInRecords
     */
/*    public void setCheckInRecords(Set<CheckInRecord> checkInRecords) {
        int year = -1;
        int month = -1;
        if (checkInRecords != null) {
            for (CheckInRecord record : checkInRecords) {
                if (year == -1 || month == -1) {
                    year = record.getYear();
                    month = record.getMonth();
                } else if (year != record.getYear() || month != record.getMonth()) {
                    throw new IllegalArgumentException("不是同一年月的数据不能设置到一个MonthlyCheckInRecord中");
                }
            }
        }
        this.checkInRecords = checkInRecords;
    }*/

    /**
     * 设置时要做校验，以保证录入记录的年、月都是一致的
     *
     * @param checkInRecord
     */
    public MonthlyCheckInRecord addCheckInRecord(CheckInRecord checkInRecord) {
        if (checkInRecords.size() != 0)
            if (checkInRecords.iterator().next().getYear() != checkInRecord.getYear() || checkInRecords.iterator().next().getMonth() != checkInRecord.getMonth())
                throw new IllegalArgumentException("不是同一年月的数据不能设置到一个MonthlyCheckInRecord中");

        checkInRecords.add(checkInRecord);

        return this;
    }

    public int getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(int entryDate) {
        this.entryDate = entryDate;
    }

    public void setCheckInRecordStatistics(List<String> dataRow) {
        this.checkInRecordStatistics = dataRow;
    }

    public List<String> getCheckInRecordStatistics() {
        return checkInRecordStatistics;
    }

    public ConfirmForm getConfirmForm() {
        return confirmForm;
    }

    public void setConfirmForm(ConfirmForm confirmForm) {
        this.confirmForm = confirmForm;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof MonthlyCheckInRecord) {
            CheckInRecord otherRecord = ((MonthlyCheckInRecord) o).getCheckInRecords().iterator().next();
            CheckInRecord myRecord = getCheckInRecords().iterator().next();

            if (otherRecord.getYear() != myRecord.getYear())
                return (myRecord.getYear() - otherRecord.getYear()) * 12;
            else if (otherRecord.getMonth() != myRecord.getMonth())
                return myRecord.getMonth() - otherRecord.getMonth();
        }
        return 0;
    }
}
