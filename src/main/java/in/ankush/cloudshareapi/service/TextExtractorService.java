package in.ankush.cloudshareapi.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Service
public class TextExtractorService {

    private static final int MAX_CHARS = 20_000;

    public String extractText(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        try {
            if (contentType.equals("application/pdf") || filename.endsWith(".pdf")) {
                return extractFromPdf(file);
            }
            if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    || filename.endsWith(".docx")) {
                return extractFromDocx(file);
            }
            if (contentType.startsWith("text/") || filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".csv")) {
                return extractFromPlainText(file);
            }
        } catch (Exception e) {
            System.err.println("Text extraction failed for " + filename + ": " + e.getMessage());
            return null;
        }

        return null; 
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return truncate(text);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return truncate(extractor.getText());
        }
    }

    private String extractFromPlainText(MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return truncate(text);
    }

    private String truncate(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;
        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }
}
