package com.memento.util;

import com.memento.entity.Memory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfUtilsTest {

    @Test
    void generateValidChinesePdf() throws Exception {
        Memory m1 = new Memory();
        m1.setTitle("人生第一辆车");
        m1.setContent("买了人生第一辆车，心情非常激动！\n\n终于可以自由地到处游玩了。");
        m1.setEventDate(LocalDate.of(2024, 5, 20));
        m1.setTags("生活, 激动");
        m1.setSentimentScore(BigDecimal.valueOf(0.9));

        Memory m2 = new Memory();
        m2.setTitle("毕业旅行");
        m2.setContent("和朋友们一起去了云南，看到了美丽的洱海。");
        m2.setEventDate(LocalDate.of(2023, 7, 1));
        m2.setTags("旅行");
        m2.setSentimentScore(BigDecimal.valueOf(0.5));

        List<Memory> memories = Arrays.asList(m1, m2);
        byte[] pdf = PdfUtils.generateMemoryBook(memories, "拾光记", "测试用户");

        assertTrue(pdf.length > 1000, "PDF 应有实质内容");
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);

        String tail = new String(pdf, pdf.length - 32, 32);
        assertTrue(tail.contains("%%EOF"), "PDF 应以 %%EOF 结尾");

        Path out = Path.of("target", "test_chinese_export.pdf");
        Files.write(out, pdf);
        assertTrue(Files.size(out) > 1000);
    }
}
