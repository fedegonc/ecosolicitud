package com.ecosolicitud.shared;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import org.springframework.stereotype.Component;

// Render del contenido editorial (guía, comunidad) escrito en markdown.
// escapeHtml + sanitizeUrls: solo sale lo que genera el parser; markup crudo
// y esquemas raros en links no pasan.
@Component
public class Markdown {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String html(String markdown) {
        return renderer.render(parser.parse(markdown));
    }
}
