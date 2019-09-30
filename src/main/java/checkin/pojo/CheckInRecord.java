package checkin.pojo;

import checkin.constants.CheckInRecordConstants;
import checkin.utils.Common;
import checkin.utils.ExcelHelper;

import java.util.*;

public class CheckInRecord implements Record, Comparable<CheckInRecord> {
    private final int[] date;//年月日
    private List<CheckInTime> checkInTimes;//打卡记录

    private String preferClassName;//用户指定的班次

    private String pairedClassName;//匹配的班次的名字
    private List<Integer> pairedClassTimes;//匹配的班次的上下班点，-1表示占位时间，可以忽略
    private int lateOrEarly;//匹配的班次的迟到+早退时间总和

    private CellStyle cellStyle;


    /**
     * @param dateStr 格式应该是 年/月/日
     */
    public CheckInRecord(String dateStr) {
        String[] date = dateStr.split("/");
        if (date.length != 3)
            throw new IllegalArgumentException("日期格式错误");


        this.date = new int[3];
        if (Common.isNumeric(date[0]))
            this.date[0] = Integer.valueOf(date[0]);
        if (Common.isNumeric(date[1]))
            this.date[1] = Integer.valueOf(date[1]);
        if (Common.isNumeric(date[2]))
            this.date[2] = Integer.valueOf(date[2]);

        checkInTimes = new ArrayList<>(4);

        preferClassName = "";

        pairedClassName = "";
        pairedClassTimes = new ArrayList<>(0);
        lateOrEarly = 0;
    }

    /**
     * 判断数据是否符合CheckInRecord要求的格式
     *
     * @param list 数据，按照约定的顺序放置
     * @return
     */
    public static boolean isCheckInRecord(List<String> list) {
        if (list == null || list.size() < 3)
            return false;

        //id不一定要是数字类型，所以只要不为空就行
        if (list.get(CheckInRecordConstants.idCol) == null || "".equals(list.get(CheckInRecordConstants.idCol)))
            return false;

        //名字，不能为空
        if (list.get(CheckInRecordConstants.nameCol) == null || "".equals(list.get(CheckInRecordConstants.nameCol)))
            return false;

        //日期格式有两个斜杠
        if (list.get(CheckInRecordConstants.dateCol) == null || list.get(CheckInRecordConstants.dateCol).split("/").length != 3)
            return false;

        //打卡时间格式是 时:分
        if (list.size() > CheckInRecordConstants.checkInTimesCol && list.get(CheckInRecordConstants.checkInTimesCol) != null && !"".equals(list.get(CheckInRecordConstants.checkInTimesCol)) && list.get(CheckInRecordConstants.checkInTimesCol).split(":").length != 2)
            return false;

        return true;
    }

    /**
     * excel表格按行读取，每行是一个ArrayList，根据读到的ArrayList生成Record
     * 数据格式：考勤号码 姓名 年月日 打卡时间（空格隔开）
     *
     * @param list excel中的一行记录
     */
    public static CheckInRecord listToRecord(List<String> list) {
        //先判断是不是打卡记录，如果不是，则返回null
        if (!isCheckInRecord(list))
            return null;

        //日期格式不是 年/月/日 的格式，需要改成 年/月/日 的格式
        CheckInRecord checkInRecord = new CheckInRecord(ExcelHelper.transferDateCell(list.get(CheckInRecordConstants.dateCol)));

        //打卡时间从指定位置开始
        for (int i = CheckInRecordConstants.checkInTimesCol; i < list.size(); i++) {
            if (list.get(i) == null || "".equals(list.get(i).trim())) { //忽略空单元格
                continue;
            } else if (list.get(i).contains(":")) { //如果是时间格式，则认为是打卡时间记录
                checkInRecord.addCheckInTime(CheckInTime.timeStringToMinutes(list.get(i)));
            } else { //如果既不是空，也不是时间，则是其他字符串，那么就认为超过了打卡时间的位置，不再往后找了
                break;
            }
        }
        if (list.size() >= CheckInRecordConstants.classNameCol + 1)
            checkInRecord.setPreferClassName(list.get(CheckInRecordConstants.classNameCol));//如果有指定班次的话，就记录下来
        return checkInRecord;
    }


    //复制自身，每次班次判断都要清空pairedClass和清空checkinTimes的pairedTime和deviation...，只保留打卡时间就可以了
    public CheckInRecord clone() {
        CheckInRecord checkInRecord = new CheckInRecord(getDateString());
        for (CheckInTime checkInTime : checkInTimes) {
            checkInRecord.addCheckInTime(checkInTime.getTime());
        }
        return checkInRecord;
    }

