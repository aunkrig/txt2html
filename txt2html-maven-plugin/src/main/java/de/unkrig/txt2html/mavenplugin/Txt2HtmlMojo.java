
/*
 * txt2html - Converts text to an HTML document
 *
 * Copyright (c) 2020 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.txt2html.mavenplugin;

import java.io.File;
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformations;
import de.unkrig.commons.file.filetransformation.FileTransformations.DirectoryCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformer.Mode;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.PatternUtil;
import de.unkrig.txt2html.CharMatrix;
import de.unkrig.txt2html.CharMatrix2Svg;

@Mojo(name = "txt2html", defaultPhase = LifecyclePhase.PACKAGE)
public
class Txt2HtmlMojo extends AbstractMojo {
    
    @Parameter(defaultValue = "${project.build.directory}/apidocs", property = "dir", required = true)
    private File directory;
    
    @Parameter(property = "outputDir", required = false)
    private File outputDirectory;

    @Parameter(property = "keepOriginals", required = false)
    private boolean keepOriginals;
    
    @Parameter(defaultValue = "true", property = "saveSpace", required = false)
    private boolean saveSpace;
    
    @Parameter(defaultValue = "UTF-8", property = "encoding", required = false)
    private String encoding;
    
    @Parameter(defaultValue = "<pre class=\"asciiart\"><code>\\.?([^<]*)</code></pre>", property = "asciiArtPattern")
    private String asciiArtRegex;

    public void
    execute() throws MojoExecutionException {
        
        try {
            this.execute2();
        } catch (Exception e) {
            throw new MojoExecutionException(null, e);
        }
    }

    private void
    execute2() throws IOException {

        // Set up a FileContentsTransformer.
        Charset charset = Charset.forName(this.encoding);
        FileTransformer ft = new FileContentsTransformer(new ContentsTransformer() {
            
            @Override public void
            transform(String path, InputStream is, OutputStream os) throws IOException {
                IoUtil.copy(
                    new InputStreamReader(is, charset),       // reader
                    make(new OutputStreamWriter(os, charset)) // writer
                );
            }
        }, this.keepOriginals);
        
        // Wrap it such that it processes directory trees recursively.
        ft = FileTransformations.directoryTreeTransformer(
            null,                                          // directoryMemberNameComparator
            PredicateUtil.never(),                         // directoryMemberRemoval
            Glob.ANY,                                      // directoryMemberRenaming
            DirectoryCombiner.NOP,                         // directoryCombiner
            ft,                                            // regularFileTransformer
            this.saveSpace,                                // saveSpace
            this.keepOriginals,                            // keepOriginals
            ExceptionHandler.<IOException>defaultHandler() // exceptionHandler
        );
        
        ft.transform(
            this.directory.getPath(),                            // path
            this.directory,                                      // in
            ObjectUtil.or(this.outputDirectory, this.directory), // out
            Mode.CHECK_AND_TRANSFORM                             // mode
        );
    }

    public Writer
    make(Writer delegate) {
        
        return PatternUtil.replaceAllFilterWriter(
            Pattern.compile(this.asciiArtRegex),     // pattern
            mr -> {                                  // matchReplacer
                String text = mr.group(1);
                text = text.replace("&lt;",   "<");
                text = text.replace("&gt;",   ">");
                text = text.replace("&quot;", "\"");
                text = text.replace("&amp;",  "&");
                
                StringWriter sw = new StringWriter();
                new CharMatrix2Svg(sw).convert(CharMatrix.read(new StringReader(text)));
                return sw.toString();
            },
            delegate                                 // delegate
        );
    }
}
