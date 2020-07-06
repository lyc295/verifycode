package com.ihandy.verifycode.service;


import com.ihandy.verifycode.model.CaptchaModel;
import com.ihandy.verifycode.utils.ResponseData;
/**
 * 验证码服务接口
 *
 */
public interface CaptchaService {


    /**
     * 获取验证码
     * @return
     */
    ResponseData getVerifyCode(CaptchaModel captchaModel);

    /**
     * 核对验证码(前端)
     * @param captchaModel
     * @return
     */
    ResponseData checkVerifyCode(CaptchaModel captchaModel);


}
