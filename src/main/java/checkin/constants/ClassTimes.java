package checkin.constants;

import java.util.*;

public class ClassTimes {


    public static Map<String, Map> classTimes;//从文件中解析出来的上下班点放在这里
    public static List<String> tempPersonNameList = new ArrayList<>(0);//存储所有读取过的人名，用于排除某人特定的班次
    public static final String annotationFlag = "#";//注释的标识符，如果某行以它开头，则不进行解析
    public static final String placeHolderTime = "-1";//文件中占位时间的标志约定为-1，解析成List<Integer>后的对应位置也是-1

    /**
     * 文件把文件名（去掉后缀）作为key找classTimes，value就是可能的班次
     * 文件夹把文件夹名作为key找classTimes得到一个Map，然后再找key中包含该文件名（去掉后缀）的（因为存在多个文件名对应一个班次的情况），对应的value就是可能的班次
     *
     * 如果多个文件（不含文件夹）都对应同一个班次，则在key中用"、"分割各文件名
     *
     *
     * 例如：
     * 不含文件夹classTime为Map<String, Map<String, List<int>>>，分别为Map<文件名, Map<班次名, List<上下班点>>>
     * 含文件夹classTime为Map<String, Map<String, Map<String, List<int>>>>，分别为Map<文件夹名, Map<文件名, Map<班次名, List<上下班点>>>>
     * 含多个文件夹classTime为Map<String, Map<String, Map<String, Map<String, List<int>>>>>，分别为Map<文件夹名, Map<文件夹名, Map<文件名, Map<班次名, List<上下班点>>>>>
     *  ...
     *
     *  其中文件/文件夹名的key会按中文顿号"、"进行split，split后每个值只要和文件名完全匹配，该文件/文件夹就会从这里找（找到一个全匹配的后就不再往后找了）
     *  班次名的key会按中文顿号"、"进行split，split后每个值只要包含某人的全名，这个人就会按这些包含他名字的班次进行匹配，而不再匹配其他班次
     *
     *  上下班点用英文逗号","进行split后，再按英文冒号":"进行split。如果按英文逗号split后时间既不是占位时间，也不含英文冒号，则会忽略该
     *  时间，不会影响迟到早退和早来晚走的判断（占位时间会影响迟到早退和早来晚走的判断）
     *
     *  Executor.pairClass的dirFullMatch参数决定了子文件夹名是否也必须匹配，如果为true，要求子文件夹名要按和key对应，且层级结构也要对应
     *  如果为false，则会对classTime进行展开，最后会展开成Map<文件名, Map<班次名, List<上下班点>>>的形式，文件夹（包含子文件夹）内所有文
     *  件的文件名只要对应classTime的倒数第二层map的key即可，而不再判断路径
     *
     * */
    static {
        classTimes = new HashMap();

/*      //加载到内存中数据格式长这样：
        //不含文件夹
        Map map1 = new HashMap();
        map1.put("A班", Arrays.asList(7 * 60 + 30, 14 * 60 + 30, 18 * 60 + 30, 20 * 60 + 30));
        map1.put("B班", Arrays.asList(8 * 60 + 30, 14 * 60 + 30, 18 * 60 + 0, 21 * 60 + 0));
        map1.put("张三班、李四班", Arrays.asList(9 * 60 + 00, 15 * 60 + 00, 18 * 60 + 0, 21 * 60 + 0));
        classTimes.put("部门1", map1);

        //含文件夹（多层文件夹同理）
        Map folder1 = new HashMap();
        Map map2 = new HashMap();
        map2.put("A班", Arrays.asList(9 * 60 + 00, 18 * 60 + 00));
        map2.put("B班", Arrays.asList(8 * 60 + 30, 14 * 60 + 30, 18 * 60 + 0, 21 * 60 + 0));
        folder1.put("文件名（部门名）", map2);
        classTimes.put("文件夹名1、文件夹名2", folder1);*/
    }

    /**
     * 展开ClassTimes.classTimes，把value为Map<String, List>的位置不变，把value为Map<String, Map>的都展开然后放到根map中
     * 最后展开成Map<文件名,Map<班次名,List<上下班点>>>的形式
     *
     * @return 展开后的集合，格式为Map<文件名,Map<班次名,List<上下班点>>>
     */
    public static Map<String, Map<String, List<Integer>>> expandClassTimesMap() {
        Map<String, Map<String, List<Integer>>> tempClassTimes = new HashMap<>();
        for (String key : ClassTimes.classTimes.keySet()) {
            Map value = ClassTimes.classTimes.get(key);
            if (value.values().iterator().next() instanceof List) {//Map<文件名,Map<班次名,List<上下班点>>>的形式
                tempClassTimes.put(key, (Map<String, List<Integer>>) ClassTimes.classTimes.get(key));
            } else if (value.values().iterator().next() instanceof Map) {//子Map是 Map<文件夹名,Map<文件名,Map<班次名,List<上下班点>>>>（包括多层文件夹名）的形式，查找所有子文件夹，并展开
                LinkedList<Map<String, Map>> tempList = new LinkedList<>();//用栈去递归
                tempList.push(value);

                while (!tempList.isEmpty()) {
                    Map<String, Map> map = tempList.removeFirst();
                    for (String key1 : map.keySet()) {
                        Map value1 = map.get(key1);
                        if (value1.values().iterator().next() instanceof List) {
                            tempClassTimes.put(key1, (Map<String, List<Integer>>) map.get(key1));
                        } else if (value1.values().iterator().next() instanceof Map) {
                            tempList.push(value1);
                        }
                    }
                }
            }
        }
        return tempClassTimes;
    }
}
