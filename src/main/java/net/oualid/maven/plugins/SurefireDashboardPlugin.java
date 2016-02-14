package net.oualid.maven.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.IOUtils;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Locale;

@Mojo( name = "surefiredashboard", defaultPhase = LifecyclePhase.TEST, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST )
public class SurefireDashboardPlugin
        extends AbstractMavenReport
{
    /**
     * Report output directory.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}" )
    private File outputDirectory;

    /**
     * Maximum test boxes per line.
     *
     * @parameter default-value="5"
     * @required
     */
    @Parameter(property = "maxTestsPerLine", defaultValue = "5", required = true)
    private int maxTestsPerLine;

    @Override
    protected Renderer getSiteRenderer() {
        return null;
    }

    @Override
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath();
    }

    @Override
    protected MavenProject getProject() {
        return null;
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {

        getLog().info("Max tests per line: " + maxTestsPerLine);

        File f = outputDirectory;

        File resourceFile = new File(f, "site");
        resourceFile = new File(resourceFile, "surefiredashboard");

        flushResource("/surefiredashboard.css", new File(resourceFile, "surefiredashboard.css"));
        flushResource("/surefiredashboard.js", new File(resourceFile, "surefiredashboard.js"));


        getLog().info("Looking for surefire reports in " + f + "...");

        File surefireReportDirectory = new File(f, "surefire-reports");

        Sink sink = getSink();

        sink.head();
        sink.title();
        sink.text("Surefire dashboard");
        sink.title_();

        SinkEventAttributeSet atts = new SinkEventAttributeSet();
        atts.addAttribute( SinkEventAttributes.TYPE, "text/javascript" );
        atts.addAttribute( SinkEventAttributes.SRC, "surefiredashboard/surefiredashboard.js" );
        sink.unknown( "script", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
        sink.unknown( "script", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

        atts = new SinkEventAttributeSet();
        atts.addAttribute( SinkEventAttributes.HREF, "surefiredashboard/surefiredashboard.css" );
        atts.addAttribute( SinkEventAttributes.REL, "stylesheet" );
        atts.addAttribute( SinkEventAttributes.TYPE, "text/css" );
        atts.addAttribute( "media", "all" );

        sink.unknown( "link", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
        sink.unknown( "link", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

        sink.head_();

        sink.body();
        sink.section1();
        sink.sectionTitle1();

        sink.text("Surefire dashboard");

        sink.sectionTitle1_();
        sink.section1_();

        File[] files = surefireReportDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".xml");
            }
        });
        if (files == null) {
            getLog().warn("no surefire tests found !");
        } else {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                getLog().info("Found surefire report: " + file.getName());

                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = null;
                try {
                    documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(files[i]);
                    NodeList nodes = document.getElementsByTagName("testcase");

                    atts = new SinkEventAttributeSet();
                    atts.addAttribute( SinkEventAttributes.CLASS, "testClassContainer" );
                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );

                    atts = new SinkEventAttributeSet();
                    atts.addAttribute( SinkEventAttributes.CLASS, "testClassTitle" );
                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
                    Element suite = (Element) document.getElementsByTagName("testsuite").item(0);
                    sink.text(suite.getAttribute("name"));
                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

                    atts = new SinkEventAttributeSet();
                    atts.addAttribute( SinkEventAttributes.CLASS, "testClass" );
                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );

                    for (int j = 0; j < nodes.getLength(); j++) {

                        if (j>0 && j%maxTestsPerLine == 0) {
                            sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

                            atts = new SinkEventAttributeSet();
                            atts.addAttribute( SinkEventAttributes.CLASS, "testClass" );
                            sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
                        }

                        Element node = (Element) nodes.item(j);
                        String name = node.getAttribute("name");
                        String time = node.getAttribute("time");

                        atts = new SinkEventAttributeSet();
                        boolean error = node.getElementsByTagName("error").getLength() > 0;
                        atts.addAttribute( SinkEventAttributes.CLASS, "testCase " + (error ? "error" : "success") );

                        sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
                        atts = new SinkEventAttributeSet();
                        sink.unknown( "span", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
                        sink.text(name);
                        sink.unknown( "span", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

                        atts = new SinkEventAttributeSet();
                        atts.addAttribute( SinkEventAttributes.CLASS, "duration" );
                        sink.unknown( "span", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_START )}, atts );
                        sink.text(time);
                        sink.unknown( "span", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );
                        sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );

                    }

                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );
                    sink.unknown( "div", new Object[]{new Integer( HtmlMarkup.TAG_TYPE_END )}, null );
                } catch (ParserConfigurationException e) {
                    getLog().error("Cannot generate section for surefire report: " + file.getName());
                } catch (SAXException e) {
                    getLog().error("Cannot generate section for surefire report: " + file.getName());
                } catch (IOException e) {
                    getLog().error("Cannot generate section for surefire report: " + file.getName());
                }

            }
        }

        sink.body_();
        sink.flush();
        sink.close();

        sink.body();

        if ( !f.exists() )
        {
            f.mkdirs();
        }
    }

    private void flushResource(String source, File dest) {
        dest.getParentFile().mkdirs();
        BufferedOutputStream os = null;
        BufferedInputStream is = null;
        try {
            is = new BufferedInputStream(SurefireDashboardPlugin.class.getResourceAsStream(source));
            os = new BufferedOutputStream(new FileOutputStream(dest));
            IOUtils.copy(is, os);
        } catch (IOException e) {
            getLog().error("Cannot flush resource on file system", e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    getLog().error("Cannot flush resource on file system", e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    getLog().error("Cannot flush resource on file system", e);
                }
            }
        }
    }

    public String getOutputName() {
        return "surefire-dashboard";
    }

    public String getName(Locale locale) {
        return "Surefire dashboard";
    }

    public String getDescription(Locale locale) {
        return "A surefire dashboard";
    }
}
