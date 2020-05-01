package checkin.utils;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EncryptUtils {

    public static String generatePassWord() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-MM-yyyy");
            String str1 = shaEncode(dateFormat.format(new Date()));
            String pwd = str1.toLowerCase()
                    .replace("1", "!")
                    .replace("a", "@")
                    .replace("b", "%")
                    .replace("o", "a")
                    .replace("p", "b")
                    .replace("9", "?")
                    .toLowerCase();System.out.println(pwd);
            return pwd;
        } catch (Exception e) {
            return "";
        }
    }

    private static String shaEncode(String inStr) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA");
        byte[] byteArray = inStr.getBytes("UTF-8");
        byte[] md5Bytes = sha.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

}
