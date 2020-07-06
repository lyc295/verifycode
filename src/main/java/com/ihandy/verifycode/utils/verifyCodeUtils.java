package com.ihandy.verifycode.utils;

import com.alibaba.fastjson.JSONObject;
import com.ihandy.verifycode.enumModel.CaptchaBaseMapEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class verifyCodeUtils {

    private static Logger logger = LoggerFactory.getLogger(verifyCodeUtils.class);
    private static Map<String, String> originalCacheMap = new ConcurrentHashMap();  //滑块底图
    private static Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap(); //滑块
    private static Map<String, String> picClickCacheMap = new ConcurrentHashMap(); //点选文字
    private static Map<String, String[]> fileNameMap = new ConcurrentHashMap<>();

    public static void cacheImage(String captchaOriginalPathJigsaw, String captchaOriginalPathClick) {
        //滑动拼图
        if (StringUtils.isBlank(captchaOriginalPathJigsaw)) {
            originalCacheMap.putAll(getResourcesImagesFile("images/jigsaw/original"));
            slidingBlockCacheMap.putAll(getResourcesImagesFile("images/jigsaw/slidingBlock"));
        } else {
            originalCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + "/" + "original"));
            slidingBlockCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + "/" + "slidingBlock"));
        }
        //点选文字
        if (StringUtils.isBlank(captchaOriginalPathClick)) {
            picClickCacheMap.putAll(getResourcesImagesFile("images/pic-click"));
        } else {
            picClickCacheMap.putAll(getImagesFile(captchaOriginalPathClick));
        }
        fileNameMap.put(CaptchaBaseMapEnum.ORIGINAL.getCodeValue(), originalCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.SLIDING_BLOCK.getCodeValue(), slidingBlockCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.PIC_CLICK.getCodeValue(), picClickCacheMap.keySet().toArray(new String[0]));
        logger.info("初始化底图:{}", JSONObject.toJSONString(fileNameMap));
    }


    public static BufferedImage getOriginal() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.ORIGINAL.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = getRandomInt(0, strings.length);
        String s = originalCacheMap.get(strings[randomInt]);
        return getBase64StrToImage(s);
    }

    public static BufferedImage getPicClick() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.PIC_CLICK.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = getRandomInt(0, strings.length);
        String s = picClickCacheMap.get(strings[randomInt]);
        return getBase64StrToImage(s);
    }

    public static String getslidingBlock() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.SLIDING_BLOCK.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = getRandomInt(0, strings.length);
        String s = slidingBlockCacheMap.get(strings[randomInt]);
        return s;
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String
     * @return
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] bytes = decoder.decode(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static Map<String, String> getResourcesImagesFile(String path) {
        //默认提供六张底图
        Map<String, String> imgMap = new HashMap<>();
        ClassLoader classLoader = verifyCodeUtils.class.getClassLoader();
        for (int i = 1; i <= 6; i++) {
            InputStream resourceAsStream = classLoader.getResourceAsStream(path.concat("/").concat(String.valueOf(i).concat(".png")));
            byte[] bytes = new byte[0];
            try {
                bytes = FileCopyUtils.copyToByteArray(resourceAsStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String string = Base64Utils.encodeToString(bytes);
            String filename = String.valueOf(i).concat(".png");
            imgMap.put(filename, string);
        }
        return imgMap;
    }

    private static Map<String, String> getImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path+"/*.png");
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64Utils.encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgMap;
    }

    /**
     * 生成UUID
     *
     * @return
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }

    /**
     * 随机范围内数字
     * @param startNum
     * @param endNum
     * @return
     */
    public static Integer getRandomInt(int startNum, int endNum) {
        return new Random().nextInt(endNum-startNum) + startNum;
    }

    /**
     * 获取指定文字的随机中文
     *
     * @return
     */
    public static String getRandomHan(String hanZi) {
        String ch = hanZi.charAt(new Random().nextInt(hanZi.length())) + "";
        return ch;
    }

    /**
     * 图片转base64 字符串
     *
     * @param templateImage
     * @return
     */
    public static String getImageToBase64Str(BufferedImage templateImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(templateImage, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = baos.toByteArray();

        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(bytes).trim();
    }

    public static int getEnOrChLength(String s) {
        int enCount = 0;
        int chCount = 0;
        for (int i = 0; i < s.length(); i++) {
            int length = String.valueOf(s.charAt(i)).getBytes(StandardCharsets.UTF_8).length;
            if (length > 1) {
                chCount++;
            } else {
                enCount++;
            }
        }
        int chOffset = (PropertiesUtil.HAN_ZI_SIZE / 2) * chCount + 5;
        int enOffset = enCount * 8;
        return chOffset + enOffset;
    }


}
