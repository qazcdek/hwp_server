# 서버 실행
mvn clean compile quarkus:dev "-Dquarkus.enforceBuildGoal=false"<br>
-각 패키지(hwplib, hwpxlib, hwp2hwpx) 설치<br>
mvn clean package "-DskipTests"

# 업로드 및 답변 텍스트 허용제한, 포트번호 구성 파일
.\src\main\resources\application.properties

# 엔드포인트
- 구성파일: .\src\main\java\kr\dogfoot\hwpxapi\HwpxResource.java

- baseurl: http://localhost:8080

- /extract/extract-test: 서버 접속 확인

- /extract: hwp 파일을 파싱한 텍스트 그대로 전달<br>
curl.exe -X POST -H "Accept: text/plain" -F "file=@path/to/file.hwp" http://localhost:8080/extract
curl.exe -X POST -H "Accept: text/plain" -F "file=@C:\Users\Admin\Desktop\i_bricks\code_space\test_space\storage\hwp_dir\real_01.hwp" http://localhost:8080/extract

- /extract/extract-hwp-stream: hwp파일을 스트리밍<br>
curl.exe -v -N -X POST -H "Accept: text/event-stream" -F "file=@path/to/file.hwp" http://localhost:8080/extract/extract-hwp-stream
curl.exe -v -N -X POST -H "Accept: text/event-stream" -F "file=@C:\Users\Admin\Desktop\i_bricks\code_space\test_space\storage\hwp_dir\real_01.hwp" http://localhost:8080/extract/extract-hwp-stream