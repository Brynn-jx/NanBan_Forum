package com.NanBan.utils;

import com.NanBan.entity.config.AppConfig;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.enums.DateTimePatternEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("imageUtils")
public class ImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    @Resource
    private AppConfig appConfig;

    /**
     * @param file
     * @param thumbnailWidth  缩略图的宽度
     * @param thumbnailHeight 缩略图的高度
     * @param targetFile
     * @return
     */
    public static Boolean createThumbnail(File file, int thumbnailWidth, int thumbnailHeight, File targetFile) {
        try {
            BufferedImage src = ImageIO.read(file);
            int sorceW = src.getWidth();
            int sorceH = src.getHeight();
            // 小于置顶高宽不压缩
            if (sorceW <= thumbnailWidth) {
                return false;
            }
            int height = sorceH; // 目标文件的高度
            if (sorceW > thumbnailWidth) {    // 目标文件的宽度大于指定宽度
                height = thumbnailWidth * sorceH / sorceW;
            } else {
                thumbnailWidth = sorceW;
                height = sorceH;
            }
            // 生成宽度为150的缩略图
            BufferedImage dst = new BufferedImage(thumbnailWidth, height, BufferedImage.TYPE_INT_RGB);
            Image scaleImage = src.getScaledInstance(thumbnailWidth, height, Image.SCALE_SMOOTH);
            Graphics2D g = dst.createGraphics();
            g.drawImage(scaleImage, 0, 0, thumbnailWidth, height, null);
            g.dispose();

            int resultH = dst.getHeight();
            // 高度过大，裁减图片
            if (resultH > thumbnailHeight) {
                resultH = thumbnailHeight;
                dst = dst.getSubimage(0, 0, thumbnailWidth, resultH);
            }
            ImageIO.write(dst, "JPEG", targetFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String resetImageHtml(String html) {
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        List<String> imageList = getImageList(html);
        for (String img : imageList) {
            resetImage(img, month);
        }
        return month;
    }


    private String resetImage(String imagePath, String month) {
        if (StringTools.isEmpty(imagePath) || !imagePath.contains(Constants.FILE_FOLDER_TEMP_2)) {
            return imagePath;
        }
        imagePath = imagePath.replace(Constants.READ_IMAGE_PATH, "");
        if (StringTools.isEmpty(month)) {
            month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        }
        String imageFileName = month + "/" + imagePath.substring(imagePath.lastIndexOf("/") + 1);

        File targeFile = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_IMAGE + imageFileName);
        try {
            File tempFile = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imagePath);
            org.apache.commons.io.FileUtils.copyFile(tempFile, targeFile);
            tempFile.delete();
        } catch (IOException e) {
            logger.error("复制图片失败", e);
            return imagePath;
        }
        return imageFileName;
    }

    /**
     * 通过正则表达式解析html图片
     *
     * @param html
     * @return
     */
    private List<String> getImageList(String html) {
        List<String> imageList = new ArrayList<>();
        String regEx_img = "(<img.*src\\s*=\\s*(.*?)[^>]*?>)";
        Pattern p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
        Matcher m_image = p_image.matcher(html);
        while (m_image.find()) {
            String img = m_image.group();
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
            while (m.find()) {
                String imageUrl = m.group(1);
                imageList.add(imageUrl);
            }
        }
        return imageList;
    }
}
