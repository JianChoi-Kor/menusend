package com.project.menusend.process;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.project.menusend.util.WebhookService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class Menusend {

    @Value("${menu.base-path}")
    private String basePath;

    private final WebhookService webhookService;

    @PostConstruct
    public void send() throws Exception {
        int dayNumber = LocalDate.now().getDayOfWeek().getValue();
        // 토요일 일요일의 경우 처리
        if (dayNumber == 6 || dayNumber == 7) {
            // 폴더 내 모든 이미지 삭제 처리
            deleteAllFiles();
        }
        // 평일의 경우
        else {
            // dailyMenuImage 있는지 확인
            if (checkDailyMenuImage()) {
                // dailyMenuImage 있는 경우 slack 발송
                String fileFullPath = findFilePath(dayNumber);
                // 텍스트 추출
                String menuStr = detectText(fileFullPath);
                // slack 전송
                webhookService.sendMenuMessage(menuStr);

            } else {
                // dailyMenuImage 없는 경우 mainImage 있는지 확인
                if (checkMainImage()) {
                    // mainImage 있는 경우 이미지 dailyMenuImage 생성
                    cutDailyMenuImage();

                    // dailyMenuImage 생성 후 slack 발송
                    String fileFullPath = findFilePath(dayNumber);
                    // 텍스트 추출
                    String menuStr = detectText(fileFullPath);
                    // slack 전송
                    webhookService.sendMenuMessage(menuStr);

                } else {
                    // mainImage 없는 경우 담당자에게 slack 발송

                }
            }
        }
    }

    private boolean checkDailyMenuImage() {
        File file = new File(basePath + "menu_1.jpg");
        return file.exists();
    }

    private boolean checkMainImage() {
        File file = new File(basePath + "menu.jpg");
        return file.exists();
    }

    private void cutDailyMenuImage() throws Exception {
        File mainImage = new File(basePath + "menu.jpg");
        BufferedImage bufferedImage = ImageIO.read(mainImage);

        int width = bufferedImage.getWidth();
        int eachWidth = width/5;
        int height = bufferedImage.getHeight();

        for (int i = 0; i < 5; i++) {
            int startX = eachWidth * i;
            BufferedImage subImage = bufferedImage.getSubimage(startX, 0, eachWidth, height);

            int imageNm = i + 1;
            ImageIO.write(subImage, "jpg", new File(basePath + "menu_" + imageNm + ".jpg"));
        }
    }

    private String findFilePath(int dayNumber) {
        return basePath + "menu_" + dayNumber + ".jpg";
    }

    private void deleteAllFiles() {
        File baseDirectory = new File(basePath);
        File[] files = baseDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
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
                    // error 처리
                    System.out.format("Error: %s%n", res.getError().getMessage());
                }

                EntityAnnotation annotation = res.getTextAnnotationsList().get(0);
                result = annotation.getDescription();
            }
        }

        return result;
    }
}
