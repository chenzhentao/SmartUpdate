package com.cundong.apkpatch.example.update;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Administrator on 2018/6/29.
 */

public class XmlParser {

    /**
     * @param in
     * @return
     * @throws Exception
     * @描述：解析从服务器上读取到的版本文档,由于XML文件比较小，因此使用DOM方式进行解析
     */
    public static UpdateInfo parseXml(InputStream in) {
        UpdateInfo updateInfo = null;
        // 实例化一个文档构建器工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 通过文档构建器工厂获取一个文档构建器
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Log.e("TAG", "parseXml: " + e.toString());
        }
        // 通过文档通过文档构建器构建一个文档实例
        Document document = null;
        try {
            document = builder.parse(in);
        } catch (SAXException e) {
            e.printStackTrace();
            Log.e("TAG", "SAXException: " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "IOException: " + e.toString());
        }
        // 获取XML文件根节点
        Element root = document.getDocumentElement();
        // 获得所有子节点
        NodeList childNodes = root.getChildNodes();

        String appName = null;
        int versionCode = -1;
        String versionName = null;
        String downUrl = null;
        String description = "";

        for (int j = 0; j < childNodes.getLength(); j++) {
            Node childNode = childNodes.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                if ("appName".equals(childElement.getNodeName())) {
                    appName = childElement.getFirstChild().getNodeValue();
                } else if ("versionCode".equals(childElement.getNodeName())) {
                    versionCode = Integer.parseInt(childElement.getFirstChild()
                            .getNodeValue());
                } else if (("versionName".equals(childElement.getNodeName()))) {
                    versionName = childNode.getFirstChild().getNodeValue();
                } else if (("downUrl".equals(childElement.getNodeName()))) {
                    downUrl = childElement.getFirstChild().getNodeValue();
                } else if ("description".equals(childElement.getNodeName())) {
                    description = childElement.getTextContent();
                }
            }
        }
        updateInfo = new UpdateInfo(appName, versionCode, versionName, downUrl,
                description);
        Log.e("TAG", "parseXml: " + updateInfo.getDownUrl() + " " + updateInfo.getAppName() + " " + updateInfo.getVersionName() + " " + updateInfo.getVersionCode());
        return updateInfo;
    }

}