    /**
     * 如果打卡次数小于匹配的班次的上下班点数量，则说明一定有漏卡，在漏卡的地方填上time为-1的占位时间
     *
     * @param replace 考虑到如果直接更改了当前对象的checkInTimes属性，那么就无法再对当前对象的checkInTimes做其他比较操作了
     *                所以应该让用户选择是否要覆盖当前对象的checkInTimes属性
     * @return 填上了占位时间的CheckInTime集合
     */
    public List<CheckInTime> reorderCheckInTimes(boolean replace) {
        int realCheckInTimes = 0;//除去占位时间，真正要打多少次卡
        for (int minutes : pairedClassTimes) {
            if (minutes != -1) //-1为占位时间，不需要打卡，也不需要和CheckInTime对应
                realCheckInTimes++;
        }

        if (checkInTimes.size() < realCheckInTimes) {//如果打卡记录的数量少于匹配的班次的打卡次数（也就是漏卡），把时间记录按匹配的班次顺序排列（也就是中间加空单元格），注意要忽略占位时间
            ArrayList<CheckInTime> indexedCheckinTimes = new ArrayList<>(checkInTimes.size());

            for (int i = 1; i <= pairedClassTimes.size(); i++) {
                if (pairedClassTimes.get(i - 1) == -1)//忽略占位时间
                    continue;

                boolean found = false;//当前班次的打卡点（i）是否找到了匹配的打卡记录
                for (CheckInTime checkInTime : checkInTimes) {
                    if (checkInTime.getPairedTime() == i) {
                        indexedCheckinTimes.add(checkInTime);
                        found = true;
                        break;
                    }
                }
                if (!found) {//如果没有找到匹配的打卡记录，则填充空单元格
                    indexedCheckinTimes.add(new CheckInTime(-1));
                }
            }

            //最后如果还有打卡记录没有被匹配（即多打了一次卡但还没有达到班次打卡次数要求的情况，也就是漏了两次卡、某一上下班点还多打了一次的情况）则在最后输出
            for (CheckInTime checkInTime : checkInTimes) {
                if (checkInTime.getPairedTime() == 0)
                    indexedCheckinTimes.add(checkInTime);
            }
            if (replace)
                setCheckInTimes(indexedCheckinTimes);
            return indexedCheckinTimes;
        } else {//如果没有漏卡则直接返回
            return checkInTimes;
        }
    }


    public int getYear() {
        return date[0];
    }

    public int getMonth() {
        return date[1];
    }

    public int getDay() {
        return date[2];
    }

    public String getDateString() {
        String year = date[0] < 10 ? "0" + date[0] : date[0] + "";
        String month = date[1] < 10 ? "0" + date[1] : date[1] + "";
        String day = date[2] < 10 ? "0" + date[2] : date[2] + "";
        return year + "/" + month + "/" + day;
    }

    public void setCheckInTimes(List<CheckInTime> checkInTimes) {
        this.checkInTimes = checkInTimes;
    }

    public List<CheckInTime> getCheckInTimes() {
        return checkInTimes;
    }

    public CheckInRecord addCheckInTime(int time) {
        checkInTimes.add(new CheckInTime(time));
        return this;
    }

    public String getPreferClassName() {
        return preferClassName;
    }

    public void setPreferClassName(String preferClassName) {
        this.preferClassName = preferClassName;
    }

    public String getPairedClassName() {
        return pairedClassName;
    }

    public void setPairedClassName(String pairedClassName) {
        this.pairedClassName = pairedClassName;
    }

    public List<Integer> getPairedClassTimes() {
        return pairedClassTimes;
    }

    public void setPairedClassTimes(List<Integer> pairedClassTimes) {
        this.pairedClassTimes = pairedClassTimes;
    }

    public int getLateOrEarly() {
        return lateOrEarly;
    }

    public void setLateOrEarly(int lateOrEarly) {
        this.lateOrEarly = lateOrEarly;
    }

    @Override
    public String toString() {
        return "CheckInRecord{" +
                "date=" + Arrays.toString(date) +
                ", checkInTimes=" + checkInTimes +
                ", preferClassName='" + preferClassName + '\'' +
                ", pairedClassName='" + pairedClassName + '\'' +
                ", pairedClassTimes=" + pairedClassTimes +
                ", lateOrEarly=" + lateOrEarly +
                ", cellStyle=" + cellStyle +
                '}';
    }

    @Override
    public CellStyle getCellStyle() {
        return cellStyle;
    }

    public void setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
    }

    @Override
    public int compareTo(CheckInRecord r) {
        return (getDay() - r.getDay()) + (getMonth() - r.getMonth()) * 31 + (getYear() - r.getYear()) * 12 * 31;
    }
}

