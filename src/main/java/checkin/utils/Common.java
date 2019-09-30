package checkin.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.*;

public final class Common {
    public static boolean isNumeric(String s) {

        if (s != null && !"".equals(s.trim()))
            return s.matches("-?[0-9]*(\\.?)[0-9]*");
        else
            return false;
    }


    //根据 年、月 获取对应的月份 的 天数
    public static int getDaysByYearMonth(int year, int month) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        int maxDate = a.get(Calendar.DATE);
        return maxDate;
    }

    public static int max(int... arr) {
        if (arr.length == 0)
            throw new IllegalArgumentException("数组长度为0");
        int ret = arr[0];
        for (int num : arr)
            if (num > ret)
                ret = num;
        return ret;
    }

    //去掉文件名的后缀，返回真正的文件名
    public static String getFileName(String fileName) {
        int index = fileName.lastIndexOf(".");

        if (index == -1) //如果没有后缀，则直接返回
            return fileName;
        else
            return fileName.substring(0, index);
    }

    //获取文件的后缀，不包含点(".")
    public static String getFileExt(String fileName) {
        int index = fileName.lastIndexOf(".");

        if (index == -1) //如果没有后缀，则返回空字符串
            return "";
        else
            return fileName.substring(index + 1);
    }

    public static byte[] readFile(String path) {
        File file = new File(path);
        byte[] ret = new byte[0];

        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file); ByteArrayOutputStream byteArray = new ByteArrayOutputStream()) {
                byte[] buf = new byte[200];
                int len = 0;
                while ((len = inputStream.read(buf)) != -1) {
                    byteArray.write(buf, 0, len);
                }
                byteArray.flush();
                ret = byteArray.toByteArray();
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
        return ret;
    }

    public static String trim(String str) {
        if (str == null)
            return "";
        else
            return str.trim();
    }

    /**
     * 去掉Windows记事本的BOM标识符，去掉不可见字符，并把换行符统一成"\n"
     *
     * @param str
     * @return
     */
    public static String cleanupString(String str) {
        //1.去掉Windows记事本的BOM标识符
        byte[] data = str.getBytes();
        int unread = 0;
        if (data.length >= 4 && (data[0] == (byte) 0x00) && (data[1] == (byte) 0x00) && (data[2] == (byte) 0xFE) && (data[3] == (byte) 0xFF)) { //UTF-32BE
            unread = 4;
        } else if (data.length >= 4 && (data[0] == (byte) 0xFF) && (data[1] == (byte) 0xFE) && (data[2] == (byte) 0x00) && (data[3] == (byte) 0x00)) {//UTF-32LE
            unread = 4;
        } else if (data.length >= 3 && (data[0] == (byte) 0xEF) && (data[1] == (byte) 0xBB) && (data[2] == (byte) 0xBF)) {//UTF-8
            unread = 3;
        } else if (data.length >= 2 && (data[0] == (byte) 0xFE) && (data[1] == (byte) 0xFF)) {//UTF-16BE
            unread = 2;
        } else if (data.length >= 2 && (data[0] == (byte) 0xFF) && (data[1] == (byte) 0xFE)) {//UTF-16LE
            unread = 2;
        }
        str = new String(data, unread, data.length - unread);
        //2.去掉不可见字符，并把换行符统一成"\n"
        str = str//.replaceAll("\\p{C}}", "")
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n");
        return str;
    }

    public static String byteToHex(byte[] bytes) {
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }

    public static String getLetter(int num) {
        return String.valueOf((char) ('a' + num));
    }

    /**
     * 用非递归的方法遍历文件夹下所有文件
     *
     * @param path   路径
     * @param filter 排除器，不能为null
     * @return 一个包含所有文件的集合，不包含文件夹，如果没有子文件/文件夹或文件/文件夹全部被排除，则返回一个空集合（防止空指针异常）
     */
    public static List<File> traverseFolder(String path, Filter filter) {
        File file = new File(path);
        File[] files = file.listFiles();

        if (!file.exists()) {//路径不存在则返回空集合
            return Collections.emptyList();
        } else if (file.isFile()) {//如果是一个文件而且没有被排除的话则直接返回一个包含它自身的集合，否则返回一个空集合
            return filter.filter(file) ? Collections.singletonList(file) : Collections.emptyList();
        } else if (files == null || files.length == 0) {//没有子文件/子文件夹，则返回空集合
            return Collections.emptyList();
        }

        List<File> ret = new ArrayList<>();//用来存储结果
        LinkedList<File> list = new LinkedList<>();//用栈储存要搜索的文件夹

        for (File f : files) {
            if (!filter.filter(f))//排除指定文件/文件夹
                continue;

            if (f.isDirectory())
                list.add(f);
            else
                ret.add(f);
        }

        File tempFile;
        while (!list.isEmpty()) {
            tempFile = list.removeFirst();
            if (tempFile == null) {
                continue;
            } else if (!filter.filter(tempFile)) {//排除指定文件/文件夹
                continue;
            }

            files = tempFile.listFiles();
            if (files == null || files.length == 0) {//没有子文件夹就跳过
                continue;
            }

            for (File f : files) {
                if (f.isDirectory())
                    list.add(f);
                else if (f.isFile() && filter.filter(f))
                    ret.add(f);
            }
        }
        return ret;
    }

    /**
     * 配合traverseFolder使用，用于排除制定文件或文件夹
     */
    public interface Filter {
        /**
         * 返回true代表需要该文件或文件夹，false代表排除该文件/文件夹
         */
        boolean filter(File file);
    }

}
