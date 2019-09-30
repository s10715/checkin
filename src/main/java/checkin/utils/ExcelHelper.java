package checkin.utils;

import checkin.constants.CheckInRecordConstants;
import checkin.constants.CheckInRecordStatisticsConstants;
import checkin.constants.ConfirmFormConstant;
import checkin.pojo.*;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.event.WriteHandler;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static checkin.utils.Common.isNumeric;
import static checkin.utils.Common.trim;

public final class ExcelHelper {

    /**
     * easyExcel读取日期时把原来的2019/8/1改成了8/1/19，需要修正
     * 但如果原来是2019/08/01，读取确实正确的
     */
    public static String transferDateCell(String dateString) {

        //如果日期格式是 2019/08/01 （补零） 就不需要转换了，可能是当成文本了
        if (dateString.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return dateString;
        }

        //否则 年+2000 / 月 / 日
        String[] dateArr = dateString.split("/");
        if (dateArr.length == 3) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.valueOf(dateArr[2]) + 2000, Integer.valueOf(dateArr[0]) - 1, Integer.valueOf(dateArr[1]));

            return new SimpleDateFormat("yyyy/MM/dd").format(calendar.getTime());
        } else {
            return dateString;
        }
    }


    /**
     * 根据路径读取Excel文件的内容
     *
     * @param path Excel文件的路径
     * @return 文件内容
     */
    public static List<List<String>> readList(String path) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            List data = EasyExcelFactory.read(inputStream, new Sheet(1, 0));
            return data;
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * 简单地写回去，如果文件存在，则在文件名后加时间戳
     *
     * @param path 文件的路径
     * @param data 要写的内容
     */
    public static void writeList(String path, List<List<String>> data) {
        File file = new File(path);

        if (file.exists())//如果文件存在，则在文件名后加时间戳
            path = file.getParent() + File.separator + Common.getFileName(file.getName()) + System.currentTimeMillis() + "." + Common.getFileExt(file.getName());

        try (OutputStream outputStream = new FileOutputStream(path)) {
            ExcelWriter writer = EasyExcelFactory.getWriter(outputStream);
            Sheet sheet = new Sheet(0, 0);
            sheet.setAutoWidth(true);//单元格大小自适应
            writer.write0(data, sheet);
            writer.finish();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static List<Person> readPerson(String path) {
        List<List<String>> data = readList(path);
        if (data == null)
            return null;

        //遍历每行数据，如果符合CheckInRecord数据格式的，就转成CheckInRecord，并放到所属person中
        Map<String, Person> personMap = new HashMap<>(data.size() / 31 + 1);
        Person lastPerson = null;//如果遇到考勤统计条，需要知道上一个录入数据的Person是谁，然后把考勤统计条的数据放到该Person中
        String lastPersonMonthlyKey = null;//如果遇到考勤统计条，需要知道上一个录入数据的Person是谁，然后把考勤统计条的数据放到该Person对应的月份中
        for (int i = 0; i < data.size(); i++) {
            List<String> row = data.get(i);

            if (CheckInRecord.isCheckInRecord(row)) {//如果符合CheckInRecord数据格式
                if (personMap.get(row.get(CheckInRecordConstants.nameCol)) == null) {
                    Person person = new Person();
                    person.setId(row.get(CheckInRecordConstants.idCol));
                    person.setName(row.get(CheckInRecordConstants.nameCol));

                    personMap.put(person.getName(), person);
                }

                //把打卡记录放到所属Person对应月份的记录中
                Person person = personMap.get(row.get(CheckInRecordConstants.nameCol));
                CheckInRecord checkInRecord = CheckInRecord.listToRecord(row);

                String monthlyKey = Person.getMonthlyCheckInRecordKey(checkInRecord);
                if (person.getMonthlyCheckInRecords().get(monthlyKey) == null) {
                    person.getMonthlyCheckInRecords().put(monthlyKey, new MonthlyCheckInRecord());
                }
                person.getMonthlyCheckInRecords().get(monthlyKey).addCheckInRecord(checkInRecord);

                //搜索是否有“入职”二字，如果有，说明是当天入职，要记录到Person的属性中
                for (String str : row) {
                    if (str.contains("入职")) {
                        person.getMonthlyCheckInRecords().get(monthlyKey).setEntryDate(checkInRecord.getDay());
                        break;
                    }
                }
                lastPerson = person;//缓存当前录入数据的Person
                lastPersonMonthlyKey = monthlyKey;
            } else if (CheckInRecordStatisticsConstants.isCheckInRecordStatistics(row) && i + 1 < data.size()) {//如果是考勤统计条的格式，那就把下一行的数据装进Person中
                List<String> dataRow = data.get(i + 1);
                lastPerson.getMonthlyCheckInRecords().get(lastPersonMonthlyKey).setCheckInRecordStatistics(dataRow);
            }
        }

        List<Person> ret = new ArrayList<>(personMap.size());
        for (Map.Entry<String, Person> entry : personMap.entrySet()) {
            ret.add(entry.getValue());
        }

        personMap.clear();
        return ret;
    }

    public static void writePerson(String path, List<Person> data) {
        //list和records每行都是对应的
        List<List<String>> list = new ArrayList<>();
        List<Record> records = new ArrayList<>();


        int startRow = 2;//用于记录生成公式时需要知道的每个人的起始行(结束行可以根据起始行算）
        for (int i = 0; i < data.size(); i++) {
            Person person = data.get(i);

            //得到这个人每个月的打卡记录
            for (MonthlyCheckInRecord monthlyCheckInRecord : person.getMonthlyCheckInRecords().values()) {

                //得到这个月的每个打开记录
                for (CheckInRecord checkInRecord : monthlyCheckInRecord.getCheckInRecords()) {
                    //把这个月的记录拆分成List<String>和对应Record
                    ArrayList<String> row = new ArrayList<>();

                    //补齐到所需的最大列数，防止后面set的时候越界
                    while (row.size() <= Common.max(CheckInRecordConstants.idCol, CheckInRecordConstants.nameCol, CheckInRecordConstants.dateCol, CheckInRecordConstants.checkInTimesCol, CheckInRecordConstants.classNameCol, CheckInRecordConstants.lateOrEarlyCol)) {
                        row.add(null);
                    }
                    row.set(CheckInRecordConstants.idCol, person.getId());
                    row.set(CheckInRecordConstants.nameCol, person.getName());
                    row.set(CheckInRecordConstants.dateCol, checkInRecord.getDateString());
                    for (int j = 0; j < checkInRecord.getCheckInTimes().size(); j++) {
                        if (checkInRecord.getCheckInTimes().get(j).getTime() == -1) {//占位时间
                            row.set(j + CheckInRecordConstants.checkInTimesCol, null);
                        } else {
                            row.set(j + CheckInRecordConstants.checkInTimesCol, CheckInTime.toTimeString(checkInRecord.getCheckInTimes().get(j).getTime()));
                        }
                    }

                    row.set(CheckInRecordConstants.classNameCol, checkInRecord.getPairedClassName());//匹配的班次
                    if (checkInRecord.getLateOrEarly() != 0) { //有迟到早退
                        row.set(CheckInRecordConstants.lateOrEarlyCol, checkInRecord.getLateOrEarly() + "");//迟到早退的时间总和
                    }

                    list.add(row);
                    records.add(checkInRecord);
                }
                //在这个人每个月后面加考勤统计条和空行（共4行）
                int start = startRow;
                int end = start + monthlyCheckInRecord.getCheckInRecords().size() - 1;

                list.add(CheckInRecordStatisticsConstants.getTitle());//考勤统计条标题

                //考勤统计条内容（上月存息、存钟，其他地方是公式）
                if (monthlyCheckInRecord.getCheckInRecordStatistics().size() == 0) {//如果原来没有考勤统计条记录，就新建考勤统计条数据行
                    List statisticsContent = CheckInRecordStatisticsConstants.getContent(start, end, end + 2);//生成公式

                    if (monthlyCheckInRecord.getConfirmForm() != null) {//填入上月存息、存钟
                        statisticsContent.set(CheckInRecordStatisticsConstants.上月存息Col, monthlyCheckInRecord.getConfirmForm().get剩余年假());
                        statisticsContent.set(CheckInRecordStatisticsConstants.上月存钟Col, monthlyCheckInRecord.getConfirmForm().get本月存钟());
                    }
                    list.add(statisticsContent);

                } else //如果原来有考勤统计条记录，则把原来的写回去
                    list.add(monthlyCheckInRecord.getCheckInRecordStatistics());

                //后面两行是用来占位的空行
                ArrayList l3 = new ArrayList(1);//占位行
                l3.add(null);
                list.add(l3);
                ArrayList l4 = new ArrayList(1);//占位行
                l4.add(null);
                list.add(l4);


                //和考勤统计条对应，也要有4行Record，用DataRecord来设置样式
                DataRecord boldText = new DataRecord();
                checkin.pojo.CellStyle cellStyle = new checkin.pojo.CellStyle();
                cellStyle.setBold(true);
                boldText.setCellStyle(cellStyle);

                records.add(boldText);//空记录，由于占位，考勤统计条标题字体要加粗
                records.add(boldText);//空记录，由于占位，考勤统计条的公式行字体要加粗
                records.add(new DataRecord());//空记录，由于占位
                records.add(new DataRecord());//空记录，由于占位


                //下一个月或下一个人的起始位置
                startRow += monthlyCheckInRecord.getCheckInRecords().size() + 4;
            }

        }


        File file = new File(path);
        if (file.exists())//如果文件存在，则在文件名后加时间戳
            path = file.getParent() + File.separator + Common.getFileName(file.getName()) + System.currentTimeMillis() + "." + Common.getFileExt(file.getName());

        //开始写到文件
        try (OutputStream outputStream = new FileOutputStream(path)) {
            ExcelWriter writer = EasyExcelFactory.getWriterWithTempAndHandler(null, outputStream, ExcelTypeEnum.XLS, false, new MyWriteHandler(records));
            Sheet sheet = new Sheet(0, 0);
            //sheet.setAutoWidth(true);//单元格大小自适应，实测没有用

            writer.write0(list, sheet);
            writer.finish();

        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

    /**
     * 从人工处理好的打卡记录中的考勤统计条中解析出考勤确认表需要的数据
     * <p>
     * 考勤确认表的格式：
     * 部门 名字 剩余年假 本月存钟 漏打卡 全勤 备注(请假、迟到、计薪)
     */
    public static Map<String, List<List<String>>> getConfirmFormData(String path) {
        File file = new File(path);
        List<Person> data = ExcelHelper.readPerson(file.getAbsolutePath());

        //每个月的考勤确认表都放到不同的文件中，Map的key是文件名，value是对应的文件内容
        Map<String, List<List<String>>> ret = new HashMap<>(1);

        for (Person p : data) {
            for (MonthlyCheckInRecord monthlyCheckInRecord : p.getMonthlyCheckInRecords().values()) {

                //如果这个月没有打卡记录，就忽略他
                if (monthlyCheckInRecord.getCheckInRecords().size() == 0)
                    continue;

                if (monthlyCheckInRecord.getCheckInRecordStatistics().size() == 0) //如果这个月没有考勤统计条，就忽略他
                    continue;

                List<String> row = new ArrayList<>(7);
                while (row.size() < ConfirmFormConstant.maxCol)
                    row.add(null);

                row.set(ConfirmFormConstant.departmentCol, trim(Common.getFileName(file.getName())));//部门
                row.set(ConfirmFormConstant.nameCol, trim(p.getName()));//名字

                //剩余年假，如果不是数字或不等于0才输出
                if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.剩余年假Col))))
                    row.set(ConfirmFormConstant.剩余年假Col, "未知");//没填不要认为是0，而是要提示未知
                else if (!isNumeric(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.剩余年假Col).trim()) || Float.valueOf(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.剩余年假Col).trim()) != 0)
                    row.set(ConfirmFormConstant.剩余年假Col, monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.剩余年假Col).trim());
                else
                    row.set(ConfirmFormConstant.剩余年假Col, null);//注意空字符串和空单元格是不一样的，如果要使单元格空，要填null，如果是空字符串，公式运算时会出错

                //本月存钟
                if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.本月存钟Col))))
                    row.set(ConfirmFormConstant.本月存钟Col, "未知");
                else if (!isNumeric(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.本月存钟Col).trim()) || Float.valueOf(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.本月存钟Col).trim()) != 0)
                    row.set(ConfirmFormConstant.本月存钟Col, monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.本月存钟Col).trim());
                else
                    row.set(ConfirmFormConstant.本月存钟Col, null);

                //漏打卡
                if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.漏打卡Col))))
                    row.set(ConfirmFormConstant.漏打卡Col, "未知");
                else if (!isNumeric(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.漏打卡Col).trim()) || Float.valueOf(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.漏打卡Col).trim()) != 0)
                    row.set(ConfirmFormConstant.漏打卡Col, monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.漏打卡Col).trim());
                else
                    row.set(ConfirmFormConstant.漏打卡Col, null);

                //全勤
                if ("有".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.全勤Col))))
                    row.set(ConfirmFormConstant.全勤Col, "√");
                else if ("无".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.全勤Col))))
                    row.set(ConfirmFormConstant.全勤Col, null);
                else
                    row.set(ConfirmFormConstant.全勤Col, trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.全勤Col)));


                String remark = "";//备注，根据情况进行拼接
                //获取当月天数（用于计算有无全勤）
                CheckInRecord lastRecord = (CheckInRecord) monthlyCheckInRecord.getCheckInRecords().toArray()[monthlyCheckInRecord.getCheckInRecords().size() - 1];//按最后一个打卡记录的日期算当月天数
                int dayOfMonth = Common.getDaysByYearMonth(lastRecord.getYear(), lastRecord.getMonth());

                if (monthlyCheckInRecord.getEntryDate() > 0) {//如果是新入职
                    remark += monthlyCheckInRecord.getEntryDate() + "号入职";
                }

                if (isNumeric(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.请假Col)))) {//请假天数（可能有小数点，比如1.5天）
                    if (Float.valueOf(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.请假Col))) > 0) {
                        if (!remark.equals(""))
                            remark += "，";
                        remark += "请假" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.请假Col)) + "天";
                    }
                } else if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.请假Col)))) {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "请假未知";
                } else {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "请假：" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.请假Col));
                }

                if (isNumeric(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.迟到Col)))) {//迟到多少分钟
                    if (Float.valueOf(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.迟到Col))) > 0) {
                        if (!remark.equals(""))
                            remark += "，";
                        remark += "迟到" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.迟到Col)) + "分钟";
                    }
                } else if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.迟到Col)))) {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "迟到未知";
                } else {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "迟到：" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.迟到Col));
                }

                if (isNumeric(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.计薪Col)))) {
                    if (Float.valueOf(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.计薪Col))) < dayOfMonth || monthlyCheckInRecord.getEntryDate() == 1) {//如果记薪不等于当月天数 或者 等于当月天数但是是1号入职新入职的才输出计薪
                        if (!remark.equals(""))
                            remark += "，";
                        remark += "计薪" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.计薪Col)) + "天";
                    }
                } else if ("".equals(trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.计薪Col)))) {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "计薪未知";
                } else {
                    if (!remark.equals(""))
                        remark += "，";
                    remark += "迟到：" + trim(monthlyCheckInRecord.getCheckInRecordStatistics().get(CheckInRecordStatisticsConstants.计薪Col));
                }

                //注意空字符串和空单元格是不一样的，如果要使单元格空，要填null，如果是空字符串，公式运算时会出错
                if ("".equals(remark))
                    remark = null;

                row.set(ConfirmFormConstant.remarkCol, remark);//请假、迟到、计薪

                //根据年、月生成每个考勤确认表的名字
                String fileName = ConfirmFormConstant.generateFileName(monthlyCheckInRecord.getCheckInRecords().iterator().next().getYear(), monthlyCheckInRecord.getCheckInRecords().iterator().next().getMonth());
                if (ret.get(fileName) == null) {
                    List<List<String>> list = new ArrayList<>();
                    ret.put(fileName, list);
                }
                ret.get(fileName).add(row);
            }
        }
        return ret;
    }

    /**
     * @param path 考勤确认表的路径
     * @return key是人名，List<String> 0是上月存息，1是上月存钟
     */
    public static Map<String, ConfirmForm> readConfirmForm(String path) {
        List<List<String>> data = ExcelHelper.readList(path);
        Map<String, ConfirmForm> ret = new HashMap<>();

        for (List<String> list : data) {
            ConfirmForm confirmForm = new ConfirmForm();
            if (list.size() > ConfirmFormConstant.departmentCol)
                confirmForm.setDepartment(trim(list.get(ConfirmFormConstant.departmentCol)).equals("") ? null : trim(list.get(ConfirmFormConstant.departmentCol)));
            if (list.size() > ConfirmFormConstant.nameCol)
                confirmForm.setName(trim(list.get(ConfirmFormConstant.nameCol)).equals("") ? null : trim(list.get(ConfirmFormConstant.nameCol)));
            if (list.size() > ConfirmFormConstant.剩余年假Col)
                confirmForm.set剩余年假(trim(list.get(ConfirmFormConstant.剩余年假Col)).equals("") ? "0" : trim(list.get(ConfirmFormConstant.剩余年假Col)));
            if (list.size() > ConfirmFormConstant.本月存钟Col)
                confirmForm.set本月存钟(trim(list.get(ConfirmFormConstant.本月存钟Col)).equals("") ? "0" : trim(list.get(ConfirmFormConstant.本月存钟Col)));
            if (list.size() > ConfirmFormConstant.漏打卡Col)
                confirmForm.set漏打卡(trim(list.get(ConfirmFormConstant.漏打卡Col)).equals("") ? "无" : trim(list.get(ConfirmFormConstant.漏打卡Col)));
            if (list.size() > ConfirmFormConstant.全勤Col)
                confirmForm.set全勤(trim(list.get(ConfirmFormConstant.全勤Col)).equals("") ? null : trim(list.get(ConfirmFormConstant.全勤Col)));
            if (list.size() > ConfirmFormConstant.remarkCol)
                confirmForm.setRemark(trim(list.get(ConfirmFormConstant.remarkCol)).equals("") ? null : trim(list.get(ConfirmFormConstant.remarkCol)));
            ret.put(confirmForm.getName(), confirmForm);
        }

        return ret;
    }


    /**
     * <pre>
     * 写Excel时传入的是List<List<String>>数据，而要判断单元格样式（迟到早退标蓝，无匹配记录的打卡时间标灰）要Record对象
     * 需要把List<List<String>>和Record对应起来
     * </pre>
     * 数据格式：考勤号码 姓名 年月日 打卡时间（空格隔开）
     */
    static class MyWriteHandler implements WriteHandler {

        //表格样式
        private CellStyle blueText;//蓝色字体
        private CellStyle redText;//红色字体
        private CellStyle yellowBackground;//黄色填充背景
        private CellStyle garyBackground;//灰色填充背景
        private CellStyle boldText;//字体加粗样式

        private List<Record> records;

        private int rowNum;
        private int colNum;


        public MyWriteHandler(List<Record> records) {
            this.records = records;

        }

        @Override
        public void sheet(int i, org.apache.poi.ss.usermodel.Sheet sheet) {
            Workbook workbook = sheet.getWorkbook();

            blueText = workbook.createCellStyle();
            Font blueFont = workbook.createFont();
            blueFont.setColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            blueText.setFont(blueFont);

            redText = workbook.createCellStyle();
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            redText.setFont(redFont);

            yellowBackground = workbook.createCellStyle();
            yellowBackground.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            yellowBackground.setFillForegroundColor(IndexedColors.YELLOW.getIndex());

            garyBackground = workbook.createCellStyle();
            garyBackground.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            garyBackground.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

            boldText = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldText.setFont(boldFont);
        }

        //i从0开始，但是EasyExcel总是从第二行开始写数据（空出了第一行），所以找records的时候，要rowNum - 1
        @Override
        public void row(int i, Row row) {
            rowNum = i - 1;
        }

        //i从0开始，列号从3开始是打卡时间
        @Override
        public void cell(int i, Cell cell) {
            colNum = i;

            if (rowNum < 0 || records.get(rowNum) == null)
                return;

            String cellValue = cell.getStringCellValue();
            //把文本格式的数字转成正真的数字
            if (isNumeric(cellValue)) {
                cell.setCellValue(Float.valueOf(cellValue));
                cell.setCellType(CellType.NUMERIC);
            }
            //把文本格式的公式转成正真的公式
            if (cellValue != null && cellValue.startsWith("=")) {
                cell.setCellFormula(cellValue.substring(cellValue.indexOf("=") + 1));
            }

            if (records.get(rowNum) instanceof CheckInRecord) {//如果是打卡记录
                //找到对应的那一行记录的打卡时间
                List<CheckInTime> checkinTimes = ((CheckInRecord) records.get(rowNum)).getCheckInTimes();

                //列号从3开始是打卡时间，所以当列号大于3时开始找对应的打卡记录
                //因为我们总是把数据填充到第15、16列（班次、迟到早退时间），列数-3 总会超出打卡记录的长度，所以如果 列数-3 超出了checkinTimes的大小，就不要再找了，否则会越界
                if (colNum < CheckInRecordConstants.checkInTimesCol || checkinTimes.size() <= colNum - CheckInRecordConstants.checkInTimesCol) {
                    return;
                }

                //占位时间，也就是漏卡，填充黄色
                if (checkinTimes.get(colNum - CheckInRecordConstants.checkInTimesCol).getTime() == -1) {
                    //如果连续两次都是占位时间，则可能是直落或休半天，这时不需要填充黄色
                    //但如果连续三次及以上都是占位时间，则无法知道是直落漏卡还是休半天漏卡，这时就应该全部填充黄色
                    int placeHolderTimeCount = 1;

                    //搜索左边
                    int left = colNum - CheckInRecordConstants.checkInTimesCol; //colNum - 3是当前打卡点的位置
                    while (--left >= 0) {
                        if (checkinTimes.get(left).getTime() == -1)
                            placeHolderTimeCount++;
                    }

                    //搜索右边
                    int right = colNum - CheckInRecordConstants.checkInTimesCol; //colNum - 3是当前打卡点的位置
                    while (++right < checkinTimes.size()) {
                        if (checkinTimes.get(right).getTime() == -1)
                            placeHolderTimeCount++;
                    }

                    if (placeHolderTimeCount != 2)
                        cell.setCellStyle(yellowBackground);
                    return;

                }

                //如果没有匹配的班次，则把填充颜色标灰，然后返回
                if (checkinTimes.get(colNum - CheckInRecordConstants.checkInTimesCol).getPairedTime() == 0) {//没有匹配的班次
                    cell.setCellStyle(garyBackground);
                    return;
                }

                //如果有迟到早退，都把字体标蓝，如果没有迟到早退但是偏差超过30，则把字体标红
                int lateOrEarly = checkinTimes.get(colNum - CheckInRecordConstants.checkInTimesCol).isLateOrEarly();
                if (lateOrEarly > 0) { //早退
                    cell.setCellStyle(blueText);
                } else if (lateOrEarly < 0) { //迟到
                    cell.setCellStyle(blueText);
                } else if (Math.abs(checkinTimes.get(colNum - CheckInRecordConstants.checkInTimesCol).getDeviation()) >= 30) { //没有迟到早退但是偏差超过30则把字体标红
                    cell.setCellStyle(redText);
                }
            } else if (records.get(rowNum) instanceof DataRecord) {//如果是普通的数据记录
                //DataRecord dataRecord = (DataRecord) records.get(rowNum);
            }

            //如果需要样式
            if (records.get(rowNum).getCellStyle() != null) {
                if (records.get(rowNum).getCellStyle().isBold()) {
                    cell.setCellStyle(boldText);
                }
            }


        }
    }


}
