package com.project.menusend.process;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.project.menusend.util.MenuProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MenuProperties.class)
public class Menusend {

    private final MenuProperties menuProperties;

    @PostConstruct
    public void send() throws Exception {
        // dailyMenuImage 있는지 확인
        if (checkDailyMenuImage()) {
            // dailyMenuImage 있는 경우 slack 발송
            String fileFullPath = findFilePath();
            // 토요일 일요일의 경우 fileFullPath null 반환
            if (fileFullPath != null) {
                // 텍스트 추출
                String menuStr = detectText(fileFullPath);
                // slack 전송

            }

        } else {
            // dailyMenuImage 없는 경우 mainImage 있는지 확인
            if (checkMainImage()) {
                // mainImage 있는 경우 이미지 dailyMenuImage 생성
                cutDailyMenuImage();

                // dailyMenuImage 생성 후 slack 발송
                String fileFullPath = findFilePath();
                // 토요일 일요일의 경우 fileFullPath null 반환
                if (fileFullPath != null) {
                    // 텍스트 추출
                    String menuStr = detectText(fileFullPath);
                    // slack 전송

                }

            } else {
                // mainImage 없는 경우 담당자에게 slack 발송

            }
        }
    }

    private boolean checkDailyMenuImage() {
        File file = new File(menuProperties.getBasePath() + "menu_1.jpg");
        return file.exists();
    }

    private boolean checkMainImage() {
        File file = new File(menuProperties.getBasePath() + "menu.jpg");
        return file.exists();
    }

    private void cutDailyMenuImage() throws Exception {
        final int pointX = 135;
        final int pointY = 345;
        final int height = 455;
        final int width = 260;
        final int distance = 12;

        File mainImage = new File(menuProperties.getBasePath() + "menu.jpg");
        BufferedImage bufferedImage = ImageIO.read(mainImage);

        for (int i = 0; i < 5; i++) {
            int startX = pointX + (width + distance) * i;
            BufferedImage subimage = bufferedImage.getSubimage(startX, pointY, width, height);

            int imageNm = i + 1;
            ImageIO.write(subimage, "jpg", new File(menuProperties.getBasePath() + "menu_" + imageNm + ".jpg"));
        }
    }

    private String findFilePath() {
        int dayNumber = LocalDate.now().getDayOfWeek().getValue();
        if (dayNumber == 6 || dayNumber == 7) {
            return null;
        }
        return menuProperties.getBasePath() + "menu_" + dayNumber + ".jpg";
    }

    // Detects text in the specified image.
    public String detectText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        String result = null;

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                }

                EntityAnnotation annotation = res.getTextAnnotationsList().get(0);
                result = annotation.getDescription();
            }
        }

        return result;
    }
}
