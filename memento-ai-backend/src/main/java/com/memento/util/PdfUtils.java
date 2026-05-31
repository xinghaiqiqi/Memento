package com.memento.util;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.entity.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆册 PDF 生成工具。
 * 中文字体优先使用系统微软雅黑/黑体，回退到 iText font-asian 内置宋体；
 * 不对中文字体调用 setBold()，避免 PDF 结构异常导致阅读器报损。
 */
public class PdfUtils {

    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(93, 64, 55);
    private static final DeviceRgb COLOR_ACCENT = new DeviceRgb(141, 110, 99);
    private static final DeviceRgb COLOR_TEXT = new DeviceRgb(62, 39, 35);
    private static final DeviceRgb COLOR_MUTED = new DeviceRgb(121, 85, 72);
    private static final DeviceRgb COLOR_LIGHT = new DeviceRgb(215, 204, 200);
    private static final DeviceRgb COLOR_BG = new DeviceRgb(250, 247, 245);
    private static final DeviceRgb COLOR_WATERMARK = new DeviceRgb(230, 220, 215);

    private static final DateTimeFormatter DATE_FULL =
            DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日");
    private static final DateTimeFormatter DATE_SHORT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 封面=1，目录=2，正文从第 3 页起 */
    private static final int FIRST_CONTENT_PAGE = 3;

    public static byte[] generateMemoryBook(List<Memory> memories, String title, String author)
            throws IOException {
        return generateMemoryBook(memories, List.of(), title, author);
    }

