package checkin.utils;

import checkin.pojo.CheckInRecord;
import checkin.pojo.CheckInTime;
import checkin.pojo.MonthlyCheckInRecord;
import checkin.pojo.Person;

import java.util.*;

public final class ClassPairHelper {


    /**
     * 传入一组以Person为单位的CheckInRecord，为每条CheckInRecord在可能的班次中匹配出最合适的班次，放到每个CheckInRecord的pairedClass字段中，并记录该匹配的班次的迟到早退时间总和
     *
     * @param personList 以Person为单位的打卡记录
     * @param classTimes 可能的班次
     * @return 匹配好班次的记录
     */
    public static List<Person> pairClasses(List<Person> personList, Map<String, List<Integer>> classTimes) {
        List<String> peopleNameList = new ArrayList<>(personList.size());
        for (Person person : personList) {
            peopleNameList.add(person.getName());
        }

        for (Person person : personList) {
            Map<String, MonthlyCheckInRecord> monthlyCheckInRecords = person.getMonthlyCheckInRecords();
            person.clearMonthlyCheckInRecords();
            Set<String> keySet = monthlyCheckInRecords.keySet();
            for (String key : keySet) {
                MonthlyCheckInRecord monthlyCheckInRecord = monthlyCheckInRecords.get(key);

                MonthlyCheckInRecord pairedMonthlyCheckInRecord = new MonthlyCheckInRecord();
                for (CheckInRecord checkInRecord : monthlyCheckInRecord.getCheckInRecords()) {
                    if (checkInRecord.getCheckInTimes().size() == 0) //没有打卡记录则不需要进行班次匹配
                        pairedMonthlyCheckInRecord.addCheckInRecord(checkInRecord);
                    else //否则进行班次匹配
                        pairedMonthlyCheckInRecord.addCheckInRecord(recordPairClasses(person.getName(), checkInRecord, classTimes, peopleNameList));
                }
                person.addMonthlyCheckInRecords(pairedMonthlyCheckInRecord);

            }
        }
        return personList;
    }

