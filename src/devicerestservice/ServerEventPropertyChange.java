/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice;

import devicemodel.DeviceNode;
import devicemodel.conversions.JsonConversions;
import devicemodel.conversions.XmlConversions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

/**
 *
 * @author root
 */
public class ServerEventPropertyChange extends EventOutput implements PropertyChangeListener {

    private final PropertyChangeSupport mySupport;
    private final String type;

    public ServerEventPropertyChange(PropertyChangeSupport support, String type) {
        mySupport = support;
        mySupport.addPropertyChangeListener(this);
        this.type = type;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            //System.out.println("Got a property change!!!! " + evt);
            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();

            DeviceNode node = (DeviceNode) evt.getNewValue();

            switch (type) {
                case MediaType.APPLICATION_XML:
                    eventBuilder.data(String.class, XmlConversions.nodeToXmlString(node));
                    break;
                case MediaType.APPLICATION_JSON:
                    eventBuilder.data(String.class, JsonConversions.nodeToJson(node));
                    break;
                default:
                    eventBuilder.data(String.class, node.getValue());
                    break;
            }
            
            final OutboundEvent event = eventBuilder.build();
            write(event);
        } catch (Exception e) {
            System.out.println("Exception in property change, need to cleanup");
           // e.printStackTrace();
            mySupport.removePropertyChangeListener(this);
            try {
                close();
            } catch (IOException ioClose) {
                throw new RuntimeException(
                        "Error when closing the event output.", ioClose);
            }
        }

    }
}
