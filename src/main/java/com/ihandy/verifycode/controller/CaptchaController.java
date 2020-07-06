package com.ihandy.verifycode.controller;

import com.ihandy.verifycode.model.CaptchaModel;
import com.ihandy.verifycode.service.CaptchaService;
import com.ihandy.verifycode.utils.ResponseData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;


@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Resource(name = "captchaServicelmpl")
    private CaptchaService captchaServicelmpl;

    @RequestMapping(value = "/getVerifyCode")
    @ResponseBody
    public ResponseData getVerifyCode(CaptchaModel captchaModel) {
        return captchaServicelmpl.getVerifyCode(captchaModel);
    }

    @RequestMapping(value = "/checkVerifyCode")
    @ResponseBody
    public ResponseData checkVerifyCode(CaptchaModel captchaModel) {
        return captchaServicelmpl.checkVerifyCode(captchaModel);
    }

}