    /**
     * 一条record有多个可能的班次，找出其中最匹配的那个
     * 对于有加补钟或休半天的情况，判断不是很准
     *
     * @param name          人的名字，如果某些班次的名字包含了某人的全名，则认为是那个人定制的班，只匹配这些班
     * @param checkInRecord 一条打卡记录，一天所有卡算作一条打卡记录（外加考勤号码、姓名、当天的日期等信息）
     * @param classTimes    该人所属部门的所有班次，即可能会匹配到的班次
     * @param people        人名集合，如果某班次包含其他人全名，但不包含自己的全名，说明是其他人定制的班次，则排除该班次
     * @return 设置好匹配班次和偏差总和的record，和原来的record不一定是同一个对象
     */
    private static CheckInRecord recordPairClasses(String name, CheckInRecord checkInRecord, Map<String, List<Integer>> classTimes, List<String> people) {
        int MAX_INT = Integer.MAX_VALUE / 3;
        //1.对班次进行筛选，找出用户指定的班次，或自己定制的班次，排除别人定制的班次
        //2.进行匹配，每个可能的班次和用户的每条打卡记录进行分析，得出每个的偏差，以偏差作为参考，加以一定的规则，得出最匹配的班次

        //-------------------------------1.筛选部分-------------------------------
        //如果有用户指定了班次，则只匹配指定的班次
        Map<String, List<Integer>> preferClassTimes = new HashMap<>(0);
        for (String className : classTimes.keySet()) {
            if (checkInRecord.getPreferClassName() != null && !checkInRecord.getPreferClassName().trim().equals("") && className.contains(checkInRecord.getPreferClassName())) {
                preferClassTimes.put(className, classTimes.get(className));
            }
        }
        if (preferClassTimes.size() > 0) {//用户指定了班次，且该班次存在
            classTimes = preferClassTimes;
        }

        //如果某班次包含其他人全名，但不包含自己的全名，说明是其他人定制的班次，则排除该班次（不能直接修改classTimes，否则对于其他人来说可能的班次也会被修改，应该新建一个集合）
        if (preferClassTimes.size() == 0) {//但是如果用户指定要使用别人的班次，就不用排除，只有用户没有指定或指定的班次不存在时才进行排除
            if (people != null) {
                Map<String, List<Integer>> tempClassTimes = new HashMap<>();
                Set<String> keySet = classTimes.keySet();
                for (String className : keySet) {
                    boolean use = true;
                    for (String personName : people) {
                        if (!personName.equals(name) && className.contains(personName) && !className.contains(name)) {
                            use = false;
                            break;
                        }
                    }
                    if (use)
                        tempClassTimes.put(className, classTimes.get(className));
                }
                classTimes = tempClassTimes;
            }
        }

        if (preferClassTimes.size() == 0) {//如果没有指定班次，且一个或多个班次名字包含了自己的全名，则认为是自己定制的班，只匹配这些班，不再检测其他班次的匹配程度
            Map<String, List<Integer>> customizedClassTimes = new HashMap<>(0);
            for (String className : classTimes.keySet()) {
                if (name != null && !name.trim().equals("") && className.contains(name)) {
                    customizedClassTimes.put(className, classTimes.get(className));
                }
            }
            if (customizedClassTimes.size() > 0) {
                classTimes = customizedClassTimes;
            }
        }

        //-------------------------------2.匹配部分-------------------------------
        CheckInRecord pairedCheckInRecord = null;//当前最匹配的班次
        int deviation = 0;//当前最匹配的班次的偏差（迟到+早退绝对值之和）
        int pairedClassCheckInCount = 0;//当前最匹配的班次的真正的上下班点个数（因为要忽略占位时间，所以pairedCheckInRecord.size()不是真正的个数，真正的个数还要另外计算）
        int pairedClassCountDeviation = MAX_INT;//匹配的打卡记录数和当前最匹配的班次的上下班点的个数之差，用于寻找打卡次数最接近的班

        //第一次匹配，优先匹配没有迟到早退的班且有匹配记录的打卡点更多的班
        //对于有加补钟的情况效果不理想
        for (Map.Entry<String, List<Integer>> entry : classTimes.entrySet()) {
            CheckInRecord tmpCheckInRecord = checkInRecord.clone();
            int tmpDeviation = recordPairClass(tmpCheckInRecord, entry.getValue());//比较前应清空pairedClass和checkinTimes的pairedTime和deviation（最好clone一份新的）
            int tmpPairedClassCheckInCount = 0;//和上下班点匹配的打卡点有几个，要忽略为-1的占位时间
            int tmpPairedClassCheckInCountDeviation = 0;//匹配的打卡点数和上下班点的个数之差，用于寻找打卡次数最接近的班

            for (CheckInTime checkInTime : tmpCheckInRecord.getCheckInTimes()) {//当前匹配的打卡记录的个数
                if (checkInTime.getPairedTime() > 0)
                    tmpPairedClassCheckInCount++;
            }
            //当前班次有多少个打卡点
            int currentRealCheckInCount = 0;
            for (int minutes : entry.getValue()) {
                if (minutes != -1) //忽略占位时间
                    currentRealCheckInCount++;
            }
            if (currentRealCheckInCount == 0) {//要是用户没有给班次设置上下班点或全设置成占位时间，则一定不匹配该班次，设置为最大最后偏差减出来就是最大的
                currentRealCheckInCount = MAX_INT;
            }
            tmpPairedClassCheckInCountDeviation = Math.abs(currentRealCheckInCount - tmpPairedClassCheckInCount);//匹配的班次数和上下班点的个数之差

            //如果找到一个没有迟到早退的班，但是已经匹配到过没有迟到早退的班（即匹配其他班也没有迟到早退）:
            //1.取匹配的打卡点数和上下班点个数最接近的班（排除漏卡和打卡次数超过上下班点个数的情况）
            //2.如果匹配数一样（可能是当前班次在其他班次的基础上增加了上下班点，也可能是恰好一样），取上下班点个数多的那个
            //3.如果匹配数一样，且上下班点个数都一样，再取偏差最小的那个
            if (tmpCheckInRecord.getLateOrEarly() == 0 && pairedCheckInRecord != null) {
                //1.找匹配的打卡点数和上下班点个数最接近的班（排除漏卡和打卡次数超过上下班点个数的情况）
                if (tmpPairedClassCheckInCountDeviation < pairedClassCountDeviation) {
                    deviation = tmpDeviation;
                    tmpCheckInRecord.setPairedClassName(entry.getKey());//设置最匹配班次的名字
                    tmpCheckInRecord.setPairedClassTimes(entry.getValue());//匹配的班次的上下班点
                    pairedCheckInRecord = tmpCheckInRecord;
                    pairedClassCheckInCount = tmpPairedClassCheckInCount;
                    pairedClassCountDeviation = tmpPairedClassCheckInCountDeviation;
                }
                //2.如果匹配数一样（可能是当前班次在其他班次的基础上增加了上下班点，也可能是恰好一样），取上下班点个数多的那个
                else if (tmpPairedClassCheckInCount > pairedClassCheckInCount) {
                    deviation = tmpDeviation;
                    tmpCheckInRecord.setPairedClassName(entry.getKey());//设置最匹配班次的名字
                    tmpCheckInRecord.setPairedClassTimes(entry.getValue());//匹配的班次的上下班点
                    pairedCheckInRecord = tmpCheckInRecord;
                    pairedClassCheckInCount = tmpPairedClassCheckInCount;
                    pairedClassCountDeviation = tmpPairedClassCheckInCountDeviation;
                }
                //3.如果匹配数一样，且上下班点个数都一样，再取偏差最小的那个
                else if (tmpPairedClassCheckInCountDeviation == pairedClassCountDeviation && tmpPairedClassCheckInCount == pairedClassCheckInCount && tmpDeviation < deviation) {
                    deviation = tmpDeviation;
                    tmpCheckInRecord.setPairedClassName(entry.getKey());//设置最匹配班次的名字
                    tmpCheckInRecord.setPairedClassTimes(entry.getValue());//匹配的班次的上下班点
                    pairedCheckInRecord = tmpCheckInRecord;
                    pairedClassCheckInCount = tmpPairedClassCheckInCount;
                    pairedClassCountDeviation = tmpPairedClassCheckInCountDeviation;
                }
            }

            //如果找到一个没有迟到早退的班，而且没有匹配过其他班，就直接记录下来
            if (tmpCheckInRecord.getLateOrEarly() == 0 && pairedCheckInRecord == null) {
                pairedCheckInRecord = tmpCheckInRecord;
                tmpCheckInRecord.setPairedClassName(entry.getKey());//设置最匹配班次的名字
                tmpCheckInRecord.setPairedClassTimes(entry.getValue());//匹配的班次的上下班点
                deviation = tmpDeviation;
                pairedClassCheckInCount = tmpPairedClassCheckInCount;
                pairedClassCountDeviation = tmpPairedClassCheckInCountDeviation;
            }
        }

        //如果没有找到一个没有迟到早退且匹配记录最多的班，再进行第二次匹配
        //为弥补第一次匹配对于加补钟时无法准确判断，如果匹配的班次偏差太大，则也进行第二次匹配（取偏差最小的班）
        if (pairedCheckInRecord == null || deviation > 100) {
            //第二次匹配，如果所有班次都迟到早退的话，再匹配偏差最小的班
            for (Map.Entry<String, List<Integer>> entry : classTimes.entrySet()) {
                CheckInRecord tmpCheckInRecord = checkInRecord.clone();
                int tmpDeviation = recordPairClass(tmpCheckInRecord, entry.getValue());//比较前应清空pairedClass和checkinTimes的pairedTime和deviation（最好clone一份新的）

                //取偏差最小的那个，记录下来
                if (deviation == 0 || tmpDeviation < deviation) {
                    deviation = tmpDeviation;
                    tmpCheckInRecord.setPairedClassName(entry.getKey());//设置最匹配班次的名字
                    tmpCheckInRecord.setPairedClassTimes(entry.getValue());//匹配的班次的上下班点
                    pairedCheckInRecord = tmpCheckInRecord;
                }
            }
        }
        return pairedCheckInRecord;
    }

