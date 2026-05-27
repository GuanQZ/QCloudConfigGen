package com.configgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class ConfigGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigGenApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String url = "http://localhost:8080";
        System.out.println("========================================");
        System.out.println("QCloud 配置文件生成工具已启动");
        System.out.println("访问地址: " + url);
        System.out.println("========================================");

        // 使用 Java 9+ 的方式打开浏览器
        try {
            // 首先尝试 AWT 方式
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI.create(url));
                    System.out.println("已自动打开浏览器");
                    return;
                }
            }
        } catch (Exception e) {
            // AWT 方式失败，继续尝试其他方式
        }

        // 尝试通过命令行打开浏览器 (跨平台)
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("linux")) {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
            System.out.println("已自动打开浏览器");
        } catch (Exception e) {
            System.out.println("请手动在浏览器中打开: " + url);
        }
    }
}