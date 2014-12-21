/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devicerestservice.types;

/**
 *
 * @author root
 */
public enum DeviceStateType {

    Invalid("hardware state not yet detected."),
    Ok("hardware operating normally."),
    Degraded("hardware operation is degraded."),
    NotOk("Hardware is not operating normally.");

    DeviceStateType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    private String message;

}
