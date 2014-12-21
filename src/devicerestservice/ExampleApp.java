/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice;

import devicemodel.DeviceNode;
import devicemodel.NodeHandler;
import devicemodel.conversions.XmlConversions;
import devicerestservice.logger.LoggerOut;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


public class ExampleApp extends App {

    /* 
      Configuration format:
    
     <device>
       <systemconfig>
       <devicename>MyDevice</devicename>
       <devicedescription>Something about My Device</devicedescription>
       <!--> <deviceproxy>10.0.0.1</deviceproxy><-->
       <deviceserver>10.0.0.1:12345</deviceserver>
       </systemconfig>
     </device>
    
    */
    
    public ExampleApp(Element configuration, Element deviceDescription) {
        try {
            System.out.println("Config " + XmlConversions.element2XmlString(configuration));
        } catch (IOException ex) {
            Logger.getLogger(ExampleApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.configuration = configuration;
        this.rootNode = XmlConversions.xmlToNode(deviceDescription);
        
        init();
    }
    
    public final void init() {
        // your stuff here
        
        // example static resources
        //
        // will be located at http://localhost:port/resources/html/js/jquery.min.js
        // addStaticResource("html/js/jquery.min.js", MediaType.APPLICATION_JSON);
        //
        // will be located at http://localhost:port/resources/html/index.html
        // addStaticResource("html/index.html", MediaType.TEXT_HTML);
        
        // example directory import (useful for web pages)
        // 
        // below will be located at: http://localhost:port/resources/web/*
        // this.addStaticResources(new File("/root/web/"), "");
        //
        // below will be located at: http://localhost:port/resources/test/mypage/web/*
        // this.addStaticResources(new File("/root/web/"), "/test/mypage");
        
        doInit();
        
        // your other stuff here
        
        LoggerOut.println(rootNode.getName() + " server started.\n");
        System.out.println("Try accessing " + getBaseURI() + " in the browser.\n");
        
//        System.out.println("Here's the tree:");
//        for(String path : rootNode.getAllChildren().keySet()) {
//            System.out.println(rootNode.getName() + path);
//        }
        
        rootNode.getChangeSupport().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Got a change!");
                DeviceNode n = (DeviceNode)evt.getNewValue();
                System.out.println(n.getName());
            }
        });
        
        final String path = "/Information/SystemConfig/DeviceName";
        
        NodeHandler nh = new NodeHandler() {
            @Override
            public boolean handle(DeviceNode node) {
                System.out.println("Got a set for " + node.getName() + " value is " + node.getValue());
                
                DeviceNode updateNode = rootNode.getChildByPath(path);
                
                rootNode.update(node);
                
                
                return true;
            }
        };
        
        rootNode.getChildByPath(path).setSetHandle(nh);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Must specify configuration file and device description");
            System.exit(1);
        }

        File xmlConfig = new File(args[0]);
        File xmlDeviceDesc = new File(args[1]);
        if (!xmlConfig.exists()) {
            System.out.println("Configuration file not found.");
            System.exit(2);
        }
        if (!xmlDeviceDesc.exists()) {
            System.out.println("Device description file not found.");
            System.exit(3);
        }

        SAXBuilder builder = new SAXBuilder();
        Element configurationElement = null;
        Element deviceDescriptionElement = null;
        
        try {
            Document document = (Document) builder.build(xmlConfig);
            configurationElement = document.getRootElement();
        } catch (JDOMException ex) {
            System.out.println("Can't parse configuration file.");
            System.exit(4);
        }
        
        try {
            Document document = (Document) builder.build(xmlDeviceDesc);
            deviceDescriptionElement = document.getRootElement();
        } catch (JDOMException ex) {
            System.out.println("Can't parse device description file.");
            System.exit(5);
        }

        if (configurationElement == null || !configurationElement.getName().equals("Device")) {
            System.out.println("Not a proper configuration XML file.");
            System.out.println("Ensure XML has elements in Pascal Case (i.e. DeviceName not deviceName or devicename)");
            System.exit(6);
        }

        new ExampleApp(configurationElement, deviceDescriptionElement);
    }
}
