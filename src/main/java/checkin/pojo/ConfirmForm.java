package checkin.pojo;

public class ConfirmForm {
    private String department;
    private String name;
    private String 剩余年假;
    private String 本月存钟;
    private String 漏打卡;
    private String 全勤;
    private String remark;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String get剩余年假() {
        return 剩余年假;
    }

    public void set剩余年假(String 剩余年假) {
        this.剩余年假 = 剩余年假;
    }

    public String get本月存钟() {
        return 本月存钟;
    }

    public void set本月存钟(String 本月存钟) {
        this.本月存钟 = 本月存钟;
    }

    public String get漏打卡() {
        return 漏打卡;
    }

    public void set漏打卡(String 漏打卡) {
        this.漏打卡 = 漏打卡;
    }

    public String get全勤() {
        return 全勤;
    }

    public void set全勤(String 全勤) {
        this.全勤 = 全勤;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
