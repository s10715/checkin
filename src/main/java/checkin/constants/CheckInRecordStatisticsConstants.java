package checkin.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 考勤统计条的位置关系
 * 格式为
 * | 上月存息 | 本月出勤 | 本月例休 | 本月休假 | 剩余年假 | 请假 | 计薪 | 迟到/早退 | 漏打卡 | 全勤 | 上月存钟 | 本月加钟 | 本月补钟 | 本月存钟 |
 */
public class CheckInRecordStatisticsConstants {
    public static final int 上月存息Col = 0;
    public static final String 上月存息Letter = "A";
    public static final String 上月存息 = "上月存息";

    public static final int 本月出勤Col = 1;
    public static final String 本月出勤Letter = "B";
    public static final String 本月出勤 = "本月出勤";


    public static final int 本月例休Col = 2;
    public static final String 本月例休Letter = "C";
    public static final String 本月例休 = "本月例休";


    public static final int 本月休假Col = 3;
    public static final String 本月休假Letter = "D";
    public static final String 本月休假 = "本月休假";


    public static final int 剩余年假Col = 4;
    public static final String 剩余年假Letter = "E";
    public static final String 剩余年假 = "剩余年假";


    public static final int 请假Col = 5;
    public static final String 请假Letter = "F";
    public static final String 请假 = "请假";


    public static final int 计薪Col = 6;
    public static final String 计薪Letter = "G";
    public static final String 计薪 = "计薪";


    public static final int 迟到Col = 7;
    public static final String 迟到Letter = "H";
    public static final String 迟到 = "迟到/早退";


    public static final int 漏打卡Col = 8;
    public static final String 漏打卡Letter = "I";
    public static final String 漏打卡 = "漏打卡";


    public static final int 全勤Col = 9;
    public static final String 全勤Letter = "J";
    public static final String 全勤 = "全勤";


    public static final int 上月存钟Col = 10;
    public static final String 上月存钟Letter = "K";
    public static final String 上月存钟 = "上月存钟";


    public static final int 本月加钟Col = 11;
    public static final String 本月加钟Letter = "L";
    public static final String 本月加钟 = "本月加钟";


    public static final int 本月补钟Col = 12;
    public static final String 本月补钟Letter = "M";
    public static final String 本月补钟 = "本月补钟";


    public static final int 本月存钟Col = 13;
    public static final String 本月存钟Letter = "N";
    public static final String 本月存钟 = "本月存钟";


    public static final int maxCol = 14;//从1开始数，最多要放到多少列


    public static List<String> getTitle() {
        List<String> list = new ArrayList<>(maxCol);
        while (list.size() < maxCol)
            list.add(null);
        list.set(上月存息Col, 上月存息);
        list.set(本月出勤Col, 本月出勤);
        list.set(本月例休Col, 本月例休);
        list.set(本月休假Col, 本月休假);
        list.set(剩余年假Col, 剩余年假);
        list.set(请假Col, 请假);
        list.set(计薪Col, 计薪);
        list.set(迟到Col, 迟到);
        list.set(漏打卡Col, 漏打卡);
        list.set(全勤Col, 全勤);
        list.set(上月存钟Col, 上月存钟);
        list.set(本月加钟Col, 本月加钟);
        list.set(本月补钟Col, 本月补钟);
        list.set(本月存钟Col, 本月存钟);

        return list;
    }

