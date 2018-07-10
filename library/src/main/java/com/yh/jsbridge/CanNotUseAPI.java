package com.yh.jsbridge;

/**
 * Created by Clistery on 18-7-10.
 */
public class CanNotUseAPI extends RuntimeException {
    public CanNotUseAPI(String message) {
        super(message);
    }
}
