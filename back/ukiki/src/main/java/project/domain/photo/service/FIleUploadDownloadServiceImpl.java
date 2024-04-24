package project.domain.photo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
public class FIleUploadDownloadServiceImpl implements FileUploadDownloadService{

    private final AmazonS3 amazonS3;
    @Value("${cloud.aws.s3.bucketName}")
    private static String bucketName;

    //커스텀 SSE KEY 인코딩
    public static String generateSSEKey(String inputKey) throws NoSuchAlgorithmException {
        // Get an instance of SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Hash the input text
        byte[] hash = digest.digest(inputKey.getBytes());

        // Encode the hash using Base64
        String base64EncodedKey = Base64.getEncoder().encodeToString(hash);

        return base64EncodedKey;
    }

    //이름 중복 방지를 위해 랜덤으로 생성
    private String changedImageName(String originName) {
        String random = UUID.randomUUID().toString();
        return random + originName;
    }

    @Override
    public List<String> fileUpload(List<MultipartFile> files, String inputKey, int partyId) throws Exception {
        // 허용할 MIME 타입들 설정 (이미지, 동영상 파일만 허용하는 경우)
        List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif", "video/mp4", "video/webm", "video/ogg", "video/3gpp", "video/x-msvideo", "video/quicktime");

        // 업로드한 파일의 업로드 경로를 담을 리스트
        List<String> urls = new ArrayList<>();

        // 로우 텍스트 키를 AWS SSE-C KEY 형식에 맞게 변환
        SSECustomerKey SSE_KEY = new SSECustomerKey(generateSSEKey(inputKey));

        for (MultipartFile file : files) {

            // 허용되지 않는 MIME 타입의 파일은 처리하지 않음
            String fileContentType = file.getContentType();
            if (!allowedMimeTypes.contains(fileContentType)) {
                throw new IllegalArgumentException("Unsupported file type");
            }

            ObjectMetadata metadata = new ObjectMetadata(); //메타데이터
            metadata.setContentLength(file.getSize()); // 파일 크기 명시
            metadata.setContentType(fileContentType);   // 파일 확장자 명시

            String originName = file.getOriginalFilename(); //원본 이미지 이름
            String changedName = changedImageName(originName); //새로 생성된 이미지 이름

            try {
                //이미지 업로드 전체 읽기 권한 허용, 데이터는 유저키로 암호화, 버킷 정책에 의해 유저키 없이 접근 불가
                amazonS3.putObject(new PutObjectRequest("ukkikki", changedName, file.getInputStream(), metadata
                ).withCannedAcl(CannedAccessControlList.PublicRead).withSSECustomerKey(SSE_KEY));
            } catch (IOException e) {
                log.error("file upload error " + e.getMessage());
            }
            urls.add(amazonS3.getUrl("ukkikki", changedName).toString());
        }
        return urls;
    }

    public void updateDatabase(int partyId, List<String> urls){
        //todo : 사진 업로드 후 데이터 베이스 업데이트 작업 수행
    }

    public void resizeImage(List<MultipartFile> files, String inputKey, int partyId) throws Exception {
        //todo : 썸네일 생성
    }
}
