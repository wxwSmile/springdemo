package com.gupao.servlet.service.impl;

import com.gupao.servlet.framework.annotation.GpService;
import com.gupao.servlet.service.IDemoService;

@GpService
public class DemoService implements IDemoService {
    public String add(String var1, String var2) throws Exception {
        throw new Exception("this is sky exception");
    }
}
