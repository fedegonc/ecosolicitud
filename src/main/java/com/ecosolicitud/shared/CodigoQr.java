package com.ecosolicitud.shared;

import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

// QR como SVG en línea: currentColor hereda el color de texto del tema.
public final class CodigoQr {

    private CodigoQr() {
    }

    public static String svg(String contenido) {
        try {
            BitMatrix m = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 2));
            var sb = new StringBuilder(4096)
                    .append("<svg xmlns=\"http://www.w3.org/2000/svg\" shape-rendering=\"crispEdges\"")
                    .append(" viewBox=\"0 0 ").append(m.getWidth()).append(' ').append(m.getHeight())
                    .append("\"><path fill=\"currentColor\" d=\"");
            for (int y = 0; y < m.getHeight(); y++) {
                for (int x = 0; x < m.getWidth(); x++) {
                    if (m.get(x, y)) {
                        sb.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                    }
                }
            }
            return sb.append("\"/></svg>").toString();
        } catch (WriterException e) {
            throw new IllegalStateException(e);
        }
    }
}
