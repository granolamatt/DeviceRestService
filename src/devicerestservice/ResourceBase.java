/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice;

import devicemodel.DeviceNode;
import devicemodel.conversions.JsonConversions;
import devicemodel.conversions.XmlConversions;
import devicerestservice.logger.LoggerOut;
import devicerestservice.logger.RestLogger;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 *
 * @author adam
 */
public final class ResourceBase extends ResourceConfig {

    private final DeviceNode rootNode;

    public ResourceBase(DeviceNode rootNode, ArrayList<App.PathResourceHolder> staticResources, ArrayList<Resource> otherResources) {
        this.rootNode = rootNode;

        final StringBuilder sb = new StringBuilder();

        sb.append("<html><body>Device Resource List:<br><br>");

        addLink(sb, "/resources");
        addLink(sb, "/logging/stdout");
        addLink(sb, "/logging/xmlevents");
        addLink(sb, "/logging/jsonevents");

        buildResources(sb, rootNode);

        for (App.PathResourceHolder holder : staticResources) {
            addLink(sb, "/resources/" + holder.getPath());
            registerStaticResource(holder);
        }

        for (Resource res : otherResources) {
            addLink(sb, res.getPath());
            registerResources(res);
        }

        sb.append("</body></html>");

        // build base content
        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path("/resources");

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");

        methodBuilder.consumes(MediaType.APPLICATION_XML).handledBy(new Inflector<ContainerRequestContext, String>() {

            @Override
            public String apply(ContainerRequestContext containerRequestContext) {

                return sb.toString();
            }
        });

        final Resource resource = resourceBuilder.build();
        //EventServices.setRoot(rootNode);
        registerResources(resource);
        register(MultiPartFeature.class);
        register(SseFeature.class);
        register(RestLogger.class);
        //register(EventServices.class);
    }

    private void addLink(StringBuilder sb, String url) {
        sb.append("<a href=\"").append(url).append("\">");
        sb.append(url);
        sb.append("</a>").append(" <br>");
    }

    private void buildResources(StringBuilder sb, DeviceNode node) {
        addResource(node);
        addLink(sb, node.getNodePath());

        for (DeviceNode c : node.getChildren().values()) {
            buildResources(sb, c);
        }
    }

    private void addResource(DeviceNode node) {

        addGetResource(node, MediaType.APPLICATION_XML);
        addPutResource(node, MediaType.APPLICATION_XML);
        addEventResource(node, MediaType.APPLICATION_XML);

        addGetResource(node, MediaType.APPLICATION_JSON);
        addPutResource(node, MediaType.APPLICATION_JSON);
        addEventResource(node, MediaType.APPLICATION_JSON);
    }

