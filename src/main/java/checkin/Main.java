package checkin;

import checkin.constants.ClassTimes;
import checkin.process.UIWindow;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        //功能一，班次匹配
        //Executor.pairDir("./原始数据");

        //功能二：生成考勤确认表
        //Executor.generateConfirmForm("./处理好数据");

        try {
            new UIWindow().run();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

}
