package devicerestservice;

import devicemodel.DeviceNode;
import devicemodel.conversions.XmlConversions;
import devicerestservice.httphelp.Client;
import devicerestservice.httphelp.ServerResponse;
import devicerestservice.logger.LoggerOut;
import devicerestservice.types.OptionListing;
import devicerestservice.types.SystemConfigType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public abstract class App {

    private final int deviceProxyContactInterval = 2000;
    private App instance = null;
    private Server server;
    private final long startTime = System.currentTimeMillis();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private boolean deviceProxyContacted = false;
    private final SystemConfigType systemConfig = new SystemConfigType();
    private final ArrayList<PathResourceHolder> pathResources = new ArrayList<>();
    private final ArrayList<Resource> otherResources = new ArrayList<>();
    private boolean started = false;
    //
    protected DeviceNode rootNode;
    protected Element configuration;

    public final void doInit() {

        try {
            System.out.println("Got Config in server: " + XmlConversions.element2XmlString(configuration));
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        instance = this;
        try {
            systemConfig.setXml(configuration);
        } catch (Exception ex) {
            System.out.println("Cannot set device configuration.");
            System.exit(-1);
        }
        
        systemConfig.setDeviceRootNode(rootNode.getName());

        if (systemConfig.isDeviceServerIpSet()) {
            try {
                startServer();
            } catch (Exception ex) {
                System.out.println("Cannot start HTTP server with specified configuration.");
                System.exit(-1);
            }
        } else {
            System.out.println("No HTTP server configuration detected.");
            System.exit(-1);
        }

        if (systemConfig.isDeviceProxyUriSet()) {
            contactDeviceProxy();
        } else {
            System.out.println("No proxy information detected. Proxy will not be contacted.");
        }

    }

    public void startServer() throws IOException {

        URI serverUri = getBaseURI();

        System.out.println("!!!!!!!!! Starting " + getBaseURI());
        ResourceBase base = new ResourceBase(rootNode, pathResources, otherResources);

        server = JettyHttpContainerFactory.createServer(serverUri, base);
    }
    
    public void addResource(Resource resource) {
        otherResources.add(resource);
    }

    public void addStaticResource(String path, String mediatype, String file) {
        PathResourceHolder nPath = new PathResourceHolder(path, mediatype, file);
        pathResources.add(nPath);
    }

    public void addStaticResource(String path, String mediatype) {
        PathResourceHolder nPath = new PathResourceHolder(path, mediatype);
        pathResources.add(nPath);
    }

    public void addStaticResources(File staticResourceDir, String virtualPathRoot) {
        if (staticResourceDir.isDirectory()) {
            System.out.println("Loading Directory: " + staticResourceDir.getAbsolutePath());
            for (File file : staticResourceDir.listFiles()) {
                addStaticResources(file, virtualPathRoot + staticResourceDir.getName() + "/");
            }
        } else if (staticResourceDir.isFile()) {
            System.out.println("Loading file: " + virtualPathRoot + staticResourceDir.getName() + " --> " + staticResourceDir.getAbsolutePath());

            String mediaType = MediaType.WILDCARD + ";charset=utf-8";
            
            if (staticResourceDir.getName().endsWith("js")) {
                mediaType = MediaType.APPLICATION_JSON + ";charset=utf-8";
            } else if (staticResourceDir.getName().endsWith("css")) {
                mediaType = "text/css;charset=utf-8";
            } else if (staticResourceDir.getName().endsWith("ttf")) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            } else if (staticResourceDir.getName().endsWith("ico")) {
                mediaType = "image/ico;charset=utf-8";
            } else if (staticResourceDir.getName().endsWith("jpg") || staticResourceDir.getName().endsWith("jpeg")) {
                mediaType = "image/jpeg;charset=utf-8";
            } else if (staticResourceDir.getName().endsWith("png")) {
                mediaType = "image/png;charset=utf-8";
            } else if (staticResourceDir.getName().endsWith("htm") || staticResourceDir.getName().endsWith("html")) {
                mediaType = MediaType.TEXT_HTML + ";charset=utf-8";
            }

            addStaticResource(virtualPathRoot + staticResourceDir.getName(), mediaType, staticResourceDir.getAbsolutePath());
        }
    }

    private void contactDeviceProxy() {
        StringBuilder deviceServerXmlString = new StringBuilder();
        try {
            deviceServerXmlString.append("<DeviceServers>");
            deviceServerXmlString.append(XmlConversions.element2XmlString(systemConfig.getXml()));
            deviceServerXmlString.append("</DeviceServers>");
        } catch (IOException ex) {
        }
        System.out.println("deviceServerXmlString: " + deviceServerXmlString);

        deviceProxyContacted = false;
        System.out.println("Device, " + systemConfig.getDeviceName() + ", attempting to contact DeviceProxy at " + systemConfig.getDeviceProxyUri() + "/DeviceServers");
        final String proxyUri = systemConfig.getDeviceProxyUri() + "/DeviceServers";
        final byte[] sendContent = deviceServerXmlString.toString().getBytes();
        final PropertyChangeListener proxyContactListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("ContactProxy")) {
                    ServerResponse serverResponse = (ServerResponse) evt.getNewValue();
                    if (serverResponse.getException() == null) {
                        //deviceProxyContacted = true;
                        //System.out.println("Proxy successfully contacted.");
                    } else {
//                        System.out.print(".");
                        //serverResponse.getException().printStackTrace();
                    }
                }
            }
        };

        new Thread(new Runnable() {

            @Override
            public void run() {
                while (!deviceProxyContacted) {
                    Client.sendRequest(Client.RequestType.Put, proxyUri, "application/xml", null, sendContent, "application/xml", 1000, "ContactProxy", proxyContactListener);
                    try {
                        Thread.sleep(deviceProxyContactInterval);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();

    }

    public App getInstance() {
        return instance;
    }

    public DeviceNode getRootNode() {
        return rootNode;
    }

    public SystemConfigType getSystemConfig() {
        return systemConfig;
    }

    private static int getPort(int defaultPort) {
        final String port = System.getProperty("jersey.config.test.container.port");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                System.out.println("Value of jersey.config.test.container.port property"
                        + " is not a valid positive integer [" + port + "]."
                        + " Reverting to default [" + defaultPort + "].");
            }
        }
        return defaultPort;
    }

    public URI getBaseURI() {
        String deviceServerIpAddress = systemConfig.getDeviceServerIpAddress();
        int deviceServerIpPort = systemConfig.getDeviceServerIpPort();
        return UriBuilder.fromUri("http://" + deviceServerIpAddress + "/").port(getPort(deviceServerIpPort)).build();
    }

    public void noExit() {
        final Thread thisThread = Thread.currentThread();
        Thread stop = new Thread() {
            @Override
            public void run() {
                synchronized (thisThread) {
                    thisThread.notify();
                }
                try {
                    thisThread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(stop);
        synchronized (thisThread) {
            try {
                thisThread.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void stopServer() {
        Timer haltTimer = new Timer();

        haltTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, 0, 3000);
    }

    public class PathResourceHolder {

        private final String path;
        private final String Mediatype;
        private final File file;

        private PathResourceHolder(String path, String mediatype) {
            this.path = path;
            this.Mediatype = mediatype;
            this.file = null;
        }

        private PathResourceHolder(String path, String mediatype, String file) {
            this.path = path;
            this.Mediatype = mediatype;
            this.file = new File(file);
        }

        /**
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * @return the Mediatype
         */
        public String getMediatype() {
            return Mediatype;
        }

        public InputStream getInputStream() throws FileNotFoundException {
            return this.file != null ? new FileInputStream(this.file) : ClassLoader.getSystemResourceAsStream(this.path);
        }
    }
}
