// 파일명: ConvertMain.java
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwpxlib.object.HWPXFile;

public class ConvertMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java ConvertMain <input.hwp> <output.hwpx>");
            return;
        }

        String input = args[0];
        String output = args[1];

        HWPFile fromFile = HWPReader.fromFile(input);
        HWPXFile toFile = Hwp2Hwpx.toHWPX(fromFile);
        HWPXWriter.toFilepath(toFile, output);

        System.out.println("변환 완료: " + output);
    }
}
