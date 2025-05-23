package kr.dogfoot.hwpxapi;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.object.bodytext.Section;

import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.RunItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Ctrl;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.CtrlItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.TItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.NormalText;
import kr.dogfoot.hwpxlib.object.common.ObjectType;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.StandardCopyOption;

import io.smallrye.mutiny.Multi;

@Path("/extract")
public class HwpxResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response extractText(@FormParam("file") FileUpload file) {
        String filename = file.fileName().toLowerCase();
        System.out.println("🔥 /extract 진입: " + filename);
        File tempFile = null;
        try (InputStream is = file.uploadedFile().toUri().toURL().openStream()) {

            // 임시 저장
            tempFile = File.createTempFile("upload", filename.endsWith(".hwp") ? ".hwp" : ".hwpx");
            Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            HWPXFile hwpx;

            if (filename.endsWith(".hwp")) {
                HWPFile hwp = HWPReader.fromFile(tempFile);
                hwpx = Hwp2Hwpx.toHWPX(hwp);
            } else if (filename.endsWith(".hwpx")) {
                hwpx = HWPXReader.fromFile(tempFile);
            } else {
                return Response.status(400).entity("Only .hwp or .hwpx supported").build();
            }

            String text = TextExtractor.extract(hwpx, TextExtractMethod.InsertControlTextBetweenParagraphText, true, new TextMarks());
            return Response.ok(text).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        } finally {
            if (tempFile != null) tempFile.delete();
        }
    }
    
    @POST
    @Path("/extract-test")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> extractHwpTest(@RestForm FileUpload file) {
        System.out.println("🔥 진입 성공: " + file.fileName());
        return Multi.createFrom().items("✅ 작동 확인 완료");
    }


    @POST
    @Path("/extract-hwp-stream")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> extractHwpStream(@RestForm FileUpload file) {
        System.out.println("🔥 [API 호출]: /extract-hwp-stream");
        File tempFile;
        try (InputStream is = file.uploadedFile().toUri().toURL().openStream()) {
            String filename = file.fileName().toLowerCase();
            if (!filename.endsWith(".hwp") && !filename.endsWith(".hwpx")) {
                return Multi.createFrom().item("Only .hwp or .hwpx supported");
            }

            tempFile = File.createTempFile("upload", filename.endsWith(".hwp") ? ".hwp" : ".hwpx");
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            final HWPXFile hwpx;
            if (filename.endsWith(".hwp")) {
                HWPFile hwp = HWPReader.fromFile(tempFile);
                hwpx = Hwp2Hwpx.toHWPX(hwp);
            } else {
                hwpx = HWPXReader.fromFile(tempFile);
            }
            return Multi.createFrom().emitter(em -> {
                for (SectionXMLFile sectionFile : hwpx.sectionXMLFileList().items()) {
                    for (Para para : sectionFile.paras()) {
                        boolean hasText = false;

                        for (Run run : para.runs()) {
                            for (int i = 0; i < run.countOfRunItem(); i++) {
                                RunItem item = run.getRunItem(i);

                                // ✅ 일반 텍스트
                                if (item instanceof T tItem) {
                                    // 먼저 onlyText
                                    if (tItem.isOnlyText()) {
                                        String text = tItem.onlyText();
                                        if (text != null && !text.isBlank()) {
                                            em.emit(text);
                                            hasText = true;
                                        }
                                    } else {
                                        for (TItem ti : tItem.items()) {
                                            if (ti instanceof NormalText nt) {
                                                String text = nt.text();
                                                if (text != null && !text.isBlank()) {
                                                    em.emit(text);
                                                    hasText = true;
                                                }
                                            }
                                        }
                                    }

                                // ✅ 표 같은 컨트롤
                                } else if (item instanceof Table table) {
                                    try {
                                        String tableText = TextExtractor.extractFrom(
                                            table,  // 직접 Table로 안전하게 접근
                                            TextExtractMethod.InsertControlTextBetweenParagraphText,
                                            new TextMarks()
                                                .tableCellSeparatorAnd(" | ")
                                                .tableRowSeparatorAnd("\n")
                                        );
                                        em.emit(tableText);
                                    } catch (Exception e) {
                                        em.emit("❌ 표 파싱 실패: " + e.getMessage());
                                    }
                                }
                            }
                        }

                        if (hasText) em.emit("\n"); // 문단 구분
                    }
                }
                em.complete();
            });

        } catch (Exception e) {
            e.printStackTrace();
            return Multi.createFrom().item("❌ Error: " + e.getMessage());
        }
    }
}
