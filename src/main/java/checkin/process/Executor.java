package checkin.process;

import checkin.constants.ClassTimes;
import checkin.constants.ConfirmFormConstant;
import checkin.pojo.CheckInRecord;
import checkin.pojo.ConfirmForm;
import checkin.pojo.MonthlyCheckInRecord;
import checkin.pojo.Person;
import checkin.utils.ClassPairHelper;
import checkin.utils.Common;
import checkin.utils.ExcelHelper;

import java.io.File;
import java.util.*;

public class Executor {

    /**
     * 查找该文件夹中（包括子文件夹）的所有excel文档，对这些Excel文档进行班次匹配，要求文件或文件夹的命名必须按照一定的规则（和ClassTimes.classTime的key对应）
     * 匹配时会忽略子路径
     *
     * @param path 路径，应该是一个文件夹的路径；如果传入了一个文件的路径，则没有任何作用
     */
    public static void pairClassInDir(String path) {
        //获取目录下所有考勤确认表
        List<File> confirmFormFileList = Common.traverseFolder(path, new Common.Filter() {
            @Override
            public boolean filter(File file) {
                //除考勤确认表外的都排除掉
                if ((file.getName().endsWith(".xls") || file.getName().endsWith("xlsx")) && ConfirmFormConstant.isConfirmFormFileName(file.getName()))
                    return true;
                else
                    return false;
            }
        });

        //解析考勤确认表
        //key是不同月份对应不同考勤确认表的文件名（不带后缀），value这个map是文件内容，文件内容又以人名为key，对应数据为value
        Map<String, Map<String, ConfirmForm>> confirmForm = new HashMap<>();
        for (File file : confirmFormFileList) {
            if (confirmForm.get(Common.getFileName(file.getName())) == null) {//如果map中没有，则新建该key然后添加
                confirmForm.put(Common.getFileName(file.getName()), ExcelHelper.readConfirmForm(file.getAbsolutePath()));
            } else {//如果已有，则合并数据
                confirmForm.get(Common.getFileName(file.getName())).putAll(ExcelHelper.readConfirmForm(file.getAbsolutePath()));
            }
        }


        //获取目录下所有xls文件（不含文件夹），排除班次匹配文件夹和考勤确认表
        List<File> classFileList = Common.traverseFolder(path, new Common.Filter() {
            @Override
            public boolean filter(File file) {
                if (file.isDirectory() && file.getName().equals("班次匹配")) {//排除班次匹配文件夹
                    return false;
                } else if (file.isFile() && ConfirmFormConstant.isConfirmFormFileName(file.getName())) {//排除考勤确认表
                    return false;
                } else if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {//文件只要xls或xlsx格式
                    return true;
                } else if (file.isDirectory()) {//不排除文件夹
                    return true;
                } else {//其他的都不要
                    return false;
                }
            }
        });

        //如果path最后带文件分割符，则去掉
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.lastIndexOf(File.separator));
        }
        //进行班次匹配
        for (File file : classFileList) {
            //为了维持原来的目录结构，需要把文件分成基本路径和子路径
            String subDir = file.getAbsolutePath().substring(path.length());
            if (subDir.startsWith(File.separator))//去掉子路径最前面的路径分割符
                subDir = subDir.substring(1);

            pairClass(path, subDir, confirmForm, true);
        }

    }

    /**
     * 对单个Excel文档进行班次匹配，这时不会找考勤确认表，进行的是非全匹配
     *
     * @param path
     */
    public static void pairClassOfFile(String path) {
        File file = new File(path);
        if (file.isFile() && file.exists() && (file.getName().endsWith(".xls") || file.getName().endsWith("xlsx"))) {
            String baseDir = new File(file.getParent()).getAbsolutePath();
            String subDir = file.getName();
            if (baseDir.endsWith(File.separator)) {//如果最后带文件分割符，则去掉
                baseDir = baseDir.substring(0, path.lastIndexOf(File.separator));
            }

            pairClass(baseDir, subDir, null, false);
        }
    }


    /**
     * 班次匹配，在新建一个班次匹配的文件夹中存放匹配好的excel文档
     * 为了维持原来的目录结构，需要吧文件分成基本路径和子路径
     *
     * @param baseDir      基本路径（绝对路径，末尾不应该带文件分割符）
     * @param subDir       子路径（开头不应该带文件分割符，而且如果是Windows下运行的话，不应该以盘符开头），应该是一个Excel文档的子路径，且不应该是文件夹
     * @param confirmForm  基本路径下所有的考勤确认表
     * @param dirFullMatch 子路径是否需要全匹配，如果为true，目录名要对应ClassTimes的key得到map，下一级目录名还要对应上个map的key再得到一个map，以此类推
     *                     如果为false，则忽略文件夹的名字，只要文件名在ClassTimes.classTimes的key中（包括类型为Map<String,Map>的子map的key中）都可以（
     *                     由于key会按"、"分割，<del>如果班次名字有重复，后面会覆盖前面</del>）
     */
    private static void pairClass(String baseDir, String subDir, Map<String, Map<String, ConfirmForm>> confirmForm, boolean dirFullMatch) {
        //1.根据文件夹名和文件名查找可能的班次
        Map<String, List<Integer>> possibleClassTimes = new HashMap<>();
        File file = new File(baseDir + File.separator + subDir);


        if (dirFullMatch) {

            //1.遇到文件夹在ClassTimes的key中查找包含该文件夹名字的下一个map/list（按照规定的目录结构，如果下面还有文件夹则得到的是map
            // ，否则是一个包含打卡时间的list），如果还有文件夹，就再在上次找到的map的key中继续找包含该文件夹名字的记录，直到没有文件夹为止
            //2.遇到文件则在ClassTimes的key中找包含该文件名（去掉后缀）的记录

            //获取子路径下每层文件夹名字和最后的文件名
            //Windows下文件分割符为“\”如果直接split(File.separator)会报错，需要转义字符
            String[] dirs = subDir.split("\\".equals(File.separator) ? "\\\\" : File.separator);


            if (dirs.length > 1) {//如果需要匹配多层

                //1.找到第一层，方便后面搜索，按文件的路径和设定的班次父路径进行一层层对比，如果第一层就没有找到，则tempMap为null，后面就不会继续了，否则找到的可能是下一层的文件夹名或文件名
                Map tempMap = null;
                for (String key : ClassTimes.classTimes.keySet()) {
                    //可能多个 文件/文件夹 都对应一个班次，为了不需要重复写，key都用"、"分隔，按"、"分开后才是可能的文件名
                    String[] names = key.split("、");
                    for (String name : names) {
                        if (name.equals(Common.getFileName(dirs[0])) && !Common.isLastMap(ClassTimes.classTimes.get(key))) {
                            tempMap = ClassTimes.classTimes.get(key);
                            break;
                        }
                    }
                }


                //2.检测所有文件夹名字是否对应，到文件为止（不包含），dirs[length-1]就是文件名，如果文件的路径没有完整匹配班次设定的父路径，则tempMap再次会置空
                //这里要找到一个就break，因为路径是唯一指定的
                if (tempMap != null) {
                    for (int i = 1; i < dirs.length - 1; i++) {
                        boolean found = false;

                        for (Object key : tempMap.keySet()) {
                            //可能多个 文件/文件夹 都对应一个班次，为了不需要重复写，key都用"、"分隔，按"、"分开后才是可能的文件名
                            String[] names = key.toString().split("、");
                            for (String name : names) {
                                if (name.equals(Common.getFileName(dirs[i])) && !Common.isLastMap(ClassTimes.classTimes.get(key))) {
                                    tempMap = (Map) tempMap.get(key);
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (tempMap == null)//要是有匹配的班次名字，但该班次还没有设置打卡时间或没有子班次，就退出查找
                            break;
                        if (!found) {//如果这一层子目录没有匹配的map，要把上一层对应的tempMap置空，然后退出查找
                            tempMap = null;
                            break;
                        }
                    }
                }


                //3.找出班次名包含文件名的班次，加进possibleClassTimes中
                //一个文件名可以对应多个班次，所以找到一个不能直接break，要继续往后找
                if (tempMap != null) {
                    for (Object key : tempMap.keySet()) {
                        //可能多个文件都对应一个班次，为了不需要重复写，key都用"、"分隔，按"、"分开后才是可能的文件名
                        String[] names = key.toString().split("、");
                        for (String name : names) {
                            //除了要满足班次的文件名按"、"分割后和当前的文件名一样外，还必须是下一层就是Map<班次名,List<上下班时间>>
                            if (name.equals(Common.getFileName(file.getName())) && Common.isLastMap(tempMap.get(key))) {
                                possibleClassTimes.putAll((Map<String, List<Integer>>) tempMap.get(key));
                            }
                        }
                    }
                }

            } else {//如果当前文件就只有一个文件，没有指定的父路径

                for (String key : ClassTimes.classTimes.keySet()) {
                    String[] names = key.split("、");
                    for (String name : names) {
                        //除了要满足班次的文件名按"、"分割后和当前的文件名一样外，还必须是下一层就是Map<班次名,List<上下班时间>>
                        if (name.equals(Common.getFileName(dirs[0])) && Common.isLastMap(ClassTimes.classTimes.get(key))) {
                            possibleClassTimes.putAll(ClassTimes.classTimes.get(key));
                            break;
                        }
                    }
                }

            }

        } else {//如果只要求匹配文件名，不需要判断父文件夹名字是否吻合
            //展开ClassTimes.classTimes，把value为Map<String, List>的位置不变，把value为Map<String, Map>的都展开然后放到根map中
            Map<String, Map<String, List<Integer>>> tempClassTimes = ClassTimes.expandClassTimesMap();
            //根据文件名（去掉后缀）查找所有班次中包含该文件名的（如果有重复的班次名称，后面的会覆盖前面的）
            for (String key : tempClassTimes.keySet()) {
                //可能多个文件都对应一个班次，为了不需要重复写，key都用"、"分隔，按"、"分开后才是可能的文件名
                String[] names = key.split("、");
                for (String name : names) {
                    if (name.equals(Common.getFileName(file.getName()))) {
                        possibleClassTimes.putAll(tempClassTimes.get(key));
                    }
                }
            }
        }

        //如果没有可能的班次，就直接返回
        if (possibleClassTimes == null || possibleClassTimes.size() == 0)
            return;

        //2.读取excel文件
        List<Person> personList = ExcelHelper.readPerson(file.getAbsolutePath());

        //补充：缓存人名，用于构建排除列表，排除某人指定的班次
        for (Person p : personList)
            ClassTimes.tempPersonNameList.add(p.getName());

        //3.根据之前找到的可能的班次进行班次匹配
        List<Person> pairedPersonList = ClassPairHelper.pairClasses(personList, possibleClassTimes);

        //4.如果打卡记录的数量少于匹配的班次的打卡次数（也就是漏卡），把时间记录按匹配的班次顺序排列（也就是中间加空单元格）
        for (Person person : pairedPersonList) {
            for (MonthlyCheckInRecord monthlyCheckInRecord : person.getMonthlyCheckInRecords().values()) {
                for (CheckInRecord checkInRecord : monthlyCheckInRecord.getCheckInRecords()) {
                    checkInRecord.reorderCheckInTimes(true);
                }
            }
        }

        //5.检测是否有上月的存息存钟数据，如果有则加到Person的MonthlyCheckInRecord的对应属性中
        if (confirmForm != null) {
            for (Person p : pairedPersonList) {
                for (MonthlyCheckInRecord monthlyCheckInRecord : p.getMonthlyCheckInRecords().values()) {

                    //这个keySet是不同月份的考勤确认表的文件名
                    Set<String> fileNameKeySet = confirmForm.keySet();

                    //找上个月的考勤确认表
                    for (String fileName : fileNameKeySet) {
                        int[] array = ConfirmFormConstant.explainFileName(fileName);
                        if (array != null && array.length == 2) {//下标0是年，下标1是月
                            CheckInRecord record = monthlyCheckInRecord.getCheckInRecords().iterator().next();

                            if (array[0] == record.getYear() && array[1] == (record.getMonth() - 1)) {
                                //如果找到上个月的考勤确认表，再找是否有这个人的记录
                                if (confirmForm.get(fileName).get(p.getName()) != null) {
                                    monthlyCheckInRecord.setConfirmForm(confirmForm.get(fileName).get(p.getName())); //有的话就设置到MonthlyCheckInRecord的对应属性中
                                    break;
                                }
                            } else if ((record.getMonth() - 1) <= 0) {//跨年的时候
                                if (array[0] == (record.getYear() - 1) && array[1] == 12) {
                                    if (confirmForm.get(fileName).get(p.getName()) != null) {
                                        monthlyCheckInRecord.setConfirmForm(confirmForm.get(fileName).get(p.getName())); //有的话就设置到MonthlyCheckInRecord的对应属性中
                                        break;
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        //6.写回去（维持原有目录结构）
        if (pairedPersonList != null && pairedPersonList.size() > 0) {
            File outputDir = new File(baseDir + File.separator + "班次匹配" + File.separator + subDir);
            if (!outputDir.getParentFile().exists()) {
                outputDir.getParentFile().mkdirs();
            }
            ExcelHelper.writePerson(outputDir.getAbsolutePath(), pairedPersonList);
        }
    }


    /**
     * 查找文件夹下所有Excel文档并生成考勤确认表
     *
     * @param path 文件夹的路径
     */
    public static void generateConfirmFormInDir(String path) {
        generateConfirmForm(path, null);
    }

    /**
     * 对指定的文件生成考勤确认表
     *
     * @param path     生成考勤确认表的路径
     * @param fileList 指定的文件列表
     */
    public static void generateConfirmFormOfFileList(String path, List<File> fileList) {
        if (fileList == null)
            return;

        List<File> tempFileList = new ArrayList<>();
        for (File file : fileList) {//筛选出xls或xlsx格式的文件
            if (file.getName().endsWith("xls") || file.getName().endsWith("xlsx"))
                tempFileList.add(file);
        }

        if (tempFileList.size() == 0)
            return;

        generateConfirmForm(path, tempFileList);
    }

    /**
     * 生成考勤确认表，可以指定路径，也可以指定文件；列表，生成的考勤确认表全部都放在指定的path下
     *
     * @param path     文件夹的路径，如果fileList为null，则遍历该路径下所有Excel文档（包括子文件夹下的）并在该路径下生成考勤确认表
     * @param fileList 指定的文件列表，如果不为null，则会覆盖path下查找到所有Excel文档，此时path的作用仅做指定生成考勤确认表的路径
     */
    private static void generateConfirmForm(String path, List<File> fileList) {
        //拿到路径下所有Excel文件（排除考勤确认表本身）
        List<File> myFileList = Common.traverseFolder(path, new Common.Filter() {
            @Override
            public boolean filter(File file) {
                boolean ret = false;
                if (file.isFile() && ConfirmFormConstant.isConfirmFormFileName(file.getName())) {//排除考勤确认表本身
                    ret = false;
                } else if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {//文件只要xls或xlsx格式
                    ret = true;
                } else if (file.isDirectory()) {//不排除文件夹
                    ret = true;
                } else {//其他的都不要
                    ret = false;
                }
                return ret;
            }
        });

        if (fileList != null)
            myFileList = fileList;
        //解析这些文件的考勤统计条，并生成考勤确认表
        //考勤确认表按不同月份分开成不同文件存放，key是每个月的考勤确认表的名字，value是对应的文件内容
        Map<String, List<List<String>>> data = new HashMap<>();
        for (File file : myFileList) {
            Map<String, List<List<String>>> rets = ExcelHelper.getConfirmFormData(file.getAbsolutePath());

            for (String key : rets.keySet()) {
                if (data.get(key) == null) {
                    data.put(key, rets.get(key));
                } else {
                    data.get(key).addAll(rets.get(key));
                }
            }
        }

        //按不同月份写到不同的文件
        if (data.size() > 0) {
            for (String fileName : data.keySet()) {
                File outputFile = new File(path + File.separator + fileName + ".xls");
                ExcelHelper.writeList(outputFile.getAbsolutePath(), data.get(fileName));
            }
        }
    }


    /**
     * 按一定的格式解析出用户配置的打卡时间
     *
     * @param content 打卡时间文件的内容
     */
    public static void parseClassTimes(String content) {
        content = Common.cleanupString(content);//去掉不可见字符，并把换行符统一成"\n"
        String[] array = content.split("\n");//得到每一行

        Map<String, Map> classTimes = new HashMap<>(array.length);
        for (String line : array) {//解析每一行，并加到classTimes中
            if ("".equals(line) || Common.trim(line).startsWith(ClassTimes.annotationFlag))//跳过空行和注释行
                continue;
            //防止ArrayIndexOutOfBoundsException，毕竟不知道用户怎么写的
            try {
                Map<String, Map> currentMap = classTimes;
                String[] folderNames = line.split("->");//如果folderNames.length>1的话，folderNames[0] 到 folderNames[folderNames.length-2] 是文件夹
                String[] departmentNames = folderNames[folderNames.length - 1].split("=");//departmentNames[0]是文件名称
                String[] classNames = departmentNames[1].split(">");//classNames[0]是班次名称
                String[] times = classNames[1].split(",");//每个打卡时间，比如6:00，还要转成分钟

                if (folderNames.length > 1) {//有文件夹
                    for (int i = 0; i < folderNames.length - 1; i++) {
                        if (currentMap.get(folderNames[i]) == null) {
                            Map<String, Map> tempMap = new HashMap<>();
                            currentMap.put(folderNames[i], tempMap);
                            currentMap = tempMap;
                        } else {
                            currentMap = currentMap.get(folderNames[i]);
                        }
                    }
                }
                Map departmentMap = currentMap.get(Common.trim(departmentNames[0]));
                if (departmentMap == null) {
                    departmentMap = new HashMap();
                    currentMap.put(Common.trim(departmentNames[0]), departmentMap);
                }
                List<Integer> timeList = new ArrayList<>(4);
                for (String time : times) {//如果时间中不含英文冒号，且不是占位时间，就会被忽略，如果某个时间写成了中文冒号，可能导致迟到早退和早来晚走标记反
                    if (ClassTimes.placeHolderTime.equals(time)) {//占位时间
                        timeList.add(-1);
                    } else if (time.contains(":")) {//HH:mm的时间格式转成分钟数
                        //转成分钟并加进list
                        String[] t = time.split(":");
                        timeList.add(Integer.parseInt(Common.trim(t[0])) * 60 + Integer.parseInt(Common.trim(t[1])));
                    }
                }
                departmentMap.put(Common.trim(classNames[0]), timeList);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        //配置打卡时间
        if (classTimes != null && classTimes.size() > 0) {
            ClassTimes.classTimes = classTimes;
        }
    }

}
