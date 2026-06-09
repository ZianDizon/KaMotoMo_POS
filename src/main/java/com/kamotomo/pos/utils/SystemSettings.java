package com.kamotomo.pos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class SystemSettings {
    private static final String CONFIG_FILE = "system_config.properties";
    private static Properties properties = new Properties();

    static {
        loadSettings();
    }

    private static void loadSettings() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    properties.load(fis);
                }
            } else {
                properties.setProperty("TAX_RATE", "0.12");
                properties.setProperty("SC_PWD_DISCOUNT", "0.20");
                properties.setProperty("WHOLESALE_DISCOUNT", "0.10");
                properties.setProperty("EMPLOYEE_DISCOUNT", "0.15");
                saveSettings();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "KaMotoMo System Configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double getTaxRate() { return Double.parseDouble(properties.getProperty("TAX_RATE", "0.12")); }
    public static void setTaxRate(double rate) { properties.setProperty("TAX_RATE", String.valueOf(rate)); saveSettings(); }

    public static double getScPwdDiscountRate() { return Double.parseDouble(properties.getProperty("SC_PWD_DISCOUNT", "0.20")); }
    public static void setScPwdDiscountRate(double rate) { properties.setProperty("SC_PWD_DISCOUNT", String.valueOf(rate)); saveSettings(); }

    public static double getWholesaleDiscountRate() { return Double.parseDouble(properties.getProperty("WHOLESALE_DISCOUNT", "0.10")); }
    public static void setWholesaleDiscountRate(double rate) { properties.setProperty("WHOLESALE_DISCOUNT", String.valueOf(rate)); saveSettings(); }

    public static double getEmployeeDiscountRate() { return Double.parseDouble(properties.getProperty("EMPLOYEE_DISCOUNT", "0.15")); }
    public static void setEmployeeDiscountRate(double rate) { properties.setProperty("EMPLOYEE_DISCOUNT", String.valueOf(rate)); saveSettings(); }
}