    public void registerStaticResource(final App.PathResourceHolder holder) {

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path("/resources/" + holder.getPath());

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        methodBuilder.produces(holder.getMediatype()).handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                try {
                    if (containerRequestContext.getMethod().equals("GET")) {
                        System.out.println("Got a GET for " + holder.getPath());
                        LoggerOut.println("Get a GET for " + holder.getPath());
                        InputStream in = holder.getInputStream();

                        return Response.ok().type(holder.getMediatype()).entity(in).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                } catch (Exception ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }

            }
        });

        final Resource resource = resourceBuilder.build();
        registerResources(resource);
    }

    private void addPutResource(final DeviceNode node, final String mediaType) {

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path(node.getNodePath());

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("PUT");
        methodBuilder.consumes(mediaType).produces(mediaType).handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                StringBuilder sb = new StringBuilder();
                try {
                    if (containerRequestContext.getMethod().equals("PUT")) {
                        LoggerOut.println("Got a PUT for " + node.getNodePath());
                        InputStream is = containerRequestContext.getEntityStream();

                        byte[] bb = new byte[65536];
                        int num = 0;
                        while ((num = is.read(bb)) != -1) {
                            byte[] data = new byte[num];
                            System.arraycopy(bb, 0, data, 0, num);
                            sb.append(new String(data));
                        }

                        switch (mediaType) {
                            case MediaType.APPLICATION_XML:
                                Element input = XmlConversions.xmlString2Element(sb.toString());
                                LoggerOut.println("Setting xml to " + node.getNodePath() + " Element " + input);
                                node.set(XmlConversions.xmlToNode(input));

                                return Response.ok().type(mediaType).entity(XmlConversions.nodeToXmlString(node)).build();
                            case MediaType.APPLICATION_JSON:
                                LoggerOut.println("Setting json to " + node.getNodePath() + " json: " + sb.toString());
                                node.set(JsonConversions.jsonToNode(sb.toString()));

                                return Response.ok().type(mediaType).build();
                            default:
                                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
                        }

                    } else {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                } catch (Exception ex) {
//                    System.out.println("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
//                    ex.printStackTrace();
//                    System.out.println("content:" + sb.toString());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
            }
        });

        final Resource resource = resourceBuilder.build();
        registerResources(resource);
    }

    // HTTP requests can have multiple values per key as part of the query
    // parameters. These are stored as String -> LinkedList<String> pairs; we 
    // convert this to a HashMap with the assumption that the last value is the
    // final value to pass into the node structure.
    public static HashMap<String, String> mutlivalueToHashMap(MultivaluedMap<String, String> map) {
        HashMap<String, String> hash = new HashMap();

        for (String str : map.keySet()) {
            List<String> values = map.get(str);

            hash.put(str, values.get(values.size() - 1));
        }
        return hash;
    }

    private void addGetResource(final DeviceNode node, final String mediaType) {

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path(node.getNodePath());

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        methodBuilder.produces(mediaType).handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                try {
                    if (containerRequestContext.getMethod().equals("GET")) {
//                        System.out.println("Got a GET for " + node.getNodePath());
                        LoggerOut.println("Get a GET for " + node.getNodePath());

                        HashMap<String, String> queryParameters = mutlivalueToHashMap(containerRequestContext.getUriInfo().getQueryParameters());

                        switch (mediaType) {
                            case MediaType.APPLICATION_XML:
                                return Response.ok().type(mediaType).entity(XmlConversions.nodeToXmlString(node.get(queryParameters))).build();
                            case MediaType.APPLICATION_JSON:
                                return Response.ok().type(mediaType).entity(JsonConversions.nodeToJson(node.get(queryParameters))).build();
                            case MediaType.TEXT_HTML:
                                return Response.ok().type(mediaType).entity(node.getValue()).build();
                            default:
                                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
                        }
                    } else {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                } catch (Exception ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }

            }
        });

        final Resource resource = resourceBuilder.build();
        registerResources(resource);
    }

    private void addEventResource(final DeviceNode node, final String mediaType) {

        String ext = "";

        switch (mediaType) {
            case MediaType.APPLICATION_XML:
                ext = "/xmlevents";
                break;
            case MediaType.APPLICATION_JSON:
                ext = "/jsonevents";
                break;
            case MediaType.TEXT_HTML:
                ext = "/htmlevents";
                break;
            default:
                return;
        }

        final String path = node.getNodePath() + ext;

        LoggerOut.println("Adding resource " + path);
        final Resource.Builder resourceBuilder = Resource.builder();
//        resourceBuilder.path(path + "/events");
        resourceBuilder.path(path);

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        methodBuilder.consumes("event-stream/text", "event-stream/xml", "event-stream/json").produces(SseFeature.SERVER_SENT_EVENTS).handledBy(new Inflector<ContainerRequestContext, EventOutput>() {
//        methodBuilder.produces(SseFeature.SERVER_SENT_EVENTS).handledBy(new Inflector<ContainerRequestContext, EventOutput>() {

            @Override
            public EventOutput apply(ContainerRequestContext containerRequestContext) {
                LoggerOut.println("Setting up event for " + path);
                try {
                    if (containerRequestContext.getMethod().equals("GET")) {
                        System.out.println("!!!!!!!!!!!!! Set up event for " + path);
                        LoggerOut.println("Set up event for " + path);
                        ServerEventPropertyChange eventOutput = new ServerEventPropertyChange(node.getChangeSupport(), mediaType);
                        return eventOutput;
                    }
                } catch (Exception ex) {
                    System.out.println("It is not working " + path);
                }
                return null;
            }
        });

        final Resource resource = resourceBuilder.build();
        registerResources(resource);
    }
}