    public static byte[] generateMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                            String title, String author)
            throws IOException {
        return generateMemoryBook(memories, narratives, List.of(), title, author);
    }

    public static byte[] generateMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                            List<Memory> chartMemories, String title, String author)
            throws IOException {
        return generateMemoryBook(memories, narratives, chartMemories, title, author, author);
    }

    public static byte[] generateMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                            List<Memory> chartMemories, String title, String author,
                                            String ownerName)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
                new PageNumberHandler(false));
        Document document = new Document(pdf);

        try {
            FontPair fonts = getFonts();
            document.setMargins(56, 52, 56, 52);

            addCover(document, fonts, title, ownerName);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addTableOfContents(document, fonts, memories);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            for (int i = 0; i < memories.size(); i++) {
                addMemoryPage(document, fonts, memories.get(i), i + 1, FIRST_CONTENT_PAGE + i);
                if (i < memories.size() - 1 || !narratives.isEmpty()) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
            }

            addNarrativeSection(document, fonts, narratives, !memories.isEmpty());
            addStatisticsSection(document, fonts, chartMemories, false);

            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addBackCover(document, fonts);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private static void addStatisticsSection(Document document, FontPair fonts, List<Memory> chartMemories, boolean hasPreviousContent) {
        if (chartMemories == null || chartMemories.isEmpty()) {
            return;
        }
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        document.add(p("统计", fonts.title, 22, COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6));
        document.add(p("STATISTICS", fonts.body, 9, COLOR_MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(2)
                .setMarginBottom(24));

        Div panel = new Div()
                .setBackgroundColor(COLOR_BG)
                .setBorder(new SolidBorder(COLOR_LIGHT, 0.6f))
                .setBorderRadius(new BorderRadius(4))
                .setPadding(22);

        StatisticsSummary summary = buildStatisticsSummary(chartMemories);
        for (String line : buildStatisticsLines(summary)) {
            panel.add(p(line, fonts.body, 12, COLOR_TEXT)
                    .setMultipliedLeading(1.65f)
                    .setMarginBottom(6));
        }
        panel.add(createEmotionBarChart(fonts, summary, false).setMarginTop(14));
        document.add(panel);
    }

    private static void addCover(Document document, FontPair fonts, String title, String ownerName) {
        Div frame = new Div()
                .setBackgroundColor(new DeviceRgb(62, 39, 35))
                .setBorder(new SolidBorder(new DeviceRgb(215, 204, 200), 1f))
                .setBorderRadius(new BorderRadius(6))
                .setPaddingTop(52)
                .setPaddingBottom(52)
                .setPaddingLeft(44)
                .setPaddingRight(44)
                .setMarginTop(28)
                .setMinHeight(560);

        frame.add(p(safeText(title), fonts.title, 34, new DeviceRgb(215, 204, 200))
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(3)
                .setMarginTop(100)
                .setMarginBottom(8));

        frame.add(p("MEMENTO AI", fonts.body, 12, new DeviceRgb(183, 156, 147))
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(4)
                .setMarginBottom(26));

        frame.add(p((ownerName == null || ownerName.isBlank() ? "记忆收藏家" : safeText(ownerName)) + "  著", fonts.body, 16, new DeviceRgb(215, 204, 200))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(150));

        frame.add(p("—  永恒的记忆  —", fonts.body, 13, new DeviceRgb(188, 170, 164))
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(1.2f));

        document.add(frame);
    }

    private static void addTableOfContents(Document document, FontPair fonts, List<Memory> memories) {
        document.add(p("目  录", fonts.title, 22, COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6));
        document.add(p("CONTENTS", fonts.body, 9, COLOR_MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(2)
                .setMarginBottom(24));

        SolidLine sep = new SolidLine(0.4f);
        sep.setColor(COLOR_LIGHT);
        document.add(new LineSeparator(sep).setMarginBottom(16));

        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 52, 38}))
                .useAllAvailableWidth();

        for (int i = 0; i < memories.size(); i++) {
            Memory memory = memories.get(i);
            int chapter = i + 1;
            int pageNo = FIRST_CONTENT_PAGE + i;
            String chapterNo = formatChapterNo(chapter);
            String memoryTitle = resolveTitle(memory);
            String dateStr = memory.getEventDate() != null
                    ? memory.getEventDate().format(DATE_SHORT) : "未知日期";

            table.addCell(tocCell(p(chapterNo, fonts.title, 12, COLOR_ACCENT)));
            table.addCell(tocCell(p(memoryTitle, fonts.body, 12, COLOR_TEXT)));
            table.addCell(tocCell(p(dateStr + "    第 " + pageNo + " 页", fonts.body, 10, COLOR_MUTED)
                    .setTextAlignment(TextAlignment.RIGHT)));
        }

        document.add(table);
    }

    private static void addMemoryPage(Document document, FontPair fonts, Memory memory,
                                      int chapter, int pageNo) {
        String chapterNo = formatChapterNo(chapter);
        String memoryTitle = resolveTitle(memory);
        String dateStr = memory.getEventDate() != null
                ? memory.getEventDate().format(DATE_FULL) : "未知日期";

        document.add(p(chapterNo, fonts.title, 40, COLOR_WATERMARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(-8)
                .setMarginBottom(-24));

        Div header = new Div()
                .setBorderBottom(new SolidBorder(COLOR_LIGHT, 0.8f))
                .setPaddingBottom(12)
                .setMarginBottom(20);

        header.add(new Paragraph()
                .add(new Text("第 " + chapterNo + " 章").setFont(fonts.title).setFontSize(11).setFontColor(COLOR_ACCENT))
                .setMarginBottom(4));
        header.add(p(memoryTitle, fonts.title, 20, COLOR_PRIMARY).setMarginBottom(4));
        header.add(p(dateStr, fonts.body, 10, COLOR_MUTED));
        document.add(header);

        String content = memory.getContent();
        if (content == null || content.isEmpty()) {
            content = "暂无内容";
        }

        Div body = new Div()
                .setBackgroundColor(COLOR_BG)
                .setBorder(new SolidBorder(COLOR_LIGHT, 0.4f))
                .setBorderRadius(new BorderRadius(4))
                .setPadding(20);

        for (String line : safeText(content).split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                body.add(new Paragraph(" ").setMargin(0).setFont(fonts.body).setFontSize(6));
                continue;
            }
            body.add(p(trimmed, fonts.body, 12, COLOR_TEXT)
                    .setMultipliedLeading(1.75f)
                    .setFirstLineIndent(24)
                    .setMarginBottom(4));
        }
        document.add(body);

        if ((memory.getTags() != null && !memory.getTags().isEmpty())
                || memory.getSentimentScore() != null) {
            Div meta = new Div().setMarginTop(18);
            if (memory.getTags() != null && !memory.getTags().isEmpty()) {
                meta.add(new Paragraph()
                        .add(label("标签  ", fonts.body))
                        .add(value(safeText(memory.getTags()), fonts.body))
                        .setMarginBottom(4));
            }
            if (memory.getSentimentScore() != null) {
                String label = getSentimentLabel(memory.getSentimentScore().doubleValue());
                meta.add(new Paragraph()
                        .add(label("情感  ", fonts.body))
                        .add(value(label, fonts.body)));
            }
            document.add(meta);
        }

    }

    public static byte[] generateModernTextMemoryBook(List<Memory> memories, String title, String author)
            throws IOException {
        return generateModernTextMemoryBook(memories, List.of(), title, author);
    }

    public static byte[] generateModernTextMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                                      String title, String author)
            throws IOException {
        return generateModernTextMemoryBook(memories, narratives, List.of(), title, author);
    }

    public static byte[] generateModernTextMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                                      List<Memory> chartMemories, String title, String author)
            throws IOException {
        return generateModernTextMemoryBook(memories, narratives, chartMemories, title, author, author);
    }

    public static byte[] generateModernTextMemoryBook(List<Memory> memories, List<GeneratedNarrative> narratives,
                                                      List<Memory> chartMemories, String title, String author,
                                                      String ownerName)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
                new PageNumberHandler(true));
        Document document = new Document(pdf);

        try {
            FontPair fonts = getFonts();
            document.setMargins(60, 56, 64, 56);

            addModernCover(document, fonts, title, ownerName);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addModernTableOfContents(document, fonts, memories);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            for (int i = 0; i < memories.size(); i++) {
                addModernMemoryPage(document, fonts, memories.get(i), i + 1);
                if (i < memories.size() - 1 || !narratives.isEmpty()) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
            }

            addModernNarrativeSection(document, fonts, narratives, !memories.isEmpty());
            addModernStatisticsSection(document, fonts, chartMemories, false);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private static void addModernStatisticsSection(Document document, FontPair fonts, List<Memory> chartMemories, boolean hasPreviousContent) {
        if (chartMemories == null || chartMemories.isEmpty()) {
            return;
        }
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        document.add(p("统计", fonts.title, 24, new DeviceRgb(15, 23, 42))
                .setMarginBottom(8));
        document.add(p("基于当前导出范围内的记忆数据生成", fonts.body, 10, new DeviceRgb(100, 116, 139))
                .setMarginBottom(24));

        StatisticsSummary summary = buildStatisticsSummary(chartMemories);
        for (String line : buildStatisticsLines(summary)) {
            document.add(p(line, fonts.body, 12, new DeviceRgb(30, 41, 59))
                    .setMultipliedLeading(1.75f)
                    .setMarginBottom(6));
        }
        document.add(createEmotionBarChart(fonts, summary, true).setMarginTop(18));
    }

    private static void addModernCover(Document document, FontPair fonts, String title, String ownerName) {
        document.add(p("MEMENTO AI", fonts.body, 10, new DeviceRgb(100, 116, 139))
                .setCharacterSpacing(4)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(120)
                .setMarginBottom(42));
        document.add(p(safeText(title), fonts.title, 34, new DeviceRgb(15, 23, 42))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(18));
        document.add(p("现代简约版", fonts.body, 13, new DeviceRgb(71, 85, 105))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));
        document.add(p((ownerName == null || ownerName.isBlank() ? "记忆收藏家" : safeText(ownerName)) + "  著", fonts.body, 12, new DeviceRgb(51, 65, 85))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0));
    }

    private static void addModernTableOfContents(Document document, FontPair fonts, List<Memory> memories) {
        document.add(p("目录", fonts.title, 24, new DeviceRgb(15, 23, 42))
                .setMarginBottom(8));
        document.add(p("所有记忆按时间从早到晚排列", fonts.body, 10, new DeviceRgb(100, 116, 139))
                .setMarginBottom(24));

        Table table = new Table(UnitValue.createPercentArray(new float[]{12, 58, 30}))
                .useAllAvailableWidth();
        for (int i = 0; i < memories.size(); i++) {
            Memory memory = memories.get(i);
            String dateStr = memory.getEventDate() != null
                    ? memory.getEventDate().format(DATE_SHORT) : "未知日期";
            table.addCell(tocCell(p(formatChapterNo(i + 1), fonts.body, 10, new DeviceRgb(100, 116, 139))));
            table.addCell(tocCell(p(resolveTitle(memory), fonts.body, 11, new DeviceRgb(30, 41, 59))));
            table.addCell(tocCell(p(dateStr + "    第 " + (FIRST_CONTENT_PAGE + i) + " 页", fonts.body, 10, new DeviceRgb(100, 116, 139))
                    .setTextAlignment(TextAlignment.RIGHT)));
        }
        document.add(table);
    }

    private static void addModernMemoryPage(Document document, FontPair fonts, Memory memory, int chapter) {
        String dateStr = memory.getEventDate() != null
                ? memory.getEventDate().format(DATE_FULL) : "未知日期";

        document.add(p(formatChapterNo(chapter), fonts.body, 11, new DeviceRgb(100, 116, 139))
                .setCharacterSpacing(2)
                .setMarginBottom(10));
        document.add(p(resolveTitle(memory), fonts.title, 22, new DeviceRgb(15, 23, 42))
                .setMarginBottom(6));
        document.add(p(dateStr, fonts.body, 10, new DeviceRgb(100, 116, 139))
                .setMarginBottom(24));

        String content = memory.getContent();
        if (content == null || content.isEmpty()) {
            content = "暂无内容";
        }
        for (String line : safeText(content).split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                document.add(new Paragraph(" ").setMargin(0).setFont(fonts.body).setFontSize(7));
                continue;
            }
            document.add(p(trimmed, fonts.body, 12, new DeviceRgb(30, 41, 59))
                    .setMultipliedLeading(1.85f)
                    .setFirstLineIndent(24)
                    .setMarginBottom(6));
        }
    }

    private static void addNarrativeSection(Document document, FontPair fonts, List<GeneratedNarrative> narratives,
                                            boolean hasPreviousSection) {
        if (narratives == null || narratives.isEmpty()) {
            return;
        }
        document.add(p("AI 叙事", fonts.title, 22, COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(18));
        for (GeneratedNarrative narrative : narratives) {
            document.add(p(resolveNarrativeTitle(narrative), fonts.title, 16, COLOR_PRIMARY)
                    .setMarginTop(8)
                    .setMarginBottom(8));
            for (String line : safeText(narrative.getContent()).split("\\n", -1)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    document.add(p(trimmed, fonts.body, 12, COLOR_TEXT)
                            .setMultipliedLeading(1.7f)
                            .setFirstLineIndent(24)
                            .setMarginBottom(4));
                }
            }
        }
    }

    private static void addModernNarrativeSection(Document document, FontPair fonts, List<GeneratedNarrative> narratives,
                                                  boolean hasPreviousSection) {
        if (narratives == null || narratives.isEmpty()) {
            return;
        }
        document.add(p("AI 叙事", fonts.title, 24, new DeviceRgb(15, 23, 42))
                .setMarginBottom(18));
        for (GeneratedNarrative narrative : narratives) {
            document.add(p(resolveNarrativeTitle(narrative), fonts.title, 17, new DeviceRgb(15, 23, 42))
                    .setMarginTop(8)
                    .setMarginBottom(8));
            for (String line : safeText(narrative.getContent()).split("\\n", -1)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    document.add(p(trimmed, fonts.body, 12, new DeviceRgb(30, 41, 59))
                            .setMultipliedLeading(1.85f)
                            .setFirstLineIndent(24)
                            .setMarginBottom(6));
                }
            }
        }
    }

    private static String resolveNarrativeTitle(GeneratedNarrative narrative) {
        String title = narrative == null ? null : narrative.getTitle();
        return (title == null || title.isEmpty()) ? "未命名 AI 叙事" : safeText(title);
    }

    private static void addBackCover(Document document, FontPair fonts) {
        Div frame = new Div()
                .setBackgroundColor(COLOR_BG)
                .setBorder(new SolidBorder(COLOR_LIGHT, 0.8f))
                .setBorderRadius(new BorderRadius(4))
                .setPadding(36)
                .setMarginTop(188);

        frame.add(p("感谢阅读", fonts.title, 22, COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(14));

        SolidLine line = new SolidLine(0.4f);
        line.setColor(COLOR_ACCENT);
        frame.add(new LineSeparator(line)
                .setWidth(UnitValue.createPercentValue(24))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setMarginBottom(14));

        frame.add(p("每一段记忆，都值得被温柔珍藏", fonts.body, 12, COLOR_MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(24));
        frame.add(p("Memento AI · 永恒归档", fonts.body, 10, COLOR_ACCENT)
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(1));

        document.add(frame);
    }

    private static StatisticsSummary buildStatisticsSummary(List<Memory> memories) {
        int total = memories.size();
        long positive = memories.stream().filter(memory -> memory.getSentimentScore() != null
                && memory.getSentimentScore().doubleValue() > 0.3).count();
        long negative = memories.stream().filter(memory -> memory.getSentimentScore() != null
                && memory.getSentimentScore().doubleValue() < -0.3).count();
        long neutral = memories.stream().filter(memory -> memory.getSentimentScore() == null
                || (memory.getSentimentScore().doubleValue() >= -0.3 && memory.getSentimentScore().doubleValue() <= 0.3)).count();

        String earliest = memories.stream()
                .filter(memory -> memory.getEventDate() != null)
                .map(memory -> memory.getEventDate().format(DATE_SHORT))
                .findFirst()
                .orElse("未知日期");
        String latest = memories.stream()
                .filter(memory -> memory.getEventDate() != null)
                .reduce((first, second) -> second)
                .map(memory -> memory.getEventDate().format(DATE_SHORT))
                .orElse("未知日期");

        Map<String, Long> monthlyCounts = memories.stream()
                .filter(memory -> memory.getEventDate() != null)
                .collect(Collectors.groupingBy(
                        memory -> memory.getEventDate().format(DateTimeFormatter.ofPattern("yyyy.MM")),
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, Long> tagCounts = memories.stream()
                .filter(memory -> memory.getTags() != null && !memory.getTags().isBlank())
                .flatMap(memory -> java.util.Arrays.stream(memory.getTags().split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        return new StatisticsSummary(total, positive, neutral, negative, earliest, latest, monthlyCounts, tagCounts);
    }

    private static List<String> buildStatisticsLines(StatisticsSummary summary) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("记忆总数：" + summary.total + " 条");
        lines.add("时间跨度：" + summary.earliest + " 至 " + summary.latest);
        lines.add("情绪分布：积极 " + summary.positive + " 条，中性 " + summary.neutral + " 条，消极 " + summary.negative + " 条");
        lines.add("每月记忆数量：" + formatCounts(summary.monthlyCounts, "暂无月份数据"));
        lines.add("高频标签：" + formatTopTags(summary.tagCounts));
        return lines;
    }

    private static String formatCounts(Map<String, Long> counts, String emptyText) {
        if (counts.isEmpty()) {
            return emptyText;
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "（" + entry.getValue() + " 条）")
                .collect(Collectors.joining("、"));
    }

    private static String formatTopTags(Map<String, Long> tagCounts) {
        if (tagCounts.isEmpty()) {
            return "暂无标签数据";
        }
        return tagCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(entry -> "#" + entry.getKey() + "（" + entry.getValue() + "）")
                .collect(Collectors.joining("、"));
    }

    private static Table createEmotionBarChart(FontPair fonts, StatisticsSummary summary, boolean modern) {
        long max = Math.max(1, Math.max(summary.positive, Math.max(summary.neutral, summary.negative)));
        DeviceRgb textColor = modern ? new DeviceRgb(30, 41, 59) : COLOR_TEXT;
        Table chart = new Table(UnitValue.createPercentArray(new float[]{18, 64, 18}))
                .useAllAvailableWidth();
        chart.addCell(barLabelCell("积极", fonts, textColor));
        chart.addCell(barCell(summary.positive, max, new DeviceRgb(34, 197, 94)));
        chart.addCell(barValueCell(summary.positive + " 条", fonts, textColor));
        chart.addCell(barLabelCell("中性", fonts, textColor));
        chart.addCell(barCell(summary.neutral, max, new DeviceRgb(148, 163, 184)));
        chart.addCell(barValueCell(summary.neutral + " 条", fonts, textColor));
        chart.addCell(barLabelCell("消极", fonts, textColor));
        chart.addCell(barCell(summary.negative, max, new DeviceRgb(59, 130, 246)));
        chart.addCell(barValueCell(summary.negative + " 条", fonts, textColor));
        return chart;
    }

    private static com.itextpdf.layout.element.Cell barLabelCell(String text, FontPair fonts, DeviceRgb color) {
        return new com.itextpdf.layout.element.Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(7)
                .setPaddingBottom(7)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(p(text, fonts.body, 10, color));
    }

    private static com.itextpdf.layout.element.Cell barValueCell(String text, FontPair fonts, DeviceRgb color) {
        return new com.itextpdf.layout.element.Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(7)
                .setPaddingBottom(7)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(p(text, fonts.body, 10, color).setTextAlignment(TextAlignment.RIGHT));
    }

    private static com.itextpdf.layout.element.Cell barCell(long value, long max, DeviceRgb color) {
        float width = value == 0 ? 3 : Math.max(8, (value * 100f) / max);
        Div track = new Div()
                .setHeight(10)
                .setBackgroundColor(new DeviceRgb(226, 232, 240))
                .setBorderRadius(new BorderRadius(5));
        Div bar = new Div()
                .setHeight(10)
                .setWidth(UnitValue.createPercentValue(width))
                .setBackgroundColor(color)
                .setBorderRadius(new BorderRadius(5));
        track.add(bar);
        return new com.itextpdf.layout.element.Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(9)
                .setPaddingBottom(9)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(track);
    }

    private static com.itextpdf.layout.element.Cell tocCell(Paragraph paragraph) {
        return new com.itextpdf.layout.element.Cell()
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingTop(6)
                .setPaddingBottom(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(paragraph);
    }

    private static Paragraph p(String text, PdfFont font, float size, DeviceRgb color) {
        return new Paragraph(safeText(text))
                .setFont(font)
                .setFontSize(size)
                .setFontColor(color);
    }

    private static Text label(String text, PdfFont font) {
        return new Text(text).setFont(font).setFontSize(10).setFontColor(COLOR_MUTED);
    }

    private static Text value(String text, PdfFont font) {
        return new Text(text).setFont(font).setFontSize(10).setFontColor(COLOR_ACCENT);
    }

    private static String formatChapterNo(int index) {
        return String.format("%02d", index);
    }

    private static String resolveTitle(Memory memory) {
        String title = memory.getTitle();
        return (title == null || title.isEmpty()) ? "未命名记忆" : safeText(title);
    }

    private static String getSentimentLabel(double score) {
        if (score > 0.3) return "积极";
        if (score < -0.3) return "消极";
        return "中性";
    }

    /** 过滤可能导致 PDF 损坏的控制字符，保留换行与常见中文标点 */
    private static String safeText(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static FontPair getFonts() throws IOException {
        return loadFonts();
    }

    private static FontPair loadFonts() throws IOException {
        PdfFont body = tryLoadFont(
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "/Library/Fonts/PingFang.ttc,0",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0"
        );
        if (body == null) {
            body = loadAsianFont();
        }

        PdfFont title = tryLoadFont(
                "C:/Windows/Fonts/msyhbd.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc,0"
        );
        if (title == null) {
            title = body;
        }

        return new FontPair(body, title);
    }

    /** 尝试加载系统 TTF/TTC 字体（TTC 必须带 ,0 索引，否则易生成损坏 PDF） */
    private static PdfFont tryLoadFont(String... candidates) {
        for (String candidate : candidates) {
            try {
                String filePart = candidate.contains(",")
                        ? candidate.substring(0, candidate.indexOf(',')) : candidate;
                Path path = Paths.get(filePart);
                if (!Files.exists(path)) {
                    continue;
                }
                return PdfFontFactory.createFont(
                        candidate,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception ignored) {
                // 尝试下一个
            }
        }
        return null;
    }

    /** font-asian 内置宋体，跨平台兜底 */
    private static PdfFont loadAsianFont() throws IOException {
        try {
            return PdfFontFactory.createFont(
                    "STSongStd-Light",
                    "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e) {
            throw new IOException("无法加载中文字体，请确认已引入 font-asian 依赖或系统安装了中文字体", e);
        }
    }

    private static final class StatisticsSummary {
        final int total;
        final long positive;
        final long neutral;
        final long negative;
        final String earliest;
        final String latest;
        final Map<String, Long> monthlyCounts;
        final Map<String, Long> tagCounts;

        StatisticsSummary(int total, long positive, long neutral, long negative, String earliest, String latest,
                          Map<String, Long> monthlyCounts, Map<String, Long> tagCounts) {
            this.total = total;
            this.positive = positive;
            this.neutral = neutral;
            this.negative = negative;
            this.earliest = earliest;
            this.latest = latest;
            this.monthlyCounts = monthlyCounts;
            this.tagCounts = tagCounts;
        }
    }

    private static final class PageNumberHandler implements com.itextpdf.kernel.events.IEventHandler {
        private final boolean modern;

        PageNumberHandler(boolean modern) {
            this.modern = modern;
        }

        @Override
        public void handleEvent(com.itextpdf.kernel.events.Event event) {
            try {
                com.itextpdf.kernel.events.PdfDocumentEvent docEvent =
                        (com.itextpdf.kernel.events.PdfDocumentEvent) event;
                PdfDocument pdf = docEvent.getDocument();
                PdfPage page = docEvent.getPage();
                int pageNo = pdf.getPageNumber(page);
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
                Canvas canvas = new Canvas(pdfCanvas, pageSize);
                PdfFont pageNumberFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                DeviceRgb color = modern ? new DeviceRgb(100, 116, 139) : COLOR_MUTED;
                canvas.showTextAligned(
                        new Paragraph("- " + pageNo + " -")
                                .setFont(pageNumberFont)
                                .setFontSize(9)
                                .setFontColor(color),
                        pageSize.getWidth() / 2,
                        pageSize.getBottom() + 28,
                        TextAlignment.CENTER);
                canvas.close();
            } catch (Exception ignored) {
                // 页码渲染失败不应中断 PDF 主体生成
            }
        }
    }

    private static final class FontPair {
        final PdfFont body;
        final PdfFont title;

        FontPair(PdfFont body, PdfFont title) {
            this.body = body;
            this.title = title;
        }
    }
}
