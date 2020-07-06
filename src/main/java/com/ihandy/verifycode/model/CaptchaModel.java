package com.ihandy.verifycode.model;

import java.io.Serializable;
import java.util.List;

public class CaptchaModel implements Serializable {
    /**
     * 验证码类型:(clickWord,blockPuzzle)
     */
    private String captchaType;
    /**
     * 原生图片base64
     */
    private String originalImageBase64;
    /**
     * 前端所传坐标
     */
    public String paramsJson;
    /**
     * 滑块图片base64
     */
    private String jigsawImageBase64;
    /**
     * 点选文字
     */
    private List<String> wordList;
    /**
     * UUID(每次请求的验证码唯一标识)
     */
    private String uuId;

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public String getOriginalImageBase64() {
        return originalImageBase64;
    }

    public void setOriginalImageBase64(String originalImageBase64) {
        this.originalImageBase64 = originalImageBase64;
    }

    public String getJigsawImageBase64() {
        return jigsawImageBase64;
    }

    public void setJigsawImageBase64(String jigsawImageBase64) {
        this.jigsawImageBase64 = jigsawImageBase64;
    }

    public List<String> getWordList() {
        return wordList;
    }

    public void setWordList(List<String> wordList) {
        this.wordList = wordList;
    }

    public String getUuId() {
        return uuId;
    }

    public void setUuId(String uuId) {
        this.uuId = uuId;
    }
}
