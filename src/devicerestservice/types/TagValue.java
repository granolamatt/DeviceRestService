/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice.types;

/**
 *
 * @author root
 */
public class TagValue {

    private String tag = "";
    private String value = "";

    public TagValue(String tag, String value) {
        this.tag = tag;
        this.value = value;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append("=");
        stringBuilder.append(value);        
        return stringBuilder.toString();
    }
}
