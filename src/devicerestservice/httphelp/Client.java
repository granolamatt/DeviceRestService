/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice.httphelp;

import devicerestservice.types.TagValue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class Client {

    public static enum RequestType {

        Get("GET"), Put("PUT"), Delete("DELETE"), Post("POST");

        RequestType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
        String value;
    }

//    private static String USER_AGENT = "Mozilla/5.0";
    public static ServerResponse sendRequest(final RequestType requestType, final String uri, final String sendContentType, final String sendContentDisposition, final byte[] sendContent, final String acceptType, final int timeOut) throws MalformedURLException, IOException {
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setMethod(requestType.toString());

        URL url = new URL(uri);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(requestType.toString());
        con.setConnectTimeout(timeOut);
        con.setReadTimeout(timeOut);
        con.setDoInput(true);
        con.setRequestProperty("charset", "utf-8");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        if (acceptType != null) {
            if (!acceptType.equals("")) {
                con.setRequestProperty("Accept", acceptType);
            }
        }

        if (sendContentType != null) {
            if (!sendContentType.equals("")) {
                con.setRequestProperty("Content-Type", sendContentType);
            }
        }

        if (sendContentDisposition != null) {
            if (!sendContentDisposition.equals("")) {
                con.setRequestProperty("Content-Disposition", sendContentDisposition);
            }
        }

        if (sendContent != null) {
            con.setDoOutput(true);

            OutputStream outStream = con.getOutputStream();
            outStream.write(sendContent);
            outStream.flush();
            outStream.close();
        }

        serverResponse.setCode(con.getResponseCode());
        serverResponse.setMessage(con.getResponseMessage());
        serverResponse.setContentType(con.getContentType());

        // Copy the input stream to an array of bytes.
        byte[] buffer = new byte[4096];
        InputStream inStream = con.getInputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        int n = -1;
        while ((n = inStream.read(buffer)) != -1) {
            if (n > 0) {
                outStream.write(buffer, 0, n);
            }
        }
        outStream.flush();
        serverResponse.setContent(outStream.toByteArray());
        inStream.close();
        outStream.close();

        return serverResponse;
    }

    /**
     * Asynchronous send request. Just implement a PropertyChangeListener in
     * your class and pass it a meaningful PropertyName and you will get
     * notified.
     *
     * @param method
     * @param uri
     * @param sendContentType
     * @param sendContent
     * @param acceptType
     * @param timeOut
     * @param propertyName
     * @param listener
     */
    public static void sendRequest(final RequestType requestType, final String uri, final String sendContentType, final String sendContentDisposition, final byte[] sendContent, final String acceptType, final int timeOut, final String propertyName, final PropertyChangeListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerResponse serverResponse = new ServerResponse();
                serverResponse.setMethod(requestType.toString());
                final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
                try {
                    changeSupport.addPropertyChangeListener(listener);
                    serverResponse = sendRequest(requestType, uri, sendContentType, sendContentDisposition, sendContent, acceptType, timeOut);
                } catch (MalformedURLException ex) {
                    serverResponse.setException(ex);
                } catch (IOException ex) {
                    serverResponse.setException(ex);
                }
                changeSupport.firePropertyChange(propertyName, null, serverResponse);
            }
        });
        thread.start();
    }

    /**
     * Put all your tag/value pairs in an array of TagValue[]. For query
     * parameters, append URL with a "?"+getEncodedTagValues(...);
     *
     * @param tagValues
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getEncodedTagValues(final TagValue[] tagValues) throws UnsupportedEncodingException {
        String charset = "UTF-8";
        StringBuilder tagStringBuilder = new StringBuilder();
        boolean subsequentTag = false;
        for (TagValue tagValue : tagValues) {
            if (subsequentTag) {
                tagStringBuilder.append("&");
            }
            tagStringBuilder.append(URLEncoder.encode(tagValue.getTag(), charset));
            tagStringBuilder.append("=");
            tagStringBuilder.append(URLEncoder.encode(tagValue.getValue(), charset));
            subsequentTag = true;
        }
        return tagStringBuilder.toString();
    }

    // "Content-Type","multipart/form-data; boundary=" + boundary
    public static String getBoundary() {
        String boundary = Long.toHexString(System.currentTimeMillis()); // Generate some unique random value        
        return boundary;
    }

    public static byte[] formatBinaryFile(File file, String boundary) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        String charSet = "UTF-8";
        String CRLF = "\r\n";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, charSet), true);
        writer.append("--" + boundary).append(CRLF);
        writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\""
                + file.getName() + "\"").append(CRLF);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(CRLF);
        writer.append("Content-Transfer-Encoding: binary").append(CRLF);
        writer.append(CRLF).flush();
        InputStream inStream = null;
        inStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        for (int length = 0; (length = inStream.read(buffer)) > 0;) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        byteArrayOutputStream.flush();
        if (inStream != null) {
            inStream.close();
        }
        writer.append(CRLF).flush();
        writer.append("--" + boundary + "--").append(CRLF);
        writer.close();
        return byteArrayOutputStream.toByteArray();
    }
}
