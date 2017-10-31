package yuema.server;


import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

/**
 * Created by martin on 17-10-11.
 */
public class Encryption {
    private static volatile Encryption instance;
    private static String key;
    private static String initVector;

    static Encryption getInstance(){
        if (instance == null) {
            synchronized (Encryption.class) {
                if (instance == null) {
                    instance = new Encryption();
                }
            }
        }
        return instance;
    }

    private Encryption(){
        key = "Bar12345Bar12345";
        initVector = "RandomInitVector";
    }


    private String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    String vulnerableEncrypt(String source){
        return encrypt(key, initVector, source);
    }

    String vulnerableDecrypt(String source){
        return decrypt(key, initVector, source);
    }



    public static void main(String[] args) {
        Encryption e = Encryption.getInstance();
        System.out.println(e.vulnerableDecrypt(e.vulnerableEncrypt("")));

    }

}
