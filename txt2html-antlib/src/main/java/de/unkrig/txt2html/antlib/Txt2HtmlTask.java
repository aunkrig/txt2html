
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

package de.unkrig.txt2html.antlib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformations;
import de.unkrig.commons.file.filetransformation.FileTransformations.ArchiveCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformer.Mode;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.txt2html.Txt2SvgFilterWriter;

public class Txt2HtmlTask extends ProjectComponent {

    private Charset              charset   = Charset.forName("UTF-8");
    @Nullable private File       tofile    = null;
    private final List<Resource> resources = new ArrayList<>();
    
    // ========================= CONFIGURATION SETTERS =========================
    
    public void
    setCharset(Charset charset) { this.charset = charset; }
    
    public void
    setFile(File file) { this.resources.add(new FileResource(file)); }
    
    public void
    setTofile(File tofile) { this.tofile = tofile; }

    public void
    addConfiguredFileSet(FileSet fileSet) {
        for (@SuppressWarnings("unchecked") Iterator<Resource> it = fileSet.iterator(); it.hasNext();) {
            resources.add(it.next());
        }
    }

    // ========================= END CONFIGURATION SETTERS =========================
    
    public void
    execute() throws BuildException {
        try {
            this.execute2();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    
    public void
    execute2() throws IOException {

        Mode                          fileTransformerMode = Mode.CHECK_AND_TRANSFORM;
        boolean                       keepOriginals       = true;
        ExceptionHandler<IOException> exceptionHandler    = ExceptionHandler.defaultHandler();


        // Create a ContentsTransformer that does the ASCII-art-to-SVG transformation.
        ContentsTransformer ct = new ContentsTransformer() {
            
            @Override public void
            transform(String path, InputStream is, OutputStream os) throws IOException {
                IoUtil.copy(
                    new InputStreamReader(is, Txt2HtmlTask.this.charset),                           // reader
                    Txt2SvgFilterWriter.make(new OutputStreamWriter(os, Txt2HtmlTask.this.charset)) // writer
                );
            }
        };

        final File tofile = this.tofile;
        if (tofile != null && resources.size() != 1) {
            throw new BuildException(
                "If tofile=... is given, then exactly one input resource must be configured"
            );
        }
        
        for (Resource resource : this.resources) {
            
            if (resource instanceof FileResource) {
                File file = ((FileResource) resource).getFile();
                
                FileTransformer ft = FileTransformations.recursiveCompressedAndArchiveFileTransformer(
                    PredicateUtil.always(), // lookIntoFormat
                    PredicateUtil.never(),  // archiveEntryRemoval
                    Glob.ANY,               // archiveEntryRenaming
                    ArchiveCombiner.NOP,    // archiveCombiner
                    ct,                     // delegate
                    keepOriginals,          // keepOriginals
                    exceptionHandler        // exceptionHandler
                );

                ft.transform(
                    file.getPath(),                 // path
                    file,                           // in
                    tofile != null ? tofile : file, // out
                    fileTransformerMode             // mode
                );
            } else {
    
                // Non-file resource
                if (tofile == null) {
                    throw new BuildException(
                        "If non-file resources are given given, then tofile=... must be configured"
                    );
                }
    
                try (InputStream is = resource.getInputStream()) {
                    try (OutputStream os = new FileResource(this.tofile).getOutputStream()) {
                        ct.transform(resource.getName(), is, os);
                    }
                }
            }
        }
    }
}
