/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice.types;

import devicemodel.conversions.XmlConversions;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Element;

/**
 *
 * @author root
 */
public class SystemConfigType {

    private String deviceName = "";
    private String deviceProxyName = "";
    private String deviceDescription = "";
    private String deviceProxyUri = "";
    private boolean deviceProxyUriSet = false;
    private String deviceServerIpAddress = "";
    private boolean deviceRestricted = false;
    private String deviceType = "";
    private String deviceRootNode = "";
    private int deviceServerIpPort = 0;
    private boolean deviceServerIpSet = false;

    public String getDeviceName() {
        return deviceName;
    }

    public Element getDeviceNameElement() {
        return new Element("DeviceName").setText(deviceName);
    }

    public void setDeviceName(String name) {
        this.deviceName = name;
    }
    public String getDeviceRootNode() {
        return deviceRootNode;
    }

    public Element getDeviceRootNodeElement() {
        return new Element("DeviceRootNode").setText(deviceRootNode);
    }

    public void setDeviceRootNode(String rootNode) {
        this.deviceRootNode = rootNode;
    }
    
    public String getDeviceType() {
        return deviceType;
    }

    public Element getDeviceTypeElement() {
        return new Element("DeviceType").setText(deviceType);
    }

    public void setDeviceType(String type) {
        this.deviceType = type;
    }

    public String getDeviceProxyName() {
        return deviceProxyName;
    }

    public Element getDeviceProxyNameElement() {
        return new Element("DeviceProxyName").setText(deviceProxyName);
    }

    public void setDeviceProxyName(String deviceProxyName) {
        this.deviceProxyName = deviceProxyName;
    }

    public String getDeviceDescription() {
        return deviceDescription;
    }

    public Element getDeviceDescriptionElement() {
        return new Element("DeviceDescription").setText(deviceDescription);
    }

    public void setDeviceDescription(String description) {
        this.deviceDescription = description;
    }

    public String getDeviceProxyUri() {
        return deviceProxyUri;
    }

    public Element getDeviceProxyElement() {
        return new Element("DeviceProxy").setText(deviceProxyUri);
    }

    public void setDeviceProxyUri(String deviceProxyUri) {
        this.deviceProxyUri = deviceProxyUri;
    }

    public boolean isDeviceProxyUriSet() {
        return deviceProxyUriSet;
    }

    public String getDeviceServerIpAddress() {
        return deviceServerIpAddress;
    }

    public Element getDeviceServerElement() {
        return new Element("DeviceServer").setText(deviceServerIpAddress + ":" + deviceServerIpPort);
    }

    public void setDeviceServerIpAddress(String deviceServerIpAddress) {
        System.out.println("Setting ip to " + deviceServerIpAddress);
        this.deviceServerIpAddress = deviceServerIpAddress;
    }

    public int getDeviceServerIpPort() {
        return deviceServerIpPort;
    }

    public void setDeviceServerIpPort(int deviceServerIpPort) {
        this.deviceServerIpPort = deviceServerIpPort;
    }

    public boolean isDeviceServerIpSet() {
        return deviceServerIpSet;
    }

    public void setDeviceRestricted(boolean deviceRestricted) {
        this.deviceRestricted = deviceRestricted;
    }

    public boolean isDeviceRestricted() {
        return deviceRestricted;
    }

    public Element getDeviceRestrictedElement() {
        return new Element("DeviceRestricted").setText(Boolean.toString(deviceRestricted));
    }

    public void setXml(Element device) {
        Element childElement;
        String childText;

        try {
            System.out.println("Got xml " + XmlConversions.element2XmlString(device));
        } catch (IOException ex) {
            Logger.getLogger(SystemConfigType.class.getName()).log(Level.SEVERE, null, ex);
        }
        Element root = device.getChild("SystemConfig");

        childElement = root.getChild("DeviceName");
        if (childElement != null) {
            setDeviceName(childElement.getText());
        }

        childElement = root.getChild("DeviceProxyName");
        if (childElement != null) {
            setDeviceProxyName(childElement.getText());
        }

        childElement = root.getChild("DeviceDescription");
        if (childElement != null) {
            setDeviceDescription(childElement.getText());
        }

        childElement = root.getChild("DeviceType");
        if (childElement != null) {
            setDeviceType(childElement.getText());
        }

        childElement = root.getChild("DeviceRestricted");
        if (childElement != null) {
            setDeviceRestricted(Boolean.parseBoolean(childElement.getText()));
        }

        childElement = root.getChild("DeviceProxy");
        if (childElement != null) {
            childText = childElement.getText();
            setDeviceProxyUri(childText);
            deviceProxyUriSet = true;
        }

        childElement = root.getChild("DeviceServer");
        if (childElement != null) {
            childText = childElement.getText();
            IpInfo ipInfo = new IpInfo(childText);
            setDeviceServerIpAddress(ipInfo.getAddress());
            setDeviceServerIpPort(ipInfo.getPort());
            deviceServerIpSet = true;
        }
    }

    public Element getXmlForProxy() {
        return getXml().setName("DeviceServers");
    }

    public Element getXml() {
        Element root = new Element("SystemConfig");

        root.addContent(getDeviceNameElement());
        root.addContent(getDeviceDescriptionElement());
        root.addContent(getDeviceProxyElement());
        root.addContent(getDeviceProxyNameElement());
        root.addContent(getDeviceServerElement());
        root.addContent(getDeviceRootNodeElement());
        root.addContent(getDeviceRestrictedElement());
        root.addContent(getDeviceTypeElement());
        return root;
    }
}
