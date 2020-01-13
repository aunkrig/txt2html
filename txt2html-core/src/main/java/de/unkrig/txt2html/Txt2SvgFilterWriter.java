/*
 * Copyright 2020 SWM Services GmbH
 */

package de.unkrig.txt2html;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformations;
import de.unkrig.commons.file.filetransformation.FileTransformer.Mode;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.text.pattern.PatternUtil;

public
class Txt2SvgFilterWriter {
    
    private Txt2SvgFilterWriter() {}

    public static Writer
    make(Writer delegate) {
        
        return PatternUtil.replaceAllFilterWriter(
            Pattern.compile("<pre class=\"asciiart\"><code>\\.?([^<]*)</code></pre>"),          // pattern
            mr -> {                                                                             // matchReplacer
                String text = mr.group(1);
                text = text.replace("&lt;",   "<");
                text = text.replace("&gt;",   ">");
                text = text.replace("&quot;", "\"");
                text = text.replace("&amp;",  "&");
                
                StringWriter sw = new StringWriter();
                new CharMatrix2Svg(sw).convert(CharMatrix.read(new StringReader(text)));
                return sw.toString();
            },
            delegate                                                                            // delegate
        );
    }
    
    public static void
    main(String[] args) throws IOException {
        boolean keepOriginals = true;
        Charset charset = Charset.forName("UTF-8");
        FileTransformations.transform(args, new FileContentsTransformer(new ContentsTransformer() {
            
            @Override public void
            transform(String path, InputStream is, OutputStream os) throws IOException {
                IoUtil.copy(
                    new InputStreamReader(is, charset),       // reader
                    make(new OutputStreamWriter(os, charset)) // writer
                );
            }
        }, keepOriginals), Mode.TRANSFORM, ExceptionHandler.defaultHandler());
    }
}
