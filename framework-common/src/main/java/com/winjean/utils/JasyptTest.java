package com.winjean.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;

/**
 * @author ：winjean
 * @date ：Created in 2019/3/12 9:28
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
public class JasyptTest {

    public static void testEncrypt() throws Exception {
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        EnvironmentPBEConfig config = new EnvironmentPBEConfig();

        config.setAlgorithm("PBEWithMD5AndDES");          // 加密的算法，这个算法是默认的
        config.setPassword("winjean");                        // 加密的密钥
        standardPBEStringEncryptor.setConfig(config);
        String plainText = "root";
        String encryptedText = standardPBEStringEncryptor.encrypt(plainText);
        System.out.println(encryptedText);
    }

    public static void testDecrypt() throws Exception {
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        EnvironmentPBEConfig config = new EnvironmentPBEConfig();

        config.setAlgorithm("PBEWithMD5AndDES");
        config.setPassword("winjean");
        standardPBEStringEncryptor.setConfig(config);
        String encryptedText = "onfrnTyfM9OPgA7mgNoFDQ==";
        String plainText = standardPBEStringEncryptor.decrypt(encryptedText);
        System.out.println(plainText);
    }

    public static void main(String[] args) throws Exception{
        testEncrypt();
        testDecrypt();
    }
}