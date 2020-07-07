package com.ihandy.verifycode.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ihandy.verifycode.model.CaptchaModel;
import com.ihandy.verifycode.model.PointModel;
import com.ihandy.verifycode.service.CaptchaService;
import com.ihandy.verifycode.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class captchaServicelmpl implements CaptchaService {

    private static Logger logger = LoggerFactory.getLogger(captchaServicelmpl.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 产生验证码
     * @param captchaModel
     * @return
     */
    @Override
    public ResponseData getVerifyCode(CaptchaModel captchaModel) {
        BufferedImage bufferedImage = null;
        CaptchaModel captcha = null;
        if("vcByWord".equals(captchaModel.getCaptchaType())){
            bufferedImage = verifyCodeUtils.getPicClick();
            if (null == bufferedImage) {
                logger.error("点字底图未初始化成功，请检查路径");
                return ResponseData.init(ResponseCode.FAIL.getValue(),"点字原生图片初始化失败，请检查路径");
            }
            captcha = getImageData(bufferedImage);
            if (captcha == null || StringUtils.isBlank(captcha.getOriginalImageBase64())) {
                return ResponseData.init(ResponseCode.FAIL.getValue(),"获取验证码失败,请联系管理员");
            }
            captcha.setCaptchaType("vcByWord");
        }else{
            bufferedImage = verifyCodeUtils.getOriginal();
            if (null == bufferedImage) {
                logger.error("滑动底图未初始化成功，请检查路径");
                return ResponseData.init(ResponseCode.FAIL.getValue(),"滑动原生图片初始化失败，请检查路径");
            }
            //设置水印
            Graphics backgroundGraphics = bufferedImage.getGraphics();
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            Font watermark = new Font(PropertiesUtil.waterFont, Font.BOLD, PropertiesUtil.HAN_ZI_SIZE / 2);
            backgroundGraphics.setFont(watermark);
            backgroundGraphics.setColor(Color.white);
            backgroundGraphics.drawString(PropertiesUtil.waterMark, width - PropertiesUtil.getEnOrChLength(PropertiesUtil.waterMark), height - (PropertiesUtil.HAN_ZI_SIZE / 2) + 7);

            //抠图图片
            String jigsawImageBase64 = verifyCodeUtils.getslidingBlock();
            BufferedImage jigsawImage = verifyCodeUtils.getBase64StrToImage(jigsawImageBase64);
            if (null == jigsawImage) {
                logger.error("滑动底图初始化失败，请检查路径");
                return ResponseData.init(ResponseCode.FAIL.getValue(),"滑动底图初始化失败，请检查路径");
            }
            captcha = pictureTemplatesCut(bufferedImage, jigsawImage, jigsawImageBase64);
            if (captcha == null || StringUtils.isBlank(captcha.getJigsawImageBase64()) || StringUtils.isBlank(captcha.getOriginalImageBase64())) {
                return ResponseData.init(ResponseCode.FAIL.getValue(),"获取验证码失败,请联系管理员");
            }
            captcha.setCaptchaType("vcByImg");
        }
        return ResponseData.init(ResponseCode.SUCCESS.getValue(),"获取验证码成功",captcha);
    }

    /**
     * 校验验证码
     * @param captchaModel
     * @return
     */
    @Override
    public ResponseData checkVerifyCode(CaptchaModel captchaModel) {
        //取坐标信息
        String codeKey = captchaModel.getUuId();
        if(StringUtils.isEmpty(codeKey)){
            return ResponseData.init(ResponseCode.FAIL.getValue(),"验证码唯一标识UUID不能为空");
        }
        if(StringUtils.isEmpty(captchaModel.getCaptchaType())){
            return ResponseData.init(ResponseCode.FAIL.getValue(),"验证码类型不能为空。");
        }
        if (!redisTemplate.hasKey(codeKey)) {
            return ResponseData.init(ResponseCode.FAIL.getValue(),"验证码已过期，请刷新后重试。");
        }
        String verifycode = redisTemplate.opsForValue().get(codeKey);
        //验证码只用一次，即刻失效
        redisTemplate.delete(codeKey);
        if("vcByWord".equals(captchaModel.getCaptchaType())){
            List<PointModel> pointByFrontList = null;
            List<PointModel> pointByRedisList = null;
            //点字验证码检验
            try {
                pointByFrontList = JSONObject.parseArray(captchaModel.getParamsJson(), PointModel.class);
                pointByRedisList = JSONObject.parseArray(verifycode, PointModel.class);
            } catch (Exception e) {
                logger.error("验证码坐标解析失败", e);
                return ResponseData.init(ResponseCode.FAIL.getValue(),"验证码坐标解析失败");
            }
            for (int i = 0; i < pointByRedisList.size(); i++) {
                if (pointByRedisList.get(i).x - PropertiesUtil.HAN_ZI_SIZE > pointByFrontList.get(i).x
                        || pointByFrontList.get(i).x > pointByRedisList.get(i).x + PropertiesUtil.HAN_ZI_SIZE
                        || pointByRedisList.get(i).y - PropertiesUtil.HAN_ZI_SIZE > pointByFrontList.get(i).y
                        || pointByFrontList.get(i).y > pointByRedisList.get(i).y + PropertiesUtil.HAN_ZI_SIZE) {
                    return ResponseData.init(ResponseCode.FAIL.getValue(),"验证失败");
                }
            }
            return ResponseData.init(ResponseCode.SUCCESS.getValue(),"验证成功");
        }else{
            PointModel pointByRedis = null;   //Redis中存储的坐标
            PointModel pointByFront = null;   //前端传过来的坐标
            //图片验证码检验
            try {
                pointByFront = JSONObject.parseObject(String.valueOf(JSONObject.parseObject(captchaModel.getParamsJson())), PointModel.class);
                pointByRedis = JSONObject.parseObject(verifycode, PointModel.class);
            } catch (Exception e) {
                logger.error("验证码坐标解析失败", e);
                return ResponseData.init(ResponseCode.FAIL.getValue(),"验证码坐标解析失败");
            }
            if (pointByRedis.x - Integer.parseInt(PropertiesUtil.slipOffset) > pointByFront.x
                    || pointByFront.x > pointByRedis.x + Integer.parseInt(PropertiesUtil.slipOffset)
                    || pointByRedis.y != pointByFront.y) {
                return ResponseData.init(ResponseCode.FAIL.getValue(),"验证失败");
            }else{
                return ResponseData.init(ResponseCode.SUCCESS.getValue(),"验证成功");
            }
        }
    }

    /**
     *  生成点字图片
     */
    private CaptchaModel getImageData(BufferedImage backgroundImage) {
        CaptchaModel captchaModel = new CaptchaModel();
        java.util.List<String> wordList = new ArrayList<String>();
        List<PointModel> pointList = new ArrayList();

        Graphics backgroundGraphics = backgroundImage.getGraphics();
        int width = backgroundImage.getWidth();
        int height = backgroundImage.getHeight();

        Font font = new Font(PropertiesUtil.fontType, Font.BOLD, PropertiesUtil.HAN_ZI_SIZE);
        int wordCount = PropertiesUtil.wordTotalCount;
        //定义随机1到arr.length某一个字不参与校验
        int num = verifyCodeUtils.getRandomInt(1, wordCount);
        Set<String> currentWords = new HashSet<String>();
        for (int i = 0; i < wordCount; i++) {
            String word;
            do {
                word = verifyCodeUtils.getRandomHan(PropertiesUtil.HAN_ZI);
                currentWords.add(word);
            } while (!currentWords.contains(word));

            //随机字体坐标
            PointModel point = randomWordPoint(width, height, i, wordCount);
            //随机字体颜色
            backgroundGraphics.setColor(new Color(verifyCodeUtils.getRandomInt(1, 255), verifyCodeUtils.getRandomInt(1, 255), verifyCodeUtils.getRandomInt(1, 255)));
            //设置角度
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(Math.toRadians(verifyCodeUtils.getRandomInt(-45, 45)), 0, 0);
            Font rotatedFont = font.deriveFont(affineTransform);
            backgroundGraphics.setFont(rotatedFont);
            backgroundGraphics.drawString(word, point.getX(), point.getY());

            if ((num - 1) != i) {
                wordList.add(word);
                pointList.add(point);
            }
        }
        Font watermark = new Font(PropertiesUtil.fontType, Font.BOLD, PropertiesUtil.HAN_ZI_SIZE / 2);
        backgroundGraphics.setFont(watermark);
        backgroundGraphics.setColor(Color.white);
        backgroundGraphics.drawString(PropertiesUtil.waterMark, width - verifyCodeUtils.getEnOrChLength(PropertiesUtil.waterMark), height - (PropertiesUtil.HAN_ZI_SIZE / 2) + 7);

        //创建合并图片
        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics combinedGraphics = combinedImage.getGraphics();
        combinedGraphics.drawImage(backgroundImage, 0, 0, null);

        captchaModel.setOriginalImageBase64(verifyCodeUtils.getImageToBase64Str(backgroundImage).replaceAll("\r|\n", ""));
        captchaModel.setWordList(wordList);
        captchaModel.setUuId(verifyCodeUtils.getUUID());

        //将坐标信息存入redis中 并设置有效时间为1分钟
        redisTemplate.opsForValue().set(captchaModel.getUuId(),JSONObject.toJSONString(pointList));
        redisTemplate.expire(captchaModel.getUuId(), PropertiesUtil.verifyCodeTime, TimeUnit.SECONDS);
        logger.debug("token：{},point:{}", captchaModel.getUuId(), JSONObject.toJSONString(pointList));
        return captchaModel;
    }


    /**
     * 根据模板切图
     */
    public CaptchaModel pictureTemplatesCut(BufferedImage originalImage, BufferedImage jigsawImage, String jigsawImageBase64) {
        try {
            CaptchaModel captchaModel = new CaptchaModel();

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int jigsawWidth = jigsawImage.getWidth();
            int jigsawHeight = jigsawImage.getHeight();

            //随机生成拼图坐标
            PointModel point = generateJigsawPoint(originalWidth, originalHeight, jigsawWidth, jigsawHeight);
            int x = point.getX();
            int y = point.getY();

            //生成新的拼图图像
            BufferedImage newJigsawImage = new BufferedImage(jigsawWidth, jigsawHeight, jigsawImage.getType());
            Graphics2D graphics = newJigsawImage.createGraphics();

            int bold = 5;
            //如果需要生成RGB格式，需要做如下配置,Transparency 设置透明
            newJigsawImage = graphics.getDeviceConfiguration().createCompatibleImage(jigsawWidth, jigsawHeight, Transparency.TRANSLUCENT);
            // 新建的图像根据模板颜色赋值,源图生成遮罩
            cutByTemplate(originalImage, jigsawImage, newJigsawImage, x, 0);
            if (PropertiesUtil.interferenceOptions > 0) {
                int position = 0;
                if (originalWidth - x - 5 > jigsawWidth * 2) {
                    //在原扣图右边插入干扰图
                    position = verifyCodeUtils.getRandomInt(x + jigsawWidth + 5, originalWidth - jigsawWidth);
                } else {
                    //在原扣图左边插入干扰图
                    position = verifyCodeUtils.getRandomInt(100, x - jigsawWidth - 5);
                }
                while (true) {
                    String s = verifyCodeUtils.getslidingBlock();
                    if (!jigsawImageBase64.equals(s)) {
                        interferenceByTemplate(originalImage, Objects.requireNonNull(verifyCodeUtils.getBase64StrToImage(s)), position, 0);
                        break;
                    }
                }
            }
            if (PropertiesUtil.interferenceOptions > 1) {
                while (true) {
                    String s = verifyCodeUtils.getslidingBlock();
                    if (!jigsawImageBase64.equals(s)) {
                        Integer randomInt = verifyCodeUtils.getRandomInt(jigsawWidth, 100 - jigsawWidth);
                        interferenceByTemplate(originalImage, Objects.requireNonNull(verifyCodeUtils.getBase64StrToImage(s)),randomInt, 0);
                        break;
                    }
                }
            }

            // 设置“抗锯齿”的属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(newJigsawImage, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream();//新建流。
            ImageIO.write(newJigsawImage, PropertiesUtil.IMAGE_TYPE_PNG, os);//利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
            byte[] jigsawImages = os.toByteArray();

            ByteArrayOutputStream oriImagesOs = new ByteArrayOutputStream();//新建流。
            ImageIO.write(originalImage, PropertiesUtil.IMAGE_TYPE_PNG, oriImagesOs);//利用ImageIO类提供的write方法，将bi以jpg图片的数据模式写入流。
            byte[] oriCopyImages = oriImagesOs.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            captchaModel.setOriginalImageBase64(encoder.encodeToString(oriCopyImages).replaceAll("\r|\n", ""));
            captchaModel.setJigsawImageBase64(encoder.encodeToString(jigsawImages).replaceAll("\r|\n", ""));
            captchaModel.setUuId(verifyCodeUtils.getUUID());

            //将坐标信息存入redis中 并设置有效时间
            redisTemplate.opsForValue().set(captchaModel.getUuId(),JSONObject.toJSONString(point));
            redisTemplate.expire(captchaModel.getUuId(), PropertiesUtil.verifyCodeTime, TimeUnit.SECONDS);
            logger.debug("token：{},point:{}", captchaModel.getUuId(), JSONObject.toJSONString(point));
            return captchaModel;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 随机生成拼图坐标
     *
     * @param originalWidth
     * @param originalHeight
     * @param jigsawWidth
     * @param jigsawHeight
     * @return
     */
    private PointModel generateJigsawPoint(int originalWidth, int originalHeight, int jigsawWidth, int jigsawHeight) {
        Random random = new Random();
        int widthDifference = originalWidth - jigsawWidth;
        int heightDifference = originalHeight - jigsawHeight;
        int x, y = 0;
        if (widthDifference <= 0) {
            x = 5;
        } else {
            x = random.nextInt(originalWidth - jigsawWidth - 100) + 100;
        }
        if (heightDifference <= 0) {
            y = 5;
        } else {
            y = random.nextInt(originalHeight - jigsawHeight) + 5;
        }
        return new PointModel(x, y);
    }

    /**
     * @param oriImage      原图
     * @param templateImage 模板图
     * @param newImage      新抠出的小图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     * @throws Exception
     */
    private static void cutByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage newImage, int x, int y) {
        //临时数组遍历用于高斯模糊存周边像素值
        int[][] martrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    newImage.setRGB(i, j, oriImage.getRGB(x + i, y + j));

                    //抠图区域高斯模糊
                    readPixel(oriImage, x + i, y + j, values);
                    fillMatrix(martrix, values);
                    oriImage.setRGB(x + i, y + j, avgMatrix(martrix));
                }

                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    newImage.setRGB(i, j, Color.white.getRGB());
                    oriImage.setRGB(x + i, y + j, Color.white.getRGB());
                }
            }
        }
    }

    /**
     * 干扰抠图处理
     *
     * @param oriImage      原图
     * @param templateImage 模板图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     * @throws Exception
     */
    private static void interferenceByTemplate(BufferedImage oriImage, BufferedImage templateImage, int x, int y) {
        //临时数组遍历用于高斯模糊存周边像素值
        int[][] martrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    //抠图区域高斯模糊
                    readPixel(oriImage, x + i, y + j, values);
                    fillMatrix(martrix, values);
                    oriImage.setRGB(x + i, y + j, avgMatrix(martrix));
                }
                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    oriImage.setRGB(x + i, y + j, Color.white.getRGB());
                }
            }
        }
    }

    private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;
        for (int i = xStart; i < 3 + xStart; i++) {
            for (int j = yStart; j < 3 + yStart; j++) {
                int tx = i;
                if (tx < 0) {
                    tx = -tx;

                } else if (tx >= img.getWidth()) {
                    tx = x;
                }
                int ty = j;
                if (ty < 0) {
                    ty = -ty;
                } else if (ty >= img.getHeight()) {
                    ty = y;
                }
                pixels[current++] = img.getRGB(tx, ty);

            }
        }
    }

    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;
        for (int i = 0; i < matrix.length; i++) {
            int[] x = matrix[i];
            for (int j = 0; j < x.length; j++) {
                x[j] = values[filled++];
            }
        }
    }

    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < matrix.length; i++) {
            int[] x = matrix[i];
            for (int j = 0; j < x.length; j++) {
                if (j == 1) {
                    continue;
                }
                Color c = new Color(x[j]);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        return new Color(r / 8, g / 8, b / 8).getRGB();
    }

    /**
     * 随机字体循环排序下标
     *
     * @param imageWidth    图片宽度
     * @param imageHeight   图片高度
     * @param wordSortIndex 字体循环排序下标(i)
     * @param wordCount     字数量
     * @return
     */
    private static PointModel randomWordPoint(int imageWidth, int imageHeight, int wordSortIndex, int wordCount) {
        int avgWidth = imageWidth / (wordCount + 1);
        int x, y;
        if (avgWidth < PropertiesUtil.HAN_ZI_SIZE_HALF) {
            x = verifyCodeUtils.getRandomInt(1 + PropertiesUtil.HAN_ZI_SIZE_HALF, imageWidth);
        } else {
            if (wordSortIndex == 0) {
                x = verifyCodeUtils.getRandomInt(1 + PropertiesUtil.HAN_ZI_SIZE_HALF, avgWidth * (wordSortIndex + 1) - PropertiesUtil.HAN_ZI_SIZE_HALF);
            } else {
                x = verifyCodeUtils.getRandomInt(avgWidth * wordSortIndex + PropertiesUtil.HAN_ZI_SIZE_HALF, avgWidth * (wordSortIndex + 1) - PropertiesUtil.HAN_ZI_SIZE_HALF);
            }
        }
        y = verifyCodeUtils.getRandomInt(PropertiesUtil.HAN_ZI_SIZE, imageHeight - PropertiesUtil.HAN_ZI_SIZE);
        return new PointModel(x, y);
    }
}