    /**
     * 班次匹配算法，得出每个打卡时间和对于上下班点的对应关系
     * //TODO 如果有多打卡而且还漏卡的情况，无法判断出来，只能按迟到早退算
     * 所有时间传入前应转成分钟，得到的是一个int值
     * <p>
     * 用第一个可能的班次的第一个点和record的每一个打卡点作差，得出偏差，
     * 再用第一个可能的班次的第二个点和record的每一个打卡点作差，得出偏差，record的该点取偏差最小的最为匹配的班次
     * 最后得出每个打卡点和该班次最匹配的点（就是把上下班时间对应起来）
     * 考虑到漏卡或多打卡的情况，最后还需要检查：是否有匹配的班次重复的现象，然后把偏差较大的匹配班次和偏差置空
     * </p>
     *
     * @param checkInRecord 打卡记录，可能会有休息(补钟)、多打、漏打、早来早走、不按班次上班的情况，长度不定
     * @param classTimes    一个可能的上班时间点，可能是两个点，可能是四个点
     * @return 该班次的所有偏差之和，结果为正数（打卡时间和对应上下班点偏差的绝对值之和）
     */
    private static int recordPairClass(CheckInRecord checkInRecord, List<Integer> classTimes) {
        List<CheckInTime> checkinTimes = checkInRecord.getCheckInTimes();//打卡记录

        for (int i = 0; i < classTimes.size(); i++) {
            int classTime = classTimes.get(i);//上班下班点
            if (classTime == -1) {//占位时间
                continue;
            }

            //上下班点和每一个打卡记录作差，得到偏差，然后取偏差最小的作为匹配的上下班点
            for (CheckInTime checkInTime : checkinTimes) {
                int deviation = classTime - checkInTime.getTime();//偏差保留符号，用于判断有没有迟到早退

                //没有匹配过或当前偏差的绝对值更小，则设置为匹配当前班次
                if ((checkInTime.getPairedTime() == 0 && checkInTime.getDeviation() == 0) || Math.abs(deviation) < Math.abs(checkInTime.getDeviation())) {
                    checkInTime.setPairedTime(i + 1);//从1开始
                    checkInTime.setDeviation(deviation);
                }
            }
        }

        //查重，避免多打卡时，班次被重复匹配
        for (int i = 0; i < checkinTimes.size(); i++) {
            for (int j = i + 1; j < checkinTimes.size(); j++) {
                //如果匹配的班次有重复，把偏差较大者匹配的班次和偏差清空
                if (checkinTimes.get(i).getPairedTime() != 0 && checkinTimes.get(i).getPairedTime() == checkinTimes.get(j).getPairedTime()) {

                    //1.一个有迟到早退一个没有的话，优先取没有迟到早退的打卡点
                    if (checkinTimes.get(i).isLateOrEarly() == 0 && checkinTimes.get(j).isLateOrEarly() != 0) {//i没有迟到早退，j有迟到早退，取i，清空j
                        checkinTimes.get(j).setPairedTime(0);
                        checkinTimes.get(j).setDeviation(0);
                        continue;
                    } else if (checkinTimes.get(i).isLateOrEarly() != 0 && checkinTimes.get(j).isLateOrEarly() == 0) {//i有迟到早退，j没有迟到早退，取j，清空i
                        checkinTimes.get(i).setPairedTime(0);
                        checkinTimes.get(i).setDeviation(0);
                        continue;
                    }


                    //2.如果都是迟到早退，取迟到早退较少的那个打卡点（其实就是偏差较小者）
                    //3.如果都没有迟到早退，取偏差较小者
                    //上述2和3的情况可以总结为取偏差较小者即可（清空较大者）
                    if (Math.abs(checkinTimes.get(i).getDeviation()) > Math.abs(checkinTimes.get(j).getDeviation())) {
                        checkinTimes.get(i).setPairedTime(0);
                        checkinTimes.get(i).setDeviation(0);
                    } else if (Math.abs(checkinTimes.get(i).getDeviation()) < Math.abs(checkinTimes.get(j).getDeviation())) {
                        checkinTimes.get(j).setPairedTime(0);
                        checkinTimes.get(j).setDeviation(0);
                    }

                    //4.如果都有迟到早退，而且偏差一样（比如上9点、下11点，而他10点整走了,中午12点又打一次卡，或者下1点、上3点，而他2点过来，4点又打一次卡），要判断前一个班是否有匹配的打卡记录，如果没有，就归到前一个班，否则归到下一个班
                    if (Math.abs(checkinTimes.get(i).getDeviation()) == Math.abs(checkinTimes.get(j).getDeviation())) {
                        //TODO 这种情况不多，可以简单地归到上一个打卡点，到时候再人工处理
                        checkinTimes.get(i).setPairedTime(0);
                        checkinTimes.get(i).setDeviation(0);

                    }
                }

            }
        }
        //统计是否有迟到早退，计算所有迟到早退的时间之和
        for (CheckInTime checkinTime : checkinTimes) {
            if (checkinTime.getPairedTime() != 0 && checkinTime.getPairedTime() % 2 == 1) {//匹配的班次从1开始算，所以模2为1才是上班点
                //上班时间偏差应该为正才没有迟到，否则就要加到迟到早退时间里去
                if (checkinTime.getDeviation() < 0)
                    checkInRecord.setLateOrEarly(checkInRecord.getLateOrEarly() + Math.abs(checkinTime.getDeviation()));
            } else {
                //下班时间偏差应该为负才没有早退，否则应该加到迟到早退时间里去
                if (checkinTime.getDeviation() > 0)
                    checkInRecord.setLateOrEarly(checkInRecord.getLateOrEarly() + Math.abs(checkinTime.getDeviation()));
            }
        }

        //计算偏差之和
        int ret = 0;
        for (CheckInTime checkInTime : checkinTimes) {
            ret += Math.abs(checkInTime.getDeviation());
        }
        return ret;
    }
}
