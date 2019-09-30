package checkin.pojo;

public class CheckInTime {
    private final int time;//打卡时间，如果时间为-1，则表示该时间用于占位，该打卡时间在输出到Excel时应该填充空单元格
    private int pairedTime;//匹配对应班次的第几个点0表示没有匹配，1表示第一个点，...。一般奇数为上班点，偶数为下班点
    private int deviation;//和对应上下班时间点的偏差（上下班点-打卡记录），上班点负为迟到，下班点正为早退

    public CheckInTime(int time) {
        this.time = time;
        pairedTime = 0;
        deviation = 0;
    }

    /**
     * @return 正为早退，0为正常，负为迟到，返回非零值时的数值就是迟到或早退的分钟数
     */
    public int isLateOrEarly() {
        if (pairedTime == 0)
            throw new IllegalStateException("没有匹配的时间点");
        else if (pairedTime % 2 == 1 && deviation < 0) //上班点迟到
            return deviation;
        else if (pairedTime % 2 == 0 && deviation > 0)//下班点早退
            return deviation;

        return 0;
    }

    public int getTime() {
        return time;
    }


    //时间只能为正数
    public static String toTimeString(int time) {
        if (time >= 0)
            return (int) Math.floor(time / 60.0) + ":" + ((time % 60 >= 10) ? (time % 60) + "" : "0" + (time % 60));
        else
            return null;
    }

    //时间的格式为00:00
    public static int timeStringToMinutes(String timeString) {
        String[] array = timeString.split(":");
        if (array.length == 2) {
            return Integer.valueOf(array[0]) * 60 + Integer.valueOf(array[1]);
        } else {
            return 0;
        }

    }

    public int getPairedTime() {
        return pairedTime;
    }

    public void setPairedTime(int pairedTime) {
        this.pairedTime = pairedTime;
    }

    public int getDeviation() {
        return deviation;
    }

    public void setDeviation(int deviation) {
        this.deviation = deviation;
    }

    @Override
    public String toString() {
        return "CheckInTime{" +
                "time=" + time +
                ", pairedTime=" + pairedTime +
                ", deviation=" + deviation +
                '}';
    }
}