package checkin.constants;

/**
 * 记录打卡记录的位置信息
 * 格式为：
 * | 考勤号码 | 姓名 | 日期 | 打卡时间1 | 打卡时间2 | ..."
 */
public class CheckInRecordConstants {

    public static final int idCol = 0;//考勤号码所在列
    public static final int nameCol = 1;//姓名所在列
    public static final int dateCol = 2;//日期所在列
    public static final String dateLetter = "C";//日期所在列
    public static final int checkInTimesCol = 3;//打卡时间开始位置，约定从下标3开始，到之后一定数量的单元格为止，都是打卡时间

    public static final int classNameCol = 14;//O列，匹配的班次的名字放置的位置
    public static final int lateOrEarlyCol = 15;//P列，迟到早退时间总和放置的位置




}
