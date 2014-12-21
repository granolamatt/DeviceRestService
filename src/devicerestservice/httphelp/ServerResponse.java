/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice.httphelp;

/**
 *
 * @author root
 */
public class ServerResponse {

    private String method = "";
    private int code = -1;
    private String message = "";
    private String contentType = "";
    private byte[] content = null;
    private Exception exception = null;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Method: ");
        stringBuilder.append(method);
        stringBuilder.append("\n");
        if (exception != null) {
            stringBuilder.append("Exception: ");
            stringBuilder.append(exception.getMessage());
            stringBuilder.append("\n");
        } else {
            stringBuilder.append("Response code: ");
            stringBuilder.append(String.valueOf(code));
            stringBuilder.append("\n");
            stringBuilder.append("Response message: ");
            stringBuilder.append(message);
            stringBuilder.append("\n");
            stringBuilder.append("Content type: ");
            stringBuilder.append(contentType);
            stringBuilder.append("\n");
            stringBuilder.append("Content available: ");
            if ((contentType != null) && (!contentType.equals(""))) {
                stringBuilder.append(content.length);
                stringBuilder.append("bytes\n");
            } else {
                stringBuilder.append("No\n");
            }

        }

        return stringBuilder.toString();
    }
}
