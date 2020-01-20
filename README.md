# txt2html

A tool for converting "ASCII art" into SVG graphics.

Scans HTML documents (e.g. generated JAVADOC) for special `<pre>` sections, and replaces them with nice SVG graphics.

Currently it recognizes "`|-+`"-based boxes and "`<>^v-|`"-based arrows.

It comes as a Java library

    public static Writer de.unkrig.txt2html.Txt2SvgFilterWriter.make(Writer)

, a command line tool

    java de.unkrig.txt2html.Main <input-dir-file-or-archive> [ [ ... ] <output-dir-file-or-archive> ]

, a MAVEN plugin

    <plugin>
      <groupId>de.unkrig.txt2html</groupId>
      <artifactId>txt2html-maven-plugin</artifactId>
      <version>0.0.1-SNAPSHOT</version>

      <executions>
        <execution>
          <phase>package</phase>
          <goals><goal>txt2html</goal></goals>
        </execution>
      </executions>
      <configuration>
        <keepOriginals>true</keepOriginals>
      </configuration>
    </plugin>

and an ANT library:

    <taskdef
      resource="de/unkrig/txt2html/antlib/ant.xml"
      classpath="path/to/txt2html-antlib-0.0.1-SNAPSHOT-antlib.jar"
    />

    <txt2html>
      <fileset dir="apidocs">
        <include name="**/*.html"/>
      </fileset>
    </txt2html>

As an example, look at these "before and after" pages:

* http://janino-compiler.github.io/janino/apidocs/index-orig.html
* http://janino-compiler.github.io/janino/apidocs/index.html

Use, enjoy, contribute!
