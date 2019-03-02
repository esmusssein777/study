package com.ligz.spring.ioc;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * IOC容器类实现
 * author:ligz
 */
public class SimpleIOC {
    private Map<String, Object> beanMap = new HashMap<>();

    public SimpleIOC(String location) throws Exception{
        loadBean(location);
    }

    public Object getBean(String name) {
        Object bean = beanMap.get(name);
        if (bean == null){
            throw new IllegalArgumentException("there is no bean with name " + name);
        }
        return bean;
    }

    private void loadBean(String location) throws Exception {
        //加载xml配置文件
        InputStream inputStream = new FileInputStream(location);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document doc = documentBuilder.parse(inputStream);
        Element root = doc.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        //遍历bean标签
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
                String id = ele.getAttribute("id");
                String className = ele.getAttribute("class");

                //加载beanClass
                Class beanClass = null;
                try {
                    beanClass = Class.forName(className);
                }catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                //创建bean
                Object bean = beanClass.newInstance();

                //遍历property标签
                NodeList propertyNodes = ele.getElementsByTagName("property");
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Node propertyNode = propertyNodes.item(j);
                    if (propertyNode instanceof  Element) {
                        Element propertyElement = (Element) propertyNode;
                        String name = propertyElement.getAttribute("name");
                        String value = propertyElement.getAttribute("value");

                        //利用反射将bean相关字段访问权限设为可访问
                        Field declareField = bean.getClass().getDeclaredField(name);
                        declareField.setAccessible(true);

                        if (value != null && value.length() > 0){
                            //将属性填充到相关的字段中
                            declareField.set(bean, value);
                        }else {
                            String ref = propertyElement.getAttribute("ref");
                            if (ref == null || ref.length() == 0) {
                                throw new IllegalArgumentException("ref config error");
                            }
                            // 将引用填充到相关字段中
                            declareField.set(bean, getBean(ref));
                        }
                        //将bean注册到bean容器中
                        registerBean(id, bean);
                    }
                }
            }
        }
    }

    private void registerBean(String id, Object bean) {
        beanMap.put(id, bean);
    }
}
