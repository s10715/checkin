package checkin;

import checkin.process.UIWindow;

public class M {

    public static void main(String[] args) {
        //功能一，班次匹配
        //Executor.pairDir("./原始数据");

        //功能二：生成考勤确认表
        //Executor.generateConfirmForm("./处理好数据");

        try {
            //不需要密码
            //new UIWindow().runMainFrame();

            //需要密码
            new UIWindow().runPasswordFrame();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

}