    public static List<String> getContent(int start, int end, int current) {
        List<String> list = new ArrayList<>(maxCol);
        while (list.size() < maxCol)
            list.add(null);

        list.set(上月存息Col, null);//上月存息，人工写
        list.set(本月出勤Col, "=SUM(" + 全勤Letter + start + ":" + 全勤Letter + end + ")");//本月出勤，在指定列求和
        list.set(本月例休Col, null);//本月例休，人工写
        list.set(本月休假Col, "=COUNTIF(" + 上月存钟Letter + start + ":" + 上月存钟Letter + end + ",\"*休息*\")+COUNTIF(" + 上月存钟Letter + start + ":" + 上月存钟Letter + end + ",\"*休半日*\")*0.5");//本月休假，用公式统计
        list.set(剩余年假Col, "=" + 上月存息Letter + current + "+" + 本月例休Letter + current + "-" + 本月休假Letter + current + "");//剩余年假，做运算（上月存息+本月例休-本月休假）
        list.set(请假Col, "=COUNTIF(" + 上月存钟Letter + start + ":" + 上月存钟Letter + end + ",\"*假*\")*1-COUNTIF(" + 上月存钟Letter + start + ":" + 上月存钟Letter + end + ",\"*假半日*\")*0.5");//请假，用公式统计
        list.set(计薪Col, "=IF(" + 上月存息Letter + current + "+" + 本月例休Letter + current + ">" + 本月休假Letter + current + "," + 本月出勤Letter + current + "+" + 本月休假Letter + current + "," + 本月出勤Letter + current + "+" + 上月存息Letter + current + "+" + 本月例休Letter + current + ")");
        list.set(迟到Col, null);//迟到早退，人工统计
        list.set(漏打卡Col, null);//漏打卡，人工统计


        //TODAY() -> CheckInRecordConstants.dateCol + end
        list.set(全勤Col, "=IF(AND(" + 计薪Letter + current + "=DAY(DATE(YEAR("+CheckInRecordConstants.dateLetter+end+"),MONTH("+CheckInRecordConstants.dateLetter+end+")+1,)),H" + current + "=0," + 漏打卡Letter + current + "=0),\"有\",\"无\")");//全勤，如果计薪等于当月天数，且无迟到早退、漏打卡则有



        list.set(上月存钟Col, null);//上月存钟，人工写
        list.set(本月加钟Col, "=SUM(" + 本月加钟Letter + start + ":" + 本月加钟Letter + end + ")");//本月加钟，在指定位置求和
        list.set(本月补钟Col, "=SUM(" + 本月补钟Letter + start + ":" + 本月补钟Letter + end + ")");//本月补种，在指定位置求和
        list.set(本月存钟Col, "=" + 上月存钟Letter + current + "+" + 本月加钟Letter + current + "-" + 本月补钟Letter + current + "");//本月存钟，做运算（上月存钟+本月加钟-本月补种）

        return list;
    }


    //检测传入的list是否为考勤统计条的标题行
    public static boolean isCheckInRecordStatistics(List<String> list) {
        boolean ret = false;
        try {
            if (上月存息.equals(list.get(上月存息Col)) &&
                    本月出勤.equals(list.get(本月出勤Col)) &&
                    本月例休.equals(list.get(本月例休Col)) &&
                    本月休假.equals(list.get(本月休假Col)) &&
                    剩余年假.equals(list.get(剩余年假Col)) &&
                    请假.equals(list.get(请假Col)) &&
                    计薪.equals(list.get(计薪Col)) &&
                    迟到.equals(list.get(迟到Col)) &&
                    漏打卡.equals(list.get(漏打卡Col)) &&
                    全勤.equals(list.get(全勤Col))) {
                ret = true;
            }
        } catch (IndexOutOfBoundsException e) {
            return false;
        }


        //有的部门不设存钟，所以可能会没有上月存钟、上月存钟、本月加钟、本月补钟、本月存钟 这些列
        if (list.size() > 上月存钟Col) {
            if (!"".equals(list.get(上月存钟Col)) && !上月存钟.equals(list.get(上月存钟Col)))
                ret = false;
        }
        if (list.size() > 本月加钟Col) {
            if (!"".equals(list.get(本月加钟Col)) && !本月加钟.equals(list.get(本月加钟Col)))
                ret = false;
        }
        if (list.size() > 本月补钟Col) {
            if (!"".equals(list.get(本月补钟Col)) && !本月补钟.equals(list.get(本月补钟Col)))
                ret = false;
        }
        if (list.size() > 本月存钟Col) {
            if (!"".equals(list.get(本月存钟Col)) && !本月存钟.equals(list.get(本月存钟Col)))
                ret = false;
        }
        return ret;
    }
}
