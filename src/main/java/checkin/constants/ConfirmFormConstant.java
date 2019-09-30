package checkin.constants;

import checkin.utils.Common;

public class ConfirmFormConstant {


    public static final int departmentCol = 0;//部门所在列
    public static final int nameCol = 1;//名字所在列

    public static final int 剩余年假Col = 2;//剩余年假所在列
    public static final int 本月存钟Col = 3;//本月存钟所在列

    public static final int 漏打卡Col = 4;//漏打卡所在列
    public static final int 全勤Col = 5;//全勤所在列

    public static final int remarkCol = 6;//备注所在列

    public static final int maxCol = 7;//从1开始数，最大有多少列

    /**
     * 按照年、月根据规则生成考勤确认表的名字（不带后缀）
     *
     * @param year
     * @param month
     */
    public static String generateFileName(int year, int month) {
        return year + "年" + month + "月" + "考勤确认表";
    }

    public static boolean isConfirmFormFileName(String fileName) {
        return Common.getFileName(Common.getFileName(fileName)).endsWith("考勤确认表");
    }

    /**
     * 根据规则解析出考勤确认表文件名中的年、月
     *
     * @param fileName
     * @return 如果文件名没有包含年月信息，则返回null
     */
    public static int[] explainFileName(String fileName) {
        fileName = Common.getFileName(fileName);
        if (fileName.matches("^?[0-9]*年?[0-9]*月考勤确认表$")) {
            int year = Integer.parseInt(fileName.split("年")[0]);
            int month = Integer.parseInt(fileName.split("年")[1].split("月")[0]);
            return new int[]{year, month};
        }
        return null;
    }
}
